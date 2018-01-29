port module Main exposing (..)

import Date exposing (Date)
import Date.Extra.Config.Config_en_au exposing (config)
import Date.Extra.Duration as Duration
import Date.Extra.Format as Format exposing (format, formatUtc, isoMsecOffsetFormat)
import Date.Format
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import Paginate exposing (..)
import Task
import Time exposing (Time)


main : Program Never Model Msg
main =
    Html.program
        { init = init
        , update = update
        , subscriptions = \_ -> Sub.none
        , view = view
        }



{-
   MODEL
   * Model type
   * Initialize model with empty values
-}


type alias Model =
    { jobs : Jobs
    , pag : PaginatedList Job
    , maxEntries : Int
    , pageSize : Int
    , currTime : Time
    }


init : ( Model, Cmd Msg )
init =
    let
        maxEntries =
            40

        pageSize =
            10
    in
    ( Model [] (Paginate.fromList pageSize []) pageSize maxEntries 0, Cmd.batch [ getJobsCmd maxEntries, getTime ] )



-----
{-
   UPDATE
   * Messages
   * Update case
-}


type Msg
    = UpdateMaxEntries String
    | GetJobsCompleted (Result Http.Error Jobs)
    | OnTime Time
    | First
    | Last
    | Next
    | Prev
    | GoTo Int


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case Debug.log "msg" msg of
        UpdateMaxEntries newValue ->
            let
                maxEntries =
                    String.toInt newValue |> Result.toMaybe |> Maybe.withDefault 1

                a =
                    Debug.log "aa" maxEntries
            in
            ( { model | maxEntries = maxEntries }, getJobsCmd maxEntries )

        GetJobsCompleted result ->
            onGetJobsCompleted model result

        OnTime time ->
            ( { model | currTime = time }, Cmd.none )

        First ->
            ( { model | pag = Paginate.first model.pag }, Cmd.none )

        Prev ->
            ( { model | pag = Paginate.prev model.pag }, Cmd.none )

        Next ->
            ( { model | pag = Paginate.next model.pag }, Cmd.none )

        Last ->
            ( { model | pag = Paginate.last model.pag }, Cmd.none )

        GoTo i ->
            ( { model | pag = Paginate.goTo i model.pag }, Cmd.none )



-----
{-
   VIEW
-}


viewItem : Job -> Html msg
viewItem job =
    let
        progressPercent =
            ((job.failedTests + job.passedTests) * 100) // job.totalTests

        progress =
            div [ class "progress" ]
                [ div [ class "progress-bar" ]
                    [ text (toString progressPercent ++ "%")
                    ]
                ]

        --            <div class="progress">
        --                        <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="min-width: {{job.progressPercent}}%;">
        --                            {{job.progressPercent}}%
        --                        </div>
        --                    </div>
        submittedTimeHour =
            Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat job.submitTime))

        submittedTimeDiff =
            Duration.diff (Date.fromTime (toFloat job.submitTime)) (Date.fromTime (toFloat job.submitTime - 10000000))

        submittedTimeText =
            toString submittedTimeDiff.hour ++ "h, " ++ toString submittedTimeDiff.minute ++ "m"
    in
    tr []
        [ td [] [ text job.state ]
        , td [] [ progress ]
        , td [] [ text job.id ]
        , td [] [ text job.suiteName ]
        , td [] [ text "duration0" ]
        , td [ title submittedTimeHour ] [ text submittedTimeText ]
        , td [] [ a [ href job.buildId ] [ text job.buildName ] ]
        , td [] [ text job.submittedBy ]
        , td [] [ text (toString (List.length job.preparingAgents)) ]
        ]


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ button [ onClick First, disabled <| Paginate.isFirst model.pag ] [ text "<<" ]
            , button [ onClick Prev, disabled <| Paginate.isFirst model.pag ] [ text "<" ]
            ]

        nextButtons =
            [ button [ onClick Next, disabled <| Paginate.isLast model.pag ] [ text ">" ]
            , button [ onClick Last, disabled <| Paginate.isLast model.pag ] [ text ">>" ]
            ]

        pagerButtonView index isActive =
            button
                [ style
                    [ ( "font-weight"
                      , if isActive then
                            "bold"
                        else
                            "normal"
                      )
                    ]
                , onClick <| GoTo index
                ]
                [ text <| toString index ]
    in
    div [ class "container" ] <|
        [ h2 [ class "text-center" ] [ text "Jobs" ]
        , h3 [] [ text ("Time: " ++ toString model.currTime) ]
        , input [ onInput UpdateMaxEntries, type_ "number", value (toString model.maxEntries) ] []
        , table [ width 1200 ]
            (List.append
                [ tr []
                    [ td [] [ text "State" ]
                    , td [] [ text "Progess" ]
                    , td [] [ text "Job Id" ]
                    , td [] [ text "Suite" ]
                    , td [] [ text "Duration" ]
                    , td [] [ text "Submitted At" ]
                    , td [] [ text "Build" ]
                    , td [] [ text "Submitted By" ]
                    , td [] [ text "# preparing agents" ]
                    ]
                ]
                (List.map viewItem <| Paginate.page model.pag)
            )
        ]
            ++ prevButtons
            ++ [ span [] <| Paginate.pager pagerButtonView model.pag ]
            ++ nextButtons



----


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


decodeJobs : Json.Decode.Decoder Jobs
decodeJobs =
    Json.Decode.field "values" (Json.Decode.list decodeJob)


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
    }


type alias Jobs =
    List Job


getJobsCmd : Int -> Cmd Msg
getJobsCmd limit =
    Http.send GetJobsCompleted (getJobs limit)


getJobs : Int -> Http.Request Jobs
getJobs limit =
    Http.get ("http://localhost:8080/api/newman/job?limit=" ++ toString limit ++ "&orderBy=-submitTime") decodeJobs



--    { method = "GET"
--    , headers = [ Http.header "Authorization" "Basic eW9oYW5hOnlvaGFuYQ==" ]
--    , url = "http://localhost:8080/api/newman/job?limit=" ++ (toString limit) ++ "&orderBy=-submitTime"
--    , body = Http.emptyBody
--    , expect = Http.expectJson decodeJobs
--    , timeout = Nothing
--    , withCredentials = True
--    }
--        |> Http.request
--
--


onGetJobsCompleted : Model -> Result Http.Error Jobs -> ( Model, Cmd Msg )
onGetJobsCompleted model result =
    case result of
        Ok jobs ->
            ( { model | jobs = jobs, pag = Paginate.fromList 10 jobs }, Cmd.none )

        Err err ->
            let
                a =
                    Debug.log "onGetJobsCompleted" err
            in
            ( model, Cmd.none )



--    { method = "GET"
--    , headers = [  ]
--    , url = "/api/newman/suites-dashboard"
--    , body = Http.emptyBody
--    , expect = Http.expectString
--    , timeout = Nothing
--    , withCredentials = True
--    }
--        |> Http.request


getTime : Cmd Msg
getTime =
    Task.perform OnTime Time.now
