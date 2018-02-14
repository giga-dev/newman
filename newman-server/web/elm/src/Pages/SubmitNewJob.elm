module Pages.SubmitNewJob exposing (..)

import Bootstrap.Form.Select as Select
import Bootstrap.Form.Input as Input
import Bootstrap.Button as Button
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Http exposing (..)
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import Maybe exposing (withDefault)


type Msg
    = GetBuildsAndSuitesCompleted (Result Http.Error BuildsAndSuites)
    | UpdateSelectedSuite String
    | UpdateSelectedBuild String
    | SubmitNewJob (Result Http.Error FutureJob)
    | SubmitRequested
    | UpdatedBuildSelection BuildSelection


type BuildSelection
    = SelectOption
    | FillInManually


type alias Model =
    { buildsAndSuites : BuildsAndSuites
    , selectedBuild : String
    , selectedSuite : String
    , submittedFutureJobId : FutureJob
    , buildSelection : BuildSelection
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
    { id : Maybe String }


init : ( Model, Cmd Msg )
init =
    ( Model (BuildsAndSuites [] []) "" "" (FutureJob Nothing) SelectOption, getBuildsAndSuitesCmd )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetBuildsAndSuitesCompleted result ->
            case result of
                Ok data ->
                    ( { model | buildsAndSuites = data }, Cmd.none )

                Err err ->
                    -- log error
                    ( model, Cmd.none )

        UpdateSelectedBuild build ->
            ( { model | selectedBuild = build }, Cmd.none )

        UpdateSelectedSuite suite ->
            ( { model | selectedSuite = suite }, Cmd.none )

        SubmitRequested ->
            ( model, submitFutureJobCmd model )

        SubmitNewJob result ->
            case result of
                Ok data ->
                    ( { model | submittedFutureJobId = data }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        UpdatedBuildSelection option ->
            ( { model | buildSelection = option }, Cmd.none )


getBuildsAndSuitesCmd : Cmd Msg
getBuildsAndSuitesCmd =
    Http.send GetBuildsAndSuitesCompleted getBuildsAndSuites


getBuildsAndSuites : Http.Request BuildsAndSuites
getBuildsAndSuites =
    Http.get "/api/newman/all-builds-and-suites" buildsAndSuitesDecoder


submitFutureJobCmd : Model -> Cmd Msg
submitFutureJobCmd model =
    let
        postReq =
            postFutureJob model.selectedBuild model.selectedSuite
    in
    Http.send SubmitNewJob postReq


postFutureJob : String -> String -> Http.Request FutureJob
postFutureJob buildId suiteId =
    Http.post ("../api/newman/futureJob/" ++ buildId ++ "/" ++ suiteId) Http.emptyBody futureJobDecoder


view : Model -> Html Msg
view model =
    let
        toOption data =
            Select.item [ value data.id ] [ text data.name ]

        submittedFutureJobFormat jobId =
            [ text ("submitted future job eith id " ++ jobId) ]
    in
    div [ id "page-wrapper" ]
        [ div [ class "container-fluid" ]
            [ div [ class "row" ]
                [ div [ class "col-lg-12" ]
                    [ h1 [ class "page-header" ]
                        [ text "Submit New Job" ]
                    ]
                ]
            , div [ class "row" ]
                [ Select.select [ Select.onChange UpdateSelectedSuite, Select.attrs [ style [("width", "500px")] ] ]
                    ([ Select.item [ value "1" ] [ text "Select a Suite" ]
                     ]
                        ++ List.map toOption model.buildsAndSuites.suites
                    )
                ]
            , br [] []
            , selectBuildView model
            ]
        , Button.button [ Button.secondary,  Button.onClick SubmitRequested, Button.attrs [ style [("margin-top", "15px")]] ] [ text "Submit Future Job" ]
        , br [] []
        , div [] (withDefault [ text "" ] (Maybe.map submittedFutureJobFormat model.submittedFutureJobId.id))
        ]


selectBuildView : Model -> Html Msg
selectBuildView model =
    let
        toOption data =
            Select.item [ value data.id ] [ text data.name ]

        ( isSelect, isManual ) =
            case model.buildSelection of
                SelectOption ->
                    ( True, False )

                _ ->
                    ( False, True )
    in
    div []
        [ div
            [ class "row" ]
            [ radio "Select build :" (UpdatedBuildSelection SelectOption)  isSelect ]
        , div
            [ class "row" ]
            [ Select.select [ Select.disabled isManual, Select.onChange UpdateSelectedBuild, Select.attrs [ style [ ("width", "500px") ] ] ]
                            ([ Select.item [ value "1" ] [ text "Select a Build" ]
                             ]
                                ++ List.map toOption model.buildsAndSuites.builds
                            )
            ]
        , br [] []
        , div
            [ class "row" ]
            [ radio "Enter build id :" (UpdatedBuildSelection FillInManually) isManual ]
        , div
            [ class "row" ]
            [ Input.text [ Input.disabled isSelect, Input.onInput UpdateSelectedBuild, Input.attrs [ style [("width", "500px")] ] ]
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


futureJobDecoder : Json.Decode.Decoder FutureJob
futureJobDecoder =
    decode FutureJob
        |> Json.Decode.Pipeline.required "id" (Json.Decode.maybe Json.Decode.string)


buildsAndSuitesDecoder : Json.Decode.Decoder BuildsAndSuites
buildsAndSuitesDecoder =
    Json.Decode.map2 BuildsAndSuites
        (Json.Decode.field "suites" (Json.Decode.list decodeSuite))
        (Json.Decode.field "builds" (Json.Decode.list decodeBuild))
