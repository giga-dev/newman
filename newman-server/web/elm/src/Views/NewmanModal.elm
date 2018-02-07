module Views.NewmanModal exposing (..)

import Bootstrap.Button as Button
import Bootstrap.Modal as Modal exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)


confirmJobDrop : Maybe String -> (State -> toMsg) -> (String -> toMsg) -> State -> Html toMsg
confirmJobDrop maybeJob toMsg confirmMsg modalState =
    case maybeJob of
        Just jobId ->
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Confirmation is required" ]
                |> Modal.body [] [ p [] [ text <| "Are you sure you want to delete job " ++ jobId ] ]
                |> Modal.footer []
                    [ Button.button
                        [ Button.danger
                        , Button.onClick <| confirmMsg jobId
                        ]
                        [ text "Confirm" ]
                    , Button.button
                        [ Button.outlinePrimary
                        , Button.onClick <| toMsg Modal.hiddenState
                        ]
                        [ text "Close" ]
                    ]
                |> Modal.view modalState

        Nothing ->
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Error: Job is not defined" ]
                |> Modal.view modalState