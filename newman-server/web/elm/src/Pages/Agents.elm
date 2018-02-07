module Pages.Agents exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
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
import Paginate exposing (..)
import Time exposing (Time)
import Utils.Types exposing (..)


type alias Model =
    { allAgents : Agents
    , agents : PaginatedAgents
    , pageSize : Int
    }


type Msg
    = GetAgentsCompleted (Result Http.Error Agents)
    | First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String


init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20
    in
    ( { allAgents = []
      , agents = Paginate.fromList pageSize []
      , pageSize = pageSize
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
            ( { model | agents = Paginate.fromList model.pageSize filteredList }
            , Cmd.none
            )


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
        [ h2 [ class "text" ] [ text "Agents" ]
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
                        ]
                    ]
                , tbody [] (List.map viewAgent (Paginate.page model.agents))
                ]
            , pagination
            ]
        ]


viewAgent : Agent -> Html msg
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
