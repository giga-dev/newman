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

        "READY" ->
            READY

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
        |> optional "startTime" (nullable int) Nothing
        |> optional "endTime" (nullable int) Nothing
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



---

type alias ActiveJobsDashboard =
    Dict String (List DashboardJob)

type alias DashboardJob =
    { id : String
    , suiteId : String
    , suiteName : String
    , totalTests : Int
    , failedTests : Int
    , passedTests : Int
    , runningTests : Int
    }

decodeDashboardJob : Decoder DashboardJob
decodeDashboardJob =
    decode DashboardJob
        |> required "id" string
        |> requiredAt [ "suite", "id" ] string
        |> requiredAt [ "suite", "name" ] string
        |> required "totalTests" int
        |> required "failedTests" int
        |> required "passedTests" int
        |> required "runningTests" int


type alias DashboardData =
    { historyBuilds : List DashboardBuild
    , futureJobs : List FutureJob
    , pendingBuilds: List DashboardBuild
    , activeBuilds : List DashboardBuild
    , activeJobs : ActiveJobsDashboard
    }


type alias FutureJob =
    { id : String
    , buildId : String
    , buildName : String
    , buildBranch : String
    , suiteId : String
    , suiteName : String
    , author : String
    , submitTime : Int
    }



type alias DashboardBuild =
    { id : BuildId
    , name : String
    , branch : String
    , buildTime : Int
    , buildStatus : DashboardBuildStatus
    }


type alias DashboardBuildStatus =
    { totalTests : Int
    , passedTests : Int
    , failedTests : Int
    , runningTests : Int
    , totalJobs : Int
    , pendingJobs : Int
    , runningJobs : Int
    , doneJobs : Int
    , brokenJobs : Int
    , suitesNames : List String
    , suitesIds : List String
    }


decodeDashboardBuild : Decoder DashboardBuild
decodeDashboardBuild =
    decode DashboardBuild
        |> required "id" string
        |> required "name" string
        |> required "branch" string
        |> required "buildTime" int
        |> required "buildStatus" decodeDashboardBuildStatus


decodeDashboardBuildStatus : Decoder DashboardBuildStatus
decodeDashboardBuildStatus =
    decode DashboardBuildStatus
        |> required "totalTests" int
        |> required "passedTests" int
        |> required "failedTests" int
        |> required "runningTests" int
        |> required "totalJobs" int
        |> required "pendingJobs" int
        |> required "runningJobs" int
        |> required "doneJobs" int
        |> required "brokenJobs" int
        |> required "suitesNames" (list string)
        |> required "suitesIds" (list string)



decodeFutureJob : Decoder FutureJob
decodeFutureJob =
    decode FutureJob
        |> required "id" string
        |> required "buildID" string
        |> required "buildName" string
        |> required "buildBranch" string
        |> required "suiteID" string
        |> required "suiteName" string
        |> required "author" string
        |> required "submitTime" int

decodeDashboardData : Decoder DashboardData
decodeDashboardData =
    decode DashboardData
        |> required "historyBuilds" (list decodeDashboardBuild)
        |> required "futureJobs" (list decodeFutureJob)
        |> required "pendingBuilds" (list decodeDashboardBuild)
        |> required "activeBuilds" (list decodeDashboardBuild)
        |> required "activeJobs" (dict (list decodeDashboardJob))

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
    }


decodeTest : Decoder Test
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
        |> optional "assignedAgent" string ""
        |> required "startTime" (nullable int)
        |> required "endTime" (nullable int)
        |> required "scheduledAt" int
        |> required "progressPercent" int

decodeTestView : Decoder Test
decodeTestView =
    decode Test
        |> required "id" string
        |> optional "jobId" string ""
        |> required "name" string
        |> required "arguments" (list string)
        |> optional "testType" string ""
        |> optional "timeout" int 0
        |> required "status" string
        |> optional "errorMessage" string ""
        |> optional "testScore" int -1
        |> optional "historyStats" string ""
        |> optional "logs" (dict string) Dict.empty
        |> optional "assignedAgent" string ""
        |> optional "startTime" (nullable int) Nothing
        |> optional "endTime" (nullable int) Nothing
        |> optional "scheduledAt" int 0
        |> required "progressPercent" int


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