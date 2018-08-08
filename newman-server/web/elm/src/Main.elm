module Main exposing (..)

import Bootstrap.CDN exposing (stylesheet)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import Navigation exposing (Location)
import Pages.Agents as Agents
import Pages.Build as Build
import Pages.Builds as Builds
import Pages.Home as Home
import Pages.Job as Job
import Pages.JobConfig as JobConfig
import Pages.JobConfigs as JobConfigs
import Pages.Jobs as Jobs
import Pages.ManageNewman as ManageNewman
import Pages.NewJobConfig as NewJobConfig
import Pages.SubmitNewJob as SubmitNewJob
import Pages.Suite as Suite
import Pages.Suites as Suites
import Pages.Test as Test
import Pages.TestHistory as TestHistory
import Task
import UrlParser exposing (..)
import Utils.Types exposing (BuildId, JobConfigId, JobId, JobRadioState, RadioState(..), SuiteId, TestId, stringToRadioState)
import Utils.WebSocket as WebSocket exposing (Event(CreatedJob))
import Views.JobsTable as JobsTable
import Views.TopBar as TopBar


main : Program Never Model Msg
main =
    Navigation.program locFor
        { init = init
        , view = view
        , update = update
        , subscriptions = subscriptions
        }


type Route
    = HomeRoute
    | SubmitNewJobRoute
    | JobsRoute
    | BuildsRoute
    | AgentsRoute
    | SuitesRoute
    | JobConfigsRoute
    | NewJobConfigsRoute
    | ManageNewmanRoute
    | JobConfigRoute JobConfigId
    | JobRoute JobId JobRadioState
    | BuildRoute BuildId
    | SuiteRoute SuiteId
    | TestRoute TestId
    | TestHistoryRoute TestId


type Page
    = HomePage
    | SubmitNewJobPage
    | JobsPage
    | BuildsPage
    | AgentsPage
    | SuitesPage
    | JobConfigsPage
    | ManageNewmanPage
    | NewJobConfigsPage
    | JobConfigPage JobConfig.Model
    | JobPage Job.Model
    | BuildPage Build.Model
    | SuitePage Suite.Model
    | TestPage Test.Model
    | TestHistoryPage TestHistory.Model


routeToPage : Route -> Page
routeToPage route =
    case route of
        HomeRoute ->
            HomePage

        SubmitNewJobRoute ->
            SubmitNewJobPage

        JobsRoute ->
            JobsPage

        BuildsRoute ->
            BuildsPage

        SuitesRoute ->
            SuitesPage

        AgentsRoute ->
            AgentsPage

        JobConfigsRoute ->
            JobConfigsPage

        NewJobConfigsRoute ->
            NewJobConfigsPage

        ManageNewmanRoute ->
            ManageNewmanPage

        JobConfigRoute id ->
            JobConfigPage (JobConfig.Model Nothing)

        JobRoute id state ->
            JobPage <| Job.initModel id (stringToRadioState state)

        BuildRoute id ->
            BuildPage <| Build.initModel

        SuiteRoute id ->
            SuitePage (Suite.Model Nothing)

        TestRoute id ->
            TestPage <| Test.initModel

        TestHistoryRoute id ->
            TestHistoryPage <| TestHistory.initModel


routeToString : Route -> String
routeToString route =
    let
        pieces =
            case route of
                HomeRoute ->
                    []

                SubmitNewJobRoute ->
                    [ "submit-new-job" ]

                JobsRoute ->
                    [ "jobs" ]

                BuildsRoute ->
                    [ "builds" ]

                SuitesRoute ->
                    [ "suites" ]

                AgentsRoute ->
                    [ "agents" ]

                JobConfigsRoute ->
                    [ "jobConfigs" ]

                ManageNewmanRoute ->
                    [ "manageNewman" ]

                NewJobConfigsRoute ->
                    [ "newJobConfig" ]

                JobConfigRoute id ->
                    [ "jobConfig", id ]

                JobRoute id state ->
                    [ "job", id, state ]

                BuildRoute id ->
                    [ "build", id ]

                SuiteRoute id ->
                    [ "suite", id ]

                TestRoute id ->
                    [ "test", id ]

                TestHistoryRoute id ->
                    [ "test-history", id ]
    in
    "#" ++ String.join "/" pieces


route : Parser (Route -> a) a
route =
    oneOf
        [ UrlParser.map HomeRoute (UrlParser.s "")
        , UrlParser.map JobsRoute (UrlParser.s "jobs")
        , UrlParser.map SubmitNewJobRoute (UrlParser.s "submit-new-job")
        , UrlParser.map BuildsRoute (UrlParser.s "builds")
        , UrlParser.map AgentsRoute (UrlParser.s "agents")
        , UrlParser.map SuitesRoute (UrlParser.s "suites")
        , UrlParser.map JobConfigsRoute (UrlParser.s "jobConfigs")
        , UrlParser.map ManageNewmanRoute (UrlParser.s "manageNewman")
        , UrlParser.map JobConfigRoute (UrlParser.s "NewmanManageConfig" </> JobConfig.parseJobConfigId)
        , UrlParser.map NewJobConfigsRoute (UrlParser.s "newJobConfig")
        , UrlParser.map JobConfigRoute (UrlParser.s "jobConfig" </> JobConfig.parseJobConfigId)
        , UrlParser.map JobRoute (UrlParser.s "job" </> Job.parseJobId </> Job.parseRadioState)
        , UrlParser.map BuildRoute (UrlParser.s "build" </> Build.parseBuildId)
        , UrlParser.map SuiteRoute (UrlParser.s "suite" </> Suite.parseSuiteId)
        , UrlParser.map TestRoute (UrlParser.s "test" </> UrlParser.string)
        , UrlParser.map TestHistoryRoute (UrlParser.s "test-history" </> UrlParser.string)
        ]


locFor : Location -> Msg
locFor location =
    fromLocation location
        |> GoTo


type alias Model =
    { currentPage : Page
    , currentRoute : Route
    , submitNewJobModel : SubmitNewJob.Model
    , jobsModel : Jobs.Model
    , buildsModel : Builds.Model
    , agentsModel : Agents.Model
    , homeModel : Home.Model
    , suitesModel : Suites.Model
    , jobConfigsModel : JobConfigs.Model
    , newJobConfigsModel : NewJobConfig.Model
    , manageNewmanModel : ManageNewman.Model
    , topBarModel : TopBar.Model
    , webSocketModel : WebSocket.Model
    }


type Msg
    = GoTo (Maybe Route)
    | SubmitNewJobMsg SubmitNewJob.Msg
    | JobsMsg Jobs.Msg
    | BuildsMsg Builds.Msg
    | AgentsMsg Agents.Msg
    | SuitesMsg Suites.Msg
    | JobConfigsMsg JobConfigs.Msg
    | NewJobConfigMsg NewJobConfig.Msg
    | JobConfigMsg JobConfig.Msg
    | HomeMsg Home.Msg
    | ManageNewmanMsg ManageNewman.Msg
    | JobMsg Job.Msg
    | BuildMsg Build.Msg
    | SuiteMsg Suite.Msg
    | TestMsg Test.Msg
    | TopBarMsg TopBar.Msg
    | TestHistoryMsg TestHistory.Msg
    | WebSocketMsg WebSocket.Msg


fromLocation : Location -> Maybe Route
fromLocation location =
    if String.isEmpty location.hash then
        Just HomeRoute
    else
        parseHash route location


init : Location -> ( Model, Cmd Msg )
init location =
    let
        currentRoute =
            case fromLocation location of
                Just route ->
                    route

                Nothing ->
                    HomeRoute

        currentPage =
            routeToPage <| currentRoute

        ( submitNewJobModel, submitNewJobCmd ) =
            SubmitNewJob.init

        ( jobsModel, jobsCmd ) =
            Jobs.init

        ( buildsModel, buildsCmd ) =
            Builds.init

        ( agentsModel, agentsCmd ) =
            Agents.init

        ( suitesModel, suitesCmd ) =
            Suites.init

        ( jobConfigsModel, jobConfigsCmd ) =
            JobConfigs.init

        ( newJobConfigsModel, newJobConfigsCmd ) =
            NewJobConfig.init

        ( homeModel, homeCmd ) =
            Home.init

        ( manageNewmanModel, manageNewmanCmd ) =
            ManageNewman.init

        ( topBarModel, topBarCmd ) =
            TopBar.init

        moreCmd =
            case currentRoute of
                JobRoute id _ ->
                    Job.initCmd id |> Cmd.map JobMsg

                JobConfigRoute id ->
                    JobConfig.getJobConfigInfoCmd id |> Cmd.map JobConfigMsg

                BuildRoute id ->
                    Build.initCmd id |> Cmd.map BuildMsg

                SuiteRoute id ->
                    Suite.getSuiteInfoCmd id |> Cmd.map SuiteMsg

                TestRoute id ->
                    Test.getTestDataCmd id |> Cmd.map TestMsg

                TestHistoryRoute id ->
                    TestHistory.initCmd id |> Cmd.map TestHistoryMsg

                _ ->
                    Cmd.none
    in
    ( { currentPage = currentPage
      , currentRoute = currentRoute
      , submitNewJobModel = submitNewJobModel
      , jobsModel = jobsModel
      , buildsModel = buildsModel
      , agentsModel = agentsModel
      , homeModel = homeModel
      , manageNewmanModel = manageNewmanModel
      , suitesModel = suitesModel
      , jobConfigsModel = jobConfigsModel
      , newJobConfigsModel = newJobConfigsModel
      , topBarModel = topBarModel
      , webSocketModel = WebSocket.initModel location
      }
    , Cmd.batch
        [ submitNewJobCmd |> Cmd.map SubmitNewJobMsg
        , jobsCmd |> Cmd.map JobsMsg
        , buildsCmd |> Cmd.map BuildsMsg
        , agentsCmd |> Cmd.map AgentsMsg
        , jobConfigsCmd |> Cmd.map JobConfigsMsg
        , newJobConfigsCmd |> Cmd.map NewJobConfigMsg
        , homeCmd |> Cmd.map HomeMsg
        , manageNewmanCmd |> Cmd.map ManageNewmanMsg
        , suitesCmd |> Cmd.map SuitesMsg
        , topBarCmd |> Cmd.map TopBarMsg
        , moreCmd
        ]
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model.currentPage ) of
        ( GoTo maybeRoute, _ ) ->
            case maybeRoute of
                Just route ->
                    case ( route, model.currentRoute ) of
                        ( JobRoute _ _, JobRoute _ _ ) ->
                            ( model, Cmd.none )

                        ( _, _ ) ->
                            let
                                newPage =
                                    routeToPage route

                                newModel =
                                    { model | currentRoute = route, currentPage = newPage }
                            in
                            case route of
                                JobRoute id _ ->
                                    ( newModel, Job.getJobInfoCmd id |> Cmd.map JobMsg )

                                JobConfigRoute id ->
                                    ( newModel, JobConfig.getJobConfigInfoCmd id |> Cmd.map JobConfigMsg )

                                BuildRoute id ->
                                    ( newModel, Build.getBuildInfoCmd id |> Cmd.map BuildMsg )

                                SuiteRoute id ->
                                    ( newModel, Suite.getSuiteInfoCmd id |> Cmd.map SuiteMsg )

                                TestRoute id ->
                                    ( newModel, Test.getTestDataCmd id |> Cmd.map TestMsg )

                                TestHistoryRoute id ->
                                    ( newModel, TestHistory.getTestHistory id |> Cmd.map TestHistoryMsg )

                                _ ->
                                    ( newModel, Cmd.none )

                Nothing ->
                    ( model, Cmd.none )

        ( SubmitNewJobMsg subMsg, _ ) ->
            let
                ( updatedSubModel, subCmd ) =
                    SubmitNewJob.update subMsg model.submitNewJobModel
            in
            ( { model | submitNewJobModel = updatedSubModel }, Cmd.map SubmitNewJobMsg subCmd )

        ( JobsMsg subMsg, _ ) ->
            let
                ( updatedSubModel, subCmd ) =
                    Jobs.update subMsg model.jobsModel
            in
            ( { model | jobsModel = updatedSubModel }, Cmd.map JobsMsg subCmd )

        ( JobMsg subMsg, JobPage subModel ) ->
            let
                ( updatedSubModel, subCmd ) =
                    Job.update subMsg subModel
            in
            ( { model | currentPage = JobPage updatedSubModel }, Cmd.map JobMsg subCmd )

        ( JobMsg subMsg, _ ) ->
            ( model, Cmd.none )

        ( BuildsMsg buildsMsg, _ ) ->
            let
                ( updatedBuildsModel, buildsCmd ) =
                    Builds.update buildsMsg model.buildsModel
            in
            ( { model | buildsModel = updatedBuildsModel }, Cmd.map BuildsMsg buildsCmd )

        ( BuildMsg (Build.JobsTableMsg (JobsTable.RequestCompletedToggleJobs subMsg)), BuildPage subModel ) ->
            let
                theMsg =
                    JobsTable.RequestCompletedToggleJobs subMsg

                ( updatedSubModel, subCmd ) =
                    Build.update (Build.JobsTableMsg theMsg) subModel

                ( updatedJobsModel, jobsSubCmd ) =
                    Jobs.update (Jobs.JobsTableMsg theMsg) model.jobsModel
            in
            ( { model | currentPage = BuildPage updatedSubModel, jobsModel = updatedJobsModel }
            , Cmd.batch [ Cmd.map BuildMsg subCmd, Cmd.map JobsMsg jobsSubCmd ]
            )

        ( BuildMsg subMsg, BuildPage subModel ) ->
            let
                ( updatedSubModel, subCmd ) =
                    Build.update subMsg subModel
            in
            ( { model | currentPage = BuildPage updatedSubModel }, Cmd.map BuildMsg subCmd )

        ( BuildMsg subMsg, _ ) ->
            ( model, Cmd.none )

        ( AgentsMsg agentMsg, _ ) ->
            let
                ( updatedAgentsModel, agentsCmd ) =
                    Agents.update agentMsg model.agentsModel
            in
            ( { model | agentsModel = updatedAgentsModel }, Cmd.map AgentsMsg agentsCmd )

        ( JobConfigsMsg jobConfigsMsg, _ ) ->
            let
                ( updatedJobConfigsModel, jobConfigsCmd ) =
                    JobConfigs.update jobConfigsMsg model.jobConfigsModel
            in
            ( { model | jobConfigsModel = updatedJobConfigsModel }, Cmd.map JobConfigsMsg jobConfigsCmd )

        ( NewJobConfigMsg newJobConfigsMsg, _ ) ->
            let
                ( updatedNewJobConfigsModel, newJobConfigsCmd ) =
                    NewJobConfig.update newJobConfigsMsg model.newJobConfigsModel
            in
            ( { model | newJobConfigsModel = updatedNewJobConfigsModel }, Cmd.map NewJobConfigMsg newJobConfigsCmd )

        ( JobConfigMsg subMsg, JobConfigPage subModel ) ->
            let
                ( updatedSubModel, subCmd ) =
                    JobConfig.update subMsg subModel
            in
            ( { model | currentPage = JobConfigPage updatedSubModel }, Cmd.map JobConfigMsg subCmd )

        ( JobConfigMsg subMsg, _ ) ->
            ( model, Cmd.none )

        ( SuitesMsg suiteMsg, _ ) ->
            let
                ( updatedSuitesModel, suitesCmd ) =
                    Suites.update suiteMsg model.suitesModel
            in
            ( { model | suitesModel = updatedSuitesModel }, Cmd.map SuitesMsg suitesCmd )

        ( SuiteMsg subMsg, SuitePage subModel ) ->
            let
                ( updatedSubModel, subCmd ) =
                    Suite.update subMsg subModel
            in
            ( { model | currentPage = SuitePage updatedSubModel }, Cmd.map SuiteMsg subCmd )

        ( HomeMsg homeMsg, _ ) ->
            let
                ( updatedSubModel, subCmd ) =
                    Home.update homeMsg model.homeModel
            in
            ( { model | homeModel = updatedSubModel }, Cmd.map HomeMsg subCmd )

        ( ManageNewmanMsg manageNewmanMsg, _ ) ->
            let
                ( updatedManageNewmanModel, manageNewmanCmd ) =
                    ManageNewman.update manageNewmanMsg model.manageNewmanModel
            in
            ( { model | manageNewmanModel = updatedManageNewmanModel }, Cmd.map ManageNewmanMsg manageNewmanCmd )

        ( SuiteMsg subMsg, _ ) ->
            ( model, Cmd.none )

        ( TestMsg subMsg, TestPage subModel ) ->
            let
                ( updatedSubModel, subCmd ) =
                    Test.update subMsg subModel
            in
            ( { model | currentPage = TestPage updatedSubModel }, Cmd.map TestMsg subCmd )

        ( TestMsg subMsg, _ ) ->
            ( model, Cmd.none )

        ( TopBarMsg subMsg, _ ) ->
            let
                ( updatedSubModel, subCmd ) =
                    TopBar.update subMsg model.topBarModel
            in
            ( { model | topBarModel = updatedSubModel }, Cmd.map TopBarMsg subCmd )

        ( TestHistoryMsg subMsg, TestHistoryPage subModel ) ->
            let
                ( updatedSubModel, subCmd ) =
                    TestHistory.update subMsg subModel
            in
            ( { model | currentPage = TestHistoryPage updatedSubModel }, Cmd.map TestHistoryMsg subCmd )

        ( TestHistoryMsg subMsg, _ ) ->
            ( model, Cmd.none )

        ( WebSocketMsg subMsg, _ ) ->
            let
                event =
                    WebSocket.toEvent subMsg

                --
                --                a =
                --                    Debug.log "MAIN GOT" event
                cmd =
                    case event of
                        Ok ev ->
                            Cmd.batch
                                [ Jobs.handleEvent ev |> Cmd.map JobsMsg
                                , Job.handleEvent ev |> Cmd.map JobMsg
                                , Agents.handleEvent ev |> Cmd.map AgentsMsg
                                , Builds.handleEvent ev |> Cmd.map BuildsMsg
                                , Suites.handleEvent ev |> Cmd.map SuitesMsg
                                , ManageNewman.handleEvent ev |> Cmd.map ManageNewmanMsg
                                , JobConfigs.handleEvent ev |> Cmd.map JobConfigsMsg
                                , Home.handleEvent ev |> Cmd.map HomeMsg
                                ]

                        Err err ->
                            Cmd.none
            in
            ( model, cmd )


bodyWrapper : Html Msg -> Html Msg
bodyWrapper inner =
    div [ id "page-wrapper" ]
        [ div [ class "container-fluid" ]
            [ div [ class "row" ]
                [ div [ class "col-lg-12" ]
                    [ h1 [ class "page-header" ]
                        [ text "Header here" ]
                    ]
                ]
            , div [ class "row" ]
                [ inner
                ]
            ]
        ]


viewBody : Model -> Html Msg
viewBody model =
    case model.currentPage of
        SubmitNewJobPage ->
            SubmitNewJob.view model.submitNewJobModel |> Html.map SubmitNewJobMsg

        JobsPage ->
            Jobs.view model.jobsModel |> Html.map JobsMsg

        JobPage subModel ->
            Job.view subModel |> Html.map JobMsg

        BuildsPage ->
            Builds.view model.buildsModel |> Html.map BuildsMsg

        BuildPage subModel ->
            Build.view subModel |> Html.map BuildMsg

        AgentsPage ->
            Agents.view model.agentsModel |> Html.map AgentsMsg

        JobConfigsPage ->
            JobConfigs.view model.jobConfigsModel |> Html.map JobConfigsMsg

        NewJobConfigsPage ->
            NewJobConfig.view model.newJobConfigsModel |> Html.map NewJobConfigMsg

        JobConfigPage subModel ->
            JobConfig.view subModel |> Html.map JobConfigMsg

        SuitesPage ->
            Suites.view model.suitesModel |> Html.map SuitesMsg

        ManageNewmanPage ->
            ManageNewman.view model.manageNewmanModel |> Html.map ManageNewmanMsg

        SuitePage subModel ->
            Suite.view subModel |> Html.map SuiteMsg

        HomePage ->
            Home.view model.homeModel |> Html.map HomeMsg

        TestPage subModel ->
            Test.view subModel |> Html.map TestMsg

        TestHistoryPage subModel ->
            TestHistory.view subModel |> Html.map TestHistoryMsg


view : Model -> Html Msg
view model =
    let
        pages =
            [ ( "Home", "#" ), ( "Submit New Job", "#submit-new-job" ), ( "Jobs", "#jobs" ), ( "Builds", "#builds" ), ( "Agents", "#agents" ), ( "Suites", "#suites" ), ( "Job Configurations", "#jobConfigs" ), ( "Manage Newman", "#manageNewman" ) ]

        isActive page =
            UrlParser.parsePath
    in
    div [ id "wrapper" ]
        [ Bootstrap.CDN.stylesheet
        , nav [ class "navbar navbar-toggleable navbar-inverse bg-primary fixed-top", attribute "role" "navigation" ]
            [ div [ class "navbar-header" ]
                [ a [ class "navbar-brand", href "#" ]
                    [ text "Newman" ]
                ]
            , TopBar.view model.topBarModel |> Html.map TopBarMsg
            , div [ class "collapse navbar-collapse", attribute "style" "position: absolute;" ]
                [ ul [ class "nav  flex-column side-nav" ]
                    (List.indexedMap
                        (\index ( name, ref ) ->
                            li [ class "nav-item" ]
                                [ a [ class "nav-link", classList [ ( "active", routeToString model.currentRoute == ref ) ], href ref ]
                                    [ text name ]
                                ]
                        )
                        pages
                    )
                ]
            ]
        , viewBody model
        ]


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [         WebSocket.subscriptions model.webSocketModel |> Sub.map WebSocketMsg
                  ,
          SubmitNewJob.subscriptions model.submitNewJobModel |> Sub.map SubmitNewJobMsg
        , Jobs.subscriptions model.jobsModel |> Sub.map JobsMsg
        ]
