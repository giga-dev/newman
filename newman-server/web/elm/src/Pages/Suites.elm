module Pages.Suites exposing (..)

import Bootstrap.Button as Button
import Bootstrap.Form.Input as FormInput
import Bootstrap.Modal as Modal
import Date exposing (Date)
import DateFormat
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Http
import List.Extra as ListExtra
import Paginate exposing (..)
import Svg.Attributes exposing (result)
import Time exposing (Time)
import Utils.Common as Common
import Utils.Types exposing (..)
import Utils.WebSocket as WebSocket exposing (..)
import Views.NewmanModal as NewmanModal


type alias Model =
    { allSuites : Suites
    , suites : PaginatedSuites
    , pageSize : Int
    , query : String
    , suiteToDrop : Maybe String
    , confirmationState : Modal.State
    }


type Msg
    = GetSuitesCompleted (Result Http.Error Suites)
    | First
    | Last
    | Next
    | Prev
    | GoTo Int
    | FilterQuery String
    | WebSocketEvent WebSocket.Event
    | OnClickDropSuite String
    | NewmanModalMsg Modal.State
    | OnSuiteDropConfirmed String
    | RequestCompletedDropSuite  String (Result Http.Error String)



init : ( Model, Cmd Msg )
init =
    let
        pageSize =
            20
    in
    ( { allSuites = []
      , suites = Paginate.fromList pageSize []
      , pageSize = pageSize
      , query = ""
      , suiteToDrop = Nothing
      , confirmationState = Modal.hiddenState
      }
    , getSuitesCmd
    )


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GetSuitesCompleted result ->
            case result of
                Ok suitesFromResult ->
                    ( { model | suites = Paginate.fromList model.pageSize suitesFromResult, allSuites = suitesFromResult }, Cmd.none )

                Err err ->
                    ( model, Cmd.none )

        First ->
            ( { model | suites = Paginate.first model.suites }, Cmd.none )

        Last ->
            ( { model | suites = Paginate.last model.suites }, Cmd.none )

        Next ->
            ( { model | suites = Paginate.next model.suites }, Cmd.none )

        Prev ->
            ( { model | suites = Paginate.prev model.suites }, Cmd.none )

        GoTo i ->
            ( { model | suites = Paginate.goTo i model.suites }, Cmd.none )

        FilterQuery query ->
            let
                filteredList =
                    List.filter (filterQuery query) model.allSuites
            in
            ( { model | query = query, suites = Paginate.fromList model.pageSize filteredList }
            , Cmd.none
            )

        NewmanModalMsg newState ->
             ( { model | suiteToDrop = Nothing, confirmationState = newState }, Cmd.none )

        OnClickDropSuite suite ->
            ({model | confirmationState = Modal.visibleState, suiteToDrop = Just suite}, Cmd.none )

        OnSuiteDropConfirmed suiteId ->
            ({model | confirmationState = Modal.hiddenState, suiteToDrop = Nothing }, dropSuiteCmd suiteId ) {-Todo -delete-}

        RequestCompletedDropSuite suiteId result ->
            case result of
                Ok suiteId ->
                    ( model, Cmd.none ) {-Todo- change Model???????????-}

                Err err ->
                    let
                        e =
                            Debug.log "ERROR:onRequestCompletedDropSuite" err
                    in
                    ( model, Cmd.none )
        WebSocketEvent event ->
            case event of
                CreatedSuite suite ->
                    ( updateSuiteAdded model suite, Cmd.none )

                ModifiedSuite suite ->
                    ( updateSuiteUpdated model suite, Cmd.none )

                DeletedSuite suite ->
                    ( updateSuiteRemoved model suite, Cmd.none )
                _ ->
                    ( model, Cmd.none )


updateAll : (List Suite -> List Suite) -> Model -> Model
updateAll f model =
    let
        newList =
            f model.allSuites

        filtered =
            List.filter (filterQuery model.query) newList

        newPaginated =
            Paginate.map (\_ -> filtered) model.suites
    in
    { model | suites = newPaginated, allSuites = newList }


updateSuiteAdded : Model -> Suite -> Model
updateSuiteAdded model addedSuite =
    updateAll (\list -> addedSuite :: list) model


updateSuiteRemoved : Model -> Suite -> Model
updateSuiteRemoved model suite =
    let
        f =
            ListExtra.filterNot (\item -> item.id == suite.id)
    in
    updateAll f model


updateSuiteUpdated : Model -> Suite -> Model
updateSuiteUpdated model suiteToUpdate =
    let
        f l =
            case ListExtra.find (\item -> item.id == suiteToUpdate.id) l of
                Just _ ->
                    ListExtra.replaceIf (\item -> item.id == suiteToUpdate.id) suiteToUpdate l

                Nothing ->
                    suiteToUpdate :: l
    in
    updateAll f model


view : Model -> Html Msg
view model =
    let
        prevButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.suites ) ], onClick First ]
                [ button [ class "page-link" ] [ text "«" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isFirst model.suites ) ], onClick Prev ]
                [ button [ class "page-link" ] [ text "‹" ]
                ]
            ]

        nextButtons =
            [ li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.suites ) ], onClick Next ]
                [ button [ class "page-link" ] [ text "›" ]
                ]
            , li [ class "page-item", classList [ ( "disabled", Paginate.isLast model.suites ) ], onClick Last ]
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

        pagination =
            nav []
                [ ul [ class "pagination " ]
                    (prevButtons
                        ++ Paginate.pager pagerButtonView model.suites
                        ++ nextButtons
                    )
                ]
    in
    div [ class "container-fluid" ] <|
        [ h2 [ class "text" ] [ text "Suites" ]
        , div []
            [ div [ class "form-inline" ]
                [ div [ class "form-group" ]
                    [ FormInput.text
                        [ FormInput.onInput FilterQuery
                        , FormInput.placeholder "Filter"
                        , FormInput.value model.query
                        ]
                    ]
                , div [ class "form-group" ] [ pagination ]
                ]
            , table [ class "table table-sm table-bordered table-striped table-nowrap table-hover" ]
                [ thead []
                    [ tr []
                        [ th [] [ text "Name" ]
                        , th [] [ text "Id" ]
                        , th [] [ text "Custom" ]
                        , th [ width 70 ] [ text "Actions"]
                        ]
                    ]
                , tbody [] (List.map viewSuite (Paginate.page model.suites))
                ]
            , pagination
            , NewmanModal.confirmSuiteDrop model.suiteToDrop NewmanModalMsg OnSuiteDropConfirmed model.confirmationState
            ]
        ]


viewSuite : Suite -> Html Msg
viewSuite suite =
    tr []
        [ a [ href <| "#suite/" ++ suite.id ] [ text suite.name ]
        , td [] [ text suite.id ]
        , td [] [ text suite.customVariables ]
        , td []
            [ Button.button [ Button.danger, Button.small, Button.disabled <| validSuite suite.name, Button.onClick <| OnClickDropSuite suite.id ]
                [ span [ class "ion-close" ] [] ]
            ]
        ]

validSuite : String -> Bool
validSuite suiteName =
    if
       String.startsWith "dev-" suiteName
    then
        False
    else
        True


getSuitesCmd : Cmd Msg
getSuitesCmd =
    Http.send GetSuitesCompleted <| Http.get "/api/newman/suite?all=true" decodeSuites


filterQuery : String -> Suite -> Bool
filterQuery query suite =
    if
        String.length query
            == 0
            || String.contains query suite.name
            || String.startsWith query suite.id
    then
        True

    else
        False

dropSuiteCmd : String -> Cmd Msg
dropSuiteCmd suiteId =
    Http.send (RequestCompletedDropSuite suiteId) <|
        Http.request <|
            { method = "DELETE"
            , headers = []
            , url = "/api/newman/suite/" ++ suiteId
            , body = Http.emptyBody
            , expect = Http.expectString
            , timeout = Nothing
            , withCredentials = False
            }

handleEvent : WebSocket.Event -> Cmd Msg
handleEvent event =
    event => WebSocketEvent
