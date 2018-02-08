module Pages.Home exposing (..)

import Bootstrap.Badge as Badge
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
import List.Extra


type Msg
    = GetDashboardDataCompleted (Result Http.Error DashboardBuilds)


type alias Model =
    { dashboardDataResponse : DashboardBuilds
    }



--NEW


init : ( Model, Cmd Msg )
init =
    ( { dashboardDataResponse = []
      }
    , getDashboardDataCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetDashboardDataCompleted result ->
            case result of
                Ok data ->
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
        [ div []
            [
            h2 [] [ text "History" ]
            , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover history-table" ]
                [ thead []
                    [ tr []
                        [ th [] [ text "Build" ]
                        , th [] [ text "Date" ]
                        , th []
                            [
                              Badge.badgeInfo [] [ text "Running" ]
                            , text " "
                            , Badge.badgeSuccess [] [ text "Done" ]
                            , text " "
                            , Badge.badgeDanger [] [ text "Broken" ]
                            , text " "
                            , Badge.badge [] [ text "Total" ]
                            , text " | Tests"
                            ]
                        , th [ align "center" ]
                            [
                            Badge.badgeInfo [] [ text "Running" ]
                            , text " "
                            , Badge.badgeSuccess [] [ text "Passed" ]
                            , text " "
                            , Badge.badgeDanger [] [ text "Failed" ]
                            , text " "
                            , Badge.badge [] [ text "Total" ]
                            , text " | Jobs"
                            ]
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

        buildStatus =
            build.buildStatus

        testsData =
            [ Badge.badgeInfo [] [ text <| toString buildStatus.runningTests ]
            , text " "
            , Badge.badgeSuccess [] [ text <| toString buildStatus.passedTests ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.failedTests ]
            , text " "
            , Badge.badge [] [ text <| toString buildStatus.totalTests ]
            ]

        jobsData =
            [
              Badge.badgeInfo [] [ text <| toString buildStatus.runningJobs ]
            , text " "
            , Badge.badgeSuccess [] [ text <| toString buildStatus.doneJobs ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.brokenJobs ]
            , text " "
            , Badge.badge [] [ text <| toString buildStatus.totalJobs ]
            ]
    in
    tr []
        [ td [] [ a [ href <| "#build/" ++ build.id ] [ text buildName ] ]
        , td [] [ text buildDate ]
        , td [ class "tests-data" ] testsData
        , td [] jobsData
        , td [] <| List.intersperse (text " ") <| List.map (\(name, id) -> a [ href <| "#suite/" ++ id] [ text name ]) <| List.Extra.zip build.buildStatus.suitesNames build.buildStatus.suitesIds
        ]
