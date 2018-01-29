module Main exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Navigation exposing (..)
import Pages.Builds as Builds exposing (..)
import Pages.Jobs as Jobs exposing (..)
import Pages.SubmitNewJob as SubmitNewJob exposing (..)
import UrlParser exposing (..)


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


route : Parser (Route -> a) a
route =
    oneOf
        [ UrlParser.map HomeRoute (UrlParser.s "home")
        , UrlParser.map JobsRoute (UrlParser.s "jobs")
        , UrlParser.map SubmitNewJobRoute (UrlParser.s "submit-new-job")
        , UrlParser.map BuildsRoute (UrlParser.s "builds")
        ]


locFor : Location -> Msg
locFor location =
    parseHash route location
        |> GoTo


type alias Model =
    { currentRoute : Route
    , submitNewJobModel : SubmitNewJob.Model
    , jobsModel : Jobs.Model
    , buildsModel : Builds.Model
    }


type Msg
    = GoTo (Maybe Route)
    | SubmitNewJobMsg SubmitNewJob.Msg
    | JobsMsg Jobs.Msg
    | BuildsMsg Builds.Msg


init : Location -> ( Model, Cmd Msg )
init location =
    let
        tempRoute =
            case parseHash route location of
                Just route ->
                    route

                Nothing ->
                    HomeRoute

        ( submitNewJobModel, submitNewJobCmd ) =
            SubmitNewJob.init

        ( jobsModel, jobsCmd ) =
            Jobs.init

        ( buildsModel, buildsCmd ) =
            Builds.init
    in
    ( Model tempRoute submitNewJobModel jobsModel buildsModel
    , Cmd.batch
        [ submitNewJobCmd |> Cmd.map SubmitNewJobMsg
        , jobsCmd |> Cmd.map JobsMsg
        , buildsCmd |> Cmd.map BuildsMsg
        ]
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model.currentRoute ) of
        ( GoTo maybeRoute, _ ) ->
            case maybeRoute of
                Just route ->
                    ( { model | currentRoute = route }, Cmd.none )

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

        ( BuildsMsg buildMsg, _ ) ->
            let
                ( updatedBuildsModel, buildsCmd ) =
                    Builds.update buildMsg model.buildsModel
            in
            ( { model | buildsModel = updatedBuildsModel }, Cmd.map BuildsMsg buildsCmd )


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
            [ ( "Home", "#home" ), ( "Submit New Job", "#submit-new-job" ), ( "Jobs", "#jobs" ), ( "Builds", "#builds" ) ]
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
    case model.currentRoute of
        SubmitNewJobRoute ->
            SubmitNewJob.view model.submitNewJobModel |> Html.map SubmitNewJobMsg

        JobsRoute ->
            Jobs.view model.jobsModel |> Html.map JobsMsg

        BuildsRoute ->
            Builds.view model.buildsModel |> Html.map BuildsMsg

        HomeRoute ->
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
