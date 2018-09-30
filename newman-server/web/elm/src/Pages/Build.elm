module Pages.Build exposing (..)

import Date exposing (..)
import DateFormat
import Dict exposing (Dict)
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Http exposing (..)
import Json.Decode
import Json.Decode.Pipeline exposing (decode)
import List exposing (length)
import Maybe exposing (withDefault)
import Platform.Cmd exposing (batch)
import Task exposing (..)
import Time exposing (Time)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)
import Views.JobsTable as JobsTable exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Utils.Common as Common

type alias Model =
    { maybeBuild : Maybe Build
    , maybeJobsTableModel : Maybe JobsTable.Model
    , currTime : Maybe Time
    }


type Msg
    = GetBuildInfoCompleted (Result Http.Error Build)
    | GetJobsInfoCompleted (Result Http.Error (List Job))
    | JobsTableMsg JobsTable.Msg
    | ReceiveTime Time
    | WebSocketEvent WebSocket.Event


parseBuildId : Parser (String -> a) a
parseBuildId =
    UrlParser.string


initModel :  Model
initModel =
    let
        maxEntries =
            40
    in
    { maybeBuild = Nothing
      , maybeJobsTableModel = Nothing
      , currTime = Nothing
    }


initCmd : BuildId -> Cmd Msg
initCmd buildId =
    Cmd.batch [ getBuildInfoCmd buildId, requestTime ]


view : Model -> Html Msg
view model =
    case ( model.maybeBuild, model.maybeJobsTableModel ) of
        ( Just build, Just subModel ) ->
            let
                jobsTableView =
                    JobsTable.viewTable subModel model.currTime |> Html.map JobsTableMsg

                buildDate =
                    DateFormat.format Common.dateTimeDateFormat (Date.fromTime (toFloat build.buildTime))

                resourcesRow =
                    tr []
                        [ td [ style [ ( "vertical-align", "top" ) ] ]
                            [ text "Resources"
                            ]
                        , td []
                            [ ul [] <|
                                List.map
                                    viewResource
                                    build.resources
                            ]
                        ]

                viewResource resource =
                    li [] [ a [ href <| resource ] [ text resource ] ]

                viewRow ( name, value ) =
                    tr []
                        [ td [ width 100 ] [ text name ]
                        , td [] [ value ]
                        ]

                shasRows =
                    let
                        toShaRow ( key, val ) =
                            viewRow ( key, a [ href <| val ] [ text val ] )
                    in
                    List.map
                        toShaRow
                    <|
                        Dict.toList build.shas

            in
            div [ class "container-fluid" ] <|
                [ h2 [ class "text" ] [ text <| "Details for build " ++ build.name ]
                , table []
                    ([ viewRow ( "Build Id", text build.id )
                     , viewRow ( "Branch", text build.branch )
                     , viewRow ( "Tags", text <| String.join "," build.tags )
                     , viewRow ( "Build Time", text buildDate )
                     , resourcesRow
                     , viewRow ( "Test Metadata", text <| String.join "," build.testsMetadata )
                     , viewRow ( "Commits:", text "" )
                     ]
                        ++ shasRows
                    )
                , br [] []
                , h2 [ class "text" ] [ text "Participate in the following jobs:" ]
                , jobsTableView
                ]

        _ ->
            div [] []


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        d =
            Debug.log "Build.update" ("was called" ++ toString msg)
    in
    case msg of
        GetBuildInfoCompleted result ->
            case result of
                Ok data ->
                    let
                        d =
                            Debug.log "Build.update" "was called"
                    in
                    ( { model | maybeBuild = Just data }, getJobsInfoCmd data.id )

                Err err ->
                    ( model, Cmd.none )

        GetJobsInfoCompleted result ->
            case result of
                Ok jobsFromResult ->
                    ( { model | maybeJobsTableModel = Just (JobsTable.init jobsFromResult) }, requestTime )

                Err err ->
                    ( model, Cmd.none )

        JobsTableMsg subMsg ->
            let
                ( updatedJobsTableModel, cmd ) =
                    case model.maybeJobsTableModel of
                        Just subModel ->
                            JobsTable.update subMsg subModel

                        Nothing ->
                            JobsTable.update subMsg (JobsTable.init [])
            in
            ( { model | maybeJobsTableModel = Just updatedJobsTableModel }, Cmd.batch [ cmd |> Cmd.map JobsTableMsg , requestTime ] )

        ReceiveTime time ->
            ( { model | currTime = Just time } , Cmd.none )

        WebSocketEvent event ->
            case event of
                ModifiedBuild build ->
                    case model.maybeBuild of
                        Just currentBuild ->
                            if (currentBuild.id == build.id) then
                                ( {model | maybeBuild = Just build} , Cmd.none )
                            else
                                ( model, Cmd.none )
                        Nothing ->
                            ( model, Cmd.none )

                _ ->
                    ( model, Cmd.none )


getBuildInfoCmd : BuildId -> Cmd Msg
getBuildInfoCmd buildId =
    Http.send GetBuildInfoCompleted <|
        Http.get ("/api/newman/build/" ++ buildId) decodeBuild


getJobsInfoCmd : BuildId -> Cmd Msg
getJobsInfoCmd buildId =
    Http.send GetJobsInfoCompleted <|
        Http.get ("/api/newman/job?buildId=" ++ buildId ++ "&all=true") <|
            Json.Decode.field "values" (Json.Decode.list decodeJob)

requestTime : Cmd Msg
requestTime =
    Task.perform ReceiveTime Time.now


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent