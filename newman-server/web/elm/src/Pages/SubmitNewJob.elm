module Pages.SubmitNewJob exposing (..)

import Bootstrap.Button as Button
import Bootstrap.Form.Input as Input
import Bootstrap.Form.Select as Select
import Bootstrap.Modal as Modal
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Http exposing (..)
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import Json.Encode
import Maybe exposing (withDefault)
import Multiselect
import Views.NewmanModal as NewmanModal


type Msg
    = GetBuildsAndSuitesCompleted (Result Http.Error BuildsAndSuites)
    | UpdateSelectedBuild String
    | SubmitNewJobCompleted (Result Http.Error (List FutureJob))
    | OnClickSubmit
    | MultiSelectMsg Multiselect.Msg
    | UpdatedBuildSelection Bool
    | NewmanModalMsg Modal.State


type alias Model =
    { buildsAndSuites : BuildsAndSuites
    , selectedBuild : String
    , selectedSuites : Multiselect.Model
    , submittedFutureJobs : List FutureJob
    , isSelect : Bool
    , modalState : Modal.State
    , errorMessage : String
    }


type alias Build =
    { id : String
    , name : String
    , branch : String
    , tags : List String
    }


type alias Suite =
    { id : String
    , name : String
    , customVariables : String
    }


type alias BuildsAndSuites =
    { suites : List Suite
    , builds : List Build
    }


type alias FutureJob =
    { id : String }


init : ( Model, Cmd Msg )
init =
    ( { buildsAndSuites = BuildsAndSuites [] []
      , selectedBuild = ""
      , selectedSuites = Multiselect.initModel [] ""
      , submittedFutureJobs = []
      , isSelect = True
      , modalState = Modal.hiddenState
      , errorMessage = ""
      }
    , getBuildsAndSuitesCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetBuildsAndSuitesCompleted result ->
            case result of
                Ok data ->
                    let
                        suites =
                            List.map (\suite -> ( suite.id, suite.name )) data.suites
                    in
                    ( { model | buildsAndSuites = data, selectedSuites = Multiselect.initModel suites "suites" }, Cmd.none )

                Err err ->
                    -- log error
                    ( model, Cmd.none )

        UpdateSelectedBuild build ->
            ( { model | selectedBuild = build }, Cmd.none )

        MultiSelectMsg subMsg ->
            let
                ( subModel, subCmd, outMsg ) =
                    Multiselect.update subMsg model.selectedSuites
            in
            ( { model | selectedSuites = subModel }, Cmd.map MultiSelectMsg subCmd )

        OnClickSubmit ->
            let
                ( buildId, suitesList ) =
                    ( model.selectedBuild, List.map (\( v, k ) -> v) (Multiselect.getSelectedValues model.selectedSuites) )
            in
            case ( buildId, suitesList ) of
                ( "", _ ) ->
                    ( { model | errorMessage = "Please select a build", modalState = Modal.visibleState }, Cmd.none )

                ( _, [] ) ->
                    ( { model | errorMessage = "Please select one or more suites", modalState = Modal.visibleState }, Cmd.none )

                _ ->
                    ( model, submitFutureJobCmd buildId suitesList )

        SubmitNewJobCompleted result ->
            case result of
                Ok data ->
                    ( { model | submittedFutureJobs = data }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        UpdatedBuildSelection select ->
            ( { model | isSelect = select }, Cmd.none )

        NewmanModalMsg newState ->
            ( { model | modalState = newState }, Cmd.none )


getBuildsAndSuitesCmd : Cmd Msg
getBuildsAndSuitesCmd =
    Http.send GetBuildsAndSuitesCompleted getBuildsAndSuites


getBuildsAndSuites : Http.Request BuildsAndSuites
getBuildsAndSuites =
    Http.get "/api/newman/all-builds-and-suites" buildsAndSuitesDecoder


submitFutureJobCmd : String -> List String -> Cmd Msg
submitFutureJobCmd buildId suites =
    let
        postReq =
            postFutureJob buildId suites
    in
    Http.send SubmitNewJobCompleted postReq


postFutureJob : String -> List String -> Http.Request (List FutureJob)
postFutureJob buildId suites =
    let
        jsonify =
            Http.jsonBody <| Json.Encode.object [ ( "buildId", Json.Encode.string buildId ), ( "suites", Json.Encode.list <| List.map Json.Encode.string suites ) ]
    in
    Http.post "../api/newman/futureJob" jsonify decodeFutureJobs


view : Model -> Html Msg
view model =
    let
        submittedFutureJobString =
            case model.submittedFutureJobs of
                [] ->
                    ""

                _ ->
                    "submitted the folowing future jobs:"
    in
    div [ class "container-fluid" ]
        [ h2 [ class "page-header" ]
            [ text "Submit New Job" ]
        , div [ style [ ( "width", "500px" ) ] ]
            [ text "Select suite:"
            , Multiselect.view model.selectedSuites |> Html.map MultiSelectMsg
            ]
        , br [] []
        , selectBuildView model
        , Button.button [ Button.secondary, Button.onClick OnClickSubmit, Button.attrs [ style [ ( "margin-top", "15px" ) ] ] ] [ text "Submit Future Job" ]
        , br [] []
        , div []
            ([ text submittedFutureJobString ]
                ++ List.map (\job -> div [] [ text job.id ]) model.submittedFutureJobs
            )
        , NewmanModal.viewError model.errorMessage NewmanModalMsg model.modalState
        ]


selectBuildView : Model -> Html Msg
selectBuildView model =
    let
        toOption data =
            Select.item [ value data.id, selected <| model.selectedBuild == data.id ] [ text <| data.name ++ " (" ++ data.branch ++ ")" ]
    in
    div []
        [ div
            []
            [ radio "Select build :" (UpdatedBuildSelection True) model.isSelect ]
        , div
            []
            [ Select.select [ Select.disabled (not model.isSelect), Select.onChange UpdateSelectedBuild, Select.attrs [ style [ ( "width", "500px" ) ] ] ]
                ([ Select.item [ value "1" ] [ text "Select a Build" ]
                 ]
                    ++ List.map toOption model.buildsAndSuites.builds
                )
            ]
        , br [] []
        , div
            []
            [ radio "Enter build id :" (UpdatedBuildSelection False) (not model.isSelect) ]
        , div
            []
            [ Input.text [ Input.disabled model.isSelect, Input.onInput UpdateSelectedBuild, Input.attrs [ style [ ( "width", "500px" ) ] ] ]
            ]
        ]


radio : String -> msg -> Bool -> Html msg
radio textVal msg isChecked =
    label
        []
        [ input [ type_ "radio", name "font-size", onClick msg, checked isChecked ] []
        , text textVal
        ]


decodeBuild : Json.Decode.Decoder Build
decodeBuild =
    decode Build
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "branch" Json.Decode.string
        |> Json.Decode.Pipeline.required "tags" (Json.Decode.list Json.Decode.string)


decodeSuite : Json.Decode.Decoder Suite
decodeSuite =
    decode Suite
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "customVariables" Json.Decode.string


decodeFutureJobs : Json.Decode.Decoder (List FutureJob)
decodeFutureJobs =
    Json.Decode.list futureJobDecoder


futureJobDecoder : Json.Decode.Decoder FutureJob
futureJobDecoder =
    decode FutureJob
        |> Json.Decode.Pipeline.required "id" Json.Decode.string


buildsAndSuitesDecoder : Json.Decode.Decoder BuildsAndSuites
buildsAndSuitesDecoder =
    Json.Decode.map2 BuildsAndSuites
        (Json.Decode.field "suites" (Json.Decode.list decodeSuite))
        (Json.Decode.field "builds" (Json.Decode.list decodeBuild))


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.map MultiSelectMsg <| Multiselect.subscriptions model.selectedSuites
