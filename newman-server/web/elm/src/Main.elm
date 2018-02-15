module Main exposing (..)

import Bootstrap.CDN exposing (stylesheet)
import Bootstrap.Navbar as Navbar
import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Navigation exposing (Location)
import Pages.Agents as Agents
import Pages.Build as Build
import Pages.Builds as Builds
import Pages.Home as Home
import Pages.Job as Job
import Pages.Jobs as Jobs
import Pages.SubmitNewJob as SubmitNewJob
import Pages.Suite as Suite
import Pages.Suites as Suites
import Pages.Test as Test
import Pages.TestHistory as TestHistory
import Task
import UrlParser exposing (..)
import Utils.Types exposing (BuildId, JobId, SuiteId, TestId)
import Utils.WebSocket as WebSocket exposing (Event(CreatedJob))
import Views.JobsTable
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
    | JobRoute JobId
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

        JobRoute id ->
            JobPage <| Job.initModel id

        BuildRoute id ->
            BuildPage (Build.Model Nothing Nothing 10)

        SuiteRoute id ->
            SuitePage (Suite.Model Nothing)

        TestRoute id ->
            TestPage <| Test.initModel

        TestHistoryRoute id ->
            TestHistoryPage <| TestHistory.initModel


route : Parser (Route -> a) a
route =
    oneOf
        [ UrlParser.map HomeRoute (UrlParser.s "home")
        , UrlParser.map JobsRoute (UrlParser.s "jobs")
        , UrlParser.map SubmitNewJobRoute (UrlParser.s "submit-new-job")
        , UrlParser.map BuildsRoute (UrlParser.s "builds")
        , UrlParser.map AgentsRoute (UrlParser.s "agents")
        , UrlParser.map SuitesRoute (UrlParser.s "suites")
        , UrlParser.map JobRoute (UrlParser.s "job" </> Job.parseJobId)
        , UrlParser.map BuildRoute (UrlParser.s "build" </> Build.parseBuildId)
        , UrlParser.map SuiteRoute (UrlParser.s "suite" </> Suite.parseSuiteId)
        , UrlParser.map TestRoute (UrlParser.s "test" </> UrlParser.string)
        , UrlParser.map TestHistoryRoute (UrlParser.s "test-history" </> UrlParser.string)
        ]


locFor : Location -> Msg
locFor location =
    parseHash route location
        |> GoTo


type alias Model =
    { currentPage : Page
    , navbarState : Navbar.State
    , submitNewJobModel : SubmitNewJob.Model
    , jobsModel : Jobs.Model
    , buildsModel : Builds.Model
    , agentsModel : Agents.Model
    , homeModel : Home.Model
    , suitesModel : Suites.Model
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
    | HomeMsg Home.Msg
    | JobMsg Job.Msg
    | NavbarMsg Navbar.State
    | BuildMsg Build.Msg
    | SuiteMsg Suite.Msg
    | TestMsg Test.Msg
    | TopBarMsg TopBar.Msg
    | TestHistoryMsg TestHistory.Msg
    | WebSocketMsg WebSocket.Msg


init : Location -> ( Model, Cmd Msg )
init location =
    let
        currentRoute =
            case parseHash route location of
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

        ( homeModel, homeCmd ) =
            Home.init

        ( topBarModel, topBarCmd ) =
            TopBar.init

        moreCmd =
            case currentRoute of
                JobRoute id ->
                    Job.initCmd id |> Cmd.map JobMsg

                BuildRoute id ->
                    Build.getBuildInfoCmd id |> Cmd.map BuildMsg

                SuiteRoute id ->
                    Suite.getSuiteInfoCmd id |> Cmd.map SuiteMsg

                TestRoute id ->
                    Test.getTestDataCmd id |> Cmd.map TestMsg

                TestHistoryRoute id ->
                    TestHistory.initCmd id |> Cmd.map TestHistoryMsg

                _ ->
                    Cmd.none

        ( navbarState, navbarCmd ) =
            Navbar.initialState NavbarMsg
    in
    ( { currentPage = currentPage
      , navbarState = navbarState
      , submitNewJobModel = submitNewJobModel
      , jobsModel = jobsModel
      , buildsModel = buildsModel
      , agentsModel = agentsModel
      , homeModel = homeModel
      , suitesModel = suitesModel
      , topBarModel = topBarModel
      , webSocketModel = WebSocket.initModel location
      }
    , Cmd.batch
        [ submitNewJobCmd |> Cmd.map SubmitNewJobMsg
        , jobsCmd |> Cmd.map JobsMsg
        , buildsCmd |> Cmd.map BuildsMsg
        , agentsCmd |> Cmd.map AgentsMsg
        , homeCmd |> Cmd.map HomeMsg
        , suitesCmd |> Cmd.map SuitesMsg
        , topBarCmd |> Cmd.map TopBarMsg
        , moreCmd
        ]
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model.currentPage ) of
        ( NavbarMsg state, _ ) ->
            ( { model | navbarState = state }, Cmd.none )

        ( GoTo maybeRoute, _ ) ->
            case maybeRoute of
                Just route ->
                    case route of
                        JobRoute id ->
                            ( { model | currentPage = routeToPage route }, Job.getJobInfoCmd id |> Cmd.map JobMsg )

                        BuildRoute id ->
                            ( { model | currentPage = routeToPage route }, Build.getBuildInfoCmd id |> Cmd.map BuildMsg )

                        SuiteRoute id ->
                            ( { model | currentPage = routeToPage route }, Suite.getSuiteInfoCmd id |> Cmd.map SuiteMsg )

                        TestRoute id ->
                            ( { model | currentPage = routeToPage route }, Test.getTestDataCmd id |> Cmd.map TestMsg )

                        TestHistoryRoute id ->
                            ( { model | currentPage = routeToPage route }, TestHistory.getTestHistory id |> Cmd.map TestHistoryMsg )

                        _ ->
                            ( { model | currentPage = routeToPage route }, Cmd.none )

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
                                ]

                        Err err ->
                            Cmd.none
            in
            ( model, cmd )


topNavBar : Html Msg
topNavBar =
    div [ class "navbar-header" ]
        [ button [ class "navbar-toggle", attribute "data-target" ".navbar-ex1-collapse", attribute "data-toggle" "collapse", type_ "button" ]
            [ span [ class "icon-bar" ]
                []
            , span [ class "icon-bar" ]
                []
            , span [ class "icon-bar" ]
                []
            ]
        , a [ class "navbar-brand", href "index" ]
            [ text "Newman" ]
        ]


leftNavBar : Html Msg
leftNavBar =
    let
        pages =
            [ ( "Home", "#home" ), ( "Submit New Job", "#submit-new-job" ), ( "Jobs", "#jobs" ), ( "Builds", "#builds" ), ( "Agents", "#agents" ), ( "Suites", "#suites" ) ]
    in
    div [ class "collapse navbar-collapse navbar-ex1-collapse" ]
        [ ul [ class "nav navbar-nav side-nav" ]
            (List.map
                (\( name, ref ) ->
                    li [ class "" ]
                        [ a [ href ref ]
                            [ text name ]
                        ]
                )
                pages
            )
        ]


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

        SuitesPage ->
            Suites.view model.suitesModel |> Html.map SuitesMsg

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
            [ ( "Home", "#home" ), ( "Submit New Job", "#submit-new-job" ), ( "Jobs", "#jobs" ), ( "Builds", "#builds" ), ( "Agents", "#agents" ), ( "Suites", "#suites" ) ]
    in
    div [ id "wrapper" ]
        [ Bootstrap.CDN.stylesheet
        , Navbar.config NavbarMsg
            |> Navbar.inverse
            |> Navbar.fixTop
            |> Navbar.brand [ href "#" ] [ text "Newman" ]
            --            |> Navbar.items
            --                [ Navbar.itemLinkActive [ href "#" ] [ text "Home" ]
            --                , Navbar.itemLink [ href "#" ] [ text "Home22" ]
            --                ]
            |> Navbar.customItems
                [ Navbar.customItem (TopBar.view model.topBarModel |> Html.map TopBarMsg)
                , Navbar.customItem
                    (ul
                        [ class "nav navbar-nav side-nav" ]
                        (List.map
                            (\( name, ref ) ->
                                li [ class "nav-item" ]
                                    [ a [ class "nav-link", href ref ]
                                        [ text name ]
                                    ]
                            )
                            pages
                        )
                    )
                ]
            |> Navbar.view model.navbarState

        {-

           [ ul [ class "nav navbar-nav side-nav" ]
                       (List.map
                           (\( name, ref ) ->
                               li [ class "" ]
                                   [ a [ href ref ]
                                       [ text name ]
                                   ]
                           )
                           pages
                       )
                   ]
        -}
        --                [ Navbar.itemLink [ href "#" ] [ text "Item 1" ]
        --                , Navbar.itemLink [ href "#" ] [ text "Item 2" ]
        --                ]
        {- , nav [ class "navbar navbar-inverse navbar-fixed-top", attribute "role" "navigation" ]
           [ topNavBar
           , leftNavBar
           ]
        -}
        , viewBody model
        ]


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Navbar.subscriptions model.navbarState NavbarMsg
        , WebSocket.subscriptions model.webSocketModel |> Sub.map WebSocketMsg
        , SubmitNewJob.subscriptions model.submitNewJobModel |> Sub.map SubmitNewJobMsg
        ]
