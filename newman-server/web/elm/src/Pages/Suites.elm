module Pages.Suites exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Bootstrap.Progress as Progress exposing (..)
import Date exposing (Date)
import Date.Extra.Config.Config_en_au exposing (config)
import Date.Extra.Duration as Duration
import Date.Extra.Format as Format exposing (format, formatUtc, isoMsecOffsetFormat)
import Date.Format
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import Paginate exposing (..)
import Task
import Time exposing (Time)
import Utils.Types exposing (..)


type alias Model =
    { allSuites : Suites
    , suites : PaginatedSuites
    , pageSize : Int
    }


type Msg
    = GetSuitesCompleted (Result Http.Error Suites)
    | First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String


init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20
    in
    ({ allSuites = []
    , suites = Paginate.fromList pageSize []
    , pageSize = pageSize
    }, getSuitesCmd)


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetSuitesCompleted result ->
            case result of
                Ok suitesFromResult ->
                    ( { model | suites = Paginate.fromList model.pageSize suitesFromResult, allSuites = suitesFromResult }, Cmd.none )

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

        FilterQuery query ->
            let
                filteredList =
                    List.filter (filterQuery query) model.allSuites
            in
            ( { model | suites = Paginate.fromList model.pageSize filteredList }
            , Cmd.none
            )


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.suites ) ], onClick First ]
                [ button [ class "page-link" ] [ text "«" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.suites ) ], onClick Prev ]
                [ button [ class "page-link" ] [ text "‹" ]
                ]
            ]

        nextButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.suites ) ], onClick Next ]
                [ button [ class "page-link" ] [ text "›" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.suites ) ], onClick Last ]
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
                        ++ Paginate.pager pagerButtonView model.suites
                        ++ nextButtons
                    )
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Suites" ]
        , div []
            [ Form.formInline []
                [ Form.group [] [ FormInput.text [ FormInput.onInput FilterQuery, FormInput.placeholder "Filter" ] ]
                , Form.group [] [ pagination ]
                ]
            , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
                [ thead []
                    [ tr []
                        [ th [] [ text "Name" ]
                        , th [] [ text "Id" ]
                        , th [] [ text "Custom" ]
                        ]
                    ]
                , tbody [] (List.map viewSuite (Paginate.page model.suites))
                ]
            , pagination
            ]
        ]

viewSuite : Suite -> Html msg
viewSuite suite =
    tr []
        [ a [ href <| "#suite/" ++ suite.id] [ text suite.name ]
        , td [] [ text suite.id ]
        , td [] [ text suite.customVariables ]
        ]


getSuitesCmd : Cmd Msg
getSuitesCmd =
    Http.send GetSuitesCompleted getSuites


getSuites : Http.Request Suites
getSuites =
    Http.get "/api/newman/suite?all=true" decodeSuites

filterQuery : String -> Suite -> Bool
filterQuery query suite =
    if
        String.length query
            == 0
            || String.startsWith query suite.name
            || String.startsWith query suite.id
    then
        True
    else
        False