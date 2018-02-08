module Views.CompareBuilds exposing (..)

import Dict exposing (Dict)
import Html exposing (..)
import Html.Attributes exposing (action, class, id, list, method, name, style, type_, value)
import Http
import Json.Decode as JD
import SelectTwo exposing (..)
import SelectTwo.Html exposing (..)
import SelectTwo.Types exposing (..)
import Utils.Types exposing (..)


type alias Model =
    { selectTwo : Maybe (SelectTwo Msg)
    , test : Maybe String
    , test4 : Maybe { id : Int, name : String }
    }


init : ( Model, Cmd Msg )
init =
    { selectTwo = Nothing
    , test = Nothing
    , test4 = Nothing
    }
        ! []


type Msg
    = Test (Maybe String)
    | Test4 (Maybe { id : Int, name : String })
    | SelectTwo (SelectTwoMsg Msg)
    | Test4Ajax AjaxParams Bool
    | Test4Res AjaxParams (Result Http.Error String)


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        a =
            Debug.log "compare builds update" msg
    in
    case msg of
        SelectTwo stmsg ->
            let
                ajaxCases =
                    Just
                        (\id_ params reset ->
                            case id_ of
                                "test-4" ->
                                    model ! [ SelectTwo.send <| Test4Ajax params reset ]

                                _ ->
                                    model ! []
                        )
            in
            SelectTwo.update SelectTwo stmsg ajaxCases model

        Test s ->
            { model | test = s } ! []

        Test4 s ->
            let
                a =
                    Debug.log "compare builds update" msg
            in
            { model | test4 = s } ! []

        Test4Ajax params reset ->
            let
                url =
                    "//api.github.com/search/repositories"

                buildUrl =
                    let
                        term =
                            if params.term == "" then
                                "test"
                            else
                                params.term
                    in
                    url ++ "?q=" ++ term ++ "&page=" ++ toString params.page
            in
            SelectTwo.setLoading params reset model ! [ sendAjax buildUrl (Test4Res params) ]

        Test4Res params (Ok str) ->
            let
                ( list, newParams ) =
                    processResult Test4 str params
            in
            SelectTwo.setList list newParams model ! []

        Test4Res params (Err _) ->
            model ! []


sendAjax : String -> (Result Http.Error String -> Msg) -> Cmd Msg
sendAjax url msg =
    Http.getString url
        |> Http.send msg


main : Program Never Model Msg
main =
    program
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none


view : Model -> Html Msg
view model =
    div
        [ style
            [ ( "width", "100%" )
            , ( "height", "100%" )
            , ( "padding", "30px" )
            , ( "font-size", "16px" )
            ]
        , select2Close SelectTwo
        ]
        [ select2Css
        , h1 [] [ text "Examples of Elm Select2" ]
        , p []
            [ text "Ajax, Single Select"
            , div []
                [ select2 SelectTwo
                    { defaults = [ model.test4 |> Maybe.map (\t -> ( Just (Test4 (Just t)), t.name, True )) |> Maybe.withDefault ( Nothing, "", True ) ]
                    , ajax = True
                    , delay = 300
                    , id_ = "test-4"
                    , parents = []
                    , clearMsg = Just (\_ -> Test4 Nothing)
                    , showSearch = True
                    , width = "300px"
                    , placeholder = "Select Test"
                    , list = []
                    , multiSelect = False
                    , disabled = False
                    , noResultsMessage = Just "YOU GET NOTHING! YOU LOSE GOODDAY SIR!"
                    }
                ]
            ]
        ]


processResult : (Maybe { id : Int, name : String } -> Msg) -> String -> AjaxParams -> ( List (GroupSelectTwoOption Msg), AjaxParams )
processResult msg string params =
    JD.decodeString
        (JD.map2 (,)
            (JD.at [ "items" ] (JD.list itemsDecoder))
            (JD.field "total_count" JD.int)
            |> JD.map
                (\( items, total_count ) ->
                    ( items |> List.map (\i -> ( Just i, i.name )) |> SelectTwo.basicSelectOptions msg
                    , { params | more = params.page * 30 < total_count }
                    )
                )
        )
        string
        |> Result.toMaybe
        |> Maybe.withDefault ( [], params )


type alias Item =
    { id : Int, name : String }


itemsDecoder : JD.Decoder Item
itemsDecoder =
    JD.map2 Item
        (JD.field "id" JD.int)
        (JD.field "name" JD.string)


testList : (Maybe String -> Msg) -> List ( String, List (SelectTwoOption Msg) )
testList msg =
    [ ( Just "a", "Harry Potter" )
    , ( Just "b", "Ender's Game" )
    , ( Just "c", "Dune" )
    , ( Just "d", "Foundation" )
    , ( Just "e", "Jurassic Park" )
    ]
        |> SelectTwo.basicSelectOptions msg
