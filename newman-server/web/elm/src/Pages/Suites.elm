module Pages.Suites exposing (..)

import Date
import Date.Format
import Html exposing (Html, button, div, h2, span, table, td, text, tr)
import Html.Attributes exposing (class, disabled, style, width)
import Html.Events exposing (onClick)
import Http
import Json.Decode exposing (at, decodeString, field, maybe)
import Json.Decode.Pipeline exposing (decode)
import Paginate exposing (..)


type alias Model =
    { suites : PaginatedList Suite
    , pageSize : Int
    }


type alias Suites =
    List Suite


type Msg
    = GetSuitesCompleted (Result Http.Error Suites)
    | First
    | Last
    | Next
    | Prev
    | GoTo Int


type alias Suite =
    { id : String
    , name : String
    , customVariables : String
    }


init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20

        a =
            Debug.log "suites init"
    in
    ( Model (Paginate.fromList pageSize []) pageSize, getSuitesCmd )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetSuitesCompleted result ->
            case result of
                Ok suitesFromResult ->
                    ( { model | suites = Paginate.fromList model.pageSize suitesFromResult }, Cmd.none )

                Err err ->
                    let
                        a =
                            Debug.log "onGetSuitesCompleted" err
                    in
                    ( model, Cmd.none )

        First ->
            ( { model | suites = Paginate.first model.suites }, Cmd.none )

        Last ->
            ( { model | suites = Paginate.last model.suites }, Cmd.none )

        Next ->
            ( { model | suites = Paginate.next model.suites }, Cmd.none )

        Prev ->
            ( { model | suites = Paginate.prev model.suites }, Cmd.none )

        GoTo i ->
            ( { model | suites = Paginate.goTo i model.suites }, Cmd.none )


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ button [ onClick First, disabled <| Paginate.isFirst model.suites ] [ text "<<" ]
            , button [ onClick Prev, disabled <| Paginate.isFirst model.suites ] [ text "<" ]
            ]

        nextButtons =
            [ button [ onClick Next, disabled <| Paginate.isLast model.suites ] [ text ">" ]
            , button [ onClick Last, disabled <| Paginate.isLast model.suites ] [ text ">>" ]
            ]

        pagerButtonView index isActive =
            button
                [ style
                    [ ( "font-weight"
                      , if isActive then
                            "bold"
                        else
                            "normal"
                      )
                    ]
                , onClick <| GoTo index
                ]
                [ text <| toString index ]
    in
    div [ class "container" ] <|
        [ h2 [ class "text-center" ] [ text "suites" ]
        , table [ width 1500 ]
            (List.append
                [ tr []
                    [ td [] [ text "Name" ]
                    , td [] [ text "Id" ]
                    , td [] [ text "Custom Variables" ]
                    ]
                ]
                (List.map viewItem <| Paginate.page model.suites)
            )
        ]
            ++ prevButtons
            ++ [ span [] <| Paginate.pager pagerButtonView model.suites ]
            ++ nextButtons


viewItem : Suite -> Html msg
viewItem suite =
    tr []
        [ td [] [ text suite.name ]
        , td [] [ text suite.id ]
        , td [] [ text suite.customVariables ]
        ]


getSuitesCmd : Cmd Msg
getSuitesCmd =
    Http.send GetSuitesCompleted getSuites


getSuites : Http.Request Suites
getSuites =
    Http.get "/api/newman/suite?all=true" decodeSuites


decodeSuites : Json.Decode.Decoder Suites
decodeSuites =
    Json.Decode.field "values" (Json.Decode.list decodeSuite)


decodeSuite : Json.Decode.Decoder Suite
decodeSuite =
    decode Suite
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "customVariables" Json.Decode.string
