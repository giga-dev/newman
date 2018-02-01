module Main exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Navigation exposing (..)
import Pages.Agents as Agents exposing (..)
import Pages.Builds as Builds exposing (..)
import Pages.Jobs as Jobs exposing (..)
import Pages.SubmitNewJob as SubmitNewJob exposing (..)
import Pages.Suites as Suites exposing (..)
import UrlParser exposing (..)
import Pages.Job as Job exposing (..)


main : Program Never Model Msg
main =
    Navigation.program locFor
        { init = init
        , view = view
        , update = update
        , subscriptions = \_ -> Sub.none
        }


type Route
    = HomeRoute
    | SubmitNewJobRoute
    | JobsRoute
    | BuildsRoute
    | AgentsRoute
    | SuitesRoute
    | JobRoute JobId


type Page
    = HomePage
    | SubmitNewJobPage
    | JobsPage
    | BuildsPage
    | AgentsPage
    | SuitesPage
    | JobPage Job.Model


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
            JobPage (Job.Model Nothing)


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
        ]


locFor : Location -> Msg
locFor location =
    parseHash route location
        |> GoTo


type alias Model =
    { currentPage : Page
    , submitNewJobModel : SubmitNewJob.Model
    , jobsModel : Jobs.Model
    , buildsModel : Builds.Model
    , agentsModel : Agents.Model
    , suitesModel : Suites.Model
    }


type Msg
    = GoTo (Maybe Route)
    | SubmitNewJobMsg SubmitNewJob.Msg
    | JobsMsg Jobs.Msg
    | BuildsMsg Builds.Msg
    | AgentsMsg Agents.Msg
    | SuitesMsg Suites.Msg
    | JobMsg Job.Msg


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

        moreCmd =
            case currentRoute of
                JobRoute id ->
                    Job.getJobInfoCmd id |> Cmd.map JobMsg

                _ ->
                    Cmd.none
    in
        ( Model currentPage submitNewJobModel jobsModel buildsModel agentsModel suitesModel
        , Cmd.batch
            [ submitNewJobCmd |> Cmd.map SubmitNewJobMsg
            , jobsCmd |> Cmd.map JobsMsg
            , buildsCmd |> Cmd.map BuildsMsg
            , agentsCmd |> Cmd.map AgentsMsg
            , suitesCmd |> Cmd.map SuitesMsg
            , moreCmd
            ]
        )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model.currentPage ) of
        ( GoTo maybeRoute, _ ) ->
            case maybeRoute of
                Just route ->
                    case route of
                        JobRoute id ->
                            ( { model | currentPage = routeToPage route }, Job.getJobInfoCmd id |> Cmd.map JobMsg )

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

        ( BuildsMsg buildMsg, _ ) ->
            let
                ( updatedBuildsModel, buildsCmd ) =
                    Builds.update buildMsg model.buildsModel
            in
                ( { model | buildsModel = updatedBuildsModel }, Cmd.map BuildsMsg buildsCmd )

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

        BuildsPage ->
            Builds.view model.buildsModel |> Html.map BuildsMsg

        JobPage subModel ->
            Job.view subModel |> Html.map JobMsg

        AgentsPage ->
            Agents.view model.agentsModel |> Html.map AgentsMsg

        SuitesPage ->
            Suites.view model.suitesModel |> Html.map SuitesMsg

        HomePage ->
            div [ id "page-wrapper" ]
                [ div [ class "container-fluid" ]
                    [ div [ class "row" ]
                        [ div [ class "col-lg-12" ]
                            [ h1 [ class "page-header" ]
                                [ text "Home Page" ]
                            ]
                        ]
                    ]
                ]


view : Model -> Html Msg
view model =
    div [ id "wrapper" ]
        [ nav [ class "navbar navbar-inverse navbar-fixed-top", attribute "role" "navigation" ]
            [ topNavBar
            , leftNavBar
            ]
        , viewBody model
        ]
