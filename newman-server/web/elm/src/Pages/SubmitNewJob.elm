module Pages.SubmitNewJob exposing (..)

import Http exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)


type Msg
    = GetBuildsAndSuitesCompleted (Result Http.Error BuildsAndSuites)


type alias Model =
    { buildsAndSuites : BuildsAndSuites
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


init : ( Model, Cmd Msg )
init =
    ( Model (BuildsAndSuites [] []), getBuildsAndSuitesCmd )


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


getBuildsAndSuitesCmd : Cmd Msg
getBuildsAndSuitesCmd =
    Http.send GetBuildsAndSuitesCompleted getBuildsAndSuites


getBuildsAndSuites : Http.Request BuildsAndSuites
getBuildsAndSuites =
    Http.get ("/api/newman/all-builds-and-suites") buildsAndSuitesDecoder



{-
   GetBuildsAndSuitesCompleted result ->
            case result of
                Ok data ->
                    ( { model | buildsAndSuites = data }, Cmd.none )

                Err err ->
                    -- log error
                    ( model, Cmd.none )

-}


view : Model -> Html Msg
view model =
    let
        toOption data =
            option [ value data.id ] [ text data.name ]
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
                    [ select []
                        ([ option [ value "1" ] [ text "Select a Suite" ]
                         ]
                            ++ List.map toOption model.buildsAndSuites.suites
                        )
                    , br [] []
                    , select []
                        ([ option [ value "1" ] [ text "Select a Build" ]
                         ]
                            ++ List.map toOption model.buildsAndSuites.builds
                        )
                    ]
                ]
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


buildsAndSuitesDecoder : Json.Decode.Decoder BuildsAndSuites
buildsAndSuitesDecoder =
    Json.Decode.map2 BuildsAndSuites
        (Json.Decode.field "suites" (Json.Decode.list decodeSuite))
        (Json.Decode.field "builds" (Json.Decode.list decodeBuild))
