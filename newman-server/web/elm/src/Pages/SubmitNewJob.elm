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
import Utils.Types as Types exposing (Agent, Agents, Build, JobConfig, Suite, decodeAgentGroups, decodeJobConfigs, decodeSuite)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal


type Msg
    = GetBuildsAndSuitesCompleted (Result Http.Error BuildsAndSuites)
    | GetAllConfigsCompleted (Result Http.Error (List JobConfig))
    | GetAllAgentGroupsCompleted (Result Http.Error (List String))
    | UpdateSelectedBuild String
    | UpdateSelectedConfig String
    | UpdateSelectedPriority String
    | SubmitNewJobCompleted (Result Http.Error (List FutureJob))
    | OnClickSubmit
    | MultiSelectAgentGroupsMsg Multiselect.Msg
    | MultiSelectMsg Multiselect.Msg
    | UpdatedBuildSelection Bool
    | NewmanModalMsg Modal.State
    | WebSocketEvent WebSocket.Event


type alias Model =
    { buildsAndSuites : BuildsAndSuites
    , selectedBuild : String
    , selectedSuites : Multiselect.Model
    , configurations : List JobConfig
    , selectedConfig : String
    , agentGroups : List String
    , selectedAgentGroups : Multiselect.Model
    , priorities : List Int
    , selectedPriority : Int
    , submittedFutureJobs : List FutureJob
    , isSelect : Bool
    , modalState : Modal.State
    , errorMessage : String
    }


type alias BuildsAndSuites =
    { suites : List Suite
    , builds : List ThinBuild
    }


type alias FutureJob =
    { id : String }


init : ( Model, Cmd Msg )
init =
    ( { buildsAndSuites = BuildsAndSuites [] []
      , selectedBuild = ""
      , selectedSuites = Multiselect.initModel [] ""
      , selectedConfig = ""
      , configurations = []
      , selectedAgentGroups = Multiselect.initModel [] ""
      , agentGroups = []
      , priorities = [ 0, 1, 2, 3, 4 ]
      , selectedPriority = 0
      , submittedFutureJobs = []
      , isSelect = True
      , modalState = Modal.hiddenState
      , errorMessage = ""
      }
    , Cmd.batch
        [ getBuildsAndSuitesCmd
        , getAllConfigsCmd
        , getAllAgentGroupsCmd
        ]
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
                    let
                        e =
                            Debug.log "ERROR:GetBuildsAndSuitesCompleted" err
                    in
                    ( model, Cmd.none )

        GetAllConfigsCompleted result ->
            case result of
                Ok data ->
                    ( { model | configurations = data, selectedConfig = Maybe.withDefault "" <| Maybe.map (\v -> v.id) <| List.head data }, Cmd.none )

                Err err ->
                    let
                        e =
                            Debug.log "ERROR:GetAllConfigsCompleted" err
                    in
                    ( model, Cmd.none )

        GetAllAgentGroupsCompleted result ->
            case result of
                Ok data ->
                    let
                        agentGroups =
                            List.map (\item -> ( item, item )) data
                    in
                    ( { model | agentGroups = data, selectedAgentGroups = Multiselect.populateValues model.selectedAgentGroups agentGroups agentGroups }, Cmd.none )

                Err err ->
                    let
                        e =
                            Debug.log "ERROR:GetAllAgentGroupsCompleted" err
                    in
                    ( model, Cmd.none )

        UpdateSelectedBuild build ->
            ( { model | selectedBuild = build }, Cmd.none )

        UpdateSelectedConfig config ->
            ( { model | selectedConfig = config }, Cmd.none )

        MultiSelectAgentGroupsMsg agentGroups ->
            let
                ( subModel, subCmd, outMsg ) =
                    Multiselect.update agentGroups model.selectedAgentGroups
            in
            ( { model | selectedAgentGroups = subModel }, Cmd.map MultiSelectAgentGroupsMsg subCmd )

        MultiSelectMsg subMsg ->
            let
                ( subModel, subCmd, outMsg ) =
                    Multiselect.update subMsg model.selectedSuites
            in
            ( { model | selectedSuites = subModel }, Cmd.map MultiSelectMsg subCmd )

        UpdateSelectedPriority priority ->
            ( { model | selectedPriority = String.toInt priority |> Result.withDefault 0 }, Cmd.none )

        OnClickSubmit ->
            let
                ( buildId, suitesList, configId, agentGroupsList, priority ) =
                    ( model.selectedBuild
                    , List.map (\( v, k ) -> v) (Multiselect.getSelectedValues model.selectedSuites)
                    , model.selectedConfig
                    , List.map (\( v, k ) -> v) (Multiselect.getSelectedValues model.selectedAgentGroups)
                    , model.selectedPriority
                    )
            in
            case ( buildId, suitesList, configId, agentGroupsList ) of
                ( "", _, _, _ ) ->
                    ( { model | errorMessage = "Please select a build", modalState = Modal.visibleState }, Cmd.none )

                ( _, [], _, _ ) ->
                    ( { model | errorMessage = "Please select one or more suites", modalState = Modal.visibleState }, Cmd.none )

                ( _, _, "", _ ) ->
                    ( { model | errorMessage = "Please select a Job Configuration", modalState = Modal.visibleState }, Cmd.none )

                ( _, _, _, [] ) ->
                    ( { model | errorMessage = "Please select one or more agent groups", modalState = Modal.visibleState }, Cmd.none )

                _ ->
                    ( model, submitFutureJobCmd buildId suitesList configId agentGroupsList priority )

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

        WebSocketEvent event ->
            case event of
                CreatedBuild build ->
                    ( updateBuildAdded model build, Cmd.none )

                CreatedSuite suite ->
                    ( updateSuiteAdded model suite, Cmd.none )

                CreatedJobConfig jobConfig ->
                    ( updateJobConfigAdded model jobConfig, Cmd.none )

                _ ->
                    ( model, Cmd.none )


updateBuildAdded : Model -> Build -> Model
updateBuildAdded model build =
    let
        thinBuild =
            buildToThinBuild build
    in
    { model | buildsAndSuites = { builds = thinBuild :: model.buildsAndSuites.builds, suites = model.buildsAndSuites.suites } }


updateSuiteAdded : Model -> Suite -> Model
updateSuiteAdded model suite =
    { model | buildsAndSuites = { suites = suite :: model.buildsAndSuites.suites, builds = model.buildsAndSuites.builds } }


updateJobConfigAdded : Model -> JobConfig -> Model
updateJobConfigAdded model jobConfig =
    { model | configurations = jobConfig :: model.configurations }


getBuildsAndSuitesCmd : Cmd Msg
getBuildsAndSuitesCmd =
    Http.send GetBuildsAndSuitesCompleted <| Http.get "/api/newman/all-builds-and-suites" buildsAndSuitesDecoder


getAllAgentGroupsCmd : Cmd Msg
getAllAgentGroupsCmd =
    Http.send GetAllAgentGroupsCompleted <| Http.get "/api/newman/availableAgentGroups" decodeAgentGroups


getAllConfigsCmd : Cmd Msg
getAllConfigsCmd =
    Http.send GetAllConfigsCompleted <| Http.get "/api/newman/job-config" decodeJobConfigs


submitFutureJobCmd : String -> List String -> String -> List String -> Int -> Cmd Msg
submitFutureJobCmd buildId suites configId agentGroups priority =
    let
        postReq =
            postFutureJob buildId suites configId agentGroups priority
    in
    Http.send SubmitNewJobCompleted postReq


postFutureJob : String -> List String -> String -> List String -> Int -> Http.Request (List FutureJob)
postFutureJob buildId suites configId agentGroupsList priority =
    let
        jsonify =
            Http.jsonBody <|
                Json.Encode.object
                    [ ( "buildId", Json.Encode.string buildId )
                    , ( "suites", Json.Encode.list <| List.map Json.Encode.string suites )
                    , ( "configId", Json.Encode.string configId )
                    , ( "agentGroups", Json.Encode.list <| List.map Json.Encode.string agentGroupsList )
                    , ( "priority", Json.Encode.int priority )
                    ]
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
                    "submitted the following future jobs:"
    in
    div [ class "container-fluid" ]
        [ h2 [ class "page-header" ]
            [ text "Submit New Job" ]
        , div [ style [ ( "width", "500px" ) ] ]
            [ text "Select suites:"
            , Multiselect.view model.selectedSuites |> Html.map MultiSelectMsg
            ]
        , br [] []
        , let
            toOption data =
                Select.item [ value data.id, selected <| model.selectedConfig == data.id ] [ text <| data.name ]
          in
          div
            []
            [ text "Select Job Configuration:"
            , Select.select
                [ Select.onChange UpdateSelectedConfig, Select.attrs [ style [ ( "width", "500px" ) ] ] ]
                (List.map toOption model.configurations)
            ]
        , br [] []
        , selectBuildView model
        , br [] []
        , div [ style [ ( "width", "500px" ) ] ]
            [ text "Select agent groups:"
            , Multiselect.view model.selectedAgentGroups |> Html.map MultiSelectAgentGroupsMsg
            ]
        , br [] []
        , let
            toPriorityOption data =
                Select.item [ value <| toString data, selected <| model.selectedPriority == data ] [ text <| toString data ]
          in
          div
            []
            [ text "Select priority:"
            , Select.select
                [ Select.onChange UpdateSelectedPriority, Select.attrs [ style [ ( "width", "500px" ) ] ] ]
                (List.map toPriorityOption model.priorities)
            ]
        , br [] []
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
            Select.item [ value data.id, selected <| model.selectedBuild == data.id ] [ text <| data.name ++ " (" ++ data.branch ++ ")" ++ " (" ++ String.join "," data.tags ++ ")" ]
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



--- Types


type alias ThinBuild =
    { id : String
    , name : String
    , branch : String
    , tags : List String
    }


buildToThinBuild : Build -> ThinBuild
buildToThinBuild build =
    ThinBuild build.id build.name build.branch build.tags


decodeThinBuild : Json.Decode.Decoder ThinBuild
decodeThinBuild =
    decode ThinBuild
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "branch" Json.Decode.string
        |> Json.Decode.Pipeline.required "tags" (Json.Decode.list Json.Decode.string)


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
        (Json.Decode.field "builds" (Json.Decode.list decodeThinBuild))


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Sub.map MultiSelectMsg <| Multiselect.subscriptions model.selectedSuites
        , Sub.map MultiSelectAgentGroupsMsg <| Multiselect.subscriptions model.selectedAgentGroups
        ]


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
