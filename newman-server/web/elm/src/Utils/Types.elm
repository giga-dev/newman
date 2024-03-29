module Utils.Types exposing (..)

import Dict exposing (Dict)
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (..)
import Json.Encode
import Paginate exposing (PaginatedList)


type alias JobId =
    String


type alias JobRadioState =
    String


type alias Job =
    { id : JobId
    , submitTime : Int
    , submittedBy : String
    , state : JobState
    , preparingAgents : List String
    , agents : List String
    , agentGroups : List String
    , priority : Maybe Int
    , buildId : String
    , buildName : String
    , buildBranch : String
    , suiteId : String
    , suiteName : String
    , jobConfigId : String
    , jobConfigName : String
    , totalTests : Int
    , failedTests : Int
    , failed3TimesTests : Int
    , passedTests : Int
    , runningTests : Int
    , numOfTestRetries : Int
    , startTime : Maybe Int
    , endTime : Maybe Int
    , jobSetupLogs : Dict String String
    , selected : Bool
    }


type alias Build =
    { id : BuildId
    , name : String
    , branch : String
    , tags : List String
    , buildTime : Int
    , buildStatus : DashboardBuildStatus
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
    , hostAddress : String
    , lastTouchTime : Int
    , currentTests : List String
    , state : String
    , capabilities : List String
    , pid : String
    , setupRetries : Int
    , jobId : Maybe String
    , buildName : Maybe String
    , suiteName : Maybe String
    , groupName : String
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
    , criteria : Value
    }


type alias Suites =
    List Suite


type alias PaginatedSuites =
    PaginatedList Suite


type alias SuiteId =
    String


type alias JobConfigId =
    String


type alias JobConfig =
    { id : String
    , name : String
    , javaVersion : String
    }


type alias JobConfigs =
    List JobConfig


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
    , runNumber : Int
    , assignedAgent : String
    , agentGroup : String
    }


type alias TestHistoryJobView =
    { id : String
    , buildId : String
    , buildName : String
    , buildBranch : String
    , jobConfigName : String
    , jobConfigId : String
    }


type alias TestHistoryItems =
    List TestHistoryItem


type RadioState
    = STATUS_RUNNING
    | STATUS_SUCCESS
    | STATUS_FAIL
    | STATUS_FAILED3TIMES
    | STATUS_ALL


stringToRadioState : String -> RadioState
stringToRadioState state =
    case state of
        "RUNNING" ->
            STATUS_RUNNING

        "SUCCESS" ->
            STATUS_SUCCESS

        "FAIL" ->
            STATUS_FAIL

        "FAILED3TIMES" ->
            STATUS_FAILED3TIMES

        "ALL" ->
            STATUS_ALL

        _ ->
            STATUS_ALL


radioStateToString : RadioState -> String
radioStateToString state =
    case state of
        STATUS_RUNNING ->
            "RUNNING"

        STATUS_SUCCESS ->
            "SUCCESS"

        STATUS_FAIL ->
            "FAIL"

        STATUS_FAILED3TIMES ->
            "FAILED3TIMES"

        STATUS_ALL ->
            "ALL"


type JobState
    = READY
    | RUNNING
    | DONE
    | PAUSED
    | BROKEN


jobStateToString : JobState -> String
jobStateToString jState =
    case jState of
        READY ->
            "Ready"

        RUNNING ->
            "RUNNING"

        DONE ->
            "DONE"

        PAUSED ->
            "PAUSED"

        BROKEN ->
            "BROKEN"


decodeJobState : Decoder JobState
decodeJobState =
    string
        |> andThen
            (\str ->
                case str of
                    "READY" ->
                        succeed READY

                    "RUNNING" ->
                        succeed RUNNING

                    "DONE" ->
                        succeed DONE

                    "PAUSED" ->
                        succeed PAUSED

                    "BROKEN" ->
                        succeed BROKEN

                    _ ->
                        fail <| "unknown job state: " ++ str
            )


decodeJob : Decoder Job
decodeJob =
    decode Job
        |> required "id" string
        |> required "submitTime" int
        |> required "submittedBy" string
        |> required "state" decodeJobState
        |> required "preparingAgents" (list string)
        |> required "agents" (list string)
        |> optional "agentGroups" (list string) []
        |> optional "priority" (nullable int) Nothing
        |> requiredAt [ "build", "id" ] string
        |> requiredAt [ "build", "name" ] string
        |> requiredAt [ "build", "branch" ] string
        |> requiredAt [ "suite", "id" ] string
        |> requiredAt [ "suite", "name" ] string
        |> optionalAt [ "jobConfig", "id" ] string ""
        |> optionalAt [ "jobConfig", "name" ] string ""
        |> required "totalTests" int
        |> required "failedTests" int
        |> required "failed3TimesTests" int
        |> required "passedTests" int
        |> required "runningTests" int
        |> required "numOfTestRetries" int
        |> optional "startTime" (nullable int) Nothing
        |> optional "endTime" (nullable int) Nothing
        |> optional "jobSetupLogs" (dict string) Dict.empty
        |> optional "selected" bool False


decodeJobList : Decoder (List Job)
decodeJobList =
    list decodeJob


decodeJobView : Decoder Job
decodeJobView =
    decode Job
        |> required "id" string
        |> required "submitTime" int
        |> required "submittedBy" string
        |> required "state" decodeJobState
        |> required "preparingAgents" (list string)
        |> optional "agents" (list string) []
        |> optional "agentGroups" (list string) []
        |> optional "priority" (nullable int) Nothing
        |> required "buildId" string
        |> required "buildName" string
        |> required "buildBranch" string
        |> required "suiteId" string
        |> required "suiteName" string
        |> optional "jobConfigId" string ""
        |> optional "jobConfigName" string ""
        |> required "totalTests" int
        |> required "failedTests" int
        |> required "failed3TimesTests" int
        |> required "passedTests" int
        |> required "runningTests" int
        |> required "numOfTestRetries" int
        |> optional "startTime" (nullable int) Nothing
        |> optional "endTime" (nullable int) Nothing
        |> optional "jobSetupLogs" (dict string) Dict.empty
        |> optional "selected" bool False


decodeBuild : Decoder Build
decodeBuild =
    decode Build
        |> required "id" string
        |> required "name" string
        |> required "branch" string
        |> required "tags" (list string)
        |> required "buildTime" int
        |> required "buildStatus" decodeDashboardBuildStatus
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
    , failed3TimesTests : Int
    , passedTests : Int
    , runningTests : Int
    , numOfTestRetries : Int
    }


decodeDashboardJob : Decoder DashboardJob
decodeDashboardJob =
    decode DashboardJob
        |> required "id" string
        |> requiredAt [ "suite", "id" ] string
        |> requiredAt [ "suite", "name" ] string
        |> required "totalTests" int
        |> required "failedTests" int
        |> required "failed3TimesTests" int
        |> required "passedTests" int
        |> required "runningTests" int
        |> required "numOfTestRetries" int


type alias DashboardData =
    { historyBuilds : List Build
    , futureJobs : List FutureJob
    , pendingBuilds : List Build
    , activeBuilds : List Build
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
    , agentGroups : List String
    , priority : Int
    , submitTime : Int
    }


type alias DashboardBuildStatus =
    { totalTests : Int
    , passedTests : Int
    , failedTests : Int
    , failed3TimesTests : Int
    , runningTests : Int
    , numOfTestRetries : Int
    , totalJobs : Int
    , pendingJobs : Int
    , runningJobs : Int
    , doneJobs : Int
    , brokenJobs : Int
    , suitesNames : List String
    , suitesIds : List String
    }


decodeDashboardBuildStatus : Decoder DashboardBuildStatus
decodeDashboardBuildStatus =
    decode DashboardBuildStatus
        |> required "totalTests" int
        |> required "passedTests" int
        |> required "failedTests" int
        |> required "failed3TimesTests" int
        |> required "numOfTestRetries" int
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
        |> required "agentGroups" (list string)
        |> required "priority" int
        |> required "submitTime" int


decodeDashboardData : Decoder DashboardData
decodeDashboardData =
    decode DashboardData
        |> required "historyBuilds" (list decodeBuild)
        |> required "futureJobs" (list decodeFutureJob)
        |> required "pendingBuilds" (list decodeBuild)
        |> required "activeBuilds" (list decodeBuild)
        |> required "activeJobs" (dict (list decodeDashboardJob))


decodeFirstBuild : Decoder Build
decodeFirstBuild =
    field "values" (index 0 decodeBuild)


decodeBuilds : Decoder (List Build)
decodeBuilds =
    field "values" (list decodeBuild)


decodeAgents : Decoder Agents
decodeAgents =
    field "values" (list decodeAgent)


decodeAgentList : Decoder Agents
decodeAgentList =
    list decodeAgent


decodeAgent : Decoder Agent
decodeAgent =
    decode Agent
        |> required "id" string
        |> required "name" string
        |> required "host" string
        |> required "hostAddress" string
        |> required "lastTouchTime" int
        |> required "currentTests" (list string)
        |> required "state" string
        |> required "capabilities" (list string)
        |> required "pid" string
        |> required "setupRetries" int
        |> optionalAt [ "job", "id" ] (maybe string) Nothing
        |> optionalAt [ "job", "build", "name" ] (maybe string) Nothing
        |> optionalAt [ "job", "suite", "name" ] (maybe string) Nothing
        |> optional "groupName" string "undefined"



-- This is a temp fix ^|^


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
        |> required "criteria" value


encodeSuiteWithCriteria : SuiteWithCriteria -> Json.Encode.Value
encodeSuiteWithCriteria suite =
    Json.Encode.object
        [ ( "id", Json.Encode.string suite.id )
        , ( "name", Json.Encode.string suite.name )
        , ( "customVariables", Json.Encode.string suite.customVariables )
        , ( "requirements", Json.Encode.list <| List.map Json.Encode.string suite.requirements )
        , ( "criteria", suite.criteria )
        ]


decodeJobConfigs : Decoder JobConfigs
decodeJobConfigs =
    Json.Decode.list decodeJobConfig


decodeJobConfig : Decoder JobConfig
decodeJobConfig =
    decode JobConfig
        |> required "id" string
        |> required "name" string
        |> optional "javaVersion" string "N/A"


decodeAgentGroups : Decoder (List String)
decodeAgentGroups =
    Json.Decode.list Json.Decode.string


type alias TestId =
    String


type TestStatus
    = TEST_RUNNING
    | TEST_SUCCESS
    | TEST_FAIL
    | TEST_PENDING


testStatusToString : TestStatus -> String
testStatusToString ts =
    case ts of
        TEST_RUNNING ->
            "Running"

        TEST_SUCCESS ->
            "Success"

        TEST_FAIL ->
            "Fail"

        TEST_PENDING ->
            "Pending"


decodeTestStatus : Decoder TestStatus
decodeTestStatus =
    string
        |> andThen
            (\str ->
                case str of
                    "RUNNING" ->
                        succeed TEST_RUNNING

                    "SUCCESS" ->
                        succeed TEST_SUCCESS

                    "FAIL" ->
                        succeed TEST_FAIL

                    "PENDING" ->
                        succeed TEST_PENDING

                    _ ->
                        fail ("unknown test state " ++ str)
            )


type alias Test =
    { id : TestId
    , jobId : String
    , name : String
    , arguments : List String
    , testType : String
    , timeout : Int
    , status : TestStatus
    , errorMessage : String
    , testScore : Int
    , historyStats : String
    , logs : Dict String String
    , assignedAgent : String
    , agentGroup : String
    , startTime : Maybe Int
    , endTime : Maybe Int
    , scheduledAt : Int
    , progressPercent : Int
    , runNumber : Int
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
        |> required "status" decodeTestStatus
        |> optional "errorMessage" string ""
        |> optional "testScore" int -1
        |> optional "historyStats" string ""
        |> required "logs" (dict string)
        |> optional "assignedAgent" string ""
        |> optional "agentGroup" string ""
        |> required "startTime" (nullable int)
        |> required "endTime" (nullable int)
        |> required "scheduledAt" int
        |> required "progressPercent" int
        |> required "runNumber" int


decodeTestView : Decoder Test
decodeTestView =
    decode Test
        |> required "id" string
        |> optional "jobId" string ""
        |> required "name" string
        |> required "arguments" (list string)
        |> optional "testType" string ""
        |> optional "timeout" int 0
        |> required "status" decodeTestStatus
        |> optional "errorMessage" string ""
        |> optional "testScore" int -1
        |> optional "historyStats" string ""
        |> optional "logs" (dict string) Dict.empty
        |> optional "assignedAgent" string ""
        |> optional "agentGroup" string ""
        |> optional "startTime" (nullable int) Nothing
        |> optional "endTime" (nullable int) Nothing
        |> optional "scheduledAt" int 0
        |> required "progressPercent" int
        |> required "runNumber" int


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
        |> required "runNumber" int
        |> required "assignedAgent" string
        |> optional "agentGroup" string ""


decodeTestHistoryJobView : Json.Decode.Decoder TestHistoryJobView
decodeTestHistoryJobView =
    decode TestHistoryJobView
        |> required "id" string
        |> required "buildId" string
        |> required "buildName" string
        |> required "buildBranch" string
        |> optional "jobConfigName" string ""
        |> optional "jobConfigId" string ""


decodeTestHistoryItems : Decoder TestHistoryItems
decodeTestHistoryItems =
    field "values" (list decodeTestHistoryItem)


encodeListOfStrings : List String -> Json.Encode.Value
encodeListOfStrings lst =
    lst
        |> List.map Json.Encode.string
        -- convert from List String to List Value --
        |> Json.Encode.list



-- convert from List Value to Value --


decodeStatus : Decoder String
decodeStatus =
    at [ "status" ] string


agentGroupsJobFormat agentGroups =
    case agentGroups of
        [] ->
            "N/A"

        _ ->
            String.join ", " agentGroups


agentGroupTestFormat agentGroup assignedAgent =
    if assignedAgent /= "" then
        if agentGroup == "" then
            "N/A"

        else
            agentGroup

    else
        ""


legendPriority : Int -> String
legendPriority priority =
    case priority of
        1 ->
            "1- low"

        2 ->
            "2- high"

        3 ->
            "3- release-default"

        4 ->
            "4- urgent"

        _ ->
            "0- daily-default"
