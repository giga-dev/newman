module Pages.OnDemandAgents exposing (..)

import Bootstrap.Button as Button
import Bootstrap.Grid as Grid
import Bootstrap.Grid.Col as Col
import Bootstrap.Modal as Modal
import Bootstrap.Table as Table
import Html exposing (..)
import Html.Attributes as Attrs
import Html.Events exposing (..)
import Http
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (..)
import Task
import Time
import Utils.WebSocket as WebSocket exposing (..)


type alias Model =
    { modalState : Modal.State
    , elasticGroups : List ElasticGroup
    , selectedElasticGroup : Maybe ElasticGroup
    , newCapacity : Int
    }


type Msg
    = CloseModal
    | ShowModal ElasticGroup
    | AnimateModal Modal.State
    | NewCapacity String
    | ConfirmUpdate ElasticGroup Int
    | GetElasticGroupsCompleted (Result Http.Error (List ElasticGroup))
    | UpdateElasticGroupCompleted (Result Http.Error ElasticGroup)
    | Reload
    | ShowError String
    | WebSocketEvent WebSocket.Event


init : ( Model, Cmd Msg )
init =
    ( { modalState = Modal.hiddenState
      , elasticGroups = []
      , selectedElasticGroup = Nothing
      , newCapacity = -1
      }
    , getElasticGroupsCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        ShowModal elasticGroup ->
            ( { model | newCapacity = elasticGroup.capacity.target, selectedElasticGroup = Just elasticGroup, modalState = Modal.visibleState }, Cmd.none )

        CloseModal ->
            ( { model | modalState = Modal.hiddenState, selectedElasticGroup = Nothing }, Cmd.none )

        AnimateModal state ->
            ( { model | modalState = state }, Cmd.none )

        NewCapacity newCapacity ->
            case String.toInt newCapacity of
                Ok num ->
                    ( { model | newCapacity = num }, Cmd.none )

                Err _ ->
                    ( model, Cmd.none )

        ConfirmUpdate elasticGroup newCapacity ->
            let
                updateElement : List ElasticGroup -> ElasticGroup -> Int -> List ElasticGroup
                updateElement elasticGroups agentGroupToUpdate newCapacity =
                    let
                        f elasticGroup =
                            if elasticGroup.id == agentGroupToUpdate.id then
                                let
                                    elasticGroupCapacity =
                                        elasticGroup.capacity

                                    newElasticGroupCapacity =
                                        { elasticGroupCapacity | target = newCapacity }
                                in
                                { elasticGroup | capacity = newElasticGroupCapacity }

                            else
                                elasticGroup
                    in
                    List.map f elasticGroups
            in
            ( { model | elasticGroups = updateElement model.elasticGroups elasticGroup newCapacity, modalState = Modal.hiddenState }, updateElasticGroupCmd elasticGroup newCapacity )

        GetElasticGroupsCompleted result ->
            case result of
                Ok elasticGroups ->
                    let
                        l =
                            Debug.log "A" elasticGroups
                    in
                    ( { model | elasticGroups = elasticGroups }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        UpdateElasticGroupCompleted result ->
            case result of
                Ok elasticGroup ->
                    let
                        l =
                            Debug.log "UpdateElasticGroupCompleted" "group has been updated"
                    in
                    ( model, Cmd.none )

                Err err ->
                    let
                        l =
                            Debug.log "UpdateElasticGroupCompleted" <| "group update has failed" ++ toString err
                    in
                    ( model, Cmd.none )

        Reload ->
            ( model, getElasticGroupsCmd )

        ShowError msg ->
            let
                e =
                    Debug.log "ERROR" msg
            in
            ( model, Cmd.none )

        WebSocketEvent event ->
            case event of
                ModifiedAgentsCount c ->
                    ( model, getElasticGroupsCmd )

                _ ->
                    ( model, Cmd.none )


view : Model -> Html Msg
view model =
    let
        rowOpt elasticGroup =
            if elasticGroup.capacity.target == elasticGroup.connectedAgents then
                Table.rowSuccess

            else
                Table.rowDanger

        createRow elasticGroup =
            Table.tr [ rowOpt elasticGroup ]
                [ Table.td [] [ text <| (elasticGroup.tags.name ++ " (" ++ elasticGroup.id ++ ")") ]
                , Table.td [] [ text <| toString elasticGroup.capacity.target ]
                , Table.td [] [ text <| toString elasticGroup.runningVMs ]
                , Table.td [] [ text <| toString elasticGroup.connectedAgents ]
                , Table.td [] [ Button.button [ Button.roleLink, Button.attrs [ Attrs.class "ion-android-options" ], Button.onClick <| ShowModal elasticGroup ] [] ]
                ]

        onDemandText =
            (toString <| List.sum <| List.map (\group -> group.connectedAgents) model.elasticGroups)
                ++ " / "
                ++ (toString <| List.sum <| List.map (\group -> group.capacity.target) model.elasticGroups)
    in
    div []
        [ h2 [] [ text <| "On Demand: " ++ onDemandText ++ " ", Button.button [ Button.attrs [ Attrs.class "ion-refresh" ], Button.onClick Reload ] [] ]
        , Table.table
            { options = [ Table.responsive, Table.hover ]
            , thead =
                Table.simpleThead
                    [ Table.th [] [ text "Group name" ]
                    , Table.th [] [ text "Target Capacity" ]
                    , Table.th [] [ text "Running VMs" ]
                    , Table.th [] [ text "Connected Agents" ]
                    , Table.th [] [ text "Actions" ]
                    ]
            , tbody =
                Table.tbody []
                    (List.map createRow model.elasticGroups)
            }
        , viewModal model
        ]


viewModal : Model -> Html Msg
viewModal model =
    case model.selectedElasticGroup of
        Nothing ->
            Modal.config AnimateModal
                |> Modal.large
                |> Modal.h3 [] [ text "Error: No selected agent group" ]
                |> Modal.view model.modalState

        Just elasticGroup ->
            let
                twoColsRow left right =
                    Grid.row []
                        [ Grid.col
                            [ Col.sm2 ]
                            [ text left ]
                        , Grid.col
                            [ Col.sm8 ]
                            [ text right ]
                        ]
            in
            Modal.config AnimateModal
                |> Modal.large
                |> Modal.h3 [] [ text <| "Update capacity for group - " ++ elasticGroup.tags.name ]
                |> Modal.body []
                    [ Grid.containerFluid []
                        [ twoColsRow "Id" elasticGroup.id
                        , twoColsRow "Name" elasticGroup.tags.name
                        , twoColsRow "Description" elasticGroup.tags.description
                        , twoColsRow "Capacity" <| "Minimum: " ++ toString elasticGroup.capacity.minimum ++ ", Maximum: " ++ toString elasticGroup.capacity.maximum
                        , Grid.row []
                            [ Grid.col
                                [ Col.sm2 ]
                                [ text "New target" ]
                            , Grid.col
                                [ Col.sm8 ]
                                [ input [ onInput NewCapacity, Attrs.type_ "number", Attrs.max <| toString elasticGroup.capacity.maximum, Attrs.min <| toString elasticGroup.capacity.minimum, Attrs.value <| toString model.newCapacity ] [] ]
                            ]
                        ]
                    ]
                |> Modal.footer []
                    [ Button.button
                        [ Button.danger
                        , Button.onClick <| ConfirmUpdate elasticGroup model.newCapacity
                        ]
                        [ text "Confirm" ]
                    , Button.button
                        [ Button.outlinePrimary
                        , Button.onClick CloseModal
                        ]
                        [ text "Close" ]
                    ]
                |> Modal.view model.modalState


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent



--


getElasticGroupsCmd : Cmd Msg
getElasticGroupsCmd =
    Http.send GetElasticGroupsCompleted <| Http.get "/api/spotinst/elasticgroup" (list decodeElasticGroup)


updateElasticGroupCmd : ElasticGroup -> Int -> Cmd Msg
updateElasticGroupCmd elasticGroup newCapacity =
    Http.send UpdateElasticGroupCompleted <| Http.post ("/api/spotinst/elasticgroup?elasticGroupId=" ++ elasticGroup.id ++ "&capacity=" ++ toString newCapacity) Http.emptyBody decodeElasticGroup


type alias ElasticGroup =
    { id : String
    , name : String
    , tags : ElasticGroupTags
    , capacity : ElasticGroupCapacity
    , connectedAgents : Int
    , runningVMs : Int
    }


type alias ElasticGroupCapacity =
    { minimum : Int
    , maximum : Int
    , target : Int
    }


type alias ElasticGroupTags =
    { name : String
    , description : String
    , owner : String
    }


decodeElasticGroup : Decoder ElasticGroup
decodeElasticGroup =
    decode ElasticGroup
        |> required "id" string
        |> required "name" string
        |> required "tags" decodeElasticGroupTags
        |> required "capacity" decodeElasticGroupCapacity
        |> required "connectedAgents" int
        |> required "runningVMs" int


decodeElasticGroupCapacity : Decoder ElasticGroupCapacity
decodeElasticGroupCapacity =
    decode ElasticGroupCapacity
        |> required "minimum" int
        |> required "maximum" int
        |> required "target" int


decodeElasticGroupTags : Decoder ElasticGroupTags
decodeElasticGroupTags =
    decode ElasticGroupTags
        |> required "name" string
        |> required "description" string
        |> required "owner" string
