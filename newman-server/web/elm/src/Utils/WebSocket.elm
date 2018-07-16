module Utils.WebSocket exposing (..)

import Json.Decode exposing (Decoder, Value, field, string, value)
import Navigation exposing (Location)
import Task
import Utils.Types exposing (Agent, Build, FutureJob, Job, Suite, Test, JobConfig, decodeJobConfig, decodeAgent, decodeBuild, decodeFutureJob, decodeJob, decodeSuite, decodeTestView)
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
    | CreatedTest Test
    | ModifiedTest Test
    | CreatedBuild Build
    | ModifiedBuild Build
    | CreatedSuite Suite
    | ModifiedSuite Suite
    | CreatedJobConfig JobConfig
    | ModifiedJobConfig JobConfig
    | CreatedFutureJob FutureJob
    | DeletedFutureJob FutureJob



{-
    Dashboard:
   private static final String MODIFIED_BUILD = "modified-build";

   TODO handle modified job in home page

   Manage Newman + App (Main)
   private static final String MODIFY_SERVER_STATUS = "modified-server-status";

-}


toEvent : Msg -> Result String Event
toEvent msg =
    case msg of
        NewMessage str ->
            let
                json =
                    Json.Decode.decodeString decodeWebSocketData str

                --                aa =
                --                    Debug.log "AA" json
            in
                case json of
                    Ok ok ->
                        let
                            parse msg decoder =
                                Result.map msg <| Json.Decode.decodeValue decoder ok.content

                            bodyRes =
                                case ok.id of
                                    "created-job" ->
                                        parse CreatedJob decodeJob

                                    "modified-job" ->
                                        parse ModifiedJob decodeJob

                                    "modified-agent" ->
                                        parse ModifiedAgent decodeAgent

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

                                    "created-job-config" ->
                                        parse CreatedJobConfig decodeJobConfig

                                    "modified-job-config" ->
                                        parse ModifiedJobConfig decodeJobConfig

                                    "created-future-job" ->
                                        parse CreatedFutureJob decodeFutureJob

                                    "deleted-future-job" ->
                                        parse DeletedFutureJob decodeFutureJob

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
