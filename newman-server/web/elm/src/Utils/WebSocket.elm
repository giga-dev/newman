module Utils.WebSocket exposing (..)

import Json.Decode exposing (Decoder, Value, field, string, value)
import Navigation exposing (Location)
import Utils.Types exposing (Job, decodeJob)
import WebSocket


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
    Model <| "ws://" ++ location.hostname ++ ":" ++ location.port_ ++ "/events"


type Msg
    = NewMessage String


type Event
    = CreatedJob Job
    | ModifiedJob Job

{-

    private static final String MODIFIED_BUILD = "modified-build";
    public static final String CREATED_TEST = "created-test";
    public static final String MODIFIED_JOB = "modified-job";
    public static final String MODIFIED_TEST = "modified-test";
    public static final String CREATED_JOB = "created-job";
    public static final String MODIFIED_AGENT = "modified-agent";
    public static final String CREATED_BUILD = "created-build";
    public static final String CREATED_SUITE = "created-suite";
    public static final String MODIFIED_SUITE = "modified-suite";
    public static final String CREATE_FUTURE_JOB = "create-future-job";
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
                        bodyRes =
                            case ok.id of
                                "created-job" ->
                                    Result.map CreatedJob <| Json.Decode.decodeValue decodeJob ok.content

                                "modified-job" ->
                                    Result.map ModifiedJob <| Json.Decode.decodeValue decodeJob ok.content

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
