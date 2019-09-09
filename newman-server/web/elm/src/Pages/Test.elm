module Pages.Test exposing (..)

import Bootstrap.Badge as Badge
import Date
import DateFormat
import Dict
import Html exposing (..)
import Html.Attributes exposing (..)
import Http
import Utils.Common as Common
import Utils.Types exposing (Test, TestId, TestStatus(..), agentGroupTestFormat, decodeTest, testStatusToString)
import Utils.WebSocket as WebSocket exposing (..)


type alias Model =
    { test : Maybe Test }


type Msg
    = GetTestDataCompleted (Result Http.Error Test)
    | WebSocketEvent WebSocket.Event


initModel : Model
initModel =
    { test = Nothing }


initCmd : TestId -> Cmd Msg
initCmd testId =
    getTestDataCmd testId


view : Model -> Html Msg
view model =
    case model.test of
        Nothing ->
            div [] [ text "Loading data..." ]

        Just test ->
            div [ class "container-fluid" ] <|
                [ h2 [ class "text" ] [ text <| "Details for test " ++ String.join " " test.arguments ]
                , viewTest test
                ]


viewTest : Test -> Html Msg
viewTest test =
    let
        viewRow ( name, value ) =
            tr []
                [ td [ width 180 ] [ text name ]
                , td [] [ value ]
                ]

        toBadge status =
            case status of
                TEST_RUNNING ->
                    Badge.badgeInfo

                TEST_FAIL ->
                    Badge.badgeDanger

                TEST_SUCCESS ->
                    Badge.badgeSuccess

                TEST_PENDING ->
                    Badge.badge

        logsRow =
            let
                toLogRow ( key, val ) =
                    li [] [ a [ href val, target "_blank" ] [ text key ], text " ", a [ href <| val ++ "?download=true" ] [ text "[Download]" ] ]
            in
            ul [ style [ ( "font-size", "14px" ) ] ] <|
                List.map
                    toLogRow
                <|
                    Dict.toList test.logs

        formatDate maybe =
            case maybe of
                Just date ->
                    DateFormat.format Common.dateTimeDateFormat <| Date.fromTime <| toFloat date

                Nothing ->
                    "N/A"

        historyStats =
            let
                delimiter =
                    "_"

                splitted =
                    String.split delimiter test.historyStats

                shorten txt =
                    String.slice 0 18 txt
            in
            case splitted of
                [ first, second ] ->
                    [ ( "History Stats branch", text <| shorten first )
                    , ( "History Stats master", text <| shorten second )
                    ]

                [ one ] ->
                    [ ( "History Stats master", text <| shorten one ) ]

                _ ->
                    []

        historyStatsClass =
            if test.status == TEST_SUCCESS then
                "black-column"

            else if test.testScore <= 3 then
                "red-column"

            else if test.testScore > 3 then
                "blue-column"

            else
                ""

        rows =
            [ ( "Status", toBadge test.status [] [ text (testStatusToString test.status) ] )
            , ( "Run Num", text (toString test.runNumber) )
            , ( "Id", text test.id )
            , ( "Job Id", text test.jobId )
            , ( "Arguments", text <| String.join " " test.arguments )
            , ( "Test Type", text test.testType )
            , ( "Timeout", text <| toString test.timeout )
            , ( "Error Message", text test.errorMessage )
            , ( "Logs", logsRow )
            , ( "Assigned Agent", text test.assignedAgent )
            , ( "Agent Group", text <| agentGroupTestFormat test.agentGroup test.assignedAgent )
            , ( "Start Time", text <| formatDate test.startTime )
            , ( "End Time", text <| formatDate test.endTime )
            , ( "Scheduled At", text <| formatDate <| Just test.scheduledAt )
            , ( "History", a [ href <| "#test-history/" ++ test.id ] [ text "History" ] )
            ]
                ++ historyStats
    in
    table [ class "job-view", style [ ( "margin-bottom", "50px" ) ] ] <|
        List.map viewRow rows


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetTestDataCompleted result ->
            case result of
                Ok data ->
                    ( { model | test = Just data }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        WebSocketEvent event ->
            case event of
                ModifiedTest modifiedTest ->
                    case model.test of
                        Just modelTest ->
                            if modifiedTest.id == modelTest.id then
                                ( { model | test = Just modifiedTest }, Cmd.none )

                            else
                                ( model, Cmd.none )

                        Nothing ->
                            ( model, Cmd.none )

                _ ->
                    ( model, Cmd.none )


getTestDataCmd : TestId -> Cmd Msg
getTestDataCmd testId =
    Http.send GetTestDataCompleted <|
        Http.get ("/api/newman/test/" ++ testId) decodeTest


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
