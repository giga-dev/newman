module Pages.JobConfig exposing (..)

import Bootstrap.Button as Button exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Http exposing (..)
import Json.Decode
import Json.Decode.Pipeline exposing (decode)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)


type alias Model =
    { maybeJobConfig : Maybe JobConfig
    }


type Msg
    = GetJobConfigInfoCompleted (Result Http.Error JobConfig)



-- external


parseJobConfigId : Parser (String -> a) a
parseJobConfigId =
    UrlParser.string


init : JobConfigId -> ( Model, Cmd Msg )
init jobConfigId =
    ( Model Nothing, getJobConfigInfoCmd jobConfigId )


viewJobConfig : JobConfig -> Html Msg
viewJobConfig jobConfig =
    let
        viewRow ( name, value ) =
            tr [ height 50 ]
                [ td [ width 150 ] [ text name ]
                , td [] [ value ]
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Job Configuration" ]
        , table []
            [ viewRow ( "Name", text jobConfig.name )
            , viewRow ( "Id", text jobConfig.id )
            , viewRow ( "Java Version", text jobConfig.javaVersion )
            ]
        ]


view : Model -> Html Msg
view model =
    case model.maybeJobConfig of
        Just jobConfig ->
            div []
                [ viewJobConfig jobConfig
                ]

        Nothing ->
            div []
                [ text "Loading jobConfig..."
                ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        d =
            Debug.log "JobConfig.update" ("was called" ++ toString msg)
    in
    case msg of
        GetJobConfigInfoCompleted result ->
            case result of
                Ok data ->
                    ( { model | maybeJobConfig = Just data }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )


getJobConfigInfoCmd : JobConfigId -> Cmd Msg
getJobConfigInfoCmd jobConfigId =
    Http.send GetJobConfigInfoCompleted <|
        Http.get ("/api/newman/job-config-by-id/" ++ jobConfigId) decodeJobConfig
