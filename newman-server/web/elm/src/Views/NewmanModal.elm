module Views.NewmanModal exposing (..)

import Bootstrap.Button as Button
import Bootstrap.Form as Form
import Bootstrap.Form.Input as FormInput exposing (onInput)
import Bootstrap.Modal as Modal exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Utils.Types exposing (Suite)


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


confirmFutureJobDrop : Maybe String -> (State -> toMsg) -> (String -> toMsg) -> State -> Html toMsg
confirmFutureJobDrop maybeJob toMsg confirmMsg modalState =
    case maybeJob of
        Just jobId ->
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Confirmation is required" ]
                |> Modal.body [] [ p [] [ text <| "Are you sure you want to delete future job " ++ jobId ] ]
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
                |> Modal.h3 [] [ text "Error: Future Job is not defined" ]
                |> Modal.view modalState


confirmSuiteDrop : Maybe Suite -> (State -> toMsg) -> (String -> toMsg) -> State -> Html toMsg
confirmSuiteDrop maybeSuite toMsg confirmMsg modalState =
    case maybeSuite of
        Just suite ->
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Confirmation is required" ]
                |> Modal.body [] [ p [] [ text <| "Are you sure you want to delete suite " ++ suite.name ++ "?"  ] ]
                |> Modal.footer []
                    [ Button.button
                        [ Button.danger
                        , Button.onClick <| confirmMsg suite.id
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
                |> Modal.h3 [] [ text "Error: Suite is not defined" ]
                |> Modal.view modalState


confirmAgentDrop : String -> (State -> toMsg) -> (String -> toMsg) -> State -> Html toMsg
confirmAgentDrop maybeAgent toMsg confirmMsg modalState =
    case maybeAgent of
        "agentNotFound" ->
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Error: Agent is not defined" ]
                |> Modal.view modalState

        agentId ->
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Confirmation is required" ]
                |> Modal.body [] [ p [] [ text <| "Are you sure you want to delete agent " ++ agentId ] ]
                |> Modal.footer []
                    [ Button.button
                        [ Button.danger
                        , Button.onClick <| confirmMsg agentId
                        ]
                        [ text "Confirm" ]
                    , Button.button
                        [ Button.outlinePrimary
                        , Button.onClick <| toMsg Modal.hiddenState
                        ]
                        [ text "Close" ]
                    ]
                |> Modal.view modalState


viewError : String -> (State -> toMsg) -> State -> Html toMsg
viewError txt toMsg modalState =
    Modal.config toMsg
        |> Modal.small
        |> Modal.h3 [] [ text "Error occurred!" ]
        |> Modal.body [] [ p [] [ text txt ] ]
        |> Modal.footer []
            [ Button.button
                [ Button.outlinePrimary
                , Button.onClick <| toMsg Modal.hiddenState
                ]
                [ text "Close" ]
            ]
        |> Modal.view modalState


createSuiteForFailedTestsModal : Maybe String -> Maybe (Result String String) -> (State -> toMsg) -> (String -> toMsg) -> (String -> toMsg) -> State -> Html toMsg
createSuiteForFailedTestsModal maybeSuiteName maybeMessage toMsg onInput onConfirm modalState =
    case maybeSuiteName of
        Just suiteName ->
            let
                ( createButtonDisabled, message ) =
                    case maybeMessage of
                        Just msg ->
                            case msg of
                                Ok m ->
                                    ( False, m )

                                Err m ->
                                    ( True, m )

                        Nothing ->
                            ( False, "" )
            in
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text "Create Suite" ]
                |> Modal.body []
                    [ p [] [ text <| "This will create a suite of all failing tests in this job" ]
                    , Form.group []
                        [ Form.label [ for "suiteName" ] [ text "Suite Name:" ]
                        , FormInput.text [ FormInput.onInput onInput, FormInput.id "suiteName", FormInput.defaultValue suiteName, FormInput.disabled createButtonDisabled ]
                        , Form.help [] [ text "Make sure it starts with 'dev-' prefix." ]
                        ]
                    ]
                |> Modal.footer []
                    [ div [ style [ ( "width", "100%" ) ] ] [ text message ]
                    , Button.button
                        [ Button.success
                        , Button.onClick <| onConfirm suiteName
                        , Button.disabled createButtonDisabled
                        ]
                        [ text "Create" ]
                    , Button.button
                        [ Button.outlinePrimary
                        , Button.disabled createButtonDisabled
                        , Button.onClick <| toMsg Modal.hiddenState
                        ]
                        [ text "Close" ]
                    ]
                |> Modal.view modalState

        Nothing ->
            viewError "Suite name was not passed" toMsg modalState



cloneSuiteModal : Maybe Suite -> (State -> toMsg) -> (String -> toMsg) -> (Suite -> String -> toMsg) -> State -> Html toMsg
cloneSuiteModal maybeSuite toMsg onInput confirmMsg modalState =
    case maybeSuite of
        Just suite ->
{-            let
                ( createButtonDisabled, message ) =
                    case maybeMessage of
                        Just msg ->
                            case msg of
                                Ok m ->
                                    ( False, m )

                                Err m ->
                                    ( True, m )

                        Nothing ->
                            ( False, "" )
            in-}
          {-  let
                message =


            in-}
            Modal.config toMsg
                |> Modal.large
                |> Modal.h3 [] [ text <| "Copy suite [" ++ suite.name ++ "] configuration" ]
                |> Modal.body []
                    [ p [] [ text <| "This will create a suite of all failing tests in this job" ]
                    , Form.group []
                        [ Form.label [ for "suiteName" ] [ text "Suite Name:" ]
                        , FormInput.text [ FormInput.onInput onInput, FormInput.id "suiteName" , FormInput.defaultValue ("copy of " ++ suite.name) ]
                        ]
                    ]
                |> Modal.footer []
                    [ Button.button
                        [ Button.outlinePrimary
                        , Button.onClick <| confirmMsg suite "suiteName"
                        ]
                        [ text "Create" ]
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
                |> Modal.h3 [] [ text "Error: Suite is not defined" ]
                |> Modal.view modalState