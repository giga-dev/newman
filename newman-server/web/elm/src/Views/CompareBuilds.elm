module Views.CompareBuilds exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Utils.Types exposing (..)
import Bootstrap.Form.Select as Select
import Bootstrap.Button as Button
import Bootstrap.Modal as Modal exposing (..)


type alias Model =
    { allBuilds : Builds
    , oldBuild : String
    , oldBuildTime : Maybe Int
    , newBuild : String
    , oldSelected : Bool
    , newerBuilds : Builds
    , confirmationState : Modal.State
    }


type Msg
    = UpdateOldBuild String
    | UpdateNewBuild String
    | ClickCompareBuilds
    | AcknowledgeDialog
    | ModalMsg Modal.State


init : Builds -> Model
init builds =
    Model builds "" Nothing "" False builds Modal.hiddenState


view : Model -> Html Msg
view model =
    let
        chooseOldBuild data =
            Select.item [ value data.id, selected <| model.oldBuild == data.id ] [ text <| data.name ++ " (" ++ data.branch ++ ")" ]

        oldBuildSelect = div []
                             [ Select.select [ Select.onChange UpdateOldBuild,
                                                        Select.attrs [ style [ ( "width", "400px" ) ] ] ]
                                                                      ([ Select.item [ value "1" ] [ text "Select Old Build" ]]
                                                                          ++ List.map chooseOldBuild model.allBuilds
                                                                      )]

        chooseNewBuild data =
            Select.item [ value data.id, selected <| model.newBuild == data.id ] [ text <| data.name ++ " (" ++ data.branch ++ ")" ]

        newBuildSelect = div []
                             [ Select.select [ Select.disabled (not model.oldSelected), Select.onChange UpdateNewBuild, Select.attrs [ style [ ( "width", "400px" ) ]]]
                                                  ([ Select.item [ value "1" ] [ text "Select New Build" ]]
                                                        ++ List.map chooseNewBuild model.newerBuilds
                                                  )]
    in
        div []
            [ div [ class "form-inline" ]
                [ oldBuildSelect
                , newBuildSelect
                , div [] [ Button.button [ Button.primary, Button.onClick ClickCompareBuilds] [ text "Compare Changes" ] ]
                ]
            , viewDialog model ModalMsg AcknowledgeDialog
            ]

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        UpdateOldBuild buildId ->
            let
                printOldBuildSelected = Debug.log "UpdateOldBuild" buildId
            in
            case (buildId == "1") of
                True ->
                        ({ model | oldBuild = "" , newBuild = "", oldSelected = False } , Cmd.none)
                False ->
                        ( { model | oldBuild = buildId
                                    , oldBuildTime = (getOldBuildTime model buildId)
                                    , oldSelected = True
                                    , newerBuilds = (onlyNewerBuilds model buildId) } , Cmd.none)

        UpdateNewBuild buildId ->
            let
                printNewBuildSelected = Debug.log "UpdateNewBuild" buildId
            in
            case (buildId == "1") of
                True ->
                    ( { model | newBuild = "" } , Cmd.none )
                False ->
                    ( { model | newBuild = buildId } , Cmd.none )

        ClickCompareBuilds ->
                ( { model | confirmationState = Modal.visibleState } , Cmd.none )

        AcknowledgeDialog ->
                ( { model | confirmationState = Modal.hiddenState } , Cmd.none )

        ModalMsg newState ->
                ( { model | confirmationState = newState } , Cmd.none )



-- With a given buildId returns buildTime from model.allBuilds --
getOldBuildTime : Model -> String -> Maybe Int
getOldBuildTime model buildId =
                     model.allBuilds
                            |> List.filter (\build -> build.id == buildId)
                            |> List.map .buildTime
                            |> List.head


-- With a given buildId returns returns only builds which have buildTime greater or equal to given build --
onlyNewerBuilds : Model -> String -> List Build
onlyNewerBuilds model buildId =
          case (getOldBuildTime model buildId) of
                Just oldBuildTime ->
                    List.filter (\build -> build.buildTime >= oldBuildTime) model.allBuilds
                Nothing ->
                    model.allBuilds


viewDialog : Model -> (State -> toMsg) -> toMsg -> Html toMsg
viewDialog model toMsg confirmMsg =
    case model.oldBuild of
        "" ->
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Please choose an old build and try again" ]
                |> Modal.footer []
                    [ Button.button
                        [ Button.outlinePrimary
                        , Button.onClick <| toMsg Modal.hiddenState
                        ]
                        [ text "Close" ]
                    ]
                |> Modal.view model.confirmationState
        oldBuild -> case model.newBuild of
                        "" ->
                            Modal.config toMsg
                                |> Modal.large
                                |> Modal.h3 [] [ text "Please choose a new build and try again" ]
                                |> Modal.footer []
                                    [ Button.button
                                        [ Button.outlinePrimary
                                        , Button.onClick <| toMsg Modal.hiddenState
                                        ]
                                        [ text "Close" ]
                                    ]
                                |> Modal.view model.confirmationState
                        newBuild ->
                            Modal.config toMsg
                                |> Modal.large
                                |> Modal.h2 [] [ text ("Compare builds " ++ model.oldBuild ++ " and " ++ model.newBuild) ]
                                |> Modal.body [] [ p [] [ text "to be continued ... " ] ]
                                |> Modal.footer []
                                    [ Button.button
                                        [ Button.outlinePrimary
                                        , Button.onClick <| toMsg Modal.hiddenState
                                        ]
                                        [ text "Close" ]
                                    ]
                                |> Modal.view model.confirmationState





----------------- Links examples

--}
--li [class "nav-item" ]
--[ a [class "nav-link", href "#" ]
--[ text "Link" ]
--,li [class "nav-item" ]
-- [ a [class "nav-link active", href "#" ]
-- [ text "Active" ]
--,li [class "nav-item" ]
-- [ a [class "nav-link disabled", href "#" ]
-- [ text "Disabled" ]
