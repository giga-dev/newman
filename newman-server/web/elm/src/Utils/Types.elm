module Utils.Types exposing (..)

import Dict exposing (Dict)
import Json.Decode
import Json.Decode.Pipeline exposing (decode)
import Paginate exposing (PaginatedList)


type alias Jobs =
    List Job


type alias PaginatedJobs =
    PaginatedList Job


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


type alias BuildId =
    String


type alias Shas =
    Dict String String


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