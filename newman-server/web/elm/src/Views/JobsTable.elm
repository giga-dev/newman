module Views.JobsTable exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Bootstrap.Modal as Modal exposing (..)
import Bootstrap.Progress as Progress exposing (..)
import Bootstrap.Dropdown as Dropdown
import Date exposing (Date)
import Date.Extra.Config.Config_en_au exposing (config)
import Date.Extra.Duration as Duration
import DateFormat
import DateFormat.Relative
import Html exposing (..)
import Html.Attributes as HtmlAttr exposing (..)
import Html.Events exposing (..)
import Http
import List.Extra as ListExtra
import Paginate exposing (PaginatedList)
import Time exposing (Time)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal exposing (..)
import Json.Decode exposing (..)
import Utils.Common as Common

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

type alias Model =
    { allJobs : List Job
    , jobs : PaginatedList Job
    , pageSize : Int
    , confirmationState : Modal.State
    , jobToDrop : Maybe String
    , query : String
    , actionState : Dropdown.State
    }


init : List Job -> Model
init jobs =
    let
        pageSize =
            15

    in
        Model jobs (Paginate.fromList pageSize jobs) pageSize Modal.hiddenState Nothing "" Dropdown.initialState


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
                    } ]

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
                        , th [ widthPct "11%" ] [ text "Job Id" ]
                        , th [ widthPct "8%" ] [ text "Suite" ]
                        , th [ widthPct "6%" ] [ text "Job Configuration" ]
                        , th [ widthPct "5%" ] [ text "Duration" ]
                        , th [ widthPct "6%" ] [ text "Submitted At" ]
                        , th [ widthPct "11%" ] [ text "Build" ]
                        , th [ widthPct "5%" ] [ text "Submitted By" ]
                        , th [ widthPct "3%" ] [ text "# p. agents" ]
                        , th [ widthPct "11%" ] [ text "Required Agent Groups" ]
                        , th [ widthPct "17%" ]
                            [ Badge.badgeInfo [ class "job-tests-badge" , title "Running Tests" ] [ text "Run" ]
                            , text "/ "
                            , Badge.badgeSuccess [ class "job-tests-badge" , title "Passed Tests" ] [ text "Pass" ]
                            , text "/ "
                            , Badge.badgeDanger [ class "job-tests-badge" , title "Failed Tests" ] [ text "Fail" ]
                            , text "/ "
                            , Badge.badgeWarning [ class "job-tests-badge", style [("background-color","DarkRed")] , title "Failed 3 Times" ] [ text "3xFail" ]
                            , text "/ "
                            , Badge.badge [ class "job-tests-badge" , title "All Tests" ] [ text "Total" ]
                            ]
                        , th [ width 80 ]
                             [ text "Actions" ]
                        ]
                    ]
                , tbody [] (List.map (viewJob currTime) <| Paginate.page model.jobs)
                ]
            , pagination
            , NewmanModal.confirmJobDrop model.jobToDrop NewmanModalMsg OnJobDropConfirmed model.confirmationState
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
                    case ( job.startTime, job.endTime , currTime ) of
                        ( Just startTime, Just endTime , _ ) ->
                            Just <| Duration.diff (Date.fromTime (toFloat endTime)) (Date.fromTime (toFloat startTime))

                        ( Just startTime, Nothing , Just time ) ->
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

        emptyAgentGroups maybeAgentGroups =
            case maybeAgentGroups of
                       [] ->
                            "N/A"

                       _ ->
                            String.join "," maybeAgentGroups

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
            , td [] [ text <| emptyAgentGroups job.requiredAgentGroups ]
            , td []
                [ Badge.badgeInfo [ class "job-tests-badge" ] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/RUNNING" , title "Running Tests" ]
                                    [text <| toString job.runningTests] ]
                , text "/ "
                , Badge.badgeSuccess [ class "job-tests-badge" ] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/SUCCESS" , title "Passed Tests" ]
                                    [text <| toString job.passedTests] ]
                , text "/ "
                , Badge.badgeDanger [ class "job-tests-badge" ] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/FAIL" , title "Failed Tests" ]
                                    [text <| toString job.failedTests] ]
                , text "/ "
                , Badge.badgeWarning [ class "job-tests-badge", style [("background-color","DarkRed")] ] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/FAILED3TIMES" , title "Failed 3 Times" ]
                                    [text <| toString job.failed3TimesTests] ]
                , text "/ "
                , Badge.badge [ class "job-tests-badge" ] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/ALL" , title "All Tests" ]
                                    [ text <| toString job.totalTests] ]
                ]
            , td []
                [ Button.button
                    [ Button.danger
                    , Button.small
                    , Button.onClick <| OnClickJobDrop job.id
                    , Button.disabled <|
                        not (List.member job.state [ DONE, PAUSED, BROKEN ] && (job.runningTests <= 0) && (List.length job.agents) <= 0)
                    ]
                    [ span [ class "ion-close" ] [] ]
                , text " "
                , playPauseButton
                ]
            ]


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
            ( { model | actionState = state } , Cmd.none)


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
            ( model, Cmd.none )


onRequestCompletedToggleJobs : Model -> Result Http.Error (List Job) -> ( Model, Cmd Msg )
onRequestCompletedToggleJobs model result =
     case result of
     Ok jobs ->
        ( List.foldr (flip updateJobUpdated) model jobs , Cmd.none)
     Err err ->
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


onRequestCompletedDropJob : String -> Model -> Result Http.Error String -> ( Model, Cmd Msg )
onRequestCompletedDropJob jobId model result =
    case result of
        Ok _ ->
            ( updateJobRemoved model jobId, Cmd.none )

        Err err ->
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