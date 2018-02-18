module Utils.WebSocket exposing (..)

import Json.Decode exposing (Decoder, Value, field, string, value)
import Navigation exposing (Location)
import Task
import Utils.Types exposing (Agent, Build, Job, Suite, Test, decodeAgent, decodeBuild, decodeJob, decodeSuite, decodeTestView)
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
    | ModifiedTest Test
    | CreatedBuild Build
    | CreatedSuite Suite



{-
    Dashboard:
   private static final String MODIFIED_BUILD = "modified-build";

    TestsTable
   public static final String CREATED_TEST = "created-test";

   Suites View
   public static final String CREATED_SUITE = "created-suite";
   public static final String MODIFIED_SUITE = "modified-suite";

    Dashboard
   public static final String CREATE_FUTURE_JOB = "create-future-job";

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

                                "modified-test" ->
                                    parse ModifiedTest decodeTestView

                                "created-build" ->
                                    parse CreatedBuild decodeBuild

                                "created-suite" ->
                                    parse CreatedSuite decodeSuite

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
