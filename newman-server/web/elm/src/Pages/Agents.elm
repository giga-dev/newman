module Pages.Agents exposing (..)

import Bootstrap.Tab as Tab
import Html exposing (Html, h4, p, text)
import Pages.AgentsAll as AgentsAll
import Pages.OnDemandAgents as OnDemandAgents
import Utils.WebSocket as WebSocket exposing (..)


type alias Model =
    { tabState : Tab.State
    , agentsAllModel : AgentsAll.Model
    , onDemandAgentsModel : OnDemandAgents.Model
    }


init : ( Model, Cmd Msg )
init =
    let
        ( agentsAllModel, agentsAllCmd ) =
            AgentsAll.init

        ( onDemandAgentsModel, onDemandAgentsCmd ) =
            OnDemandAgents.init
    in
    ( { tabState = Tab.initialState
      , agentsAllModel = agentsAllModel
      , onDemandAgentsModel = onDemandAgentsModel
      }
    , Cmd.batch
        [ agentsAllCmd |> Cmd.map AgentsAllMsg
        , onDemandAgentsCmd |> Cmd.map OnDemandAgentsMsg
        ]
    )


type Msg
    = TabMsg Tab.State
    | AgentsAllMsg AgentsAll.Msg
    | OnDemandAgentsMsg OnDemandAgents.Msg


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        TabMsg state ->
            ( { model | tabState = state }
            , Cmd.none
            )

        AgentsAllMsg msg ->
            let
                ( newModel, newCmd ) =
                    AgentsAll.update msg model.agentsAllModel
            in
            ( { model | agentsAllModel = newModel }, newCmd |> Cmd.map AgentsAllMsg )

        OnDemandAgentsMsg msg ->
            let
                ( newModel, newCmd ) =
                    OnDemandAgents.update msg model.onDemandAgentsModel
            in
            ( { model | onDemandAgentsModel = newModel }, newCmd |> Cmd.map OnDemandAgentsMsg )


view : Model -> Html Msg
view model =
    Tab.config TabMsg
        |> Tab.withAnimation
        -- remember to wire up subscriptions when using this option
        |> Tab.justified
        |> Tab.items
            [ Tab.item
                { id = "tabItem1"
                , link = Tab.link [] [ text "All Agents" ]
                , pane =
                    Tab.pane []
                        [ AgentsAll.view model.agentsAllModel |> Html.map AgentsAllMsg
                        ]
                }
            , Tab.item
                { id = "tabItem2"
                , link = Tab.link [] [ text "On Demand Agents" ]
                , pane = Tab.pane [] [ OnDemandAgents.view model.onDemandAgentsModel |> Html.map OnDemandAgentsMsg ]
                }
            ]
        |> Tab.view model.tabState


subscriptions : Model -> Sub Msg
subscriptions model =
    Tab.subscriptions model.tabState TabMsg


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    Cmd.batch
        [ AgentsAll.handleEvent event |> Cmd.map AgentsAllMsg
        , OnDemandAgents.handleEvent event |> Cmd.map OnDemandAgentsMsg
        ]
