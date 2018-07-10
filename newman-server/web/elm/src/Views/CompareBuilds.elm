module Views.CompareBuilds exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Utils.Types exposing (..)
import Bootstrap.Form.Select as Select


type alias Model =
    { allBuilds : Builds
    , oldBuild : String
    , oldBuildTime : Maybe Int
    , newBuild : String
    , newSelected : Bool
    , newerBuilds : Builds
    }


type Msg
    = UpdateOldBuild String
    | UpdateNewBuild String


init : Builds -> Model
init builds =
    Model builds "" Nothing "" False builds


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
                             [ Select.select [ Select.disabled (not model.newSelected), Select.onChange UpdateNewBuild, Select.attrs [ style [ ( "width", "400px" ) ]]]
                                                  ([ Select.item [ value "1" ] [ text "Select New Build" ]]
                                                        ++ List.map chooseNewBuild model.newerBuilds
                                                  )]
    in
        div []
            [ div [ class "form-inline" ]
                [ oldBuildSelect
                , newBuildSelect
                ]
            ]

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        UpdateOldBuild buildId ->
            let
                printOldBuildSelected = Debug.log "UpdateOldBuild" buildId
            in
            ( { model | oldBuild = buildId
                        , oldBuildTime = (getOldBuildTime model buildId)
                        , newSelected = True
                        , newerBuilds = (onlyNewerBuilds model buildId) } , Cmd.none)

        UpdateNewBuild buildId ->
            let
                printNewBuildSelected = Debug.log "UpdateNewBuild" buildId
            in
            ( { model | newBuild = buildId } , Cmd.none )




getOldBuildTime : Model -> String -> Maybe Int
getOldBuildTime model buildId =
                     model.allBuilds
                            |> List.filter (\build -> build.id == buildId)
                            |> List.map .buildTime
                            |> List.head


onlyNewerBuilds : Model -> String -> List Build
onlyNewerBuilds model buildId =
          case (getOldBuildTime model buildId) of
                Just oldBuildTime ->
                    List.filter (\build -> build.buildTime >= oldBuildTime) model.allBuilds
                Nothing ->
                    model.allBuilds



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
