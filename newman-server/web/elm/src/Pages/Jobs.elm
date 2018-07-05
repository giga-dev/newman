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
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket
import Views.JobsTable as JobsTable

type alias Model =
    { jobsTableModel : JobsTable.Model
    , maxEntries : Int
    , currTime : Time
    }


type Msg
    = UpdateMaxEntries String
    | GetJobsCompleted (Result Http.Error (List Job))
    | OnTime Time
    | JobsTableMsg JobsTable.Msg
    | UpdateJobsNumber String
    | ApplyJobsNumberAndRetrieveJobs


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    JobsTable.handleEvent event |> Cmd.map JobsTableMsg


init : ( Model, Cmd Msg )
init =
    let
        maxEntries =
            60
    in
    ( { jobsTableModel = JobsTable.init []
      , maxEntries = maxEntries
      , currTime = 0
      }
    , Cmd.batch [ getJobsCmd maxEntries, getTime ]
    )



-----
{-
   UPDATE
   * Messages
   * Update case
-}


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

        JobsTableMsg subMsg ->
            let
                ( updatedJobsTableModel, cmd ) =
                    JobsTable.update subMsg model.jobsTableModel
            in
            ( { model | jobsTableModel = updatedJobsTableModel }, cmd |> Cmd.map JobsTableMsg )

        UpdateJobsNumber jobsNum -> ( { model | maxEntries = String.toInt jobsNum |> Result.withDefault 1 }, Cmd.none )

        ApplyJobsNumberAndRetrieveJobs ->( { model | jobsTableModel = JobsTable.init [] },
                                    Cmd.batch [ getJobsCmd model.maxEntries, getTime ] )

-----
{-
   VIEW
-}


view : Model -> Html Msg
view model =
    div []
        [ div [ class "form-inline" ]
            [ h1 [ class "jobs-label" ] [ text "Jobs" ]
            , div[class "form-inline jobs-list-max-job-count"][
                 div[ class "jobs-list-max-job-count-label" ] [text("Max. Job count:")]
                , div[ class "jobs-list-max-job-count-input"] [ FormInput.number [ FormInput.value  <| toString <| model.maxEntries,
                                          FormInput.onInput UpdateJobsNumber,
                                          FormInput.attrs [style [ ( "margin-left", "2px" ), ( "width", "85px" ) ] ] ] ]
                , div[class "jobs-list-max-job-count-button"] [ Button.button [ Button.primary, Button.onClick ApplyJobsNumberAndRetrieveJobs ] [ text "Apply" ] ]
              ]
            ],
    div [ class "container-fluid" ] <|
           [
            JobsTable.viewTable model.jobsTableModel model.currTime |> Html.map JobsTableMsg
           ]
       ]


----


getJobsCmd : Int -> Cmd Msg
getJobsCmd limit =
    Http.send GetJobsCompleted (getJobs limit)


getJobs : Int -> Http.Request (List Job)
getJobs limit =
    Http.get ("/api/newman/jobs-view?limit=" ++ toString limit ++ "&orderBy=-submitTime") <|
        Json.Decode.field "values" (Json.Decode.list decodeJobView)



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


onGetJobsCompleted : Model -> Result Http.Error (List Job) -> ( Model, Cmd Msg )
onGetJobsCompleted model result =
    case result of
        Ok jobs ->
            ( { model
                | jobsTableModel = JobsTable.init jobs
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