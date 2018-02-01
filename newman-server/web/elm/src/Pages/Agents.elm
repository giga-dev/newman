module Pages.Agents exposing (..)

import Date
import Date.Format
import Html exposing (Html, button, div, h2, span, table, td, text, tr)
import Html.Attributes exposing (class, disabled, style, width)
import Html.Events exposing (onClick)
import Http
import Json.Decode exposing (at, decodeString, field, maybe)
import Json.Decode.Pipeline exposing (decode)
import Paginate exposing (..)


type alias Model =
    { agents : PaginatedList Agent
    , pageSize : Int
    }


type alias Agents =
    List Agent


type Msg
    = GetAgentsCompleted (Result Http.Error Agents)
    | First
    | Last
    | Next
    | Prev
    | GoTo Int


type alias Agent =
    { id : String
    , name : String
    , host : String
    , lastTouchTime : Int
    , currentTests : List String
    , state : String
    , capabilities : List String
    , pid : String
    , setupRetries : Int
    , jobId : Maybe String
    , buildName : Maybe String
    , suiteName : Maybe String
    }


init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20
    in
    ( Model (Paginate.fromList pageSize []) pageSize, getAgentsCmd )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetAgentsCompleted result ->
            case result of
                Ok agentsFromResult ->
                    ( { model | agents = Paginate.fromList model.pageSize agentsFromResult }, Cmd.none )

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


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ button [ onClick First, disabled <| Paginate.isFirst model.agents ] [ text "<<" ]
            , button [ onClick Prev, disabled <| Paginate.isFirst model.agents ] [ text "<" ]
            ]

        nextButtons =
            [ button [ onClick Next, disabled <| Paginate.isLast model.agents ] [ text ">" ]
            , button [ onClick Last, disabled <| Paginate.isLast model.agents ] [ text ">>" ]
            ]

        pagerButtonView index isActive =
            button
                [ style
                    [ ( "font-weight"
                      , if isActive then
                            "bold"
                        else
                            "normal"
                      )
                    ]
                , onClick <| GoTo index
                ]
                [ text <| toString index ]
    in
    div [ class "container" ] <|
        [ h2 [ class "text-center" ] [ text "Agents" ]
        , table [ width 1200 ]
            (List.append
                [ tr []
                    [ td [] [ text "Name" ]
                    , td [] [ text "Capabilities" ]
                    , td [] [ text "State" ]
                    , td [] [ text "Host" ]
                    , td [] [ text "PID" ]
                    , td [] [ text "Setup Retries" ]
                    , td [] [ text "Job" ]
                    , td [] [ text "Build" ]
                    , td [] [ text "Suite" ]
                    , td [] [ text "Current Tests" ]
                    , td [] [ text "Last seen" ]
                    ]
                ]
                (List.map viewItem <| Paginate.page model.agents)
            )
        ]
            ++ prevButtons
            ++ [ span [] <| Paginate.pager pagerButtonView model.agents ]
            ++ nextButtons


viewItem : Agent -> Html msg
viewItem agent =
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


decodeAgents : Json.Decode.Decoder Agents
decodeAgents =
    Json.Decode.field "values" (Json.Decode.list decodeAgent)


decodeAgent : Json.Decode.Decoder Agent
decodeAgent =
    decode Agent
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "host" Json.Decode.string
        |> Json.Decode.Pipeline.required "lastTouchTime" Json.Decode.int
        |> Json.Decode.Pipeline.required "currentTests" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "state" Json.Decode.string
        |> Json.Decode.Pipeline.required "capabilities" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "pid" Json.Decode.string
        |> Json.Decode.Pipeline.required "setupRetries" Json.Decode.int
        |> Json.Decode.Pipeline.optionalAt [ "job", "id" ] (Json.Decode.maybe Json.Decode.string) Nothing
        |> Json.Decode.Pipeline.optionalAt [ "job", "build", "name" ] (Json.Decode.maybe Json.Decode.string) Nothing
        |> Json.Decode.Pipeline.optionalAt [ "job", "suite", "name" ] (Json.Decode.maybe Json.Decode.string) Nothing
