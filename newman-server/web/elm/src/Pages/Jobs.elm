module Pages.Jobs exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Bootstrap.Progress as Progress exposing (..)
import Date exposing (Date)
import Date.Extra.Config.Config_en_au exposing (config)
import Date.Extra.Duration as Duration
import Date.Extra.Format as Format exposing (format, formatUtc, isoMsecOffsetFormat)
import Date.Format
import Html exposing (..)
import Html.Attributes as HtmlAttr exposing (..)
import Html.Events exposing (..)
import Http
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import Paginate exposing (..)
import Task
import Time exposing (Time)


type alias Model =
    { allJobs : List Job
    , jobs : PaginatedList Job
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
    ( { allJobs = []
      , jobs = Paginate.fromList pageSize []
      , maxEntries = maxEntries
      , pageSize = pageSize
      , currTime = 0
      }
    , Cmd.batch [ getJobsCmd maxEntries, getTime ]
    )


type JobState
    = READY
    | RUNNING
    | DONE
    | PAUSED
    | BROKEN


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
    | FilterQuery String


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        UpdateMaxEntries newValue ->
            let
                maxEntries =
                    String.toInt newValue |> Result.toMaybe |> Maybe.withDefault 1
            in
            ( { model | maxEntries = maxEntries }, getJobsCmd maxEntries )

        GetJobsCompleted result ->
            onGetJobsCompleted model result

        OnTime time ->
            ( { model | currTime = time }, Cmd.none )

        First ->
            ( { model | jobs = Paginate.first model.jobs }, Cmd.none )

        Prev ->
            ( { model | jobs = Paginate.prev model.jobs }, Cmd.none )

        Next ->
            ( { model | jobs = Paginate.next model.jobs }, Cmd.none )

        Last ->
            ( { model | jobs = Paginate.last model.jobs }, Cmd.none )

        GoTo i ->
            ( { model | jobs = Paginate.goTo i model.jobs }, Cmd.none )

        FilterQuery query ->
            ( { model | jobs = Paginate.fromList model.pageSize (List.filter (filterQuery query) model.allJobs) }
            , Cmd.none
            )



-----
{-
   VIEW
-}


filterQuery : String -> Job -> Bool
filterQuery query job =
    if
        String.length query
            == 0
            || String.startsWith query job.id
            || String.startsWith query job.buildName
            || String.startsWith query job.suiteName
    then
        True
    else
        False


viewItem : Model -> Job -> Html msg
viewItem model job =
    let
        progressPercent =
            ((job.failedTests + job.passedTests) * 100) // job.totalTests

        progress =
            Progress.progress
                [ Progress.label <| toString <| progressPercent
                , Progress.value <| toFloat <| progressPercent
                ]

        jobState =
            let
                badge =
                    case job.state |> toJobState of
                        BROKEN ->
                            Badge.badgeDanger

                        DONE ->
                            Badge.badgeSuccess

                        RUNNING ->
                            Badge.badgeInfo

                        PAUSED ->
                            Badge.badgeWarning

                        READY ->
                            Badge.badge
            in
            badge [ class "newman-job-state-label" ] [ text job.state ]

        submittedTimeHourFull =
            Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat job.submitTime))

        submittedTimeHour =
            Date.Format.format "%H:%M:%S" (Date.fromTime (toFloat job.submitTime))

        durationText =
            let
                diffTime =
                    case ( job.startTime, job.endTime ) of
                        ( Just startTime, Just endTime ) ->
                            Just <| Duration.diff (Date.fromTime (toFloat endTime)) (Date.fromTime (toFloat startTime))
                        ( Just startTime, Nothing ) ->
                            Just <| Duration.diff (Date.fromTime model.currTime) (Date.fromTime (toFloat startTime))
                        ( _, _ ) ->
                            Nothing
            in
            case diffTime of
                Just diff ->
                    (toString diff.hour) ++ "h, " ++ (toString diff.minute) ++ "m"

                Nothing ->
                    ""

        --        submittedTimeDiff =
        --            Duration.diff (Date.fromTime (toFloat job.submitTime)) (Date.fromTime (toFloat job.submitTime - model.currTime))
        --
        --        submittedTimeText =
        --            toString submittedTimeDiff.hour ++ "h, " ++ toString submittedTimeDiff.minute ++ "m"
        playPauseButton =
            case toJobState job.state of
                PAUSED ->
                    Button.button [ Button.success, Button.small ]
                        [ span [ class "ion-play" ] [] ]

                _ ->
                    Button.button [ Button.warning, Button.small ]
                        [ span [ class "ion-pause" ] [] ]
    in
    tr []
        [ td [] [ jobState ]
        , td [] [ progress ]
        , td [] [ a [ href <| "#job/" ++ job.id ] [ text job.id ] ]
        , td [] [ text job.suiteName ]
        , td [] [ text durationText ]
        , td [ title submittedTimeHourFull ] [ text submittedTimeHour ]
        , td [] [ a [ href job.buildId ] [ text job.buildName ] ]
        , td [] [ text job.submittedBy ]
        , td [] [ text (toString (List.length job.preparingAgents)) ]
        , td []
            [ Badge.badgeInfo [] [ text <| toString job.runningTests ]
            , text "/ "
            , Badge.badgeSuccess [] [ text <| toString job.passedTests ]
            , text "/ "
            , Badge.badgeDanger [] [ text <| toString job.failedTests ]
            , text "/ "
            , Badge.badge [] [ text <| toString job.totalTests ]
            ]
        , td []
            [ Button.button [ Button.success, Button.small ]
                [ span [ class "ion-close" ] [] ]
            , text " "
            , playPauseButton
            ]
        ]


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.jobs ) ], onClick First ]
                [ button [ class "page-link" ] [ text "«" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.jobs ) ], onClick Prev ]
                [ button [ class "page-link" ] [ text "‹" ]
                ]
            ]

        nextButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.jobs ) ], onClick Next ]
                [ button [ class "page-link" ] [ text "›" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.jobs ) ], onClick Last ]
                [ button [ class "page-link" ] [ text "»" ]
                ]
            ]

        pagerButtonView index isActive =
            case isActive of
                True ->
                    li [ class "page-item active" ]
                        [ button [ class "page-link" ]
                            [ text <| toString index
                            , span [ class "sr-only" ] [ text "(current)" ]
                            ]
                        ]

                --                        <li class="page-item"><a class="page-link" href="#">1</a></li>
                False ->
                    li [ class "page-item", onClick <| GoTo index ]
                        [ button [ class "page-link" ] [ text <| toString index ]
                        ]

        pagination =
            nav []
                [ ul [ class "pagination " ]
                    (prevButtons
                        ++ Paginate.pager pagerButtonView model.jobs
                        ++ nextButtons
                    )
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Jobs" ]

        --        , h3 [] [ text ("Time: " ++ toString model.currTime) ]
        , Form.formInline []
            [ Form.group [] [ FormInput.text [ FormInput.onInput FilterQuery, FormInput.placeholder "Filter" ] ]
            , Form.group [] [ pagination ]
            ]
        , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
            [ thead []
                [ tr []
                    [ th [] [ text "State" ]
                    , th [] [ text "Progess" ]
                    , th [] [ text "Job Id" ]
                    , th [] [ text "Suite" ]
                    , th [] [ text "Duration" ]
                    , th [] [ text "Submitted At" ]
                    , th [] [ text "Build" ]
                    , th [] [ text "Submitted By" ]
                    , th [ style [ ( "width", "6%" ) ] ] [ text "# p. agents" ]
                    , th [ style [ ( "width", "15%" ) ] ]
                        [ Badge.badgeInfo [] [ text "# Running" ]
                        , text " "
                        , Badge.badgeSuccess [] [ text "# Passed" ]
                        , text " "
                        , Badge.badgeDanger [] [ text "# Failed" ]
                        , text " "
                        , Badge.badge [] [ text "# Total" ]
                        ]
                    , th [ style [ ( "width", "80px" ) ] ]
                        [ text "Actions" ]
                    ]

                {-
                   <span class="label label-info">
                                                       <a href="{{urlFor('job', {id: item.id, filterByStatus: 'RUNNING'})}}" class="tests-num-link">{{item.runningTests}}</a>
                                                   </span> /
                                                   <span class="label label-success">
                                                       <a href="{{urlFor('job', {id: item.id, filterByStatus: 'SUCCESS'})}}" class="tests-num-link">{{item.passedTests}}</a>
                                                   </span> /
                                                   <span class="label label-danger">
                                                       <a href="{{urlFor('job', {id: item.id, filterByStatus: 'FAIL'})}}" class="tests-num-link">{{item.failedTests}}</a>
                                                   </span> /
                                                   <span class="label label-default">
                                                       <a href="{{urlFor('job', {id: item.id, filterByStatus: 'ALL'})}}" class="tests-num-link">{{item.totalTests}}</a>
                                                   </span>

                -}
                ]
            , tbody [] (List.map (viewItem model) <| Paginate.page model.jobs)
            ]
        , pagination
        ]



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
        |> Json.Decode.Pipeline.required "runningTests" Json.Decode.int
        |> Json.Decode.Pipeline.required "startTime" (Json.Decode.nullable Json.Decode.int)
        |> Json.Decode.Pipeline.required "endTime" (Json.Decode.nullable Json.Decode.int)


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
    , runningTests : Int
    , startTime : Maybe Int
    , endTime : Maybe Int
    }


type alias Jobs =
    List Job


getJobsCmd : Int -> Cmd Msg
getJobsCmd limit =
    Http.send GetJobsCompleted (getJobs limit)


getJobs : Int -> Http.Request Jobs
getJobs limit =
    Http.get ("/api/newman/job?limit=" ++ toString limit ++ "&orderBy=-submitTime") decodeJobs



--dropJobCmd : String -> Cmd Msg
--dropJobCmd jobId =
--    Http.send
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
            ( { model
                | allJobs = jobs
                , jobs = Paginate.fromList model.pageSize jobs
              }
            , Cmd.none
            )

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
