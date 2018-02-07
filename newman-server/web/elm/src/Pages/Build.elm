module Pages.Build exposing (..)

import Date
import Date.Format
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
import Time exposing (Time)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)
import Views.JobsTable as JobsTable exposing (..)


type alias Model =
    { maybeBuild : Maybe Build
    , maybeJobsTableModel : Maybe JobsTable.Model
    , currTime : Time
    }


type Msg
    = GetBuildInfoCompleted (Result Http.Error Build)
    | GetJobsInfoCompleted (Result Http.Error Jobs)
    | JobsTableMsg JobsTable.Msg
    | OnTime Time


parseBuildId : Parser (String -> a) a
parseBuildId =
    UrlParser.string


init : BuildId -> ( Model, Cmd Msg )
init buildId =
    let
        maxEntries =
            40
    in
    ( { maybeBuild = Nothing
      , maybeJobsTableModel = Nothing
      , currTime = 0
      }
    , Cmd.none
    )


view : Model -> Html Msg
view model =
    case ( model.maybeBuild, model.maybeJobsTableModel ) of
        ( Just build, Just subModel ) ->
            let
                jobsTableView =
                    JobsTable.viewTable subModel model.currTime |> Html.map JobsTableMsg

                buildDate =
                    Date.Format.format "%b %d, %H:%M:%S" (Date.fromTime (toFloat build.buildTime))

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

                xapOpenSha =
                    withDefault "" (Dict.get "xap-open" build.shas)

                xapSha =
                    withDefault "" (Dict.get "xap" build.shas)

                insightEdgeSha =
                    withDefault "" (Dict.get "InsightEdge" build.shas)
            in
            div [ class "container-fluid" ] <|
                [ h2 [ class "text" ] [ text <| "Details for build " ++ build.name ]
                , table []
                    [ viewRow ( "Build Id", text build.id )
                    , viewRow ( "Branch", text build.branch )
                    , viewRow ( "Tags", text <| String.join "," build.tags )
                    , viewRow ( "Build Time", text buildDate )
                    , resourcesRow
                    , viewRow ( "Test Metadata", text <| String.join "," build.testsMetadata )
                    , viewRow ( "Commits:", text "" )
                    , viewRow ( "xap-open", a [ href <| xapOpenSha ] [ text xapOpenSha ] )
                    , viewRow ( "xap", a [ href <| xapSha ] [ text xapSha ] )
                    , viewRow ( "InsightEdge", a [ href <| insightEdgeSha ] [ text insightEdgeSha ] )
                    ]
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
                    ( { model | maybeJobsTableModel = Just (JobsTable.init jobsFromResult) }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        OnTime time ->
            ( { model | currTime = time }, Cmd.none )

        JobsTableMsg subMsg ->
            let
                ( updatedJobsTableModel, cmd ) =
                    case model.maybeJobsTableModel of
                        Just subModel ->
                            JobsTable.update subMsg subModel

                        Nothing ->
                            JobsTable.update subMsg (JobsTable.init [])
            in
            ( { model | maybeJobsTableModel = Just updatedJobsTableModel }, cmd |> Cmd.map JobsTableMsg )


getBuildInfoCmd : BuildId -> Cmd Msg
getBuildInfoCmd buildId =
    Http.send GetBuildInfoCompleted <|
        Http.get ("/api/newman/build/" ++ buildId) decodeBuild


getJobsInfoCmd : BuildId -> Cmd Msg
getJobsInfoCmd buildId =
    Http.send GetJobsInfoCompleted <|
        Http.get ("/api/newman/job?buildId=" ++ buildId ++ "&all=true") decodeJobs
