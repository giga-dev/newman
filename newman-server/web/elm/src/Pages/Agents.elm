module Pages.Agents exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Bootstrap.Modal as Modal
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
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import List.Extra as ListExtra
import Paginate exposing (..)
import Time exposing (Time)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal exposing (..)


type alias Model =
    { allAgents : Agents
    , agents : PaginatedAgents
    , pageSize : Int
    , query : String
    , confirmationState : Modal.State
    , agentToDrop : Maybe String
    }


type Msg
    = GetAgentsCompleted (Result Http.Error Agents)
    | First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String
    | WebSocketEvent WebSocket.Event
    | OnClickDropAgent String
    | OnAgentDropConfirmed String
    | NewmanModalMsg Modal.State
    | RequestCompletedDropAgent String (Result Http.Error String)


init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20
    in
    ( { allAgents = []
      , agents = Paginate.fromList pageSize []
      , pageSize = pageSize
      , query = ""
      , confirmationState = Modal.hiddenState
      , agentToDrop = Nothing
      }
    , getAgentsCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetAgentsCompleted result ->
            case result of
                Ok agentsFromResult ->
                    ( { model | agents = Paginate.fromList model.pageSize agentsFromResult, allAgents = agentsFromResult }, Cmd.none )

                Err err ->
                    let
                        a =
                            Debug.log "onGetAgentsCompleted" err
                    in
                    ( model, Cmd.none )

        First ->
            ( { model | agents = Paginate.first model.agents }, Cmd.none )

        Last ->
            ( { model | agents = Paginate.last model.agents }, Cmd.none )

        Next ->
            ( { model | agents = Paginate.next model.agents }, Cmd.none )

        Prev ->
            ( { model | agents = Paginate.prev model.agents }, Cmd.none )

        GoTo i ->
            ( { model | agents = Paginate.goTo i model.agents }, Cmd.none )

        FilterQuery query ->
            let
                filteredList =
                    List.filter (filterQuery query) model.allAgents
            in
            ( { model | query = query, agents = Paginate.fromList model.pageSize filteredList }
            , Cmd.none
            )

        WebSocketEvent event ->
            case event of
                ModifiedAgent agent ->
                    ( updateAgentUpdated model agent, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        OnClickDropAgent agentId ->
            ( { model | confirmationState = Modal.visibleState, agentToDrop = Just agentId }, Cmd.none )

        NewmanModalMsg newState ->
            ( { model | agentToDrop = Nothing, confirmationState = newState }, Cmd.none )

        OnAgentDropConfirmed agentId ->
            ( { model | confirmationState = Modal.hiddenState }, dropAgentCmd agentId )

        RequestCompletedDropAgent agentId result ->
            onRequestCompletedDropAgent agentId model result


onRequestCompletedDropAgent : String -> Model -> Result Http.Error String -> ( Model, Cmd Msg )
onRequestCompletedDropAgent agentId model result =
    case result of
        Ok _ ->
            ( updateAgentRemoved model agentId, Cmd.none )

        Err err ->
            ( model, Cmd.none )

updateAll : (List Agent -> List Agent) -> Model -> Model
updateAll f model =
    let
        newList =
            f model.allAgents

        filtered =
            List.filter (filterQuery model.query) newList

        newPaginated =
            Paginate.map (\_ -> filtered) model.agents
    in
    { model | agents = newPaginated, allAgents = newList }


updateAgentUpdated : Model -> Agent -> Model
updateAgentUpdated model agentToUpdate =
    let
        f =
            ListExtra.replaceIf (\item -> item.id == agentToUpdate.id) agentToUpdate
    in
    updateAll f model

updateAgentRemoved : Model -> String -> Model
updateAgentRemoved model idToRemove =
    let
        f =
            ListExtra.filterNot (\item -> item.id == idToRemove)
    in
    updateAll f model

view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.agents ) ], onClick First ]
                [ button [ class "page-link" ] [ text "«" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.agents ) ], onClick Prev ]
                [ button [ class "page-link" ] [ text "‹" ]
                ]
            ]

        nextButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.agents ) ], onClick Next ]
                [ button [ class "page-link" ] [ text "›" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.agents ) ], onClick Last ]
                [ button [ class "page-link" ] [ text "»" ]
                ]
            ]

        pagerButtonView index isActive =
            case isActive of
                True ->
                    li [ class "page-item active" ]
                        [ button [ class "page-link" ]
                            [ text <| toString index
                            , span [ class "sr-only" ] [ text "(current)" ]
                            ]
                        ]

                False ->
                    li [ class "page-item", onClick <| GoTo index ]
                        [ button [ class "page-link" ] [ text <| toString index ]
                        ]

        pagination =
            nav []
                [ ul [ class "pagination " ]
                    (prevButtons
                        ++ Paginate.pager pagerButtonView model.agents
                        ++ nextButtons
                    )
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text <| "Agents (" ++ (toString <| List.length model.allAgents) ++ ")" ]
        , div []
            [ div [ class "form-inline" ]
                [ div [ class "form-group" ] [ FormInput.text [ FormInput.onInput FilterQuery, FormInput.placeholder "Filter" ] ]
                , div [ class "form-group" ] [ pagination ]
                ]
            , table [ class "table table-hover table-striped table-bordered table-condensed" ]
                [ thead []
                    [ tr []
                        [ th [] [ text "Name" ]
                        , th [] [ text "Capabilities" ]
                        , th [] [ text "State" ]
                        , th [] [ text "Host" ]
                        , th [] [ text "PID" ]
                        , th [ style [ ( "width", "6%" ) ] ] [ text "Setup Retries" ]
                        , th [] [ text "Job" ]
                        , th [] [ text "Build" ]
                        , th [] [ text "Suite" ]
                        , th [] [ text "Current Tests" ]
                        , th [] [ text "Last seen" ]
                        , th [] [ text "Actions" ]
                        ]
                    ]
                , tbody [] (List.map viewAgent (Paginate.page model.agents))
                ]
            , pagination
            ]
        , NewmanModal.confirmAgentDrop model.agentToDrop NewmanModalMsg OnAgentDropConfirmed model.confirmationState
        ]


viewAgent : Agent -> Html Msg
viewAgent agent =
    let
        agentCapabilities =
            String.join "," agent.capabilities

        currentTests =
            String.join "," agent.currentTests

        jobString =
            case agent.jobId of
                Just s ->
                    s

                Nothing ->
                    ""

        buildString =
            case agent.buildName of
                Just s ->
                    s

                Nothing ->
                    ""

        suiteString =
            case agent.suiteName of
                Just s ->
                    s

                Nothing ->
                    ""

        lastSeen =
            Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat agent.lastTouchTime))
    in
    tr []
        [ td [] [ text agent.name ]
        , td [] [ text agentCapabilities ]
        , td [] [ text agent.state ]
        , td [] [ text agent.host ]
        , td [] [ text agent.pid ]
        , td [] [ text (toString agent.setupRetries) ]
        , td [] [ text jobString ]
        , td [] [ text buildString ]
        , td [] [ text suiteString ]
        , td [] [ text currentTests ]
        , td [] [ text lastSeen ]
        , td []
            [ Button.button [ Button.danger, Button.small, Button.onClick <| OnClickDropAgent agent.id ]
                [ span [ class "ion-close" ] [] ]
            ]
        ]


getAgentsCmd : Cmd Msg
getAgentsCmd =
    Http.send GetAgentsCompleted getAgents


getAgents : Http.Request Agents
getAgents =
    Http.get "/api/newman/agent?all=true" decodeAgents


filterQuery : String -> Agent -> Bool
filterQuery query agent =
    let
        a =
            Debug.log ("filtered agents with query " ++ query ++ ":") (toString filteredList ++ " agent: " ++ toString agent)

        b =
            Debug.log "filtering capabiliteis" (toString agent.capabilities)

        filteredList =
            List.filter (String.startsWith query) agent.capabilities

        capabilitiesCheck =
            List.length filteredList > 0

        jobIdCheck =
            case agent.jobId of
                Just jobId ->
                    True

                Nothing ->
                    False
    in
    if
        String.length query
            == 0
            || String.startsWith query agent.name
            || capabilitiesCheck
            || String.startsWith query agent.state
            || String.startsWith query agent.host
            || String.startsWith query agent.pid
            || jobIdCheck
    then
        True
    else
        False


dropAgentCmd : String -> Cmd Msg
dropAgentCmd agentId =
    Http.send (RequestCompletedDropAgent agentId) <|
        Http.request <|
            { method = "DELETE"
            , headers = []
            , url = "/api/newman/agent/" ++ agentId
            , body = Http.emptyBody
            , expect = Http.expectString
            , timeout = Nothing
            , withCredentials = False
            }


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
