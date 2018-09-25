module Views.CompareBuilds exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Utils.Types exposing (..)
import Bootstrap.Form.Select as Select
import Bootstrap.Button as Button
import Bootstrap.Modal as Modal exposing (..)
import Dict exposing (Dict)
import List.Extra as ListExtra
import Utils.WebSocket as WebSocket exposing (..)


type alias Model =
    { allBuilds : Builds
    , oldBuild : Maybe Build
    , newBuild : Maybe Build
    , newerBuilds : Builds
    , confirmationState : Modal.State
    }


type Msg
    = UpdateOldBuild String
    | UpdateNewBuild String
    | ClickCompareBuilds
    | AcknowledgeDialog
    | ModalMsg Modal.State
    | WebSocketEvent WebSocket.Event


init : Builds -> Model
init builds =
    Model builds Nothing Nothing builds Modal.hiddenState


view : Model -> Html Msg
view model =
    let
        chooseOldBuild data =
            Select.item [ value data.id ]
                [ text <| data.name ++ " (" ++ data.branch ++ ")" ]

        oldBuildSelect =
            div []
                [ Select.select
                    [ Select.onChange UpdateOldBuild, Select.attrs [ style [ ( "width", "400px" ) ] ] ]
                    ([ Select.item [ value "1" ] [ text "Select Old Build" ] ]
                        ++ List.map chooseOldBuild model.allBuilds
                    )
                ]

        chooseNewBuild data =
            Select.item [ value data.id ]
                [ text <| data.name ++ " (" ++ data.branch ++ ")" ]

        newBuildSelect =
            div []
                [ Select.select
                    [ Select.disabled (model.oldBuild == Nothing)
                    , Select.onChange UpdateNewBuild
                    , Select.attrs [ style [ ( "width", "400px" ) ] ]
                    ]
                    ([ Select.item [ value "1" ] [ text "Select New Build" ] ]
                        ++ List.map chooseNewBuild model.newerBuilds
                    )
                ]
    in
        div []
            [ div [ class "form-inline" ]
                [ oldBuildSelect
                , newBuildSelect
                , div []
                    [ Button.button
                        [ Button.disabled ((model.oldBuild == Nothing) || (model.newBuild == Nothing))
                        , Button.primary
                        , Button.onClick ClickCompareBuilds
                        ]
                        [ text "Compare Changes" ]
                    ]
                ]
            , viewDialog model ModalMsg AcknowledgeDialog
            ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        UpdateOldBuild build ->
            case (maybeGetBuild model.allBuilds build) of
                Just build ->
                    ( { model
                        | oldBuild = Just build
                        , newerBuilds = (onlyNewerBuilds model.allBuilds build)
                      }
                    , Cmd.none
                    )

                Nothing ->
                    ( { model | oldBuild = Nothing , newBuild = Nothing }
                    , Cmd.none
                    )

        UpdateNewBuild build ->
            ( { model | newBuild = maybeGetBuild model.allBuilds build }, Cmd.none )

        ClickCompareBuilds ->
            ( { model | confirmationState = Modal.visibleState }, Cmd.none )

        AcknowledgeDialog ->
            ( { model | confirmationState = Modal.hiddenState }, Cmd.none )

        ModalMsg newState ->
            ( { model | confirmationState = newState }, Cmd.none )

        WebSocketEvent event ->
            case event of
                CreatedBuild build ->
                    ( { model | allBuilds = build :: model.allBuilds } , Cmd.none )

                _ ->
                    ( model, Cmd.none )



-- With a given buildId returns only builds which have buildTime greater or equal to given build --
onlyNewerBuilds : List Build -> Build -> List Build
onlyNewerBuilds builds newBuild =
    List.filter (\build -> build.buildTime > newBuild.buildTime) builds



-- get build from build id after selecting item from dropdown --
maybeGetBuild : List Build -> String -> Maybe Build
maybeGetBuild builds buildId =
    case buildId of
        "1" ->
            Nothing

        value ->
            builds
                 |> ListExtra.find (\build -> build.id == value)



-- view of dialog that appears when clicking on "Compare Changes" button --
viewDialog : Model -> (State -> toMsg) -> toMsg -> Html toMsg
viewDialog model toMsg confirmMsg =
    let
        errorModel =
                Modal.config toMsg
                    |> Modal.large
                    |> Modal.h3 [] [ text "ERROR! Can't find OldBuild or NewBuild :(" ]
                    |> Modal.footer []
                        [ Button.button
                            [ Button.outlinePrimary
                            , Button.onClick <| toMsg Modal.hiddenState
                            ]
                            [ text "Close" ]
                        ]
                    |> Modal.view model.confirmationState

        body oldShas newShas =
              [ ul [] <| List.map buildBody (Dict.toList (createUrls oldShas newShas)) ]

        buildBody (key, value) =
            case (value == "") of
                True ->
                    div [] []
                False ->
                    li [] [ a [ href value, target "_blank" ] [ text key ] ]

    in
        case model.oldBuild of
                Just oldBuild ->
                    case model.newBuild of
                        Just newBuild ->
                            Modal.config toMsg
                                |> Modal.large
                                |> Modal.h5 [] [ text ("Compare builds "
                                                        ++ oldBuild.name
                                                        ++ " ("
                                                        ++ oldBuild.branch
                                                        ++ ") and "
                                                        ++ newBuild.name
                                                        ++ " ("
                                                        ++ newBuild.branch
                                                        ++ ")") ]
                                |> Modal.body [] (body oldBuild.shas newBuild.shas)
                                |> Modal.footer []
                                    [ Button.button
                                        [ Button.outlinePrimary
                                        , Button.onClick <| toMsg Modal.hiddenState
                                        ]
                                        [ text "Close" ]
                                    ]
                                |> Modal.view model.confirmationState
                        Nothing ->
                            errorModel
                Nothing ->
                    errorModel

-- Example of url: https://github.com/Gigaspaces/xap-premium/compare/12.3.0-m4-MILESTONE...9e5bca8f040314332cb18d5c86d0a7b754fa9a35 --
-- gets oldBuildShas & newBuildShas creates urls for each pair of repos that appear in both Shas
-- returns a new Dict with key (repo) value (url)
createUrls : Shas -> Shas -> Dict String String
createUrls oldBuildShas newBuildShas =
        case (Dict.isEmpty oldBuildShas || Dict.isEmpty newBuildShas) of
            True ->
                Dict.empty
            False ->
                let
                    step newKey newVal =
                        case (Dict.get newKey oldBuildShas) of
                            Just oldVal ->
                                (begingOfUrl newKey ++ "/compare/" ++ getSha oldVal ++ "..." ++ getSha newVal)
                            Nothing ->
                                 ""
                in
                    Dict.map step newBuildShas


getSha : String -> String
getSha fullShaUrl =
        case (List.head (List.reverse (String.split "/" fullShaUrl))) of
            Just sha ->
                sha
            Nothing ->
                ""


begingOfUrl : String -> String
begingOfUrl repo =
    case repo of
        "xap-open" ->
                "https://github.com/xap/xap"
        "xap" ->
                "https://github.com/Gigaspaces/xap-premium"
        "InsightEdge" ->
                "https://github.com/Insightedge/insightedge"
        value ->
                "WrongRepo"


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent