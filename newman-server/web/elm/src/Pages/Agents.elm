module Pages.Agents exposing (..)

import Bootstrap.Button as Button
import Bootstrap.Form.Checkbox as CheckBox
import Bootstrap.Form.Input as FormInput
import Bootstrap.Modal as Modal
import Date exposing (Date)
import Date.Extra.Config.Config_en_au exposing (config)
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
    , offlineAgents : Agents
    , agents : PaginatedAgents
    , pageSize : Int
    , query : String
    , filterFailingAgents : Bool
    , filterOfflineAgents : Bool
    , confirmationState : Modal.State
    , agentToDrop : Maybe String
    , offlineAgentToDrop : Maybe String
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
    | OnClickDropOfflineAgent String
    | OnOfflineAgentDropConfirmed String
    | NewmanModalMsg Modal.State
    | RequestCompletedDropAgent String String (Result Http.Error String)
    | FilterFailingAgents Bool
    | CleanSetupRetries
    | FilterOfflineAgents Bool
    | GetOfflineAgentsCompleted (Result Http.Error Agents)


init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20
    in
    ( { allAgents = []
      , offlineAgents = []
      , agents = Paginate.fromList pageSize []
      , pageSize = pageSize
      , query = ""
      , filterFailingAgents = False
      , filterOfflineAgents = False
      , confirmationState = Modal.hiddenState
      , agentToDrop = Nothing
      , offlineAgentToDrop = Nothing
      }
    , getAgentsCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetAgentsCompleted result ->
            case result of
                    Ok agentsFromResult ->
                        let
                            filteredList =
                                if (model.filterOfflineAgents) then
                                      model.offlineAgents
                                else
                                    List.filter (filterQuery model.query model.filterFailingAgents) agentsFromResult
                        in
                            ( { model | agents = Paginate.fromList model.pageSize filteredList
                                      , allAgents = agentsFromResult }, Cmd.none )

                    Err err ->
                        ( model, Cmd.none )

        GetOfflineAgentsCompleted result ->
            case result of
                Ok offlineAgentsResult ->
                         ({model | agents = Paginate.fromList model.pageSize offlineAgentsResult,
                                    offlineAgents = offlineAgentsResult } , Cmd.none )
                Err err ->
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
                        if (model.filterOfflineAgents) then
                              model.offlineAgents
                        else
                            List.filter (filterQuery query model.filterFailingAgents) model.allAgents
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

        OnClickDropOfflineAgent agentName ->
            ( { model | confirmationState = Modal.visibleState, offlineAgentToDrop = Just agentName }, Cmd.none )

        NewmanModalMsg newState ->
            ( { model | agentToDrop = Nothing, offlineAgentToDrop = Nothing, confirmationState = newState }, Cmd.none )

        OnAgentDropConfirmed agentId ->
            ( { model | confirmationState = Modal.hiddenState }, dropAgentCmd agentId )

        OnOfflineAgentDropConfirmed agentName ->
            ( { model | confirmationState = Modal.hiddenState }, dropOfflineAgentCmd agentName )

        RequestCompletedDropAgent offlineOrOnline agentId result ->
            onRequestCompletedDropAgent offlineOrOnline agentId model result

        FilterFailingAgents filterFailingAgents ->
            let
                filteredList =
                    if ((not filterFailingAgents) && model.filterOfflineAgents) then
                          model.offlineAgents
                    else
                        List.filter (filterQuery model.query filterFailingAgents) model.allAgents
            in
            ( { model | filterFailingAgents = filterFailingAgents,
                        agents = Paginate.fromList model.pageSize filteredList,
                        filterOfflineAgents = if filterFailingAgents then
                                                    False
                                              else   model.filterOfflineAgents }
                        , Cmd.none
            )

        FilterOfflineAgents filterOfflineAgents ->
            let
                filteredList =
                    if (filterOfflineAgents) then
                          model.offlineAgents
                    else
                        List.filter (filterQuery model.query model.filterFailingAgents) model.allAgents

                cmd =  if (filterOfflineAgents) then
                            getOfflineAgentsCmd
                       else
                            Cmd.none
            in
            ( {model | filterOfflineAgents = filterOfflineAgents
                     , agents = Paginate.fromList model.pageSize filteredList
                     , filterFailingAgents = if filterOfflineAgents then
                                                    False
                                             else model.filterFailingAgents } , cmd )

        CleanSetupRetries ->
            ( model, cleanSetupRetriesCmd )



onRequestCompletedDropAgent : String -> String -> Model -> Result Http.Error String -> ( Model, Cmd Msg )
onRequestCompletedDropAgent offlineOrOnline agentId model result =
    case result of
        Ok _ ->
                case offlineOrOnline of
                        "online" ->
                                ( updateAgentRemoved model agentId, Cmd.none )
                        "offline" ->
                            let
                                offlineAgentList =
                                    ListExtra.filterNot (\agent -> agent.name == agentId ) model.offlineAgents
                            in
                                ( {model | offlineAgents = offlineAgentList
                                         , agents = Paginate.fromList model.pageSize offlineAgentList } , Cmd.none )
                        err ->
                                ( model, Cmd.none )
        Err err ->
                ( model, Cmd.none )


updateAll : (List Agent -> List Agent) -> Model -> Model
updateAll f model =
    let
        newList =
            f model.allAgents

        filtered =
            List.filter (filterQuery model.query model.filterFailingAgents) newList

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

        cleanSetupRetriesButton =
            div [] [ Button.button [ Button.primary, Button.onClick CleanSetupRetries ] [ text "Clear Failing Agents" ] ]

        failingAgentsFilterButton =
            CheckBox.custom [ CheckBox.inline, CheckBox.onCheck FilterFailingAgents , CheckBox.checked model.filterFailingAgents ] "Failing agents only"

        offlineAgentsFilterButton =
            CheckBox.custom [ CheckBox.inline, CheckBox.onCheck FilterOfflineAgents , CheckBox.checked model.filterOfflineAgents ] "Offline agents only"

        confirmAgentDropDialog =
                case  model.agentToDrop of
                    Just agentId ->
                        NewmanModal.confirmAgentDrop agentId NewmanModalMsg OnAgentDropConfirmed model.confirmationState
                    Nothing ->
                        case model.offlineAgentToDrop of
                            Just agentName ->
                                NewmanModal.confirmAgentDrop agentName NewmanModalMsg OnOfflineAgentDropConfirmed model.confirmationState
                            Nothing ->
                                NewmanModal.confirmAgentDrop "agentNotFound" NewmanModalMsg OnOfflineAgentDropConfirmed model.confirmationState

    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text <| "Agents (" ++ (toString <| Paginate.length model.agents) ++ ")" ]
        , div []
            [ div [ class "form-inline" ]
                [ div [ class "form-group" ]
                    [ FormInput.text
                        [ FormInput.onInput FilterQuery
                        , FormInput.placeholder "Filter"
                        , FormInput.value model.query
                        ]
                    ]
                , div [ class "form-group" ] [ pagination ]
                , cleanSetupRetriesButton
                ]
            , div [] [ failingAgentsFilterButton ]
            , div [] [ offlineAgentsFilterButton ]
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
                , tbody [] (List.map (viewAgent model.filterOfflineAgents) (Paginate.page model.agents))
                ]
            , pagination
            ]
        , confirmAgentDropDialog
        ]


viewAgent : Bool -> Agent -> Html Msg
viewAgent filterOfflineAgents agent =
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

        closeButtonMsg agentId agentName =
                if filterOfflineAgents then
                    OnClickDropOfflineAgent agentName
                else
                    OnClickDropAgent agentId
    in
    tr []
        [ td [] [ text agent.name ]
        , td [] [ text agentCapabilities ]
        , td [] [ text <| if filterOfflineAgents then
                            ""
                          else
                            agent.state ]
        , td [] [ text agent.host ]
        , td [] [ text agent.pid ]
        , td [] [ text (toString agent.setupRetries) ]
        , td [] [ text jobString ]
        , td [] [ text buildString ]
        , td [] [ text suiteString ]
        , td [] [ text currentTests ]
        , td [] [ text lastSeen ]
        , td [] [ Button.button [ Button.danger, Button.small, Button.onClick <| closeButtonMsg agent.id agent.name ]
                    [ span [ class "ion-close" ] [] ]
                ]
        ]


getAgentsCmd : Cmd Msg
getAgentsCmd =
    Http.send GetAgentsCompleted <| Http.get "/api/newman/agent?all=true" decodeAgents


cleanSetupRetriesCmd :Cmd Msg
cleanSetupRetriesCmd =
    Http.send GetAgentsCompleted <| Http.post "/api/newman/agent/clean-setup-retries?all=true" Http.emptyBody decodeAgents

getOfflineAgentsCmd : Cmd Msg
getOfflineAgentsCmd =
    Http.send GetOfflineAgentsCompleted <| Http.get "/api/newman/agent/offline" decodeAgentList


filterQuery : String -> Bool -> Agent -> Bool
filterQuery query filterFailingAgents agent =
    let
        filteredList =
            List.filter (String.startsWith query) agent.capabilities

        capabilitiesCheck =
            List.length filteredList > 0

        jobIdCheck =
            case agent.jobId of
                Just jobId ->
                    String.startsWith query jobId

                Nothing ->
                    False
    in
    if
        (not filterFailingAgents || filterFailingAgents && agent.setupRetries > 0)
        &&  (String.length query == 0
            || String.startsWith query agent.name
            || capabilitiesCheck
            || String.startsWith query agent.state
            || String.contains query agent.host
            || String.startsWith query agent.pid
            || jobIdCheck)
    then
        True
    else
        False


dropAgentCmd : String -> Cmd Msg
dropAgentCmd agentId =
    Http.send (RequestCompletedDropAgent "online" agentId) <|
        Http.request <|
            { method = "DELETE"
            , headers = []
            , url = "/api/newman/agent/" ++ agentId
            , body = Http.emptyBody
            , expect = Http.expectString
            , timeout = Nothing
            , withCredentials = False
            }


dropOfflineAgentCmd : String -> Cmd Msg
dropOfflineAgentCmd agentName =
    Http.send (RequestCompletedDropAgent "offline" agentName) <|
        Http.request <|
            { method = "DELETE"
            , headers = []
            , url = "/api/newman/offlineAgent/" ++ agentName
            , body = Http.emptyBody
            , expect = Http.expectString
            , timeout = Nothing
            , withCredentials = False
            }


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
