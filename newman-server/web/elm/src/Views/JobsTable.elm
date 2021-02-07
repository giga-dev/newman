module Views.JobsTable exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Dropdown as Dropdown
import Bootstrap.Form.Input as FormInput
import Bootstrap.Form.Select as Select
import Bootstrap.Grid as Grid
import Bootstrap.Grid.Col as Col
import Bootstrap.Modal as Modal exposing (..)
import Bootstrap.Progress as Progress exposing (..)
import Date exposing (Date)
import Date.Extra.Duration as Duration
import DateFormat
import DateFormat.Relative
import Html exposing (..)
import Html.Attributes as HtmlAttr exposing (..)
import Html.Events exposing (..)
import Http
import Json.Encode exposing (Value)
import List.Extra as ListExtra
import Multiselect
import Paginate exposing (PaginatedList)
import Time exposing (Time)
import Utils.Common as Common
import Utils.Types as Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal exposing (..)



type Msg
    = First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String
    | OnClickToggleJob String
    | RequestCompletedToggleJob (Result Http.Error Job)
    | RequestCompletedToggleJobs (Result Http.Error (List Job))
    | NewmanModalMsg Modal.State
    | OnClickJobDrop String
    | OnJobDropConfirmed String
    | RequestCompletedDropJob String (Result Http.Error String)
    | WebSocketEvent WebSocket.Event
    | PauseAll
    | ResumeAll
    | ActionStateMsg Dropdown.State
    | AnimateModal Modal.State
    | CloseModal
    | NewJobPriorityMsg String
    | ConfirmEditJob Job
    | RequestCompletedEditJob (Result Http.Error Job)
    | MultiSelectAgentGroupsMsg Multiselect.Msg
    | GetAllAgentGroupsCompleted (Result Http.Error (List String))
    | GetAllAgentGroupsCmd Job


type alias Model =
    { allJobs : List Job
    , jobs : PaginatedList Job
    , pageSize : Int
    , confirmationState : Modal.State
    , jobToDrop : Maybe String
    , jobToEdit : Maybe Job
    , newPriority : Int
    , newAgentGroups : Multiselect.Model
    , query : String
    , actionState : Dropdown.State
    , modalState : Modal.State
    , newPriorityMessage : String
    , priorities : List Int
    , currentAgentGroups : List (String , String)
    }


init : List Job -> Model
init jobs =
    let
        pageSize =
            15
    in
    Model jobs (Paginate.fromList pageSize jobs) pageSize Modal.hiddenState Nothing Nothing 0 (Multiselect.initModel [] "") "" Dropdown.initialState Modal.hiddenState "" [0, 1, 2, 3, 4] []


viewTable : Model -> Maybe Time -> Html Msg
viewTable model currTime =
    let
        prevButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.jobs ) ], onClick First ]
                [ button [ class "page-link" ] [ text "«" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.jobs ) ], onClick Prev ]
                [ button [ class "page-link" ] [ text "‹" ]
                ]
            ]

        nextButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.jobs ) ], onClick Next ]
                [ button [ class "page-link" ] [ text "›" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.jobs ) ], onClick Last ]
                [ button [ class "page-link" ] [ text "»" ]
                ]
            ]

        pagerButtonView index isActive =
            case isActive of
                True ->
                    li [ class "page-item active" ]
                        [ button [ class "page-link" ]
                            [ text <| toString index
                            , span [ class "sr-only" ] [ text "(current)" ]
                            ]
                        ]

                False ->
                    li [ class "page-item", onClick <| GoTo index ]
                        [ button [ class "page-link" ] [ text <| toString index ]
                        ]

        pagination =
            nav []
                [ ul [ class "pagination " ]
                    (prevButtons
                        ++ Paginate.pager pagerButtonView model.jobs
                        ++ nextButtons
                    )
                ]

        widthPct pct =
            style [ ( "width", pct ) ]

        actionButton =
            div []
                [ Dropdown.dropdown
                    model.actionState
                    { options = []
                    , toggleMsg = ActionStateMsg
                    , toggleButton = Dropdown.toggle [ Button.primary ] [ text "Actions" ]
                    , items =
                        [ Dropdown.buttonItem [ onClick PauseAll ] [ text "Pause All" ]
                        , Dropdown.buttonItem [ onClick ResumeAll ] [ text "Resume All" ]
                        ]
                    }
                ]
    in
    div []
        [ div [ class "form-inline" ]
            [ div [ class "form-group" ]
                [ FormInput.text
                    [ FormInput.onInput FilterQuery
                    , FormInput.placeholder "Filter"
                    , FormInput.value model.query
                    ]
                ]
            , div [ class "form-group" ] [ pagination ]
            , actionButton
            ]
        , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
            [ thead []
                [ tr []
                    [ th [ class "job-tests-state" ] [ text "State" ]
                    , th [ class "job-tests-progress" ] [ text "Progess" ]
                    , th [ widthPct "8%" ] [ text "Job Id" ]
                    , th [ widthPct "8%" ] [ text "Suite" ]
                    , th [ widthPct "6%" ] [ text "Job Conf." ]
                    , th [ widthPct "6%" ] [ text "Duration" ]
                    , th [ widthPct "8%" ] [ text "Submitted At" ]
                    , th [ widthPct "9%" ] [ text "Build" ]
                    , th [ widthPct "7%" ] [ text "Submitted By" ]
                    , th [ widthPct "6%" ] [ text "# p. agents" ]
                    , th [ widthPct "8%" ] [ text "Agent Groups" ]
                    , th [ widthPct "4%" ] [ text "Priority" ]
                    , th [ widthPct "17%" ]
                        [ Badge.badgeInfo [ class "job-tests-badge", title "Running Tests" ] [ text "Run" ]
                        , text "/ "
                        , Badge.badgeSuccess [ class "job-tests-badge", title "Passed Tests" ] [ text "Pass" ]
                        , text "/ "
                        , Badge.badgeDanger [ class "job-tests-badge", title "Failed Tests" ] [ text "Fail" ]
                        , text "/ "
                        , Badge.badgeWarning [ class "job-tests-badge", style [ ( "background-color", "DarkRed" ) ], title "Failed 3 Times" ] [ text "3xFail" ]
                        , text "/ "
                        , Badge.badge [ class "job-tests-badge", title "All Tests" ] [ text "Total" ]
                        ]
                    , th [ width 100 ]
                        [ text "Actions" ]
                    ]
                ]
            , tbody [] (List.map (viewJob currTime) <| Paginate.page model.jobs)
            ]
        , pagination
        , NewmanModal.confirmJobDrop model.jobToDrop NewmanModalMsg OnJobDropConfirmed model.confirmationState
        , viewModal model
        ]


viewJob : Maybe Time -> Job -> Html Msg
viewJob currTime job =
    let
        progressPercent =
            ((job.failedTests + job.passedTests) * 100) // job.totalTests

        progress =
            Progress.progress
                [ Progress.customLabel [ text <| toString progressPercent ++ " %" ]
                , Progress.value <| toFloat <| progressPercent
                , Progress.info
                ]

        jobState =
            let
                badge =
                    case job.state of
                        BROKEN ->
                            Badge.badgeDanger

                        DONE ->
                            Badge.badgeSuccess

                        RUNNING ->
                            Badge.badgeInfo

                        PAUSED ->
                            Badge.badgeWarning

                        READY ->
                            Badge.badge
            in
            badge [ class "newman-job-state-label" ] [ text <| jobStateToString job.state ]

        submittedTimeHourFull =
            DateFormat.format Common.dateTimeDateFormat (Date.fromTime (toFloat job.submitTime))

        submittedTimeHour =
            DateFormat.Relative.relativeTime (Date.fromTime <| Maybe.withDefault (toFloat job.submitTime) currTime) (Date.fromTime (toFloat job.submitTime))

        durationText =
            let
                diffTime =
                    case ( job.startTime, job.endTime, currTime ) of
                        ( Just startTime, Just endTime, _ ) ->
                            Just <| Duration.diff (Date.fromTime (toFloat endTime)) (Date.fromTime (toFloat startTime))

                        ( Just startTime, Nothing, Just time ) ->
                            Just <| Duration.diff (Date.fromTime time) (Date.fromTime (toFloat startTime))

                        ( _, _, _ ) ->
                            Nothing
            in
            case diffTime of
                Just diff ->
                    toString diff.hour ++ "h, " ++ toString diff.minute ++ "m"

                Nothing ->
                    ""

        playPauseButton =
            case job.state of
                PAUSED ->
                    Button.button [ Button.success, Button.small, Button.onClick <| OnClickToggleJob job.id ]
                        [ span [ class "ion-play" ] [] ]

                state ->
                    Button.button [ Button.warning, Button.small, Button.disabled <| (state /= RUNNING && state /= READY), Button.onClick <| OnClickToggleJob job.id ]
                        [ span [ class "ion-pause" ] [] ]

        editJobButton =
            Button.button [ Button.roleLink, Button.attrs [ style [ ( "padding", "0px 5px 0px 5px" ) ], class "ion-android-options" ], Button.disabled <| job.state == DONE, Button.onClick <| GetAllAgentGroupsCmd job ] []
    in
    tr [ classList [ ( "succeed-row", job.passedTests == job.totalTests ) ] ]
        [ td [] [ jobState ]
        , td [] [ progress ]
        , td [] [ a [ href <| "#job/" ++ job.id ++ "/ALL", title job.id ] [ text job.id ] ]
        , td [ title job.suiteName ] [ text job.suiteName ]
        , td [ title job.jobConfigName ] [ text job.jobConfigName ]
        , td [] [ text durationText ]
        , td [ title submittedTimeHourFull ] [ text submittedTimeHour ]
        , td [] [ a [ href <| "#build/" ++ job.buildId, title <| job.buildName ++ " (" ++ job.buildBranch ++ ")" ] [ text <| job.buildName ++ " (" ++ job.buildBranch ++ ")" ] ]
        , td [] [ text job.submittedBy ]
        , td [] [ text (toString (List.length job.preparingAgents)) ]
        , td [ title <| agentGroupsJobFormat job.agentGroups ] [ text <| agentGroupsJobFormat job.agentGroups ]
        , td [] [ text (toString <| (job.priority |> Maybe.withDefault 0)) ]
        , td []
            [ Badge.badgeInfo [ class "job-tests-badge" ]
                [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/RUNNING", title "Running Tests" ]
                    [ text <| toString job.runningTests ]
                ]
            , text "/ "
            , Badge.badgeSuccess [ class "job-tests-badge" ]
                [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/SUCCESS", title "Passed Tests" ]
                    [ text <| toString job.passedTests ]
                ]
            , text "/ "
            , Badge.badgeDanger [ class "job-tests-badge" ]
                [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/FAIL", title "Failed Tests" ]
                    [ text <| toString job.failedTests ]
                ]
            , text "/ "
            , Badge.badgeWarning [ class "job-tests-badge", style [ ( "background-color", "DarkRed" ) ] ]
                [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/FAILED3TIMES", title "Failed 3 Times" ]
                    [ text <| toString job.failed3TimesTests ]
                ]
            , text "/ "
            , Badge.badge [ class "job-tests-badge" ]
                [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/ALL", title "All Tests" ]
                    [ text <| toString job.totalTests ]
                ]
            ]
        , td []
            [ Button.button
                [ Button.danger
                , Button.small
                , Button.onClick <| OnClickJobDrop job.id
                , Button.disabled <|
                    not (List.member job.state [ DONE, PAUSED, BROKEN ] && (job.runningTests <= 0) && List.length job.agents <= 0)
                ]
                [ span [ class "ion-close" ] [] ]
            , text "  "
            , playPauseButton
            , text "  "
            , editJobButton
            ]
        ]


viewModal : Model -> Html Msg
viewModal model =
    case model.jobToEdit of
        Nothing ->
            Modal.config AnimateModal
                |> Modal.large
                |> Modal.h3 [] [ text "Error: No selected job" ]
                |> Modal.view model.modalState

        Just job ->
            let
                twoColsRow left right =
                    Grid.row []
                        [ Grid.col
                            [ Col.sm3 ]
                            [ text left ]
                        , Grid.col
                            [ Col.sm7 ]
                            [ text right ]
                        ]
                currentPriority =
                    Maybe.withDefault 0 job.priority

                toPriorityOption data =
                    Select.item [ HtmlAttr.value <| toString data, selected <| currentPriority == data ] [ text <| Types.legendPriority data ]
            in
            Modal.config AnimateModal
                |> Modal.large
                |> Modal.h3 [] [ text <| "Edit job: " ++ job.id ]
                |> Modal.body []
                    [ Grid.containerFluid [ style [] ]
                        [ twoColsRow "Suite" job.suiteName
                        , twoColsRow "Build" (job.buildName++" ("++job.buildBranch++")")
                        , Grid.row []
                            [ Grid.col
                                [ Col.sm3 ]
                                [ text "New Priority" ]
                            , Grid.col
                                [ Col.sm7 ]
                                    [ Select.select
                                         [ Select.onChange NewJobPriorityMsg, Select.attrs [ style [ ( "width", "200px"),(  "margin-bottom", "5px")] ] ]
                                          (List.map toPriorityOption model.priorities)
                                    ]
                            ]
                        , Grid.row []
                            [ Grid.col
                                [ Col.sm3 ]
                                [ text "New Agent Groups" ]
                            , Grid.col
                                [ Col.sm7 ]
                                    [ Multiselect.view model.newAgentGroups |> Html.map MultiSelectAgentGroupsMsg]
                            ]
                        ]
                    ]
                |> Modal.footer []
                    [ div [ style [ ( "width", "100%" ) ] ] [ text model.newPriorityMessage ]
                    , Button.button
                        [ Button.success
                        , Button.onClick <| ConfirmEditJob job
                        ]
                        [ text "Confirm" ]
                    , Button.button
                        [ Button.outlinePrimary
                        , Button.onClick <| CloseModal
                        ]
                        [ text "Close" ]
                    ]
                |> Modal.view model.modalState


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        First ->
            ( { model | jobs = Paginate.first model.jobs }, Cmd.none )

        Prev ->
            ( { model | jobs = Paginate.prev model.jobs }, Cmd.none )

        Next ->
            ( { model | jobs = Paginate.next model.jobs }, Cmd.none )

        Last ->
            ( { model | jobs = Paginate.last model.jobs }, Cmd.none )

        GoTo i ->
            ( { model | jobs = Paginate.goTo i model.jobs }, Cmd.none )

        FilterQuery query ->
            ( { model | query = query, jobs = Paginate.fromList model.pageSize (List.filter (filterQuery query) model.allJobs) }
            , Cmd.none
            )

        OnClickToggleJob jobId ->
            ( model, toggleJobCmd jobId )

        RequestCompletedToggleJob result ->
            onRequestCompletedToggleJob model result

        RequestCompletedToggleJobs result ->
            onRequestCompletedToggleJobs model result

        NewmanModalMsg newState ->
            ( { model | jobToDrop = Nothing, confirmationState = newState }, Cmd.none )

        OnClickJobDrop jobId ->
            ( { model | confirmationState = Modal.visibleState, jobToDrop = Just jobId }, Cmd.none )

        OnJobDropConfirmed jobId ->
            ( { model | confirmationState = Modal.hiddenState }, dropJobCmd jobId )

        RequestCompletedDropJob jobId result ->
            onRequestCompletedDropJob jobId model result

        WebSocketEvent event ->
            case event of
                CreatedJob job ->
                    ( updateJobAdded model job, Cmd.none )

                ModifiedJob job ->
                    ( updateJobUpdated model job, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        PauseAll ->
            ( model
            , Paginate.allItems model.jobs
                |> List.filter (\job -> job.state == RUNNING || job.state == READY)
                |> List.map .id
                |> toggleJobsPauseCmd
            )

        ResumeAll ->
            ( model
            , Paginate.allItems model.jobs
                |> List.filter (\job -> job.state == PAUSED)
                |> List.map .id
                |> toggleJobsResumeCmd
            )

        ActionStateMsg state ->
            ( { model | actionState = state }, Cmd.none )

        AnimateModal state ->
            ( { model | modalState = state }, Cmd.none )

        CloseModal ->
            ( { model | modalState = Modal.hiddenState, jobToEdit = Nothing, newPriorityMessage = "" }, Cmd.none )

        NewJobPriorityMsg updatePriority ->
            ( { model | newPriority = String.toInt updatePriority |> Result.withDefault 0 }, Cmd.none )

        MultiSelectAgentGroupsMsg agentGroups ->
            let
                ( subModel, subCmd, outMsg ) =
                    Multiselect.update agentGroups model.newAgentGroups
            in
            ( { model | newAgentGroups = subModel }, Cmd.map MultiSelectAgentGroupsMsg subCmd )

        ConfirmEditJob job ->
            if model.newPriority <= 4 && model.newPriority >= 0 then
                let
                    agentGroupsList =
                        List.map (\( v, k ) -> v) (Multiselect.getSelectedValues model.newAgentGroups)
                in
                    case agentGroupsList of
                        [] ->
                            ({ model | newPriorityMessage = "Please select one or more agent groups" }, Cmd.none )
                        _ ->
                            ({ model | modalState = Modal.hiddenState, jobToEdit = Nothing, newPriorityMessage = "" }, editJobCmd job.id model.newPriority agentGroupsList )

            else
                ( { model | newPriorityMessage = "priority must be between: 0 - 4" }, Cmd.none )

        RequestCompletedEditJob result ->
            case result of
                Ok data ->
                    ( model, Cmd.none )

                Err err ->
                    let
                        e =
                            Debug.log "ERROR:onRequestCompletedDropJob" err
                    in
                    ( model, Cmd.none )

        GetAllAgentGroupsCompleted result ->
            case result of
                Ok data ->
                    let
                        agentGroups =
                            List.map (\item -> ( item, item )) data
                    in
                        ( { model | modalState = Modal.visibleState, newAgentGroups = Multiselect.populateValues model.newAgentGroups agentGroups model.currentAgentGroups }, Cmd.none )
                Err err ->
                    let
                        e =
                            Debug.log "ERROR:GetAllAgentGroupsCompleted" err
                    in
                    ( model, Cmd.none )

        GetAllAgentGroupsCmd job ->
            ( { model | newPriority = job.priority |> Maybe.withDefault 0, jobToEdit = Just job,  currentAgentGroups = List.map (\item -> ( item, item )) job.agentGroups }, getAllAgentGroupsCmd )

filterQuery : String -> Job -> Bool
filterQuery query job =
    if
        String.length query
            == 0
            || String.startsWith query job.id
            || String.startsWith query job.buildName
            || String.contains query job.suiteName
            || String.contains query job.submittedBy
            || String.contains query job.jobConfigName
    then
        True

    else
        False



-- commands


updateAllJobs : (List Job -> List Job) -> Model -> Model
updateAllJobs f model =
    let
        newList =
            f model.allJobs

        filtered =
            List.filter (filterQuery model.query) newList

        newPaginated =
            Paginate.map (\_ -> filtered) model.jobs
    in
    { model | jobs = newPaginated, allJobs = newList }


updateJobAdded : Model -> Job -> Model
updateJobAdded model addedJob =
    updateAllJobs (\list -> addedJob :: list) model


updateJobUpdated : Model -> Job -> Model
updateJobUpdated model jobToUpdate =
    let
        f =
            ListExtra.replaceIf (\item -> item.id == jobToUpdate.id) jobToUpdate
    in
    updateAllJobs f model


updateJobRemoved : Model -> JobId -> Model
updateJobRemoved model jobIdToRemove =
    let
        f =
            ListExtra.filterNot (\item -> item.id == jobIdToRemove)
    in
    updateAllJobs f model


onRequestCompletedToggleJob : Model -> Result Http.Error Job -> ( Model, Cmd Msg )
onRequestCompletedToggleJob model result =
    case result of
        Ok job ->
            ( updateJobUpdated model job, Cmd.none )

        Err err ->
            let
                e =
                    Debug.log "ERROR:onRequestCompletedToggleJob" err
            in
            ( model, Cmd.none )


onRequestCompletedToggleJobs : Model -> Result Http.Error (List Job) -> ( Model, Cmd Msg )
onRequestCompletedToggleJobs model result =
    case result of
        Ok jobs ->
            ( List.foldr (flip updateJobUpdated) model jobs, Cmd.none )

        Err err ->
            let
                e =
                    Debug.log "ERROR:onRequestCompletedToggleJobS" err
            in
            ( model, Cmd.none )


toggleJobCmd : String -> Cmd Msg
toggleJobCmd jobId =
    Http.send RequestCompletedToggleJob <| Http.post ("/api/newman/job/" ++ jobId ++ "/toggle") Http.emptyBody decodeJob


toggleJobsPauseCmd : List String -> Cmd Msg
toggleJobsPauseCmd jobIds =
    Http.send RequestCompletedToggleJobs <| Http.post "/api/newman/jobs/pause/" (Http.jsonBody (encodeListOfStrings jobIds)) decodeJobList


toggleJobsResumeCmd : List String -> Cmd Msg
toggleJobsResumeCmd jobIds =
    Http.send RequestCompletedToggleJobs <| Http.post "/api/newman/jobs/resume/" (Http.jsonBody (encodeListOfStrings jobIds)) decodeJobList


editJobCmd : String -> Int -> List String -> Cmd Msg
editJobCmd jobId updatePriority updateAgentGroupsList =
    Http.send RequestCompletedEditJob <| Http.post ("/api/newman/job/" ++ jobId++"/edit") (Http.jsonBody (decodeJobForEdit updatePriority updateAgentGroupsList)) decodeJob

decodeJobForEdit : Int -> List String -> Value
decodeJobForEdit updatePriority updateAgentGroupsList =
    Json.Encode.object [( "agentGroups", Json.Encode.list <| List.map Json.Encode.string updateAgentGroupsList ), ( "priority", Json.Encode.int updatePriority )]


onRequestCompletedDropJob : String -> Model -> Result Http.Error String -> ( Model, Cmd Msg )
onRequestCompletedDropJob jobId model result =
    case result of
        Ok _ ->
            ( updateJobRemoved model jobId, Cmd.none )

        Err err ->
            let
                e =
                    Debug.log "ERROR:onRequestCompletedDropJob" err
            in
            ( model, Cmd.none )


dropJobCmd : String -> Cmd Msg
dropJobCmd jobId =
    Http.send (RequestCompletedDropJob jobId) <|
        Http.request <|
            { method = "DELETE"
            , headers = []
            , url = "/api/newman/job/" ++ jobId
            , body = Http.emptyBody
            , expect = Http.expectString
            , timeout = Nothing
            , withCredentials = False
            }


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent


subscriptions : Model -> Sub Msg
subscriptions model =
    Dropdown.subscriptions model.actionState ActionStateMsg

getAllAgentGroupsCmd : Cmd Msg
getAllAgentGroupsCmd =
    Http.send GetAllAgentGroupsCompleted <| Http.get "/api/newman/availableAgentGroups" decodeAgentGroups
