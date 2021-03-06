module Views.TestsTable exposing (..)

import Bootstrap.Badge as Badge exposing (..)
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Date exposing (Date)
import Date.Extra.Duration as Duration
import Html exposing (..)
import Html.Attributes as HtmlAttr exposing (..)
import Html.Events exposing (..)
import Http
import List.Extra as ListExtra
import Navigation
import Paginate exposing (PaginatedList)
import Paginate.Custom exposing (Paginated)
import Time exposing (Time)
import Utils.Types exposing (JobId, RadioState(..), Test, TestStatus(..), agentGroupTestFormat, decodeTest, radioStateToString, testStatusToString)
import Utils.WebSocket as WebSocket exposing (..)


type Msg
    = First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String
    | WebSocketEvent WebSocket.Event
    | UpdateFilterState JobId RadioState


type alias Model =
    { all : List Test
    , paginated : PaginatedList Test
    , pageSize : Int
    , query : String
    , jobId : String
    , filterState : RadioState
    }


init : String -> List Test -> RadioState -> Model
init jobId list state =
    let
        pageSize =
            25
    in
    { all = list
    , paginated = Paginate.fromList pageSize <| filterTests list "" state
    , pageSize = pageSize
    , query = ""
    , jobId = jobId
    , filterState = state
    }


viewTable : Model -> Maybe Time -> Html Msg
viewTable model currTime =
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
                    [ th [ style [ ( "width", "25%" ) ] ] [ text "Name" ]
                    , th [ width 65 ] [ text "Status" ]
                    , th [ width 105 ] [ text "History Stats" ]
                    , th [ width 65 ] [ text "History" ]
                    , th [ width 210 ] [ text "Error Message" ]
                    , th [ width 210 ] [ text "Assigned Agent" ]
                    , th [ width 80 ] [ text "Agent Group" ]
                    , th [ width 100 ] [ text "Duration" ]
                    ]
                ]
            , tbody [] (List.map (viewTest currTime) <| Paginate.page model.paginated)
            ]
        , pagination
        ]


viewTest : Maybe Time -> Test -> Html Msg
viewTest currTime test =
    let
        status =
            case test.status of
                TEST_RUNNING ->
                    Badge.badgeInfo

                TEST_PENDING ->
                    Badge.badge

                TEST_SUCCESS ->
                    Badge.badgeSuccess

                TEST_FAIL ->
                    Badge.badgeDanger

        durationText =
            let
                diffTime =
                    case ( test.startTime, test.endTime, currTime ) of
                        ( Just startTime, Just endTime, _ ) ->
                            Just <| Duration.diff (Date.fromTime (toFloat endTime)) (Date.fromTime (toFloat startTime))

                        ( Just startTime, Nothing, Just time ) ->
                            Just <| Duration.diff (Date.fromTime time) (Date.fromTime (toFloat startTime))

                        ( _, _, _ ) ->
                            Nothing
            in
            case diffTime of
                Just diff ->
                    case ( diff.hour, diff.minute, diff.second ) of
                        ( 0, 0, s ) ->
                            toString s ++ " seconds"

                        ( 0, m, _ ) ->
                            toString m ++ " minutes"

                        ( h, m, _ ) ->
                            toString h ++ " hours and " ++ toString m ++ " minutes"

                Nothing ->
                    ""

        historyStats =
            let
                delimiter =
                    "_"

                splitted =
                    String.split delimiter test.historyStats

                shorten txt =
                    String.slice 0 18 txt
            in
            case splitted of
                [ first, second ] ->
                    [ text <| shorten first, br [] [], text <| shorten second ]

                [ one ] ->
                    [ text <| shorten one ]

                _ ->
                    [ text "" ]

        historyStatsClass =
            if test.status == TEST_SUCCESS then
                "black-column"

            else if test.testScore <= 3 then
                "red-column"

            else if test.testScore > 3 then
                "blue-column"

            else
                ""
    in
    tr []
        [ td [] [ a [ href <| "#test/" ++ test.id, title <| String.join "" test.arguments ] [ text <| toTestName test ] ]
        , td [] [ status [] [ text (testStatusToString test.status) ] ]
        , td [ class historyStatsClass ] historyStats
        , td [] [ a [ href <| "#test-history/" ++ test.id ] [ text "History" ] ]
        , td [] [ span [ title test.errorMessage ] [ text test.errorMessage ] ]
        , td [] [ text test.assignedAgent ]
        , td [ title <| agentGroupTestFormat test.agentGroup test.assignedAgent ] [ text <| agentGroupTestFormat test.agentGroup test.assignedAgent ]
        , td [] [ text durationText ]
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
            ( { model
                | query = query
                , paginated =
                    filterTests model.all query model.filterState
                        |> Paginate.fromList model.pageSize
              }
            , Cmd.none
            )

        WebSocketEvent event ->
            case event of
                CreatedTest test ->
                    if model.jobId == test.jobId then
                        ( updateTestAdded model test, Cmd.none )

                    else
                        ( model, Cmd.none )

                ModifiedTest test ->
                    if model.jobId == test.jobId then
                        ( updateTestUpdated model test, Cmd.none )

                    else
                        ( model, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        UpdateFilterState jobId state ->
            ( { model | filterState = state, paginated = filterTests model.all model.query state |> Paginate.fromList model.pageSize }
            , modifyUrl jobId (radioStateToString state)
            )


modifyUrl : String -> String -> Cmd Msg
modifyUrl jobId state =
    Navigation.modifyUrl <| "#" ++ String.join "/" [ "job", jobId, state ]


filterTests : List Test -> String -> RadioState -> List Test
filterTests tests query filterState =
    tests
        |> List.filter (filterQuery query)
        |> List.filter (filterByTestStatus filterState)


filterByTestStatus : RadioState -> Test -> Bool
filterByTestStatus currentState test =
    case currentState of
        STATUS_RUNNING ->
            test.status == TEST_RUNNING

        STATUS_SUCCESS ->
            test.status == TEST_SUCCESS

        STATUS_FAIL ->
            test.status == TEST_FAIL

        STATUS_FAILED3TIMES ->
            test.runNumber == 3 && test.status == TEST_FAIL

        STATUS_ALL ->
            True


updateAllTests : (List Test -> List Test) -> Model -> Model
updateAllTests f model =
    let
        newList =
            f model.all

        filtered =
            filterTests newList model.query model.filterState

        newPaginated =
            Paginate.map (\_ -> filtered) model.paginated
    in
    { model | paginated = newPaginated, all = newList }


updateTestAdded : Model -> Test -> Model
updateTestAdded model addedTest =
    updateAllTests (\list -> addedTest :: list) model


updateTestUpdated : Model -> Test -> Model
updateTestUpdated model testToUpdate =
    let
        f =
            ListExtra.replaceIf (\item -> item.id == testToUpdate.id) testToUpdate
    in
    updateAllTests f model


filterQuery : String -> Test -> Bool
filterQuery query test =
    if
        String.length query
            == 0
            || String.startsWith query test.id
            || String.contains (String.toLower query) (String.toLower test.name)
            || String.contains (String.toLower query) (String.toLower <| String.join " " test.arguments)
            || String.contains query test.assignedAgent
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


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent


toTestName : Test -> String
toTestName { arguments, runNumber } =
    let
        num =
            case runNumber of
                1 ->
                    ""

                other ->
                    String.append "#" (toString other)

        testName =
            List.append arguments [ num ]
    in
    String.join " " testName
