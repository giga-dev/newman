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
    | GetBuildsAndSuitesCompleted (Result Http.Error Jobs)


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
        ( Model tempRoute, Cmd.none )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GoTo maybeRoute ->
            case maybeRoute of
                Just route ->
                    ( { model | currentRoute = route }, Cmd.none )

                Nothing ->
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
                    [ option [ value "1" ] [ text "Select a Suite" ]
                    , option [ value "1" ] [ text "first" ]
                    , option [ value "1" ] [ text "first" ]
                    ]
                , br [] []
                , select []
                    [ option [ value "1" ] [ text "Select a Build" ]
                    , option [ value "1" ] [ text "first" ]
                    , option [ value "1" ] [ text "first" ]
                    ]
                ]
            ]
        ]



----


type alias Build =
    { id : String
    , name : String
    , branch : String
    , tags : List String
    }


getBuildsAndSuitesCmd : Cmd Msg
getBuildsAndSuitesCmd =
    Http.send GetBuildsAndSuitesCompleted getBuildsAndSuites


getBuildsAndSuites : Int -> Http.Request Jobs
getBuildsAndSuites limit =
    Http.get ("http://localhost:8080/api/newman/job?limit=" ++ (toString limit) ++ "&orderBy=-submitTime") buildsAndSuitesDecoder
