module Pages.TestHistory exposing (..)

import Html exposing (Html, div, h1, h2, h4, text)
import Html.Attributes exposing (class)
import Http
import UrlParser exposing (Parser)
import Utils.Types exposing (..)
import Views.TestHistoryTable as TestHistoryTable exposing (..)


type alias Model =
    { table : TestHistoryTable.Model
    , arguments : List String
    }


type Msg
    = GetTestHistoryCompleted (Result Http.Error TestHistoryItems)
    | TestHistoryTableMsg TestHistoryTable.Msg


parseBuildId : Parser (String -> a) a
parseBuildId =
    UrlParser.string


initModel : Model
initModel =
    { table = TestHistoryTable.init []
    , arguments = []
    }


initCmd : BuildId -> Cmd Msg
initCmd buildId =
    getTestHistory buildId


view : Model -> Html Msg
view model =
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Test History" ]
        , h4 [] [ text <| "Arguments: " ++ String.join " " model.arguments ]
        , TestHistoryTable.viewTable model.table |> Html.map TestHistoryTableMsg
        ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetTestHistoryCompleted result ->
            case result of
                Ok data ->
                    let
                        arguments =
                            case List.head data of
                                Just head ->
                                    head.test.arguments

                                Nothing ->
                                    []
                    in
                    ( { model | arguments = arguments, table = TestHistoryTable.init data }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        TestHistoryTableMsg subMsg ->
            let
                ( updatedTableModel, cmd ) =
                    TestHistoryTable.update subMsg model.table
            in
            ( { model | table = updatedTableModel }, cmd |> Cmd.map TestHistoryTableMsg )


getTestHistory : TestId -> Cmd Msg
getTestHistory testId =
    Http.send GetTestHistoryCompleted <|
        Http.get ("/api/newman/test-history?id=" ++ testId) decodeTestHistoryItems
