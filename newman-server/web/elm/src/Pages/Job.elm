module Pages.Job exposing (..)

import Bootstrap.Badge as Badge
import Bootstrap.Button as Button
import Bootstrap.Progress as Progress exposing (..)
import Date
import Date.Format
import Dict
import Html exposing (..)
import Html.Attributes exposing (..)
import Http exposing (..)
import Json.Decode
import Json.Decode.Pipeline exposing (decode)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)


type alias Model =
    { maybeJob : Maybe Job
    , collapseState : CollapseState
    }


type CollapseState
    = Hidden
    | Shown


type Msg
    = GetJobInfoCompleted (Result Http.Error Job)
    | ToggleButton



-- external


parseJobId : Parser (String -> a) a
parseJobId =
    UrlParser.string


initModel : JobId -> Model
initModel jobId =
    { maybeJob = Nothing
    , collapseState = Hidden
    }


initCmd : JobId -> Cmd Msg
initCmd jobId =
    getJobInfoCmd jobId


viewHeader : Model -> Job -> Html Msg
viewHeader model job =
    let
        buildRow =
            tr []
                [ td []
                    [ text "Build"
                    ]
                , td []
                    [ a [ href <| "#build/" ++ job.build.id ] [ text <| job.build.name ++ " (" ++ job.build.branch ++ ")" ]
                    ]
                ]

        progressRow =
            viewRow
                ( "Progress"
                , Progress.progress
                    [ Progress.value <| toFloat <| (job.runningTests + job.failedTests + job.passedTests) * 100 // job.totalTests
                    , Progress.label <| toString <| (job.runningTests + job.failedTests + job.passedTests) * 100 // job.totalTests
                    ]
                )

        dateFormat maybeDate =
            case maybeDate of
                Just date ->
                    Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat date))

                Nothing ->
                    "N/A"

        stateRow =
            let
                badge =
                    case job.state |> toJobState of
                        BROKEN ->
                            Badge.badgeDanger

                        DONE ->
                            Badge.badgeSuccess

                        RUNNING ->
                            Badge.badgeInfo

                        PAUSED ->
                            Badge.badgeWarning

                        READY ->
                            Badge.badge
            in
            viewRow
                ( "State"
                , badge [] [ text job.state ]
                )

        testsStatus =
            [ Badge.badgeInfo [] [ text <| toString job.runningTests ]
            , text "/ "
            , Badge.badgeSuccess [] [ text <| toString job.passedTests ]
            , text "/ "
            , Badge.badgeDanger [] [ text <| toString job.failedTests ]
            , text "/ "
            , Badge.badge [] [ text <| toString job.totalTests ]
            ]

        jobToT ( key, val ) =
            li []
                [ a [ href val ] [ text key ]
                , text " "
                , a [ href <| val ++ "?download=true" ] [ text "[Download]" ]
                ]

        jobSetupLogsData =
            div [ classList [ ( "collapse", model.collapseState == Hidden ), ( "collapse.show", model.collapseState == Shown ) ] ]
                [ ul [] <|
                    List.map jobToT
                        (Dict.toList job.jobSetupLogs)
                ]

        jobSetupButton =
            let
                tt =
                    case model.collapseState of
                        Hidden ->
                            "ion-chevron-down"

                        Shown ->
                            "ion-chevron-up"
            in
            Button.button [ Button.success, Button.small, Button.onClick ToggleButton ] [ span [ class tt ] [] ]

        jobSetupLogs =
            div []
                [ jobSetupButton
                , jobSetupLogsData
                ]

        headerRows =
            [ ( "Suite", text job.suiteName )
            , ( "Submit Time", text <| dateFormat <| Just job.submitTime )
            , ( "Start Time", text <| dateFormat job.startTime )
            , ( "End Time", text <| dateFormat job.endTime )
            , ( "# Agents", text <| toString <| List.length job.agents )
            , ( "# Prep. Agents", text <| toString <| List.length job.preparingAgents )
            , ( "Submitted by", text <| job.submittedBy )
            , ( "Status", div [] testsStatus )
            , ( "Job Setup Logs", jobSetupLogs )
            ]

        viewRow ( name, value ) =
            tr []
                [ td [ width 130 ] [ text name ]
                , td [] [ value ]
                ]
    in
    table [ class "job-view" ] <|
        [ buildRow
        , stateRow
        , progressRow
        ]
            ++ List.map
                viewRow
                headerRows


view : Model -> Html Msg
view model =
    case model.maybeJob of
        Just job ->
            div [ class "container-fluid" ] <|
                [ h2 [ class "text" ] [ text <| "Details for job " ++ job.id ]
                , viewHeader model job
                ]

        Nothing ->
            div []
                [ text "Loading Job..."
                ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        d =
            Debug.log "Job.update" "was called"
    in
    case msg of
        GetJobInfoCompleted result ->
            case result of
                Ok data ->
                    ( { model | maybeJob = Just data }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        ToggleButton ->
            let
                newState =
                    case model.collapseState of
                        Hidden ->
                            Shown

                        Shown ->
                            Hidden
            in
            ( { model | collapseState = newState }, Cmd.none )


getJobInfoCmd : JobId -> Cmd Msg
getJobInfoCmd jobId =
    Http.send GetJobInfoCompleted <|
        Http.get ("/api/newman/job/" ++ jobId) decodeJob


