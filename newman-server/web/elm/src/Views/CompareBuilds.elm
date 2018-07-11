module Views.CompareBuilds exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Utils.Types exposing (..)
import Bootstrap.Form.Select as Select
import Bootstrap.Button as Button
import Bootstrap.Modal as Modal exposing (..)
import Dict exposing (Dict)


type alias Model =
    { allBuilds : Builds
    , oldBuildId : String
    , newBuildId : String
    , oldBuild : Maybe Build
    , newBuild : Maybe Build
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
    Model builds "" "" Nothing Nothing False builds Modal.hiddenState


view : Model -> Html Msg
view model =
    let
        chooseOldBuild data =
            Select.item [ value data.id, selected <| model.oldBuildId == data.id ]
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
            Select.item [ value data.id, selected <| model.newBuildId == data.id ]
                [ text <| data.name ++ " (" ++ data.branch ++ ")" ]

        newBuildSelect =
            div []
                [ Select.select
                    [ Select.disabled (not model.oldSelected)
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
            case (maybeGetBuild model build) of
                Just build ->
                    ( { model
                        | oldBuild = Just build
                        , oldSelected = True
                        , oldBuildId = build.id
                        , newerBuilds = (onlyNewerBuilds model build)
                      }
                    , Cmd.none
                    )

                Nothing ->
                    ( { model
                        | oldBuild = Nothing
                        , oldBuildId = ""
                        , newBuild = Nothing
                        , newBuildId = ""
                        , oldSelected = False
                      }
                    , Cmd.none
                    )

        UpdateNewBuild build ->
            case (maybeGetBuild model build) of
                Just build ->
                    ( { model | newBuild = Just build, newBuildId = build.id }, Cmd.none )

                Nothing ->
                    ( { model | newBuild = Nothing, newBuildId = "" }, Cmd.none )

        ClickCompareBuilds ->
            ( { model | confirmationState = Modal.visibleState }, Cmd.none )

        AcknowledgeDialog ->
            ( { model | confirmationState = Modal.hiddenState }, Cmd.none )

        ModalMsg newState ->
            ( { model | confirmationState = newState }, Cmd.none )



-- With a given buildId returns only builds which have buildTime greater or equal to given build --
onlyNewerBuilds : Model -> Build -> List Build
onlyNewerBuilds model newBuild =
    List.filter (\build -> build.buildTime >= newBuild.buildTime) model.allBuilds



-- get build from build id after selecting item from dropdown --
maybeGetBuild : Model -> String -> Maybe Build
maybeGetBuild model buildId =
    case buildId of
        "1" ->
            Nothing

        value ->
            model.allBuilds
                |> List.filter (\build -> build.id == value)
                |> List.head



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

    in
        case model.oldBuild of
                Just oldBuild ->
                    case model.newBuild of
                        Just newBuild ->
                            Modal.config toMsg
                                |> Modal.large
                                |> Modal.h3 [] [ text ("Compare builds "
                                                        ++ oldBuild.name
                                                        ++ " ("
                                                        ++ oldBuild.branch
                                                        ++ ") and "
                                                        ++ newBuild.name
                                                        ++ " ("
                                                        ++ newBuild.branch
                                                        ++ ")") ]
                                |> Modal.body [] [ li [ class "nav-item" ]
                                                     [ a [ class "nav-link", href "https://www.kongcompany.com/products/for-dogs/rubber-toys/" ]
                                                         [ text "puppy stuf" ]
                                                     ]
                                                 ]
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

--createUrls : Shas -> Shas -> List String
--createUrls oldBuildShas newBuildShas =
--        case (Dict.isEmpty oldBuildShas || Dict.isEmpty newBuildShas) of
--            True ->
--                []
--            False ->
--                Dict.map (case Dict.get k newBuildShas of
--                                        Just newSha ->
--                                            ("/compare/" ++ v ++ "..." ++ newSha)
--                                        Nothing ->
--                                            ""
--                         ) oldBuildShas





----------------- Links examples
--li [class "nav-item" ]
--[ a [class "nav-link", href "#" ]
--[ text "Link" ]
--,li [class "nav-item" ]
-- [ a [class "nav-link active", href "#" ]
-- [ text "Active" ]
--,li [class "nav-item" ]
-- [ a [class "nav-link disabled", href "#" ]
-- [ text "Disabled" ]
