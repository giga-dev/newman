module Pages.ManageNewman exposing (..)

import Bootstrap.Alert as Alert exposing (..)
import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Bootstrap.ListGroup as ListGroup
import Bootstrap.Progress as Progress exposing (..)
import Date exposing (Date)
import Date.Extra.Config.Config_en_au exposing (config)
import Date.Extra.Duration as Duration
import Date.Extra.Format as Format exposing (format, formatUtc, isoMsecOffsetFormat)
import Date.Format
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import Json.Decode exposing (Decoder, at, int, string,succeed)
import Json.Decode.Pipeline exposing (decode, required)
import List.Extra as ListExtra
import Paginate exposing (..)
import Task
import Time exposing (Time)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)


--define and init all members.


type alias Model =
    { currentStatus : String
    , buttonText : String
    }


type Msg
    = NoOp
    | OnClickSuspendUnsespendButton
    | GotNewmansStatus (Result Http.Error String)
    | GotSuspendUnsuspend (Result Http.Error String)
    | GetNewmansStatus
    | WebSocketEvent WebSocket.Event


init : ( Model, Cmd Msg )
init =
    ( { currentStatus = ""
      , buttonText = ""
      }
    , getNewmanStatusCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        NoOp ->
            ( model, Cmd.none )

        GetNewmansStatus ->
            ( model, Http.send GotNewmansStatus getNewmanStatus )

        GotNewmansStatus result ->
            case result of
                Err httpError ->
                    let
                        _ =
                            Debug.log "error" httpError
                    in
                    ( model, Cmd.none )

                Ok status ->
                    ( { model | currentStatus = status }, Cmd.none )

        WebSocketEvent event ->
            ( model, Cmd.none )

        --
        OnClickSuspendUnsespendButton ->
            --update return model and cmd
            ( model, onClickButtonCmd model )

        GotSuspendUnsuspend result ->
            case result of
                Err httpError ->
                    let
                        _ =
                            Debug.log "error" httpError
                    in
                    ( model, Cmd.none )

                Ok status ->
                    ( { model | currentStatus = status }, Cmd.none )


view : Model -> Html Msg
view model =
    div [ class "container-fluid" ] <|
        [ --Alert.warning  [ text "This is a primary message."],
          Html.h2
            [ class "text" ]
            [ text <| "Newman Status - " ++ model.currentStatus ]
        , Button.button [ Button.secondary, Button.onClick OnClickSuspendUnsespendButton, Button.attrs [ style [ ( "margin-top", "15px" ) ] ] ] [ text (updateButtonText model) ]
        ]


api : String
api =
    "/api/newman/status"


getNewmanStatus : Http.Request String
getNewmanStatus =
    Http.get api decodeStatus


decodeStatus : Decoder String
decodeStatus =
    at [ "status" ] string

--decodeSucess : Decoder
--decodeSucess =
--    succeed


getNewmanStatusCmd : Cmd Msg
getNewmanStatusCmd =
    Http.send GotNewmansStatus
        (Http.get "/api/newman/status" decodeStatus)


onClickButtonCmd : Model -> Cmd Msg
onClickButtonCmd model =
    Http.send GotSuspendUnsuspend
        (if model.currentStatus == "RUNNING" then
            Http.post "/api/newman/suspend" Http.emptyBody <| succeed ""
         else
            Http.post "/api/newman/unsuspend" Http.emptyBody <| succeed ""
        )


updateButtonText : Model -> String
updateButtonText model =
    if model.currentStatus == "RUNNING" then
        "Suspend"
    else
        "Unsuspend"


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
