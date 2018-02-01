module Pages.Builds exposing (..)

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


type alias Model =
    { builds : PaginatedList Build
    , pageSize : Int
    }


type alias Build =
    { id : String
    , name : String
    , branch : String
    , buildTime : Int
    , tags : List String
    }


type alias Builds =
    List Build


type Msg
    = GetBuildsCompleted (Result Http.Error Builds)
    | First
    | Last
    | Next
    | Prev
    | GoTo Int


init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20
    in
    ( Model (Paginate.fromList pageSize []) pageSize, getBuildsCmd )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetBuildsCompleted result ->
            case result of
                Ok buildsFromResult ->
                    ( { model | builds = Paginate.fromList model.pageSize buildsFromResult }, Cmd.none )

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


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ button [ onClick First, disabled <| Paginate.isFirst model.builds ] [ text "<<" ]
            , button [ onClick Prev, disabled <| Paginate.isFirst model.builds ] [ text "<" ]
            ]

        nextButtons =
            [ button [ onClick Next, disabled <| Paginate.isLast model.builds ] [ text ">" ]
            , button [ onClick Last, disabled <| Paginate.isLast model.builds ] [ text ">>" ]
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
        [ h2 [ class "text-center" ] [ text "Builds" ]
        , table [ width 1200 ]
            (List.append
                [ tr []
                    [ td [] [ text "Build" ]
                    , td [] [ text "Tags" ]
                    , td [] [ text "Id" ]
                    , td [] [ text "Build Date" ]
                    ]
                ]
                (List.map viewItem <| Paginate.page model.builds)
            )
        ]
            ++ prevButtons
            ++ [ span [] <| Paginate.pager pagerButtonView model.builds ]
            ++ nextButtons


viewItem : Build -> Html msg
viewItem build =
    let
        buildName =
            build.name ++ "(" ++ build.branch ++ ")"

        buildDate =
            Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat build.buildTime))

        buildTags =
            String.join "," build.tags
    in
    tr []
        [ td [] [ text buildName ]
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


decodeBuilds : Json.Decode.Decoder Builds
decodeBuilds =
    Json.Decode.field "values" (Json.Decode.list decodeBuild)


decodeBuild : Json.Decode.Decoder Build
decodeBuild =
    decode Build
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "branch" Json.Decode.string
        |> Json.Decode.Pipeline.required "buildTime" Json.Decode.int
        |> Json.Decode.Pipeline.required "tags" (Json.Decode.list Json.Decode.string)
