module Main exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Navigation exposing (..)
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
    = SubmitNewJobRoute


route : Parser (Route -> a) a
route =
    oneOf
        [ UrlParser.map SubmitNewJobRoute (UrlParser.s "submit-new-job")
        ]


locFor : Location -> Msg
locFor location =
    parseHash route location
        |> GoTo


type alias Model =
    { currentRoute : Route
    }


type Msg
    = GoTo (Maybe Route)
    | GetBuildsAndSuitesCompleted (Result Http.Error BuildsAndSuites)


init : Location -> ( Model, Cmd Msg )
init location =
    let
        tempRoute =
            case (parseHash route location) of
                Just route ->
                    route

                Nothing ->
                    SubmitNewJobRoute

    in
        ( Model tempRoute, getBuildsAndSuitesCmd )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GoTo maybeRoute ->
            case maybeRoute of
                Just route ->
                    ( { model | currentRoute = route }, Cmd.none )

                Nothing ->
                    ( model, Cmd.none )

        GetBuildsAndSuitesCompleted result ->
            case result of
                Ok data ->
                    ( { model | buildsAndSuites = data }, Cmd.none )

                Err err ->
                    -- log error
                    ( model, Cmd.none )


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
    div [ class "collapse navbar-collapse navbar-ex1-collapse" ]
        [ ul [ class "nav navbar-nav side-nav" ]
            [ li [ class "active" ]
                [ a [ href "#first" ]
                    [ text "First Page" ]
                ]
            , li [ class "" ]
                [ a [ href "#second" ]
                    [ text "Second Page" ]
                ]
            , li [ class "" ]
                [ a [ href "#admin" ]
                    [ text "admin" ]
                ]
            ]
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
            viewSubmitNewJob model



--            bodyWrapper (text ("Hello there 2" ++ (toString model.currentRoute)))


view : Model -> Html Msg
view model =
    div [ id "wrapper" ]
        [ nav [ class "navbar navbar-inverse navbar-fixed-top", attribute "role" "navigation" ]
            [ topNavBar
            , leftNavBar
            ]
        , viewBody model
        ]


viewSubmitNewJob : Model -> Html Msg
viewSubmitNewJob model =
    let
        toOption data =
            option [ value data.id ] [ text data.name ]
    in
        div [ id "page-wrapper" ]
            [ div [ class "container-fluid" ]
                [ div [ class "row" ]
                    [ div [ class "col-lg-12" ]
                        [ h1 [ class "page-header" ]
                            [ text "Submit New Job" ]
                        ]
                    ]
                , div [ class "row" ]
                    [ select []
                        ([ option [ value "1" ] [ text "Select a Suite" ]
                         ]
                            ++ List.map toOption model.buildsAndSuites.suites
                        )
                    , br [] []
                    , select []
                        ([ option [ value "1" ] [ text "Select a Build" ]
                         ]
                            ++ List.map toOption model.buildsAndSuites.builds
                        )
                    ]
                ]
            ]



---

type alias Build =
    { id : String
    , name : String
    , branch : String
    , tags : List String
    }


type alias Suite =
    { id : String
    , name : String
    , customVariables : String
    }


type alias BuildsAndSuites =
    { suites : List Suite
    , builds : List Build
    }


getBuildsAndSuitesCmd : Cmd Msg
getBuildsAndSuitesCmd =
    Http.send GetBuildsAndSuitesCompleted getBuildsAndSuites


getBuildsAndSuites : Http.Request BuildsAndSuites
getBuildsAndSuites =
    Http.get ("http://localhost:8080/api/newman/all-builds-and-suites") buildsAndSuitesDecoder
