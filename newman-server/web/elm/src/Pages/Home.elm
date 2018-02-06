module Pages.Home exposing (..)

import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Date
import Date.Format
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Http exposing (..)
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import Maybe exposing (withDefault)
import Utils.Types exposing (..)


type Msg
    = GetDashboardDataCompleted (Result Http.Error DashboardBuilds)


type alias Model =
    { dashboardDataResponse : DashboardBuilds
    }



--NEW


init : ( Model, Cmd Msg )
init =
    (     { dashboardDataResponse = []
          }, getDashboardDataCmd )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        a =
            Debug.log "home.update" ("was called " ++ toString msg)
    in
    case msg of
        GetDashboardDataCompleted result ->
            case result of
                Ok data ->
                    let
                        a =
                            Debug.log "home.update" ("was called in >Data" ++ toString data)
                    in
                    ( { model | dashboardDataResponse = data }, Cmd.none )

                Err err ->
                    -- log error
                    ( model, Cmd.none )


getDashboardDataCmd : Cmd Msg
getDashboardDataCmd =
    Http.send GetDashboardDataCompleted getDashboardData


getDashboardData : Http.Request DashboardBuilds
getDashboardData =
    Http.get "/api/newman/dashboard" decodeDashboardBuilds


--dashboardDecoder : Json.Decode.Decoder DashboardBuilds
--dashboardDecoder =
--    decode DashboardBuilds
--        |> Json.Decode.Pipeline.required "historyBuild" decodeDashboardBuilds


view : Model -> Html Msg
view model =
    let
        a =
            Debug.log "home.view" ("was called " ++ toString model.dashboardDataResponse)

        toOption data =
            option [ value data.id ] [ text data.name ]

        submittedFutureJobFormat jobId =
            [ text ("submitted future job eith id " ++ jobId) ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Home Page Under constrction - History" ]
        , div []
            [ table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
                [ thead []
                    [ tr []
                        [ th [] [ text "Build" ]
                        , th [] [ text "Date" ]
                        , th [] [ text "Tests" ]
                        , th [] [ text "Jobs" ]
                        , th [] [ text "Suites" ]
                        ]
                    ]
                , tbody [] (List.map viewHistory model.dashboardDataResponse)
                ]
            ]
        ]



--api/newman/dashboard
--viewHistory :  Build -> Html msg
--viewHistory history =
--    let
--       a = "ddd"
--    in
--    tr []
--        [ td [] [ text "gfsdfds" ]
--        , td [] [ text "buildTags" ]
--        , td [] [ text "buildTagssdfsdf" ]
--        , td [] [ text "build.id" ]
--        , td [] [ text "buildDate" ]
--        ]


viewHistory : DashboardBuild -> Html msg
viewHistory build =
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
