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
import DateFormat
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
import Utils.Common as Common

--define and init all members.


type alias Model =
    { currentStatus : NewmanStatus
    }


type NewmanStatus = RUNNING
    | SUSPENDING
    | SUSPENDED
    | SUSPEND_FAILED
    | WrongStatus


type Msg
    = OnClickSuspendButton
    | GotNewmanStatus (Result Http.Error String)
    | SuspendRequestCompleted (Result Http.Error String)
    | WebSocketEvent WebSocket.Event



init : ( Model, Cmd Msg )
init =
    ( { currentStatus = RUNNING
      }
    , getNewmanStatusCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GotNewmanStatus result ->
            case result of
                Err httpError ->
                    let
                        _ =
                            Debug.log "error" httpError
                    in
                    ( model, Cmd.none )

                Ok status ->
                    ( { model | currentStatus = stringToStatus status }, Cmd.none )

        WebSocketEvent event ->
            case event of
                ModifiedServerStatus status ->
                    ( { model | currentStatus = stringToStatus status } , Cmd.none )

                _ ->
                    ( model, Cmd.none )

        OnClickSuspendButton ->
            ( model, onClickButtonCmd model )

        SuspendRequestCompleted result ->
            case result of
                Err httpError ->
                    let
                        _ =
                            Debug.log "error" httpError
                    in
                    ( model, Cmd.none )

                Ok status -> -- Ignore result as it is empty!
                    ( model, Cmd.none )


view : Model -> Html Msg
view model =
    let
       updateButtonText =
            if model.currentStatus == RUNNING then
                    "Suspend"
                else
                    "Unsuspend"
       buttonColor =
            if model.currentStatus == RUNNING then
                    Button.danger
                else
                    Button.success
    in
        div [ class "container-fluid" ] <|
            [
              Html.h2
                [ class "text" ]
                [ text <| "Newman Status - " ++ statusToString model.currentStatus ]
            , Button.button [ Button.primary, buttonColor, Button.onClick OnClickSuspendButton, Button.attrs [ style [ ( "margin-top", "15px" ) ] ] ] [ text updateButtonText ]
            ]


getNewmanStatusCmd : Cmd Msg
getNewmanStatusCmd =
    Http.send GotNewmanStatus
        (Http.get "/api/newman/status" decodeStatus)



onClickButtonCmd : Model -> Cmd Msg
onClickButtonCmd model =
    let
        op = case model.currentStatus of
            RUNNING -> "suspend"
            _ -> "unsuspend"
    in
        Http.send SuspendRequestCompleted <|
            Http.request <|
                { method = "POST"
                , headers = []
                , url = "/api/newman/" ++ op
                , body = Http.emptyBody
                , expect = Http.expectString
                , timeout = Nothing
                , withCredentials = False
                }


stringToStatus : String -> NewmanStatus
stringToStatus str =
        case str of
            "RUNNING" -> RUNNING
            "SUSPENDING" -> SUSPENDING
            "SUSPENDED" -> SUSPENDED
            "SUSPEND_FAILED" -> SUSPEND_FAILED
            _ -> WrongStatus

statusToString : NewmanStatus -> String
statusToString status =
        case status of
            RUNNING -> "RUNNING"
            SUSPENDING -> "SUSPENDING"
            SUSPENDED -> "SUSPENDED"
            SUSPEND_FAILED -> "SUSPEND_FAILED"
            WrongStatus -> "Received Wrong Status"


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
