module Pages.Home exposing (..)

import Bootstrap.Badge as Badge
import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput
import Bootstrap.Modal as Modal
import Date
import DateFormat
import Dict exposing (Dict)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Http exposing (..)
import Json.Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (decode, required)
import List.Extra as ListExtra
import Maybe exposing (withDefault)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal
import Utils.Common as Common

type Msg
    = GetDashboardDataCompleted (Result Http.Error DashboardData)
    | OnClickDropFutureJob String
    | OnFutureJobDropConfirmed String
    | NewmanModalMsg Modal.State
    | RequestCompletedDropFutureJob String (Result Http.Error String)
    | WebSocketEvent WebSocket.Event


type alias Model =
    { historyBuilds : List Build
    , futureJobs : List FutureJob
    , pendingBuilds : List Build
    , activeBuilds : List Build
    , activeJobs : ActiveJobsDashboard
    , confirmationState : Modal.State
    , futureJobToDrop : Maybe String
    }


init : ( Model, Cmd Msg )
init =
    ( { historyBuilds = []
      , futureJobs = []
      , pendingBuilds = []
      , activeBuilds = []
      , activeJobs = Dict.empty
      , confirmationState = Modal.hiddenState
      , futureJobToDrop = Nothing
      }
    , getDashboardDataCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetDashboardDataCompleted result ->
            case result of
                Ok data ->
                    ( { model
                        | historyBuilds = data.historyBuilds
                        , futureJobs = data.futureJobs
                        , pendingBuilds = data.pendingBuilds
                        , activeBuilds = data.activeBuilds
                        , activeJobs = data.activeJobs
                      }
                    , Cmd.none
                    )

                Err err ->
                    -- log error
                    let
                        a =
                            Debug.log "AA" err
                    in
                    ( model, Cmd.none )

        NewmanModalMsg newState ->
            ( { model | futureJobToDrop = Nothing, confirmationState = newState }, Cmd.none )

        OnClickDropFutureJob id ->
            ( { model | futureJobToDrop = Just id, confirmationState = Modal.visibleState }, Cmd.none )

        OnFutureJobDropConfirmed id ->
            ( { model | confirmationState = Modal.hiddenState }, dropFutureJobCmd id )

        RequestCompletedDropFutureJob id result ->
            onRequestCompletedDropFutureJob id model result

        WebSocketEvent event ->
            case event of
                CreatedFutureJob futureJob ->
                    ( { model | futureJobs = futureJob :: model.futureJobs }, Cmd.none )

                DeletedFutureJob futureJob ->
                    ( { model | futureJobs = removeFromList futureJob.id model.futureJobs} , Cmd.none)

                ModifiedBuild build ->
                    ( onEventModifiedBuild build model, Cmd.none )

                _ ->
                    ( model, Cmd.none )


findById id list =
    List.any (\item -> item.id == id) list


updateList id newItem list =
    ListExtra.replaceIf (\item -> item.id == id) newItem list


removeFromList id list =
    List.filter (\item -> item.id /= id) list

addToList item list =
    let
        newList = item :: list
    in
    if (List.length newList > 5) then
        ListExtra.removeAt ((List.length newList) - 1) newList
    else
        newList

handleActiveBuilds : Build -> Model -> Model
handleActiveBuilds build model =
    if (build.buildStatus.totalJobs == build.buildStatus.doneJobs) then
        let
            newActiveBuilds = removeFromList build.id model.activeBuilds
            newHistoryBuilds = addToList build model.historyBuilds
        in
        { model | activeBuilds = newActiveBuilds, historyBuilds = newHistoryBuilds }
    else if (build.buildStatus.runningJobs == 0 && build.buildStatus.pendingJobs > 0) then
        let
            newActiveBuilds = removeFromList build.id model.activeBuilds
            newPendingBuilds = addToList build model.pendingBuilds
        in
        { model | activeBuilds = newActiveBuilds, pendingBuilds = newPendingBuilds }
    else
        model





handlePendingBuilds : Build -> Model -> Model
handlePendingBuilds build model =
    if (build.buildStatus.totalJobs == build.buildStatus.doneJobs) then
        let
            newPendingBuilds = removeFromList build.id model.pendingBuilds
            newHistoryBuilds = addToList build model.historyBuilds
        in
        { model | pendingBuilds = newPendingBuilds, historyBuilds = newHistoryBuilds }
    else if (build.buildStatus.runningJobs > 0) then
        let
            newPendingBuilds = removeFromList build.id model.pendingBuilds
            newActiveBuilds = addToList build model.activeBuilds
        in
        { model | activeBuilds = newActiveBuilds, pendingBuilds = newPendingBuilds }
    else
        model


handleHistoryBuilds : Build -> Model -> Model
handleHistoryBuilds build orgModel =
    if (build.buildStatus.doneJobs < build.buildStatus.totalJobs) then
        let
            newHistoryBuilds = removeFromList build.id orgModel.historyBuilds
            model = { orgModel | historyBuilds = newHistoryBuilds }
        in
        if (build.buildStatus.runningJobs > 0) then
            let
                newActiveBuilds = addToList build model.activeBuilds
            in
            { model | activeBuilds = newActiveBuilds }
        else if (build.buildStatus.pendingJobs > 0) then
            let
                newPendingBuilds = addToList build model.pendingBuilds
            in
            { model | pendingBuilds = newPendingBuilds }
        else
            model

    else
        orgModel

onEventModifiedBuild : Build -> Model -> Model
onEventModifiedBuild build model =
    if findById build.id model.activeBuilds then
        handleActiveBuilds build model
    else if findById build.id model.pendingBuilds then
        handlePendingBuilds build model
    else if findById build.id model.historyBuilds then
        handleHistoryBuilds build model
    else
        model


onRequestCompletedDropFutureJob : String -> Model -> Result Http.Error String -> ( Model, Cmd Msg )
onRequestCompletedDropFutureJob jobId model result =
    case result of
        Ok _ ->
            let
                newList =
                    ListExtra.filterNot (\item -> item.id == jobId) model.futureJobs
            in
            ( { model | futureJobs = newList }, Cmd.none )

        Err err ->
            ( model, Cmd.none )


getDashboardDataCmd : Cmd Msg
getDashboardDataCmd =
    Http.send GetDashboardDataCompleted <| Http.get "/api/newman/dashboard" decodeDashboardData


view : Model -> Html Msg
view model =
    let
        toOption data =
            option [ value data.id ] [ text data.name ]

        submittedFutureJobFormat jobId =
            [ text ("submitted future job with id " ++ jobId) ]
    in
    div [ class "container-fluid" ] <|
        [ viewActiveBuilds model.activeBuilds model.activeJobs
        , viewPendingBuilds model.pendingBuilds
        , viewHistory model.historyBuilds
        , viewFutureJobs model.futureJobs
        , NewmanModal.confirmFutureJobDrop model.futureJobToDrop NewmanModalMsg OnFutureJobDropConfirmed model.confirmationState
        ]


viewHistory : List Build -> Html Msg
viewHistory dashboardBuilds =
    div []
        [ h2 [] [ text "History" ]
        , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover history-table" ]
            [ thead []
                [ tr []
                    [ th [] [ text "Build" ]
                    , th [] [ text "Date" ]
                    , th []
                        [ Badge.badgeSuccess [] [ text "Passed" ]
                        , text " "
                        , Badge.badgeDanger [] [ text "Failed" ]
                        , text " "
                        , Badge.badge [] [ text "Total" ]
                        , text " | Tests"
                        ]
                    , th [ align "center" ]
                        [ Badge.badgeSuccess [] [ text "Done" ]
                        , text " "
                        , Badge.badgeDanger [] [ text "Broken" ]
                        , text " "
                        , Badge.badge [] [ text "Total" ]
                        , text " | Jobs"
                        ]
                    , th [] [ text "Suites" ]
                    ]
                ]
            , tbody [] (List.map viewHistoryBuild dashboardBuilds)
            ]
        ]


viewFutureJobs : List FutureJob -> Html Msg
viewFutureJobs futureJobs =
    let
        formatTime time =
            DateFormat.format Common.dateTimeDateFormat <| Date.fromTime <| toFloat time

        viewFutureJob futureJob =
            tr []
                [ td [] [ a [ href <| "#build/" ++ futureJob.buildId ] [ text <| futureJob.buildName ++ " (" ++ futureJob.buildBranch ++ ")" ] ]
                , td [] [ a [ href <| "#suite/" ++ futureJob.suiteId ] [ text futureJob.suiteName ] ]
                , td [] [ text futureJob.author ]
                , td [] [ text <| formatTime futureJob.submitTime ]
                , td []
                    [ Button.button [ Button.danger, Button.small, Button.onClick <| OnClickDropFutureJob futureJob.id ]
                        [ span [ class "ion-close" ] [] ]
                    ]
                ]
    in
    div []
        [ h2 [] [ text "Future Jobs" ]
        , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover history-table" ]
            [ thead []
                [ tr []
                    [ th [] [ text "Build" ]
                    , th [] [ text "Suite Name" ]
                    , th [] [ text "Author" ]
                    , th [] [ text "Submit Time" ]
                    , th [] [ text "Actions" ]
                    ]
                ]
            , tbody [] (List.map viewFutureJob futureJobs)
            ]
        ]


viewHistoryBuild : Build -> Html msg
viewHistoryBuild build =
    let
        buildName =
            build.name ++ "(" ++ build.branch ++ ")"

        buildDate =
            DateFormat.format Common.dateTimeDateFormat (Date.fromTime (toFloat build.buildTime))

        buildStatus =
            build.buildStatus

        testsData =
            [ Badge.badgeSuccess [] [ text <| toString buildStatus.passedTests ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.failedTests ]
            , text " "
            , Badge.badge [] [ text <| toString buildStatus.totalTests ]
            ]

        jobsData =
            [ Badge.badgeSuccess [] [ text <| toString buildStatus.doneJobs ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.brokenJobs ]
            , text " "
            , Badge.badge [] [ text <| toString buildStatus.totalJobs ]
            ]
    in
    tr []
        [ td [] [ a [ href <| "#build/" ++ build.id ] [ text buildName ] ]
        , td [] [ text buildDate ]
        , td [ class "tests-data" ] testsData
        , td [] jobsData
        , td [] <| List.intersperse (text " ") <| List.map (\( name, id ) -> a [ href <| "#suite/" ++ id ] [ text name ]) <| ListExtra.zip build.buildStatus.suitesNames build.buildStatus.suitesIds
        ]


viewPendingBuilds : List Build -> Html Msg
viewPendingBuilds builds =
    div []
        [ h2 [] [ text "Pending Builds" ]
        , table [ class "table table-sm table-bordered table-striped table-hover history-table" ]
            [ thead []
                [ tr []
                    [ th [ widthPcnt "18%" ] [ text "Build" ]
                    , th [ widthPcnt "8%" ] [ text "Date" ]
                    , th [ widthPcnt "20%" ]
                        [ Badge.badgeSuccess [] [ text "Passed" ]
                        , text " "
                        , Badge.badgeDanger [] [ text "Failed" ]
                        , text " "
                        , Badge.badgeWarning [ style [("background-color","DarkRed")] ] [ text "Failed 3 Times" ]
                        , text " "
                        , Badge.badgePrimary [] [ text "Pending" ]
                        , text " | Tests"
                        ]
                    , th [ align "center", widthPcnt "15%" ]
                        [ Badge.badgeSuccess [] [ text "Done" ]
                        , text " "
                        , Badge.badgeDanger [] [ text "Broken" ]
                        , text " "
                        , Badge.badge [] [ text "Total" ]
                        , text " | Jobs"
                        ]
                    , th [ widthPcnt "42%" ] [ text "Suites" ]
                    ]
                ]
            , tbody [] (List.map viewPendingBuild builds)
            ]
        ]


widthPcnt : String -> Html.Attribute Msg
widthPcnt pcnt =
    style [ ( "width", pcnt ) ]


viewPendingBuild : Build -> Html Msg
viewPendingBuild build =
    let
        buildName =
            build.name ++ "(" ++ build.branch ++ ")"

        buildDate =
            DateFormat.format Common.dateTimeDateFormat (Date.fromTime (toFloat build.buildTime))

        buildStatus =
            build.buildStatus

        testsData =
            [ Badge.badgeSuccess [] [ text <| toString buildStatus.passedTests ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.failedTests ]
            , text " "
            , Badge.badgeWarning [ style [("background-color","DarkRed")] ] [ text <| toString buildStatus.failed3TimesTests ]
            , text " "
            , Badge.badgePrimary [] [ text <| toString (buildStatus.totalTests - buildStatus.passedTests - buildStatus.failedTests) ]
            ]

        jobsData =
            [ Badge.badgeSuccess [] [ text <| toString buildStatus.doneJobs ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.brokenJobs ]
            , text " "
            , Badge.badge [] [ text <| toString buildStatus.totalJobs ]
            ]
    in
    tr []
        [ td [] [ a [ href <| "#build/" ++ build.id ] [ text buildName ] ]
        , td [] [ text buildDate ]
        , td [ class "tests-data" ] testsData
        , td [] jobsData
        , td [] <| List.intersperse (text " ") <| List.map (\( name, id ) -> a [ href <| "#suite/" ++ id ] [ text name ]) <| ListExtra.zip build.buildStatus.suitesNames build.buildStatus.suitesIds
        ]


viewActiveBuilds : List Build -> ActiveJobsDashboard -> Html Msg
viewActiveBuilds builds activeJobs =
    div []
        [ h2 [] [ text "Active Builds" ]
        , table [ class "table table-sm table-bordered table-striped table-hover history-table" ]
            [ thead []
                [ tr []
                    [ th [ widthPcnt "18%" ] [ text "Build" ]
                    , th [ widthPcnt "8%" ] [ text "Date" ]
                    , th [ widthPcnt "18%" ]
                        [ Badge.badgeInfo [] [ text "Running" ]
                        , text " "
                        , Badge.badgeSuccess [] [ text "Passed" ]
                        , text " "
                        , Badge.badgeDanger [] [ text "Failed" ]
                        , text " "
                        , Badge.badgeWarning [ style [("background-color","DarkRed")] ] [ text "Failed 3 Times" ]
                        , text " "
                        , Badge.badgePrimary [] [ text "Pending" ]
                        , text " | Tests"
                        ]
                    , th [ align "center", widthPcnt "15%" ]
                        [ Badge.badgeSuccess [] [ text "Done" ]
                        , text " "
                        , Badge.badgeDanger [] [ text "Broken" ]
                        , text " "
                        , Badge.badge [] [ text "Total" ]
                        , text " | Jobs"
                        ]
                    , th [] [ text "Suites" ]
                    ]
                ]
            , tbody [] <| List.concat (List.map (viewActiveBuild activeJobs) builds)
            ]
        ]


viewActiveBuild : ActiveJobsDashboard -> Build -> List (Html Msg)
viewActiveBuild activeJobs build =
    let
        buildName =
            build.name ++ "(" ++ build.branch ++ ")"

        buildDate =
            formatDate build.buildTime

        formatDate time =
            DateFormat.format Common.dateTimeDateFormat (Date.fromTime (toFloat time))

        buildStatus =
            build.buildStatus

        testsData =
            [ Badge.badgeInfo [] [ text <| toString buildStatus.runningTests ]
            , text " "
            , Badge.badgeSuccess [] [ text <| toString buildStatus.passedTests ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.failedTests ]
            , text " "
            , Badge.badgeWarning [ style [("background-color","DarkRed")] ] [ text <| toString buildStatus.failed3TimesTests ]
            , text " "
            , Badge.badgePrimary [] [ text <| toString (buildStatus.totalTests - buildStatus.passedTests - buildStatus.failedTests) ]
            ]

        jobsData =
            [ Badge.badgeSuccess [] [ text <| toString buildStatus.doneJobs ]
            , text " "
            , Badge.badgeDanger [] [ text <| toString buildStatus.brokenJobs ]
            , text " "
            , Badge.badge [] [ text <| toString buildStatus.totalJobs ]
            ]

        viewJobForActiveBuild job =
            tr []
                [ td [] [ text <| "↳ Job: ", a [ href <| "#job/" ++ job.id ] [ text job.id ] ]
                , td [] []
                , td [ colspan 2, class "tests-data" ]
                    [ Badge.badgeInfo [] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/RUNNING" ] [text <| toString job.runningTests] ]
                    , text " "
                    , Badge.badgeSuccess [] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/SUCCESS" ] [text <| toString job.passedTests] ]
                    , text " "
                    , Badge.badgeDanger [] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/FAIL" ] [text <| toString job.failedTests] ]
                    , text " "
                    , Badge.badgeWarning [ style [("background-color","DarkRed")] ] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/FAILED3TIMES" , title "Failed 3 Times" ]
                                                [text <| toString job.failed3TimesTests] ]
                    , text " "
                    , Badge.badge [] [ a [ class "tests-num-link", href <| "#job/" ++ job.id ++ "/ALL" ] [ text <| toString job.totalTests] ]
                    ]
                , td [] [ a [ href <| "#suite/" ++ job.suiteId ] [ text job.suiteName ] ]
                ]
    in
    [ tr []
        [ td [] [ a [ href <| "#build/" ++ build.id ] [ text buildName ] ]
        , td [] [ text buildDate ]
        , td [ class "tests-data" ] testsData
        , td [] jobsData
        , td [] <| List.intersperse (text " ") <| List.map (\( name, id ) -> a [ href <| "#suite/" ++ id ] [ text name ]) <| ListExtra.zip build.buildStatus.suitesNames build.buildStatus.suitesIds
        ]
    ]
        ++ List.map viewJobForActiveBuild (Maybe.withDefault [] (Dict.get build.id activeJobs))



--- CMD


dropFutureJobCmd : String -> Cmd Msg
dropFutureJobCmd futureJobId =
    Http.send (RequestCompletedDropFutureJob futureJobId) <|
        Http.request <|
            { method = "DELETE"
            , headers = []
            , url = "/api/newman/deleteFutureJob/" ++ futureJobId
            , body = Http.emptyBody
            , expect = Http.expectString
            , timeout = Nothing
            , withCredentials = False
            }


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
