module Pages.Job exposing (..)

import Bootstrap.Badge as Badge
import Bootstrap.Button as Button
import Bootstrap.ButtonGroup as ButtonGroup
import Bootstrap.Progress as Progress exposing (..)
import Date
import Date.Format
import Dict
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Http exposing (..)
import Json.Decode
import Json.Decode.Pipeline exposing (decode)
import Task
import Time exposing (Time)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.TestsTable as TestsTable


type alias Model =
    { maybeJob : Maybe Job
    , collapseState : CollapseState
    , testsTable : TestsTable.Model
    , currTime : Time
    , statusState : RadioState
    }


type CollapseState
    = Hidden
    | Shown



type Msg
    = GetJobInfoCompleted (Result Http.Error Job)
    | ToggleButton
    | GetTestsViewCompleted (Result Http.Error (List Test))
    | TestsTableMsg TestsTable.Msg
    | OnTime Time
    | WebSocketEvent WebSocket.Event
    | StatusMsg RadioState



-- external


parseJobId : Parser (String -> a) a
parseJobId =
    UrlParser.string

parseRadioState : Parser (String -> a) a
parseRadioState =
    UrlParser.string

initModel : JobId -> RadioState -> Model
initModel jobId state =
    { maybeJob = Nothing
    , collapseState = Hidden
    , testsTable = TestsTable.init jobId [] state
    , currTime = 0
    , statusState = state
    }


initCmd : JobId -> Cmd Msg
initCmd jobId =
    Cmd.batch [ getJobInfoCmd jobId, getTime ]


viewHeader : Model -> Job -> Html Msg
viewHeader model job =
    let
        buildRow =
            tr []
                [ td []
                    [ text "Build"
                    ]
                , td []
                    [ a [ href <| "#build/" ++ job.buildId ] [ text <| job.buildName ++ " (" ++ job.buildBranch ++ ")" ]
                    ]
                ]

        progressRow =
            viewRow
                ( "Progress"
                , Progress.progress
                    [ Progress.value <| toFloat <| (job.runningTests + job.failedTests + job.passedTests) * 100 // (job.totalTests + job.numOfTestRetries)
                                        , Progress.label <| toString <| (job.runningTests + job.failedTests + job.passedTests) * 100 // (job.totalTests + job.numOfTestRetries)
                    ]
                )

        dateFormat maybeDate =
            case maybeDate of
                Just date ->
                    Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat date))

                Nothing ->
                    "N/A"

        stateRow =
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
            viewRow
                ( "State"
                , badge [] [ text <| jobStateToString job.state ]
                )

        testsStatus =
            ButtonGroup.radioButtonGroup []
                    [ ButtonGroup.radioButton
                        (model.statusState == STATUS_RUNNING)
                        [ Button.attrs [title "Running Tests"] , Button.outlinePrimary, Button.outlineInfo, Button.onClick <| StatusMsg STATUS_RUNNING ]
                        [ text <| toString job.runningTests ]
                    , ButtonGroup.radioButton
                        (model.statusState == STATUS_SUCCESS)
                        [ Button.attrs [title "Passed Tests"] , Button.outlinePrimary, Button.outlineSuccess, Button.onClick <| StatusMsg STATUS_SUCCESS ]
                        [ text <| toString job.passedTests ]
                    , ButtonGroup.radioButton
                        (model.statusState == STATUS_FAIL)
                        [ Button.attrs [title "Failed Tests"] , Button.outlinePrimary, Button.outlineDanger, Button.onClick <| StatusMsg STATUS_FAIL ]
                        [ text <| toString job.failedTests ]
                    , ButtonGroup.radioButton
                        (model.statusState == STATUS_FAILED3TIMES)
                        [ Button.attrs [title "Failed 3 Times"] , Button.outlinePrimary, Button.outlineWarning, Button.onClick <| StatusMsg STATUS_FAILED3TIMES ]
                        [ text <| toString job.failed3TimesTests ]
                    , ButtonGroup.radioButton
                        (model.statusState == STATUS_ALL)
                        [ Button.attrs [title "All Tests"] , Button.outlinePrimary, Button.onClick <| StatusMsg STATUS_ALL ]
                        [ text <| toString job.totalTests ]
                    ]




        jobToT ( key, val ) =
            li []
                [ a [ href val ] [ text key ]
                , text " "
                , a [ href <| val ++ "?download=true" ] [ text "[Download]" ]
                ]

        jobSetupLogsData =
            div [ classList [ ( "collapse", model.collapseState == Hidden ), ( "collapse.show", model.collapseState == Shown ) ] ]
                [ ul [] <|
                    List.map jobToT
                        (Dict.toList job.jobSetupLogs)
                ]

        jobSetupButton =
            let
                tt =
                    case model.collapseState of
                        Hidden ->
                            "ion-chevron-down"

                        Shown ->
                            "ion-chevron-up"
            in
            Button.button [ Button.success, Button.small, Button.onClick ToggleButton ] [ span [ class tt ] [] ]

        jobSetupLogs =
            div []
                [ jobSetupButton
                , jobSetupLogsData
                ]

        headerRows =
            [ ( "Suite", a [ href  <| "#suite/" ++ job.suiteId] [ text job.suiteName ] )
            , ( "Job configuration", text job.jobConfigName )
            , ( "Submit Time", text <| dateFormat <| Just job.submitTime )
            , ( "Start Time", text <| dateFormat job.startTime )
            , ( "End Time", text <| dateFormat job.endTime )
            , ( "# Agents", text <| toString <| List.length job.agents )
            , ( "# Prep. Agents", text <| toString <| List.length job.preparingAgents )
            , ( "Submitted by", text <| job.submittedBy )
            , ( "Status", div [] [ testsStatus ] )
            , ( "Job Setup Logs", jobSetupLogs )
            ]

        viewRow ( name, value ) =
            tr []
                [ td [ width 200 ] [ text name ]
                , td [] [ value ]
                ]
    in
    table [ class "job-view" ] <|
        [ buildRow
        , stateRow
        , progressRow
        ]
            ++ List.map
                viewRow
                headerRows


viewBody : Model -> Html Msg
viewBody model =
    TestsTable.viewTable model.testsTable model.currTime |> Html.map TestsTableMsg


view : Model -> Html Msg
view model =
    case model.maybeJob of
        Just job ->
            div [ class "container-fluid" ] <|
                [ h2 [ class "text" ] [ text <| "Details for job " ++ job.id ]
                , viewHeader model job
                , viewBody model
                ]

        Nothing ->
            div []
                [ text "Loading Job..."
                ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetJobInfoCompleted result ->
            case result of
                Ok data ->
                    ( { model | maybeJob = Just data }, getTestsViewCmd data.id )

                Err err ->
                    ( model, Cmd.none )

        GetTestsViewCompleted result ->
            case result of
                Ok data ->
                    ( { model | testsTable = TestsTable.init (Maybe.withDefault "" <| Maybe.map .id model.maybeJob) data model.statusState }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        TestsTableMsg subMsg ->
            let
                ( newSubModel, newCmd ) =
                    TestsTable.update subMsg model.testsTable
            in
            ( { model | testsTable = newSubModel }, newCmd |> Cmd.map TestsTableMsg )

        ToggleButton ->
            let
                newState =
                    case model.collapseState of
                        Hidden ->
                            Shown

                        Shown ->
                            Hidden
            in
            ( { model | collapseState = newState }, Cmd.none )

        OnTime time ->
            ( { model | currTime = time }, Cmd.none )

        WebSocketEvent event ->
            case event of
                ModifiedJob job ->
                    case model.maybeJob of
                        Just currentJob ->
                            if currentJob.id == job.id then
                                ( { model | maybeJob = Just job }, Cmd.none )
                            else
                                ( model, Cmd.none )

                        Nothing ->
                            ( model, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        StatusMsg state ->
            let
                ( newSubModel, newCmd ) =
                    TestsTable.update (TestsTable.UpdateFilterState (Maybe.withDefault "" <| Maybe.map .id model.maybeJob) state) model.testsTable
            in
            ( { model | statusState = state , testsTable = newSubModel } , newCmd |> Cmd.map TestsTableMsg )

getJobInfoCmd : JobId -> Cmd Msg
getJobInfoCmd jobId =
    Http.send GetJobInfoCompleted <|
        Http.get ("/api/newman/job/" ++ jobId) decodeJob


getTestsViewCmd : JobId -> Cmd Msg
getTestsViewCmd jobId =
    Http.send GetTestsViewCompleted <|
        Http.get ("/api/newman/job-tests-view?jobId=" ++ jobId ++ "&all=true&orderBy=name") <|
            Json.Decode.field "values" (Json.Decode.list decodeTestView)


getTime : Cmd Msg
getTime =
    Task.perform OnTime Time.now


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    Cmd.batch
        [ event => WebSocketEvent
        , TestsTable.handleEvent event |> Cmd.map TestsTableMsg
        ]
