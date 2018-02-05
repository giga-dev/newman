module Views.JobsTable exposing (..)

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
import List.Extra as ListExtra
import Paginate exposing (PaginatedList)
import Time exposing (Time)
import Utils.Types exposing (..)


type Msg
    = First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String
    | OnClickToggleJob String
    | ToggleJobCompleted (Result Http.Error Job)


type alias Model =
    { allJobs : Jobs
    , jobs : PaginatedList Job
    , pageSize : Int
    }


init : List Job -> Model
init jobs =
    let
        pageSize =
            10
    in
    Model jobs (Paginate.fromList pageSize jobs) pageSize


viewTable : Model -> Time -> Html Msg
viewTable model currTime =
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
    div []
        [ Form.formInline []
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
            , tbody [] (List.map (viewJob currTime) <| Paginate.page model.jobs)
            ]
        , pagination
        ]


viewJob : Time -> Job -> Html Msg
viewJob currTime job =
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
                            Just <| Duration.diff (Date.fromTime currTime) (Date.fromTime (toFloat startTime))

                        ( _, _ ) ->
                            Nothing
            in
            case diffTime of
                Just diff ->
                    toString diff.hour ++ "h, " ++ toString diff.minute ++ "m"

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
                    Button.button [ Button.success, Button.small, Button.onClick <| OnClickToggleJob job.id ]
                        [ span [ class "ion-play" ] [] ]

                state ->
                    Button.button [ Button.warning, Button.small, Button.disabled <| (state /= RUNNING && state /= READY) , Button.onClick <| OnClickToggleJob job.id ]
                        [ span [ class "ion-pause" ] [] ]
    in
    tr []
        [ td [] [ jobState ]
        , td [] [ progress ]
        , td [] [ a [ href <| "#job/" ++ job.id ] [ text job.id ] ]
        , td [] [ text job.suiteName ]
        , td [] [ text durationText ]
        , td [ title submittedTimeHourFull ] [ text submittedTimeHour ]
        , td [] [ a [ href <| "#build/" ++ job.buildId ] [ text job.buildName ] ]
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
            [ Button.button [ Button.danger, Button.small ]
                [ span [ class "ion-close" ] [] ]
            , text " "
            , playPauseButton
            ]
        ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
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

        OnClickToggleJob jobId ->
            ( model, toggleJobCmd jobId )

        ToggleJobCompleted result ->
            onToggleJobCompleted model result


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



-- commands


onToggleJobCompleted : Model -> Result Http.Error Job -> ( Model, Cmd Msg )
onToggleJobCompleted model result =
    case result of
        Ok job ->
            let
                newList =
                    Paginate.map (ListExtra.replaceIf (\item -> item.id == job.id) job) model.jobs
            in
            ( { model | jobs = newList }, Cmd.none )

        Err err ->
            ( model, Cmd.none )


toggleJobCmd : String -> Cmd Msg
toggleJobCmd jobId =
    Http.send ToggleJobCompleted <| Http.post ("/api/newman/job/" ++ jobId ++ "/toggle") Http.emptyBody decodeJob