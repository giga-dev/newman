module Pages.Job exposing (..)

import Bootstrap.Badge as Badge
import Bootstrap.Button as Button
import Bootstrap.ButtonGroup as ButtonGroup
import Bootstrap.Modal as Modal
import Bootstrap.Progress as Progress exposing (..)
import Date
import DateFormat
import Dict
import Html exposing (..)
import Html.Attributes exposing (..)
import Http exposing (..)
import Json.Decode
import Task
import Time exposing (Time)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.TestsTable as TestsTable
import Utils.Common as Common
import Views.NewmanModal as NewmanModal


type alias Model =
    { maybeJob : Maybe Job
    , collapseState : CollapseState
    , testsTable : TestsTable.Model
    , currTime : Maybe Time
    , statusState : RadioState
    , confirmationState : Modal.State
    , newSuiteName: Maybe String
    , newSuiteMessage: Maybe (Result String String)
    }


type CollapseState
    = Hidden
    | Shown



type Msg
    = GetJobInfoCompleted (Result Http.Error Job)
    | ToggleButton
    | GetTestsViewCompleted (Result Http.Error (List Test))
    | TestsTableMsg TestsTable.Msg
    | ReceiveTime Time
    | WebSocketEvent WebSocket.Event
    | StatusMsg RadioState
    | NewmanModalMsg Modal.State
    | OnNewSuiteConfirm String
    | OnNewSuiteCreateButton
    | CreateSuiteResponse (Result Http.Error Suite)
    | OnNewSuiteNameChanged String




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
    , currTime = Nothing
    , statusState = state
    , confirmationState = Modal.hiddenState
    , newSuiteName = Nothing
    , newSuiteMessage = Nothing
    }


initCmd : JobId -> Cmd Msg
initCmd jobId =
    Cmd.batch [ getJobInfoCmd jobId, requestTime ]


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
                    [ Progress.value <| toFloat <| (job.failedTests + job.passedTests) * 100 // job.totalTests
                                        , Progress.label <| toString <| (job.failedTests + job.passedTests) * 100 // job.totalTests
                    ]
                )

        dateFormat maybeDate =
            case maybeDate of
                Just date ->
                    DateFormat.format Common.dateTimeDateFormat (Date.fromTime (toFloat date))

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
                        [ Button.attrs [title "Failed 3 Times", class "job-radio-button-failed3X"] , Button.onClick <| StatusMsg STATUS_FAILED3TIMES ]
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
        createSuiteButton =
                Button.button [ Button.onClick OnNewSuiteCreateButton , Button.outlinePrimary, Button.attrs [ style [("vertical-align", "top"), ( "margin-left" , "10px")] ]   ] [ text "Create suite" ]


        headerRows =
            [ ( "Suite", a [ href  <| "#suite/" ++ job.suiteId] [ text job.suiteName ] )
            , ( "Job configuration", text job.jobConfigName )
            , ( "Submit Time", text <| dateFormat <| Just job.submitTime )
            , ( "Start Time", text <| dateFormat job.startTime )
            , ( "End Time", text <| dateFormat job.endTime )
            , ( "# Agents", text <| toString <| List.length job.agents )
            , ( "# requiredAgentGroups", text <| toString <| List.length job.requiredAgentGroups )
            , ( "# Prep. Agents", text <| toString <| List.length job.preparingAgents )
            , ( "Submitted by", text <| job.submittedBy )
            , ( "Status", div [] [ testsStatus, createSuiteButton ] )
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
                , NewmanModal.createSuiteForFailedTestsModal model.newSuiteName model.newSuiteMessage NewmanModalMsg OnNewSuiteNameChanged OnNewSuiteConfirm model.confirmationState
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
                    ( { model | testsTable = TestsTable.init (Maybe.withDefault "" <| Maybe.map .id model.maybeJob) data model.statusState }, requestTime )

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

        ReceiveTime time ->
            ( { model | currTime = Just time }, Cmd.none )

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

        OnNewSuiteCreateButton ->
            let
                suiteName = case model.newSuiteName of
                        Just suiteName ->
                            suiteName
                        Nothing ->
                            case (model.currTime, model.maybeJob) of
                                (Just time, Just job) ->
                                    "dev-failing-" ++ job.suiteName ++ "-" ++ (DateFormat.format Common.timestamp <| Date.fromTime time)
                                (_, _) ->
                                    "..."
            in
                ( {model | confirmationState = Modal.visibleState, newSuiteName = Just suiteName } , Cmd.none )
        NewmanModalMsg newState ->
            let
                cleanup = (newState == Modal.hiddenState)
                newModel = { model | confirmationState = newState }
            in
                if (cleanup) then
                    ( { newModel | newSuiteName = Nothing, newSuiteMessage = Nothing }, Cmd.none )
                else
                    ( newModel , Cmd.none )
        OnNewSuiteConfirm suiteName ->
            case model.maybeJob of
                Just job ->
                    if (String.startsWith "dev-" suiteName) then
                        ( { model | newSuiteMessage = Just <| Err "Sending request..." } , createSuiteCmd suiteName job.id)
                    else
                        ( { model | newSuiteMessage = Just <| Ok "Suite name does not start with 'dev-'" } , Cmd.none)

                Nothing ->
                    ( { model |confirmationState = Modal.hiddenState, newSuiteName = Nothing, newSuiteMessage = Nothing } , Cmd.none )

        CreateSuiteResponse result ->
            case result of
                Ok suite ->
                    ({ model | newSuiteMessage = Just <| Ok <| "Suite with id ["++suite.id++"] has been created"} , Cmd.none)
                Err err ->
                    let
                        errMsg = case err of
                            BadStatus msg ->
                                msg.body
                            _ ->
                                toString err
                    in
                        ({model | newSuiteMessage = Just <| Ok errMsg }, Cmd.none)
        OnNewSuiteNameChanged newName ->
            ( { model | newSuiteName = Just newName } , Cmd.none)


getJobInfoCmd : JobId -> Cmd Msg
getJobInfoCmd jobId =
    Http.send GetJobInfoCompleted <|
        Http.get ("/api/newman/job/" ++ jobId) decodeJob


getTestsViewCmd : JobId -> Cmd Msg
getTestsViewCmd jobId =
    Http.send GetTestsViewCompleted <|
        Http.get ("/api/newman/job-tests-view?jobId=" ++ jobId ++ "&all=true&orderBy=name") <|
            Json.Decode.field "values" (Json.Decode.list decodeTestView)


requestTime : Cmd Msg
requestTime =
    Task.perform ReceiveTime Time.now


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    Cmd.batch
        [ event => WebSocketEvent
        , TestsTable.handleEvent event |> Cmd.map TestsTableMsg
        ]


createSuiteCmd : String -> JobId -> Cmd Msg
createSuiteCmd suiteName jobId =
    Http.send CreateSuiteResponse <| Http.post ("/api/newman/suite/failedTests?jobId=" ++ jobId++"&suiteName="++suiteName) Http.emptyBody decodeSuite