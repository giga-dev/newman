module Views.TestsTable exposing (..)

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
import Date.Format
import Html exposing (..)
import Html.Attributes as HtmlAttr exposing (..)
import Html.Events exposing (..)
import Http
import List.Extra as ListExtra
import Paginate exposing (PaginatedList)
import Paginate.Custom exposing (Paginated)
import Time exposing (Time)
import Utils.Types exposing (Test, decodeTest)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal exposing (..)


type Msg
    = First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String
    | WebSocketEvent WebSocket.Event


type alias Model =
    { all : List Test
    , paginated : PaginatedList Test
    , pageSize : Int
    , query : String
    , jobId : String
    }


init : String -> List Test -> Model
init jobId list =
    let
        pageSize =
            25

        aa =
            Debug.log "TestsTable" "init is called!"
    in
    { all = list
    , paginated = Paginate.fromList pageSize list
    , pageSize = pageSize
    , query = ""
    , jobId = jobId
    }


viewTable : Model -> Time -> Html Msg
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
                    [ th [ style [ ( "width", "35%" ) ] ] [ text "Name" ]
                    , th [ width 65 ] [ text "Status" ]
                    , th [ width 105 ] [ text "History Stats" ]
                    , th [ width 65 ] [ text "History" ]
                    , th [ width 210 ] [ text "Error Message" ]
                    , th [ width 210 ] [ text "Assigned Agent" ]
                    , th [ width 100 ] [ text "Duration" ]
                    ]
                ]
            , tbody [] (List.map (viewTest currTime) <| Paginate.page model.paginated)
            ]
        , pagination
        ]



--
{-
   PENDING
       | SUCCESS
       | FAIL
       | RUNNING
-}


viewTest : Time -> Test -> Html Msg
viewTest currTime test =
    let
        status =
            case test.status |> toTestStatus of
                RUNNING ->
                    Badge.badgeInfo

                PENDING ->
                    Badge.badge

                SUCCESS ->
                    Badge.badgeSuccess

                FAIL ->
                    Badge.badgeDanger

        durationText =
            let
                diffTime =
                    case ( test.startTime, test.endTime ) of
                        ( Just startTime, Just endTime ) ->
                            Just <| Duration.diff (Date.fromTime (toFloat endTime)) (Date.fromTime (toFloat startTime))

                        ( Just startTime, Nothing ) ->
                            Just <| Duration.diff (Date.fromTime currTime) (Date.fromTime (toFloat startTime))

                        ( _, _ ) ->
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
            if toTestStatus test.status == SUCCESS then
                "black-column"
            else if test.testScore <= 3 then
                "red-column"
            else if test.testScore > 3 then
                "blue-column"
            else
                ""
    in
    tr []
        [ td [] [ a [ href <| "#test/" ++ test.id, title <| String.join "" test.arguments ] [ text <| String.join " " test.arguments ] ]
        , td [] [ status [] [ text test.status ] ]
        , td [ class historyStatsClass ] historyStats
        , td [] [ a [ href <| "#test-history/" ++ test.id ] [ text "History" ] ]
        , td [] [ span [ title test.errorMessage ] [ text test.errorMessage ] ]
        , td [] [ text test.assignedAgent ]
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
            ( { model | query = query, paginated = Paginate.fromList model.pageSize (List.filter (filterQuery query) model.all) }
            , Cmd.none
            )

        WebSocketEvent event ->
            case event of
                CreatedTest test ->
                    ( updateTestAdded model test, Cmd.none )

                ModifiedTest test ->
                    if model.jobId == test.jobId then
                        ( updateTestUpdated model test, Cmd.none )
                    else
                        ( model, Cmd.none )

                _ ->
                    ( model, Cmd.none )


updateAllTests : (List Test -> List Test) -> Model -> Model
updateAllTests f model =
    let
        newList =
            f model.all

        filtered =
            List.filter (filterQuery model.query) newList

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
            || String.startsWith (String.toLower query) (String.toLower test.name)
            || String.startsWith query test.status
            || String.startsWith query test.assignedAgent
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
