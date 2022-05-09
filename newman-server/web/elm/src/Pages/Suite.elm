module Pages.Suite exposing (..)

import Bootstrap.Form.Input as Input
import Bootstrap.Modal as Modal
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http exposing (..)
import Json.Decode exposing (decodeString, keyValuePairs)
import Json.Encode exposing (Value)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal
import String exposing (..)
import Json.Encode exposing (encode, string)

appendNewLine: String -> String
appendNewLine str = String.append str "\n"


isNullable: String -> Bool
isNullable x =
  if contains "null," x  || endsWith "{}," x || endsWith "[]," x || (contains ": 0," x && contains "progressPercent" x || (contains "runNumber" x && contains ": 1," x) ) then

    False
  else
    True

type alias Model =
    { maybeSuite : Maybe SuiteWithCriteria
    , criteriaString : String
    , confirmationUpdateSuiteState : Modal.State
    , updateErrorState : Modal.State
    }


type Msg
    = GetSuiteInfoCompleted (Result Http.Error SuiteWithCriteria)
    | WebSocketEvent WebSocket.Event
    | UpdateSuiteCriteria String
    | UpdateSuiteName String
    | UpdateSuiteRequirements String
    | UpdateSuiteCustomVariables String
    | CompleteApplyChanges (Result Http.Error SuiteWithCriteria)
    | CloseUpdateSuiteModal Modal.State
    | OnSuiteUpdateConfirmed SuiteWithCriteria
    | OnClickCApply
    | CloseErrorModal Modal.State


parseSuiteId : Parser (String -> a) a
parseSuiteId =
    UrlParser.string


init : SuiteId -> ( Model, Cmd Msg )
init suiteId =
    ( { maybeSuite = Nothing, criteriaString = "", confirmationUpdateSuiteState = Modal.hiddenState, updateErrorState = Modal.hiddenState }, getSuiteInfoCmd suiteId )


viewSuite : SuiteWithCriteria -> String -> Modal.State -> Modal.State -> Html Msg
viewSuite suite criteriaString updateModalState errorModalState =
    let
        viewRow ( name, value ) =
            tr [ height 50 ]
                [ td [ width 150 ] [ text name ]
                , td [] [ value ]
                ]

        viewEditableNameRow ( name, value ) =
            tr [ height 50 ]
                [ td [ width 150 ] [ text name ]
                , div
                    []
                    [ Input.text [ Input.onInput UpdateSuiteName, Input.defaultValue value, Input.attrs [ style [ ( "width", "1300px" ) ] ] ]
                    ]
                ]

        viewEditableCustomVariablesRow ( name, value ) =
            tr [ height 50 ]
                [ td [ width 150 ] [ text name ]
                , div []
                    [ Input.text [ Input.onInput UpdateSuiteCustomVariables, Input.defaultValue value, Input.attrs [ style [ ( "width", "1300px" ) ] ] ]
                    ]
                ]

        viewEditableRequirementsRow ( name, value ) =
            tr [ height 50 ]
                [ td [ width 150 ] [ text name ]
                , div []
                    [ Input.text [ Input.onInput UpdateSuiteRequirements, Input.defaultValue value, Input.attrs [ style [ ( "width", "1300px" ) ] ] ]
                    ]
                ]

        formCriteria =
            let
                styleTextArea =
                    case Json.Decode.decodeString Json.Decode.value criteriaString of
                        Ok ok ->
                            style [ ( "margin", "0px" ), ( "overflow", "auto" ), ( "resize", "both" ) ]

                        Err error ->
                            style [ ( "outline-color", "red" ), ( "border", "solid 2px red" ), ( "margin", "0px" ), ( "overflow", "auto" ), ( "resize", "both" ) ]
            in
            tr []
                [ td []
                    [ text "Criteria" ]
                , td []
                    [ Html.form []
                        [ textarea
                            [ name "criteria"
                            , cols 150
                            , rows 30
                            , styleTextArea
                            , onInput UpdateSuiteCriteria
                            ]
                            [ text <| criteriaString ]
                        ]
                    ]
                ]

        viewErrorMessage =
            tr []
                [ td [] []
                , if String.isEmpty suite.name then
                    td [ style [ ( "height", "30px" ), ( "color", "red" ) ] ] [ text "Empty name is not allowed" ]

                  else
                    case Json.Decode.decodeString Json.Decode.value criteriaString of
                        Ok ok ->
                            td [ style [ ( "height", "30px" ), ( "color", "white" ) ] ] [ text "Valid Json" ]

                        Err err ->
                            td [ style [ ( "height", "30px" ), ( "color", "red" ) ] ] [ text "Invalid Json format" ]
                ]

        viewButton =
            tr []
                [ td [] []
                , if String.isEmpty suite.name then
                    button [ disabled True, onClick OnClickCApply ] [ text "Apply Changes" ]

                  else
                    case Json.Decode.decodeString Json.Decode.value criteriaString of
                        Ok ok ->
                            button [ onClick OnClickCApply ] [ text "Apply Changes" ]

                        Err err ->
                            button [ disabled True, onClick OnClickCApply ] [ text "Apply Changes" ]
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Suite" ]
        , table []
            [ viewEditableNameRow ( "Name", suite.name )
            , viewRow ( "Id", text suite.id )
            , viewEditableCustomVariablesRow ( "Custom Variables", suite.customVariables )
            , viewEditableRequirementsRow ( "Requirements", String.join "," suite.requirements )
            , formCriteria
            , viewErrorMessage
            , viewButton
            , NewmanModal.updateSuiteConfirmationModal suite CloseUpdateSuiteModal OnSuiteUpdateConfirmed updateModalState
            , NewmanModal.viewError ("An error occured while attempting to update suite: " ++ suite.name) CloseErrorModal errorModalState
            ]
        , br [] []
        ]


view : Model -> Html Msg
view model =
    case model.maybeSuite of
        Just suite ->
            div []
                [ viewSuite suite model.criteriaString model.confirmationUpdateSuiteState model.updateErrorState
                ]

        Nothing ->
            div []
                [ text "Loading suite..."
                ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetSuiteInfoCompleted result ->
            case result of
                Ok data ->
                    ( Debug.log (toString data.criteria)
                    {
                    model | maybeSuite = Just data, criteriaString = concat (List.map appendNewLine (List.filter isNullable (split "\n" ( Json.Encode.encode 4 data.criteria)))) }, Cmd.none )
                Err err ->
                    ( model, Cmd.none )

        WebSocketEvent event ->
            case event of
                ModifiedSuite suite ->
                    case model.maybeSuite of
                        Just modelSuite ->
                            if modelSuite.id == suite.id then
                                ( { model | maybeSuite = updateSuite suite modelSuite }, Cmd.none )

                            else
                                ( model, Cmd.none )

                        Nothing ->
                            ( model, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        UpdateSuiteCriteria newCriteria ->
            ( { model | criteriaString = newCriteria }, Cmd.none )

        CompleteApplyChanges result ->
            case result of
                Ok suite ->
                    ( model, Cmd.none )

                Err err ->
                    ( { model | updateErrorState = Modal.visibleState }, Cmd.none )

        UpdateSuiteName name ->
            case model.maybeSuite of
                Just modelSuite ->
                    ( { model | maybeSuite = updateSuiteName modelSuite name }, Cmd.none )

                Nothing ->
                    ( model, Cmd.none )

        UpdateSuiteRequirements requirements ->
            case model.maybeSuite of
                Just modelSuite ->
                    ( { model | maybeSuite = updateSuiteRequirements modelSuite requirements }, Cmd.none )

                Nothing ->
                    ( model, Cmd.none )

        UpdateSuiteCustomVariables customVariables ->
            case model.maybeSuite of
                Just modelSuite ->
                    ( { model | maybeSuite = updateSuiteCustomVariables modelSuite customVariables }, Cmd.none )

                Nothing ->
                    ( model, Cmd.none )

        CloseUpdateSuiteModal state ->
            ( { model | confirmationUpdateSuiteState = state }, Cmd.none )

        OnSuiteUpdateConfirmed suite ->
            ( { model | confirmationUpdateSuiteState = Modal.hiddenState }, applyChangesCmd (updateSuiteCriteria suite model.criteriaString) )

        OnClickCApply ->
            ( { model | confirmationUpdateSuiteState = Modal.visibleState }, Cmd.none )

        CloseErrorModal state ->
            ( { model | updateErrorState = state }, Cmd.none )


getSuiteInfoCmd : SuiteId -> Cmd Msg
getSuiteInfoCmd suiteId =
    Http.send GetSuiteInfoCompleted <|
        Http.get ("/api/newman/suite/" ++ suiteId) decodeSuiteWithCriteria


applyChangesCmd : SuiteWithCriteria -> Cmd Msg
applyChangesCmd suite =
    Http.send CompleteApplyChanges <| Http.post "/api/newman/update-suite/" (Http.jsonBody (encodeSuiteWithCriteria suite)) decodeSuiteWithCriteria


updateSuite : Suite -> SuiteWithCriteria -> Maybe SuiteWithCriteria
updateSuite suite modelSuite =
    Just { modelSuite | customVariables = suite.customVariables }


updateSuiteCriteria : SuiteWithCriteria -> String -> SuiteWithCriteria
updateSuiteCriteria modelSuite newCriteria =
    case Json.Decode.decodeString Json.Decode.value newCriteria of
        Ok newCriteriaValue ->
            { modelSuite | criteria = newCriteriaValue }

        Err err ->
            modelSuite


updateSuiteName : SuiteWithCriteria -> String -> Maybe SuiteWithCriteria
updateSuiteName suite name =
    Just { suite | name = name }


updateSuiteCustomVariables : SuiteWithCriteria -> String -> Maybe SuiteWithCriteria
updateSuiteCustomVariables suite customVariables =
    Just { suite | customVariables = customVariables }


updateSuiteRequirements : SuiteWithCriteria -> String -> Maybe SuiteWithCriteria
updateSuiteRequirements suite requirements =
    Just { suite | requirements = String.split "," requirements }


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
