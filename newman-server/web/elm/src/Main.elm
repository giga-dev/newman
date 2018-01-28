import Html exposing (..)
import Html.Attributes exposing (..)


main = Html.beginnerProgram
        { model = init
        , view = view
        , update = update
        }

init = {}
update model = model
view model =
    div [ id "wrapper" ]
        [ nav [ class "navbar navbar-inverse navbar-fixed-top", attribute "role" "navigation" ]
            [ div [ class "navbar-header" ]
                [ button [ class "navbar-toggle", attribute "data-target" ".navbar-ex1-collapse", attribute "data-toggle" "collapse", type_ "button" ]
                    [ span [ class "icon-bar" ]
                        []
                    , span [ class "icon-bar" ]
                        []
                    , span [ class "icon-bar" ]
                        []
                    ]
                , a [ class "navbar-brand", href "index" ]
                    [ text "Newman" ]
                ]
            , text "        "
            , div [ class "collapse navbar-collapse navbar-ex1-collapse" ]
                [ ul [ class "nav navbar-nav side-nav" ]
                    [ li [ class "active" ]
                        [ a [ href "first" ]
                            [ text "First Page" ]
                        ]
                    , li [ class "" ]
                        [ a [ href "second" ]
                            [ text "Second Page" ]
                        ]
                    , li [ class "" ]
                        [ a [ href "admin" ]
                            [ text "admin                    " ]
                        ]
                    ]
                ]
            , text "    "
            ]
        , div [ id "page-wrapper" ]
            [ div [ class "container-fluid" ]
                [ div [ class "row" ]
                    [ div [ class "col-lg-12" ]
                        [ h1 [ class "page-header" ]
                            [ text "Header here                    " ]
                        ]
                    ]
                , text "        "
                ]
            , text "    "
            ]
        , text ""
        ]