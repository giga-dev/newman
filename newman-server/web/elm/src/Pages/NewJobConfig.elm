module Pages.NewJobConfig exposing (..)

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
import Views.NewmanModal as NewmanModal
import Utils.Types exposing (JobConfig,decodeJobConfig)
import Views.NewmanModal as NewmanModal


type Msg
    = GetAllJavaVersionsCompleted (Result Http.Error (List String))
    | UpdateSelectedJavaVersion String
    | UpdateName String
    | SubmitNewJobConfigCompleted (Result Http.Error JobConfig)
    | OnClickSave
    | NewmanModalMsg Modal.State

type alias Model =
    {
      javaVersions : List String
    , selectedJavaVersion : String
    , name : String
    , modalState : Modal.State
        , errorMessage : String
        , savedJobConfig :  Maybe JobConfig
    }



init : ( Model, Cmd Msg )
init =
    ( {
        javaVersions = []
        , selectedJavaVersion =""
        , name = ""
        , modalState = Modal.hiddenState
              , errorMessage = ""
              ,savedJobConfig = Nothing
      }
    , getAllJavaVersionsCmd

    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetAllJavaVersionsCompleted result ->
              case result of
                  Ok versions ->
                      ( { model | javaVersions = versions, selectedJavaVersion = List.head versions |> Maybe.withDefault "" }, Cmd.none )

                  Err err ->
                      let
                          a =
                              Debug.log "GetAllJavaVersionsCompleted" err
                      in
                          ( model, Cmd.none )
        UpdateName msgName->
             ( { model | name = msgName }, Cmd.none )
        UpdateSelectedJavaVersion msgJavaVersion->
             ( { model | selectedJavaVersion = msgJavaVersion }, Cmd.none )
        OnClickSave ->
            case ( model.name, model.selectedJavaVersion ) of
                ( "", _ ) ->
                    ( { model | errorMessage = "Please enter a name", modalState = Modal.visibleState }, Cmd.none )

                ( _, "" ) ->
                    ( { model | errorMessage = "Please select a Java Version", modalState = Modal.visibleState }, Cmd.none )

                _ ->
                    ( model,  saveJobConfigCmd model.name model.selectedJavaVersion)

        SubmitNewJobConfigCompleted  result ->
                case result of
                    Ok data ->
                        ( { model | savedJobConfig = Just data }, Cmd.none )

                    Err err ->
                        ( model, Cmd.none )
        NewmanModalMsg newState ->
                ( { model | modalState = newState }, Cmd.none )






getAllJavaVersionsCmd : Cmd Msg
getAllJavaVersionsCmd =
    Http.send GetAllJavaVersionsCompleted getAllJavaVersions


getAllJavaVersions : Http.Request (List String)
getAllJavaVersions =
    Http.get "/api/newman/java-versions" decodeJavaVersions

decodeJavaVersions : Decoder (List String)
decodeJavaVersions =
    Json.Decode.list Json.Decode.string

saveJobConfigCmd : String -> String -> Cmd Msg
saveJobConfigCmd name javaVersion =
    let
        postReq =
            postJobConfig name javaVersion
    in
        Http.send SubmitNewJobConfigCompleted postReq


postJobConfig : String -> String -> Http.Request  JobConfig
postJobConfig name javaVersion =
--    let
--        jsonify =
--            Http.jsonBody <| Json.Encode.object [ ( "name", Json.Encode.string name ), ( "javaVersion", Json.Encode.string javaVersion ) ]
--    in
      Http.post ("/api/newman/job-config-from-gui?name=" ++ name ++ "&javaVersion="++javaVersion) Http.emptyBody decodeJobConfig


view : Model -> Html Msg
view model =
    let
        savedJobConfigString =
            case model.savedJobConfig of
                Nothing ->
                    ""
                Just jobConfig ->
                    "Finished Saving New Job configuration: " ++ jobConfig.name
--        goBackToJobsConfig =
--                    case model.savedJobConfig of
--                        Nothing ->
--                            ""
--                        Just jobConfig ->
--                            "Go Back to Jobs Configurartions"
    in
        div [ class "container-fluid" ]
            [ h2 [ class "page-header" ]
                [ text "New Job Configuration" ]
            , div [][ text "Name:"]
             ,div
                 []
                 [ Input.text [Input.placeholder "New Configuration Name", Input.onInput UpdateName, Input.attrs [ style [ ( "width", "500px" ) ] ] ]
                 ]
             ,br [][]
            ,let
                toOption data =
                    Select.item [ value data, selected <| model.selectedJavaVersion == data ] [ text <| data ]
              in
                div
                    []
                    [ text "Select Java Version:"
                    , Select.select
                        [ Select.onChange UpdateSelectedJavaVersion, Select.attrs [ style [ ( "width", "500px" ) ] ] ]
                        (List.map toOption model.javaVersions)
                    ]
            , br [] []

            , Button.button [ Button.secondary, Button.onClick OnClickSave, Button.attrs [ style [ ( "margin-top", "15px" ) ] ] ] [ text "Save New Job configuration" ]
            , br [] []
            , br [] []
            , div [style [ ( "color", "#2FDC62" ), ( "width", "500px" ),( "font-weight" , "bold" ) ]]
                ([ text savedJobConfigString ] )
            , br [] []
--            , div [style [ ( "color", "#0275d8" ), ( "width", "500px" ),( "font-weight" , "bold" ) ]]
--                  ([ text goBackToJobsConfig ] )
            , NewmanModal.viewError model.errorMessage NewmanModalMsg model.modalState
            ]



