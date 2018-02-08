module Utils.Types exposing (..)

import Dict exposing (Dict)
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (..)
import Json.Encode
import Paginate exposing (PaginatedList)


type alias Jobs =
    List Job


type alias JobId =
    String


type alias Job =
    { id : String
    , submitTime : Int
    , submittedBy : String
    , state : String
    , preparingAgents : List String
    , agents : List String
    , build : Build
    , suiteName : String
    , totalTests : Int
    , failedTests : Int
    , passedTests : Int
    , runningTests : Int
    , startTime : Maybe Int
    , endTime : Maybe Int
    , jobSetupLogs : Dict String String
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


type alias DashboardBuild =
    { id : BuildId
    , name : String
    , branch : String
    , tags : List String
    , buildTime : Int
    , buildStatus : String
    }


type alias DashboardBuilds =
    List DashboardBuild


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


type alias User =
    { userName : String
    }

type alias TestHistoryItem =
    { test : TestHistoryTestView
    , job : TestHistoryJobView
    }


type alias TestHistoryTestView =
    { id : String
    , jobId : String
    , name : String
    , arguments : List String
    , status : String
    , errorMessage : String
    , startTime : Int
    , endTime : Int
    }


type alias TestHistoryJobView =
    { id : String
    , buildId : String
    , buildName : String
    , buildBranch : String
    }

type alias TestHistoryItems =
    List TestHistoryItem

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


decodeJob : Decoder Job
decodeJob =
    decode Job
        |> required "id" string
        |> required "submitTime" int
        |> required "submittedBy" string
        |> required "state" string
        |> required "preparingAgents" (list string)
        |> required "agents" (list string)
        |> required "build" decodeBuild
        |> requiredAt [ "suite", "name" ] string
        |> required "totalTests" int
        |> required "failedTests" int
        |> required "passedTests" int
        |> required "runningTests" int
        |> required "startTime" (nullable int)
        |> required "endTime" (nullable int)
        |> optional "jobSetupLogs" (dict string) Dict.empty


decodeJobs : Decoder Jobs
decodeJobs =
    field "values" (list decodeJob)


decodeBuild : Decoder Build
decodeBuild =
    decode Build
        |> required "id" string
        |> required "name" string
        |> required "branch" string
        |> required "tags" (list string)
        |> required "buildTime" int
        |> required "resources" (list string)
        |> required "testsMetadata" (list string)
        |> required "shas" (dict string)


decodeDashboardBuild : Decoder DashboardBuild
decodeDashboardBuild =
    decode DashboardBuild
        |> required "id" string
        |> required "name" string
        |> required "branch" string
        |> required "tags" (list string)
        |> required "buildTime" int
        |> required "buildStatus" (map (Json.Encode.encode 4) value)


decodeDashboardBuilds : Decoder DashboardBuilds
decodeDashboardBuilds =
    field "historyBuilds" (list decodeDashboardBuild)


decodeBuilds : Decoder Builds
decodeBuilds =
    field "values" (list decodeBuild)


decodeAgents : Decoder Agents
decodeAgents =
    field "values" (list decodeAgent)


decodeAgent : Decoder Agent
decodeAgent =
    decode Agent
        |> required "id" string
        |> required "name" string
        |> required "host" string
        |> required "lastTouchTime" int
        |> required "currentTests" (list string)
        |> required "state" string
        |> required "capabilities" (list string)
        |> required "pid" string
        |> required "setupRetries" int
        |> optionalAt [ "job", "id" ] (maybe string) Nothing
        |> optionalAt [ "job", "build", "name" ] (maybe string) Nothing
        |> optionalAt [ "job", "suite", "name" ] (maybe string) Nothing


decodeSuites : Decoder Suites
decodeSuites =
    field "values" (list decodeSuite)


decodeSuite : Decoder Suite
decodeSuite =
    decode Suite
        |> required "id" string
        |> required "name" string
        |> required "customVariables" string


decodeSuiteWithCriteria : Decoder SuiteWithCriteria
decodeSuiteWithCriteria =
    decode SuiteWithCriteria
        |> required "id" string
        |> required "name" string
        |> required "customVariables" string
        |> required "requirements" (list string)
        |> required "criteria" (map (Json.Encode.encode 4) value)


type alias TestView =
    { id : String
    , name : String
    , arguments : List String
    , status : String
    , errorMessage : String
    , testScore : Int
    , historyStats : String
    , assignedAgent : String
    , startTime : Maybe Int
    , endTime : Maybe Int
    , progressPercent : Int
    }


decodeTestView : Decoder TestView
decodeTestView =
    decode TestView
        |> required "id" string
        |> required "name" string
        |> required "arguments" (list string)
        |> required "status" string
        |> optional "errorMessage" string ""
        |> optional "testScore" int -1
        |> optional "historyStats" string ""
        |> optional "assignedAgent" string ""
        |> required "startTime" (nullable int)
        |> required "endTime" (nullable int)
        |> required "progressPercent" int


type alias TestId =
    String


type alias Test =
    { id : TestId
    , jobId : String
    , name : String
    , arguments : List String
    , testType : String
    , timeout : Int
    , status : String
    , errorMessage : String
    , testScore : Int
    , historyStats : String
    , logs : Dict String String
    , assignedAgent : String
    , startTime : Maybe Int
    , endTime : Maybe Int
    , scheduledAt : Int
    , progressPercent : Int
    , sha : String
    }

decodeTest : Json.Decode.Decoder Test
decodeTest =
    decode Test
        |> required "id" string
        |> required "jobId" string
        |> required "name" string
        |> required "arguments" (list string)
        |> required "testType" string
        |> required "timeout" int
        |> required "status" string
        |> optional "errorMessage" string ""
        |> optional "testScore" int -1
        |> optional "historyStats" string ""
        |> required "logs" (dict string)
        |> required "assignedAgent" string
        |> required "startTime" (nullable int)
        |> required "endTime" (nullable int)
        |> required "scheduledAt" int
        |> required "progressPercent" int
        |> required "sha" string


decodeUser =
    decode User
        |> required "userName" string

decodeTestHistoryItem : Json.Decode.Decoder TestHistoryItem
decodeTestHistoryItem =
    decode TestHistoryItem
        |> required "test" decodeTestHistoryTestView
        |> required "job" decodeTestHistoryJobView

decodeTestHistoryTestView : Json.Decode.Decoder TestHistoryTestView
decodeTestHistoryTestView =
    decode TestHistoryTestView
        |> required "id" string
        |> required "jobId" string
        |> required "name" string
        |> required "arguments" (list string)
        |> required "status" string
        |> optional "errorMessage" string ""
        |> required "startTime" int
        |> required "endTime" int

decodeTestHistoryJobView : Json.Decode.Decoder TestHistoryJobView
decodeTestHistoryJobView =
    decode TestHistoryJobView
        |> required "id" string
        |> required "buildId" string
        |> required "buildName" string
        |> required "buildBranch" string

decodeTestHistoryItems : Decoder TestHistoryItems
decodeTestHistoryItems =
    field "values" (list decodeTestHistoryItem)