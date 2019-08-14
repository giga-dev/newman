module Pages.JobConfigs exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
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
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import List.Extra as ListExtra
import Paginate exposing (..)
import Task
import Time exposing (Time)
import Utils.Common as Common
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)


type alias Model =
    { allJobConfigs : JobConfigs
    }


type Msg
    = GetJobConfigsCompleted (Result Http.Error JobConfigs)
    | WebSocketEvent WebSocket.Event
    | OnClickLinkButton


init : ( Model, Cmd Msg )
init =
    ( { allJobConfigs = []
      }
    , getJobConfigsCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetJobConfigsCompleted result ->
            case result of
                Ok jobConfigsFromResult ->
                    ( { model | allJobConfigs = jobConfigsFromResult }, Cmd.none )

                Err err ->
                    let
                        a =
                            Debug.log "onGetJobConfigsCompleted" err
                    in
                    ( model, Cmd.none )

        WebSocketEvent event ->
            case event of
                CreatedJobConfig jobConfig ->
                    ( updateJobConfigAdded model jobConfig, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        OnClickLinkButton ->
            ( model, Cmd.none )


updateAll : (List JobConfig -> List JobConfig) -> Model -> Model
updateAll f model =
    let
        newList =
            f model.allJobConfigs
    in
    { model | allJobConfigs = newList }


updateJobConfigAdded : Model -> JobConfig -> Model
updateJobConfigAdded model addedJobConfig =
    updateAll (\list -> addedJobConfig :: list) model


view : Model -> Html Msg
view model =
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ]
            [ text "Job Configurations"
            , Button.linkButton [ Button.primary, Button.attrs [ href "#newJobConfig", style [ ( "margin-left", "15px" ) ] ] ] [ text "New Job Configuration" ]
            ]
        , div []
            [ table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
                [ thead []
                    [ tr []
                        [ th [] [ text "Name" ]
                        , th [] [ text "Id" ]
                        ]
                    ]
                , tbody [] (List.map viewJobConfig model.allJobConfigs)
                ]
            ]
        ]


viewJobConfig : JobConfig -> Html msg
viewJobConfig jobConfig =
    tr []
        [ a [ href <| "#jobConfig/" ++ jobConfig.id ] [ text jobConfig.name ]
        , td [] [ text jobConfig.id ]
        ]


getJobConfigsCmd : Cmd Msg
getJobConfigsCmd =
    Http.send GetJobConfigsCompleted <| Http.get "/api/newman/job-config" decodeJobConfigs


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
