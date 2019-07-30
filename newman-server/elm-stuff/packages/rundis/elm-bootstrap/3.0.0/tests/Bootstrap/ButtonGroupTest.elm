module Bootstrap.ButtonGroupTest exposing (..)

import Bootstrap.ButtonGroup as ButtonGroup
import Bootstrap.Button as Button
import Html
import Test exposing (Test, test, describe)
import Test.Html.Query as Query
import Test.Html.Selector exposing (text, tag, class, classes, attribute, checked)


simpleGroup : Test
simpleGroup =
    let
        html =
            ButtonGroup.buttonGroup []
                [ ButtonGroup.button [] [ Html.text "First" ]
                , ButtonGroup.button [] [ Html.text "Second" ]
                ]
    in
        describe "Simple group"
            [ test "expect btn-group" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.has [ tag "div", class "btn-group", attribute "role" "group" ]
            , test "expect btn class" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.findAll [ tag "button" ]
                        |> Query.each
                            (Query.has [ class "btn" ])
            ]


groupWithOptions : Test
groupWithOptions =
    let
        html =
            ButtonGroup.buttonGroup [ ButtonGroup.small, ButtonGroup.vertical ]
                [ ButtonGroup.button [ Button.primary ] [ Html.text "First" ] ]
    in
        describe "Optioned group"
            [ test "expect classes" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.has
                            [ classes
                                [ "btn-group"
                                , "btn-group-sm"
                                , "btn-group-vertical"
                                ]
                            ]
            , test "Expect button classes" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.first
                        |> Query.has
                            [ class "btn-primary"
                            , text "First"
                            ]
            ]


linkGroup : Test
linkGroup =
    let
        html =
            ButtonGroup.linkButtonGroup []
                [ ButtonGroup.linkButton [ Button.primary ] [ Html.text "First" ] ]
    in
        describe "Link group"
            [ test "Expect button classes" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.first
                        |> Query.has
                            [ tag "a"
                            , class "btn-primary"
                            , text "First"
                            ]
            ]


checkGroup : Test
checkGroup =
    let
        html =
            ButtonGroup.checkboxButtonGroup []
                [ ButtonGroup.checkboxButton False [ Button.primary ] [ Html.text "First" ] ]
    in
        describe "Checkbox group"
            [ test "Except label with classes" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.first
                        |> Query.has
                            [ tag "label"
                            , class "btn-primary"
                            , text "First"
                            ]
            , test "Expect checkbox input" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.index 0
                        |> Query.children []
                        |> Query.index 0
                        |> Query.has
                            [ tag "input"
                            , attribute "type" "checkbox"
                            , checked False
                            ]
            ]


radioGroup : Test
radioGroup =
    let
        html =
            ButtonGroup.radioButtonGroup []
                [ ButtonGroup.radioButton False [ Button.primary ] [ Html.text "First" ] ]
    in
        describe "Radio group"
            [ test "Except label with classes" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.first
                        |> Query.has
                            [ tag "label"
                            , class "btn-primary"
                            , text "First"
                            ]
            , test "Expect radio input" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.index 0
                        |> Query.children []
                        |> Query.index 0
                        |> Query.has
                            [ tag "input"
                            , attribute "type" "radio"
                            , checked False
                            ]
            ]


toolbar : Test
toolbar =
    let
        html =
            ButtonGroup.toolbar []
                [ ButtonGroup.buttonGroupItem []
                    [ ButtonGroup.button [] [ Html.text "Button" ] ]
                ]
    in
        describe "Toolbar"
            [ test "expect toolbar" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.has
                            [ tag "div"
                            , class "btn-toolbar"
                            ]
            , test "Expect button group" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.index 0
                        |> Query.has
                            [ tag "div"
                            , class "btn-group"
                            ]
            , test "Expect button" <|
                \() ->
                    html
                        |> Query.fromHtml
                        |> Query.children []
                        |> Query.index 0
                        |> Query.children []
                        |> Query.index 0
                        |> Query.has
                            [ tag "button"
                            , class "btn"
                            ]
            ]
