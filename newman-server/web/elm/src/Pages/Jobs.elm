module Pages.Jobs exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Form
import Bootstrap.Button
import Bootstrap.Form.Input
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
    ( Model [] (Paginate.fromList pageSize []) maxEntries pageSize 0, Cmd.batch [ getJobsCmd maxEntries, getTime ] )



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


viewItem : Job -> Html msg
viewItem job =
    let
        progressPercent =
            ((job.failedTests + job.passedTests) * 100) // job.totalTests

        progress =
            Progress.progress
                [ Progress.label <| toString <| progressPercent
                , Progress.value <| toFloat <| progressPercent
                ]

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
        , td [] [ a [ href <| "#job/" ++ job.id ] [ text job.id ] ]
        , td [] [ text job.suiteName ]
        , td [] [ text "duration0" ]
        , td [ title submittedTimeHour ] [ text submittedTimeText ]
        , td [] [ a [ href job.buildId ] [ text job.buildName ] ]
        , td [] [ text job.submittedBy ]
        , td [] [ text (toString (List.length job.preparingAgents)) ]
        , td []
            [ Bootstrap.Button.button [ Bootstrap.Button.small, Bootstrap.Button.info ] [ text <| toString job.runningTests ]
            , text "/ "
            , Bootstrap.Button.button [ Bootstrap.Button.small, Bootstrap.Button.success ] [ text <| toString job.passedTests ]
            , text "/ "
            , Bootstrap.Button.button [ Bootstrap.Button.small, Bootstrap.Button.danger] [ text <| toString job.failedTests ]
            , text "/ "
            , Bootstrap.Button.button [ Bootstrap.Button.small ] [ text <| toString job.totalTests ]
            ]
        , td [] [
            Bootstrap.Button.button [ Bootstrap.Button.success, Bootstrap.Button.small ] [
                span [ class "ion-close-circled" ] []
                ]
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
        , h3 [] [ text ("Time: " ++ toString model.currTime) ]
        , Bootstrap.Form.formInline []
            [ Bootstrap.Form.group [] [ Bootstrap.Form.Input.text [ Bootstrap.Form.Input.onInput FilterQuery, Bootstrap.Form.Input.placeholder "Filter" ] ]
            , Bootstrap.Form.group [] [ pagination ]
            ]
        , table []
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
                    , td []
                        [ Bootstrap.Button.button [ Bootstrap.Button.small, Bootstrap.Button.info ] [ text "# Running" ]
                        , text " "
                        , Bootstrap.Button.button [ Bootstrap.Button.small, Bootstrap.Button.success ] [ text "# Passed" ]
                        , text " "
                        , Bootstrap.Button.button [ Bootstrap.Button.small, Bootstrap.Button.danger] [ text "# Failed" ]
                        , text " "
                        , Bootstrap.Button.button [ Bootstrap.Button.small ] [ text "# Total" ]
                        ]
                    , td []
                        [ text "Drop" ]
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
                (List.map viewItem <| Paginate.page model.jobs)
            )
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
    }


type alias Jobs =
    List Job


getJobsCmd : Int -> Cmd Msg
getJobsCmd limit =
    Http.send GetJobsCompleted (getJobs limit)


getJobs : Int -> Http.Request Jobs
getJobs limit =
    Http.get ("/api/newman/job?limit=" ++ toString limit ++ "&orderBy=-submitTime") decodeJobs



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
