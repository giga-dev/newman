module Pages.Suite exposing (..)

import Html exposing (..)
import Html.Attributes exposing (..)
import Http exposing (..)
import UrlParser exposing (Parser)
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)


type alias Model =
    { maybeSuite : Maybe SuiteWithCriteria
    }


type Msg
    = GetSuiteInfoCompleted (Result Http.Error SuiteWithCriteria)
    | WebSocketEvent WebSocket.Event


parseSuiteId : Parser (String -> a) a
parseSuiteId =
    UrlParser.string


init : SuiteId -> ( Model, Cmd Msg )
init suiteId =
    ( Model Nothing, getSuiteInfoCmd suiteId )


viewSuite : SuiteWithCriteria -> Html Msg
viewSuite suite =
    let
        viewRow ( name, value ) =
            tr [ height 50 ]
                [ td [ width 150 ] [ text name ]
                , td [] [ value ]
                ]

        viewCriteria =
            tr []
                [ td []
                    [ text "Criteria" ]
                , td []
                    [ textarea [ readonly True, style [ ( "margin", "0px" ), ("overflow","auto"), ("resize", "both")] ] [ text suite.criteria ]
                    ]
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Suite" ]
        , table []
            [ viewRow ( "Name", text suite.name )
            , viewRow ( "Id", text suite.id )
            , viewRow ( "Custome Variables", text suite.customVariables )
            , viewCriteria
            , viewRow ( "Requirements", text <| String.join "," suite.requirements )
            ]
        ]


view : Model -> Html Msg
view model =
    case model.maybeSuite of
        Just suite ->
            div []
                [ viewSuite suite
                ]

        Nothing ->
            div []
                [ text "Loading suite..."
                ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetSuiteInfoCompleted result ->
            case result of
                Ok data ->
                    ( { model | maybeSuite = Just data }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        WebSocketEvent event ->
                    case event of
                        ModifiedSuite suite ->
                            case model.maybeSuite of
                                Just modelSuite ->
                                    if modelSuite.id == suite.id then
                                        ( { model | maybeSuite = updateSuite suite modelSuite } , Cmd.none )
                                    else
                                        ( model, Cmd.none )
                                Nothing ->
                                    ( model, Cmd.none )

                        _ ->
                            ( model, Cmd.none )


getSuiteInfoCmd : SuiteId -> Cmd Msg
getSuiteInfoCmd suiteId =
    Http.send GetSuiteInfoCompleted <|
        Http.get ("/api/newman/suite/" ++ suiteId) decodeSuiteWithCriteria


updateSuite : Suite -> SuiteWithCriteria -> Maybe SuiteWithCriteria
updateSuite suite modelSuite =
        Just { modelSuite | customVariables = suite.customVariables }


handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent