module Pages.Builds exposing (..)

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
    { allBuilds : Builds
    , builds : PaginatedBuilds
    , pageSize : Int
    }


type Msg
    = GetBuildsCompleted (Result Http.Error Builds)
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
    ( { allBuilds = []
      , builds = Paginate.fromList pageSize []
      , pageSize = pageSize
      }
    , getBuildsCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetBuildsCompleted result ->
            case result of
                Ok buildsFromResult ->
                    ( { model | builds = Paginate.fromList model.pageSize buildsFromResult, allBuilds = buildsFromResult }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        First ->
            ( { model | builds = Paginate.first model.builds }, Cmd.none )

        Last ->
            ( { model | builds = Paginate.last model.builds }, Cmd.none )

        Next ->
            ( { model | builds = Paginate.next model.builds }, Cmd.none )

        Prev ->
            ( { model | builds = Paginate.prev model.builds }, Cmd.none )

        GoTo i ->
            ( { model | builds = Paginate.goTo i model.builds }, Cmd.none )

        FilterQuery query ->
            let
                filteredList =
                    List.filter (filterQuery query) model.allBuilds
            in
            ( { model | builds = Paginate.fromList model.pageSize filteredList }
            , Cmd.none
            )


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.builds ) ], onClick First ]
                [ button [ class "page-link" ] [ text "«" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.builds ) ], onClick Prev ]
                [ button [ class "page-link" ] [ text "‹" ]
                ]
            ]

        nextButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.builds ) ], onClick Next ]
                [ button [ class "page-link" ] [ text "›" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.builds ) ], onClick Last ]
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

                --                        <li class="page-item"><a class="page-link" href="#">1</a></li>
                False ->
                    li [ class "page-item", onClick <| GoTo index ]
                        [ button [ class "page-link" ] [ text <| toString index ]
                        ]

        pagination =
            nav []
                [ ul [ class "pagination " ]
                    (prevButtons
                        ++ Paginate.pager pagerButtonView model.builds
                        ++ nextButtons
                    )
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Builds" ]
        , div []
            [ div [ class "form-inline" ]
                [ div [ class "form-group" ] [ FormInput.text [ FormInput.onInput FilterQuery, FormInput.placeholder "Filter" ] ]
                , div [ class "form-group" ] [ pagination ]
                ]
            , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
                [ thead []
                    [ tr []
                        [ th [] [ text "Build" ]
                        , th [] [ text "Tags" ]
                        , th [] [ text "Id" ]
                        , th [] [ text "Build Date" ]
                        ]
                    ]
                , tbody [] (List.map viewBuild (Paginate.page model.builds))
                ]
            , pagination
            ]
        ]


viewBuild : Build -> Html msg
viewBuild build =
    let
        buildName =
            build.name ++ "(" ++ build.branch ++ ")"

        buildDate =
            Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat build.buildTime))

        buildTags =
            String.join "," build.tags
    in
    tr []
        [ td [] [ a [ href <| "#build/" ++ build.id ] [ text buildName ] ]
        , td [] [ text buildTags ]
        , td [] [ text build.id ]
        , td [] [ text buildDate ]
        ]


getBuildsCmd : Cmd Msg
getBuildsCmd =
    Http.send GetBuildsCompleted getBuilds


getBuilds : Http.Request Builds
getBuilds =
    Http.get "/api/newman/build" decodeBuilds


filterQuery : String -> Build -> Bool
filterQuery query build =
    if
        String.length query
            == 0
            || String.startsWith query build.id
            || String.startsWith query build.name
            || String.startsWith query build.branch
    then
        True
    else
        False
