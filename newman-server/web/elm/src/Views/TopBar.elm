module Views.TopBar exposing (..)

import Filesize exposing (..)
import Html exposing (Html, p, span, text)
import Html.Attributes exposing (..., class)
import Http
import Json.Decode
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket


type alias Model =
    { failingAgents : Int
    , agentsCount : Int
    , logsSize : Int
    , user : User
    }


type Msg
    = GetFailingAgentsCompleted (Result Http.Error Int)
    | GetAgentsCountCompleted (Result Http.Error Int)
    | GetLogsSizeCompleted (Result Http.Error Int)
    | GetUserNameCompleted (Result Http.Error User)
    | WebSocketEvent WebSocket.Event


init : ( Model, Cmd Msg )
init =
    ( { failingAgents = 0
      , agentsCount = 0
      , logsSize = 0
      , user = { userName = "unknown user" }
      }
    , Cmd.batch [ getFailingAgentsCmd, getAgentsCountCmd, getLogsSizeCmd, getUserNameCmd ]
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetFailingAgentsCompleted result ->
            case result of
                Ok data ->
                    ( { model | failingAgents = data }, Cmd.none )

                Err err ->
                    let
                        a =
                            Debug.log "top bar GetFailingAgentsCompleted failed" err
                    in
                    ( model, Cmd.none )

        GetAgentsCountCompleted result ->
            case result of
                Ok data ->
                    ( { model | agentsCount = data }, Cmd.none )

                Err err ->
                    let
                        a =
                            Debug.log "top bar GetAgentsCountCompleted failed" err
                    in
                    ( model, Cmd.none )

        GetLogsSizeCompleted result ->
            case result of
                Ok data ->
                    ( { model | logsSize = data }, Cmd.none )

                Err err ->
                    let
                        a =
                            Debug.log "top bar GetLogsSizeCompleted failed" err
                    in
                    ( model, Cmd.none )

        GetUserNameCompleted result ->
            case result of
                Ok data ->
                    ( { model | user = data }, Cmd.none )

                Err err ->
                    let
                        a =
                            Debug.log "top bar GetUserNameCompleted failed" err
                    in
                    ( model, Cmd.none )

        WebSocketEvent event ->
            case event of
                ModifiedAgentsCount agentsCount ->
                    ( { model | agentsCount = agentsCount }, Cmd.none )

                ModifiedFailingAgents failingAgents ->
                    ( { model | failingAgents = failingAgents }, Cmd.none )

                _ ->
                    ( model, Cmd.none )


view : Model -> Html Msg
view model =
    let
        size =
            formatWith { defaultSettings | decimalSeparator = "," } model.logsSize

        userNameString =
            "Hello " ++ model.user.userName

        failingAgentsString =
            "Failing agents: " ++ toString model.failingAgents

        agentsCountString =
            "Agents count: " ++ toString model.agentsCount

        logSizeString =
            "Logs size: " ++ size
    in
    span [ class "topbar-info" ]
        [ text <| userNameString ++ ",  " ++ failingAgentsString ++ ",  " ++ agentsCountString ++ ",  " ++ logSizeString ]


getFailingAgentsCmd : Cmd Msg
getFailingAgentsCmd =
    Http.send GetFailingAgentsCompleted <|
        Http.get "/api/newman/agents/failing" Json.Decode.int


getAgentsCountCmd : Cmd Msg
getAgentsCountCmd =
    Http.send GetAgentsCountCompleted <|
        Http.get "/api/newman/agents/count" Json.Decode.int


getLogsSizeCmd : Cmd Msg
getLogsSizeCmd =
    Http.send GetLogsSizeCompleted <|
        Http.get "/api/newman/log/size" Json.Decode.int


getUserNameCmd : Cmd Msg
getUserNameCmd =
    Http.send GetUserNameCompleted <|
        Http.get "/api/newman/user" decodeUser


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
