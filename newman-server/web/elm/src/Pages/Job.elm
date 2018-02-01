module Pages.Job exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import UrlParser exposing (Parser)
import Http exposing (..)
import Json.Decode
import Json.Decode.Pipeline exposing (decode)
import Bootstrap.Progress as Progress exposing (..)
import Bootstrap.Button as Button exposing (..)


type alias Model =
    { maybeJob : Maybe Job
    }


type alias JobId =
    String


type alias Build =
    { id : String
    , name : String
    , branch : String
    }


type alias Suite =
    { id : String
    , name : String
    , requirements : List String
    }


type alias Job =
    { id : String
    , submitTime : Int

    --    , startTime : Int
    --    , endTime : Int
    , submittedBy : String
    , state : String
    , totalTests : Int
    , passedTests : Int
    , failedTests : Int
    , runningTests : Int

    --    , startPrepareTime : Int
    , preparingAgents : List String
    , agents : List String

    --    , jobSetupLogs : String -- TODO - how to Dict in ELM
    , build : Build
    , suite : Suite
    }


type Msg
    = GetJobInfoCompleted (Result Http.Error Job)



-- external


parseJobId : Parser (String -> a) a
parseJobId =
    UrlParser.string


init : JobId -> ( Model, Cmd Msg )
init jobId =
    ( Model Nothing, getJobInfoCmd jobId )


viewHeader : Job -> Html Msg
viewHeader job =
    let
        buildRow =
            tr []
                [ td []
                    [ text "Build"
                    ]
                , td []
                    [ a [ href <| "#build/" ++ job.build.id ] [ text <| job.build.name ++ " (" ++ job.build.branch ++ ")" ]
                    ]
                ]

        progressRow =
            viewRow
                ( "Progress"
                , Progress.progress
                    [ Progress.value <| toFloat <| (job.runningTests + job.failedTests + job.passedTests) * 100 // job.totalTests
                    , Progress.label <| toString <| (job.runningTests + job.failedTests + job.passedTests) * 100 // job.totalTests
                    ]
                )

        stateRow =
            let
                buttonColor =
                    case job.state of
                        "BROKEN" ->
                            Button.danger
                        "DONE" ->
                            Button.success
                        "PAUSED" ->
                            Button.warning
                        _ ->
                            Button.info
            in
                viewRow
                    ( "State"
                    , Button.button [ buttonColor ]
                        [ text job.state
                        ]
                    )

        headerRows =
            []

        viewRow ( name, value ) =
            tr []
                [ td [] [ text name ]
                , td [] [ value ]
                ]
    in
        table [ ] <|
            [ buildRow
            , progressRow
            , stateRow
            ]
                ++ List.map
                    viewRow
                    headerRows


view : Model -> Html Msg
view model =
    case model.maybeJob of
        Just job ->
            div []
                [ viewHeader job
                ]

        Nothing ->
            div []
                [ text "Loading..."
                ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        d =
            Debug.log "Job.update" "was called"
    in
        case msg of
            GetJobInfoCompleted result ->
                case result of
                    Ok data ->
                        ( { model | maybeJob = Just data }, Cmd.none )

                    Err err ->
                        ( model, Cmd.none )


getJobInfoCmd : JobId -> Cmd Msg
getJobInfoCmd jobId =
    Http.send GetJobInfoCompleted <|
        Http.get ("/api/newman/job/" ++ jobId) decodeJob


decodeBuild : Json.Decode.Decoder Build
decodeBuild =
    decode Build
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "branch" Json.Decode.string


decodeSuite : Json.Decode.Decoder Suite
decodeSuite =
    decode Suite
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "requirements" (Json.Decode.list Json.Decode.string)


decodeJob : Json.Decode.Decoder Job
decodeJob =
    decode Job
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "submitTime" Json.Decode.int
        --        |> Json.Decode.Pipeline.required "startTime" Json.Decode.int
        --        |> Json.Decode.Pipeline.required "endTime" Json.Decode.int
        |> Json.Decode.Pipeline.required "submittedBy" Json.Decode.string
        |> Json.Decode.Pipeline.required "state" Json.Decode.string
        |> Json.Decode.Pipeline.required "totalTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "passedTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "failedTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "runningTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "preparingAgents" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "agents" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "build" decodeBuild
        |> Json.Decode.Pipeline.required "suite" decodeSuite
