module Views.TestHistoryTable exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Bootstrap.Modal as Modal exposing (..)
import Bootstrap.Progress as Progress exposing (..)
import Date exposing (Date)
import Date.Extra.Config.Config_en_au exposing (config)
import Date.Extra.Duration as Duration
import Date.Extra.Format as Format exposing (format, formatUtc, isoMsecOffsetFormat)
import DateFormat
import Html exposing (..)
import Html.Attributes as HtmlAttr exposing (..)
import Html.Events exposing (..)
import Http
import List.Extra as ListExtra
import Paginate exposing (PaginatedList)
import Paginate.Custom exposing (Paginated)
import Time exposing (Time)
import Utils.Types exposing (..)
import Views.NewmanModal as NewmanModal exposing (..)
import Utils.Common as Common

type Msg
    = First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String


type alias Model =
    { all : TestHistoryItems
    , paginated : PaginatedList TestHistoryItem
    , pageSize : Int
    , query : String
    }


init : TestHistoryItems -> Model
init list =
    let
        pageSize =
            25

        aa =
            Debug.log "TestHistoryTable" "init is called!"
    in
    { all = list
    , paginated = Paginate.fromList pageSize list
    , pageSize = pageSize
    , query = ""
    }


viewTable : Model -> Html Msg
viewTable model =
    let
        prevButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.paginated ) ], onClick First ]
                [ button [ class "page-link" ] [ text "«" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.paginated ) ], onClick Prev ]
                [ button [ class "page-link" ] [ text "‹" ]
                ]
            ]

        nextButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.paginated ) ], onClick Next ]
                [ button [ class "page-link" ] [ text "›" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.paginated ) ], onClick Last ]
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

                False ->
                    li [ class "page-item", onClick <| GoTo index ]
                        [ button [ class "page-link" ] [ text <| toString index ]
                        ]

        customPager f paginated =
            let
                currentPage =
                    Paginate.currentPage paginated

                totalPages =
                    Paginate.totalPages paginated

                maxPagesToShow =
                    10

                leftBound =
                    Basics.max (((currentPage - 1) // maxPagesToShow) * maxPagesToShow + 1) 1

                rightBound =
                    Basics.min (((currentPage - 1) // maxPagesToShow) * maxPagesToShow + maxPagesToShow) totalPages
            in
            List.range leftBound rightBound
                |> List.map (\i -> f i (i == Paginate.currentPage paginated))

        pagination =
            nav []
                [ ul [ class "pagination " ]
                    (prevButtons
                        ++ customPager pagerButtonView model.paginated
                        ++ nextButtons
                    )
                ]
    in
    div []
        [ div [ class "form-inline" ]
            [ div [ class "form-group" ]
                [ div [ class "btn-group" ]
                    [ FormInput.text
                        [ FormInput.onInput FilterQuery
                        , FormInput.placeholder "Filter"
                        , FormInput.value model.query
                        , FormInput.attrs [ class "filterinput" ]
                        ]
                    , span [ title "Clear filter", class "ion-close-circled searchclear", onClick <| FilterQuery "" ]
                        []
                    ]
                ]
            , div [ class "form-group" ] [ pagination ]
            ]
        , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
            [ thead []
                [ tr []
                    [ th [ width 65 ] [ text "Test Id" ]
                    , th [ width 75 ] [ text "Job Id" ]
                    , th [ width 65 ] [ text "Build" ]
                    , th [ width 65 ] [ text "Job Configuration" ]
                    , th [ width 65 ] [ text "End Time" ]
                    , th [ width 50 ] [ text "Duration" ]
                    , th [ width 30 ] [ text "Run Num" ]
                    , th [ width 70 ] [ text "Agent ID" ]
                    , th [ width 30 ] [ text "Agent Group" ]
                    , th [ width 30 ] [ text "Status" ]
                    , th [ width 155 ] [ text "Error Message" ]
                    ]
                ]
            , tbody [] (List.map viewRecord <| Paginate.page model.paginated)
            ]
        , pagination
        ]


viewRecord : TestHistoryItem -> Html Msg
viewRecord testHistory =
    let
        status =
            case testHistory.test.status |> toTestStatus of
                RUNNING ->
                    Badge.badgeInfo

                PENDING ->
                    Badge.badge

                SUCCESS ->
                    Badge.badgeSuccess

                FAIL ->
                    Badge.badgeDanger

        diffTime =
            Duration.diff (Date.fromTime (toFloat testHistory.test.endTime)) (Date.fromTime (toFloat testHistory.test.startTime))

        durationText =
            case ( diffTime.hour, diffTime.minute, diffTime.second ) of
                ( 0, 0, s ) ->
                    toString s ++ " seconds"

                ( 0, m, _ ) ->
                    toString m ++ " minutes"

                ( h, m, _ ) ->
                    toString h ++ " hours and " ++ toString m ++ " minutes"

        dateFormat date =
            DateFormat.format Common.dateTimeDateFormat (Date.fromTime (toFloat date))
    in
    tr []
        [ td [] [ a [ href <| "#test/" ++ testHistory.test.id ] [ text testHistory.test.id ] ]
        , td [] [ a [ href <| "#job/" ++ testHistory.test.jobId ++ "/ALL" ] [ text testHistory.test.jobId ] ]
        , td [] [ a [ href <| "#build/" ++ testHistory.job.buildId ] [ text <| testHistory.job.buildName ++ "(" ++ testHistory.job.buildBranch ++ ")" ] ]
        , td [] [ a [ href <| "#jobConfig/" ++ testHistory.job.jobConfigId ] [ text testHistory.job.jobConfigName ] ]
        , td [] [ text <| dateFormat testHistory.test.endTime ]
        , td [] [ text durationText ]
        , td [] [ text (toString testHistory.test.runNumber)]
        , td [] [ text testHistory.test.assignedAgent ]
        , td [] [ text testHistory.test.agentGroup ]
        , td [] [ status [] [ text testHistory.test.status ] ]
        , td [] [ span [ title testHistory.test.errorMessage ] [ text testHistory.test.errorMessage ] ]
        ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        First ->
            ( { model | paginated = Paginate.first model.paginated }, Cmd.none )

        Prev ->
            ( { model | paginated = Paginate.prev model.paginated }, Cmd.none )

        Next ->
            ( { model | paginated = Paginate.next model.paginated }, Cmd.none )

        Last ->
            ( { model | paginated = Paginate.last model.paginated }, Cmd.none )

        GoTo i ->
            ( { model | paginated = Paginate.goTo i model.paginated }, Cmd.none )

        FilterQuery query ->
            ( { model | query = query, paginated = Paginate.fromList model.pageSize (List.filter (filterQuery query) model.all) }
            , Cmd.none
            )


filterQuery : String -> TestHistoryItem -> Bool
filterQuery query testHistory =
    if
        String.length query
            == 0
            || String.startsWith query testHistory.test.status
            || String.startsWith query testHistory.test.jobId
            || String.startsWith query testHistory.job.buildName
            || String.startsWith query testHistory.job.buildBranch
            || String.startsWith query testHistory.test.assignedAgent
    then
        True
    else
        False


type TestStatus
    = PENDING
    | SUCCESS
    | FAIL
    | RUNNING


toTestStatus : String -> TestStatus
toTestStatus str =
    case str of
        "PENDING" ->
            PENDING

        "SUCCESS" ->
            SUCCESS

        "FAIL" ->
            FAIL

        "RUNNING" ->
            RUNNING

        _ ->
            FAIL
