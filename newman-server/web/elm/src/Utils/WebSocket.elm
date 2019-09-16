module Utils.WebSocket exposing (..)

import Json.Decode exposing (Decoder, Value, decodeString, decodeValue, field, int, string, value)
import Navigation exposing (Location)
import Task
import Utils.Types exposing (Agent, Build, FutureJob, Job, JobConfig, Suite, Test, decodeAgent, decodeBuild, decodeFutureJob, decodeJob, decodeJobConfig, decodeStatus, decodeSuite, decodeTestView)
import WebSocket


(=>) : d -> (d -> msg) -> Cmd msg
(=>) d msg =
    Task.perform msg <| Task.succeed <| d


type alias Model =
    { serverAddress : String }


type alias WebSocketData =
    { id : String
    , content : Value
    }


decodeWebSocketData : Decoder WebSocketData
decodeWebSocketData =
    Json.Decode.map2 WebSocketData
        (field "id" string)
        (field "content" value)


initModel : Location -> Model
initModel location =
    let
        protocol =
            case location.protocol of
                "https:" ->
                    "wss"

                _ ->
                    "ws"
    in
    Model <| protocol ++ "://" ++ location.hostname ++ ":" ++ location.port_ ++ "/events"


type Msg
    = NewMessage String


type Event
    = CreatedJob Job
    | ModifiedJob Job
    | ModifiedAgent Agent
    | CreatedOfflineAgent Agent
    | DeletedOfflineAgent String
    | DeletedAgent Agent
    | CreatedTest Test
    | ModifiedTest Test
    | CreatedBuild Build
    | ModifiedBuild Build
    | CreatedSuite Suite
    | ModifiedSuite Suite
    | DeletedSuite Suite
    | CreatedJobConfig JobConfig
    | CreatedFutureJob FutureJob
    | DeletedFutureJob FutureJob
    | ModifiedServerStatus String
    | ModifiedAgentsCount Int
    | ModifiedFailingAgents Int


toEvent : Msg -> Result String Event
toEvent msg =
    case msg of
        NewMessage str ->
            let
                json =
                    decodeString decodeWebSocketData str
            in
            case json of
                Ok ok ->
                    let
                        parse msg decoder =
                            Result.map msg <| decodeValue decoder ok.content

                        bodyRes =
                            case ok.id of
                                "created-job" ->
                                    parse CreatedJob decodeJob

                                "modified-job" ->
                                    parse ModifiedJob decodeJob

                                "modified-agent" ->
                                    parse ModifiedAgent decodeAgent

                                "deleted-agent" ->
                                    parse DeletedAgent decodeAgent

                                "created-offline-agent" ->
                                    parse CreatedOfflineAgent decodeAgent

                                "deleted-offline-agent" ->
                                    parse DeletedOfflineAgent string

                                "modified-agents-count" ->
                                    parse ModifiedAgentsCount int

                                "modified-failing-agents" ->
                                    parse ModifiedFailingAgents int

                                "created-test" ->
                                    parse CreatedTest decodeTestView

                                "modified-test" ->
                                    parse ModifiedTest decodeTestView

                                "created-build" ->
                                    parse CreatedBuild decodeBuild

                                "modified-build" ->
                                    parse ModifiedBuild decodeBuild

                                "created-suite" ->
                                    parse CreatedSuite decodeSuite

                                "modified-suite" ->
                                    parse ModifiedSuite decodeSuite

                                "deleted-suite" ->
                                    parse DeletedSuite decodeSuite

                                "created-job-config" ->
                                    parse CreatedJobConfig decodeJobConfig

                                "created-future-job" ->
                                    parse CreatedFutureJob decodeFutureJob

                                "deleted-future-job" ->
                                    parse DeletedFutureJob decodeFutureJob

                                "modified-server-status" ->
                                    parse ModifiedServerStatus decodeStatus

                                other ->
                                    Err <| "Unhandled event id: " ++ other
                    in
                    case bodyRes of
                        Ok body ->
                            Ok body

                        Err err ->
                            Err err

                Err err ->
                    Err err


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ WebSocket.listen model.serverAddress NewMessage
        , WebSocket.keepAlive model.serverAddress
        ]
