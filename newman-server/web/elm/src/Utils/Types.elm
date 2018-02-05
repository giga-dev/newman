module Utils.Types exposing (..)

import Dict exposing (Dict)
import Json.Decode exposing (Value)
import Json.Decode.Pipeline exposing (decode)
import Json.Encode
import Paginate exposing (PaginatedList)


type alias Jobs =
    List Job


type alias Job =
    { id : String
    , submitTime : Int
    , submittedBy : String
    , state : String
    , preparingAgents : List String
    , buildId : String
    , buildName : String
    , suiteName : String
    , totalTests : Int
    , failedTests : Int
    , passedTests : Int
    , runningTests : Int
    , startTime : Maybe Int
    , endTime : Maybe Int
    }


type JobState
    = READY
    | RUNNING
    | DONE
    | PAUSED
    | BROKEN


type alias Build =
    { id : BuildId
    , name : String
    , branch : String
    , tags : List String
    , buildTime : Int
    , resources : List String
    , testsMetadata : List String
    , shas : Shas
    }


type alias Builds =
    List Build


type alias PaginatedBuilds =
    PaginatedList Build


type alias BuildId =
    String


type alias Shas =
    Dict String String


type alias Agent =
    { id : String
    , name : String
    , host : String
    , lastTouchTime : Int
    , currentTests : List String
    , state : String
    , capabilities : List String
    , pid : String
    , setupRetries : Int
    , jobId : Maybe String
    , buildName : Maybe String
    , suiteName : Maybe String
    }


type alias Agents =
    List Agent


type alias PaginatedAgents =
    PaginatedList Agent


type alias Suite =
    { id : SuiteId
    , name : String
    , customVariables : String
    }


type alias SuiteWithCriteria =
    { id : SuiteId
    , name : String
    , customVariables : String
    , requirements : List String
    , criteria : String
    }


type alias Suites =
    List Suite


type alias PaginatedSuites =
    PaginatedList Suite


type alias SuiteId =
    String


toJobState : String -> JobState
toJobState str =
    case str of
        "RUNNING" ->
            RUNNING

        "DONE" ->
            DONE

        "PAUSED" ->
            PAUSED

        "BROKEN" ->
            BROKEN

        _ ->
            BROKEN


decodeJob : Json.Decode.Decoder Job
decodeJob =
    decode Job
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "submitTime" Json.Decode.int
        |> Json.Decode.Pipeline.required "submittedBy" Json.Decode.string
        |> Json.Decode.Pipeline.required "state" Json.Decode.string
        |> Json.Decode.Pipeline.required "preparingAgents" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.requiredAt [ "build", "id" ] Json.Decode.string
        |> Json.Decode.Pipeline.requiredAt [ "build", "name" ] Json.Decode.string
        |> Json.Decode.Pipeline.requiredAt [ "suite", "name" ] Json.Decode.string
        |> Json.Decode.Pipeline.required "totalTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "failedTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "passedTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "runningTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "startTime" (Json.Decode.nullable Json.Decode.int)
        |> Json.Decode.Pipeline.required "endTime" (Json.Decode.nullable Json.Decode.int)


decodeJobs : Json.Decode.Decoder Jobs
decodeJobs =
    Json.Decode.field "values" (Json.Decode.list decodeJob)


decodeBuild : Json.Decode.Decoder Build
decodeBuild =
    decode Build
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "branch" Json.Decode.string
        |> Json.Decode.Pipeline.required "tags" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "buildTime" Json.Decode.int
        |> Json.Decode.Pipeline.required "resources" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "testsMetadata" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "shas" (Json.Decode.dict Json.Decode.string)


decodeBuilds : Json.Decode.Decoder Builds
decodeBuilds =
    Json.Decode.field "values" (Json.Decode.list decodeBuild)


decodeAgents : Json.Decode.Decoder Agents
decodeAgents =
    Json.Decode.field "values" (Json.Decode.list decodeAgent)


decodeAgent : Json.Decode.Decoder Agent
decodeAgent =
    decode Agent
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "host" Json.Decode.string
        |> Json.Decode.Pipeline.required "lastTouchTime" Json.Decode.int
        |> Json.Decode.Pipeline.required "currentTests" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "state" Json.Decode.string
        |> Json.Decode.Pipeline.required "capabilities" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "pid" Json.Decode.string
        |> Json.Decode.Pipeline.required "setupRetries" Json.Decode.int
        |> Json.Decode.Pipeline.optionalAt [ "job", "id" ] (Json.Decode.maybe Json.Decode.string) Nothing
        |> Json.Decode.Pipeline.optionalAt [ "job", "build", "name" ] (Json.Decode.maybe Json.Decode.string) Nothing
        |> Json.Decode.Pipeline.optionalAt [ "job", "suite", "name" ] (Json.Decode.maybe Json.Decode.string) Nothing


decodeSuites : Json.Decode.Decoder Suites
decodeSuites =
    Json.Decode.field "values" (Json.Decode.list decodeSuite)


decodeSuite : Json.Decode.Decoder Suite
decodeSuite =
    decode Suite
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "customVariables" Json.Decode.string


decodeSuiteWithCriteria : Json.Decode.Decoder SuiteWithCriteria
decodeSuiteWithCriteria =
    decode SuiteWithCriteria
        |> Json.Decode.Pipeline.required "id" Json.Decode.string
        |> Json.Decode.Pipeline.required "name" Json.Decode.string
        |> Json.Decode.Pipeline.required "customVariables" Json.Decode.string
        |> Json.Decode.Pipeline.required "requirements" (Json.Decode.list Json.Decode.string)
        |> Json.Decode.Pipeline.required "criteria" (Json.Decode.map (Json.Encode.encode 4) Json.Decode.value)
