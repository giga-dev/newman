module Main exposing (..)

import Bootstrap.Text as Text
import Bootstrap.Button as Button
import Bootstrap.CDN as CDN
import Bootstrap.Dropdown as Dropdown
import Bootstrap.Grid as Grid
import Bootstrap.Modal as Modal
import Bootstrap.Navbar as Navbar
import Bootstrap.Tab as Tab
import Bootstrap.Accordion as Accordion
import Bootstrap.ListGroup as ListGroup
import Bootstrap.Badge as Badge
import Bootstrap.Form as Form
import Bootstrap.Form.Checkbox as Chk
import Bootstrap.Form.Radio as Radio
import Bootstrap.Form.Input as Input
import Bootstrap.Form.InputGroup as InputGrp
import Bootstrap.Form.Select as Select
import Bootstrap.Form.Fieldset as Fieldset
import Bootstrap.Card as Card
import Bootstrap.Table as Table
import Bootstrap.Progress as Progress
import Bootstrap.Popover as Popover
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Color
import Bootstrap.Grid.Col as Col
import Bootstrap.Grid.Row as Row


main : Program Never Model Msg
main =
    Html.program
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }


type alias Model =
    { dummy : String
    , dropdownState : Dropdown.State
    , splitDropState : Dropdown.State
    , modalState : Modal.State
    , tabState : Tab.State
    , accordionState : Accordion.State
    , navbarState : Navbar.State
    , navMsgCounter : Int
    , popoverStateLeft : Popover.State
    , popoverStateRight : Popover.State
    , popoverStateTop : Popover.State
    , popoverStateBottom : Popover.State
    }


init : ( Model, Cmd Msg )
init =
    let
        ( navbarState, navbarCmd ) =
            Navbar.initialState NavbarMsg
    in
        ( { dummy = "init"
          , dropdownState = Dropdown.initialState
          , splitDropState = Dropdown.initialState
          , modalState = Modal.hiddenState
          , tabState = Tab.initialState
          , accordionState = Accordion.initialState
          , navbarState = navbarState
          , navMsgCounter = 0
          , popoverStateLeft = Popover.initialState
          , popoverStateRight = Popover.initialState
          , popoverStateBottom = Popover.initialState
          , popoverStateTop = Popover.initialState
          }
        , navbarCmd
        )


type Msg
    = NoOp
    | DropdownMsg Dropdown.State
    | SplitMsg Dropdown.State
    | Item1Msg
    | Item2Msg
    | SplitMainMsg
    | SplitItem1Msg
    | SplitItem2Msg
    | ModalMsg Modal.State
    | TabMsg Tab.State
    | AccordionMsg Accordion.State
    | NavbarMsg Navbar.State
    | TogglePopoverLeftMsg Popover.State
    | TogglePopoverRightMsg Popover.State
    | TogglePopoverBottomMsg Popover.State
    | TogglePopoverTopMsg Popover.State


update : Msg -> Model -> ( Model, Cmd msg )
update msg ({ accordionState } as model) =
    case msg of
        NoOp ->
            ( { model | dummy = "NoOp" }, Cmd.none )

        Item1Msg ->
            ( { model | dummy = "item1" }, Cmd.none )

        Item2Msg ->
            ( { model | dummy = "item2" }, Cmd.none )

        DropdownMsg state ->
            ( { model | dropdownState = state }
            , Cmd.none
            )

        SplitMainMsg ->
            ( { model | dummy = "splitmain" }, Cmd.none )

        SplitItem1Msg ->
            ( { model | dummy = "splititem1" }, Cmd.none )

        SplitItem2Msg ->
            ( { model | dummy = "splititem2" }, Cmd.none )

        SplitMsg state ->
            ( { model | splitDropState = state }
            , Cmd.none
            )

        ModalMsg state ->
            ( { model | modalState = state }
            , Cmd.none
            )

        TabMsg state ->
            ( { model | tabState = state }
            , Cmd.none
            )

        AccordionMsg state ->
            ( { model | accordionState = state }
            , Cmd.none
            )

        NavbarMsg state ->
            ( { model | navbarState = state, navMsgCounter = model.navMsgCounter + 1 }
            , Cmd.none
            )

        TogglePopoverLeftMsg state ->
            ( { model | popoverStateLeft = state }
            , Cmd.none
            )

        TogglePopoverRightMsg state ->
            ( { model | popoverStateRight = state }
            , Cmd.none
            )

        TogglePopoverBottomMsg state ->
            ( { model | popoverStateBottom = state }
            , Cmd.none
            )

        TogglePopoverTopMsg state ->
            ( { model | popoverStateTop = state }
            , Cmd.none
            )


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Accordion.subscriptions model.accordionState AccordionMsg
        , Dropdown.subscriptions model.dropdownState DropdownMsg
        , Dropdown.subscriptions model.splitDropState SplitMsg
        , Tab.subscriptions model.tabState TabMsg
        , Navbar.subscriptions model.navbarState NavbarMsg
        ]


view : Model -> Html Msg
view model =
    Grid.container []
        [ CDN.stylesheet
        , CDN.fontAwesome
        , mainContent model
        ]


popoverContent : Popover.Config Msg -> Popover.Config Msg
popoverContent config =
    Popover.titleH4 [] [ text "Popover title" ] config
        |> Popover.content []
            [ text "Some concent to display. There is even more."
            , p [] [ text "There is even a paragraph here too." ]
            , h6 [] [ text "A header even" ]
            ]


popoverButton : Popover.State -> (Popover.State -> msg) -> Html msg
popoverButton state msg =
    Button.button
        [ Button.attrs <|
            Popover.onHover state msg
        ]
        [ text "Toggle tooltip" ]


mainContent : Model -> Html Msg
mainContent model =
    div [ style [ ( "margin-top", "60px" ) ] ]
        [ navbar model
        , simpleForm
        , gridForm
        , div []
            [ span [] [ text "Tooltip buttons: " ]
            , Popover.config
                (popoverButton model.popoverStateTop TogglePopoverTopMsg)
                |> Popover.top
                |> popoverContent
                |> Popover.view model.popoverStateTop
            , Popover.config
                (popoverButton model.popoverStateBottom TogglePopoverBottomMsg)
                |> Popover.bottom
                |> popoverContent
                |> Popover.view model.popoverStateBottom
            , Button.button []
                [ Popover.config
                    (span (class "fa fa-car" :: Popover.onHover model.popoverStateLeft TogglePopoverLeftMsg) [])
                    |> Popover.left
                    |> popoverContent
                    |> Popover.view model.popoverStateLeft
                , text " Icon hover"
                ]
            , Popover.config
                (div (Popover.onHover model.popoverStateRight TogglePopoverRightMsg) [ text "A Div" ])
                |> Popover.right
                |> popoverContent
                |> Popover.view model.popoverStateRight
            ]
        , Grid.row
            [ Row.bottomXs, Row.attrs [ rowStyle ] ]
            [ Grid.col
                [ Col.xs2
                , Col.attrs [ colStyle ]
                ]
                [ span [ class "fa fa-car" ] []
                , text " Col 1 Row 1"
                , div [ class "form-inline" ]
                    [ Chk.checkbox [ Chk.inline ] "Chk"
                    , Chk.checkbox [ Chk.inline, Chk.disabled True ] "Chk"
                    , Chk.checkbox [] "Stacked"
                    ]
                ]
            , Grid.col
                [ Col.topXs
                , Col.attrs [ colStyle ]
                ]
                []
            , Grid.col
                [ Col.xs5
                , Col.middleXs
                , Col.attrs [ colStyle ]
                ]
                [ text "Col 3 Row 1"
                , Fieldset.config
                    |> Fieldset.asGroup
                    |> Fieldset.disabled True
                    |> Fieldset.legend [] [ text "My radios" ]
                    |> Fieldset.children
                        (Radio.radioList "myradiogroup"
                            [ Radio.create [] "Radio 1"
                            , Radio.create [] "Radio 2"
                            ]
                        )
                    |> Fieldset.view
                ]
            , Grid.col
                [ Col.attrs [ colStyle ] ]
                [ text "Col 4 Row 1" ]
            ]
        , Grid.row
            [ Row.middleXs, Row.attrs [ rowStyle ] ]
            [ Grid.col
                [ Col.xs5 ]
                [ Button.linkButton
                    [ Button.small
                    , Button.outlineSuccess
                    , Button.block
                    , Button.attrs [ onClick <| ModalMsg Modal.visibleState ]
                    ]
                    [ text "Show modal" ]
                ]
            ]
        , Grid.row
            [ Row.topXs, Row.attrs [ rowStyle ] ]
            [ Grid.col
                [ Col.xs5, Col.attrs [ colStyle ] ]
                [ Dropdown.dropdown
                    model.dropdownState
                    { options = [ Dropdown.alignMenuRight ]
                    , toggleMsg = DropdownMsg
                    , toggleButton =
                        Dropdown.toggle
                            [ Button.warning ]
                            [ text "MyDropdown "
                            , span [ class "tag tag-pill tag-info" ] [ text "(2)" ]
                            ]
                    , items =
                        [ Dropdown.anchorItem
                            [ href "#", onClick Item1Msg ]
                            [ text "Item 1" ]
                        , Dropdown.anchorItem
                            [ href "#", onClick Item2Msg ]
                            [ text "Item 2" ]
                        , Dropdown.divider
                        , Dropdown.header [ text "Silly items" ]
                        , Dropdown.anchorItem [ href "#meh", class "disabled" ] [ text "DoNothing1" ]
                        , Dropdown.anchorItem [ href "#" ] [ text "DoNothing2" ]
                        ]
                    }
                ]
            , Grid.col
                [ Col.xs5, Col.attrs [ colStyle ] ]
                [ Dropdown.splitDropdown
                    model.splitDropState
                    { options = [ Dropdown.dropUp, Dropdown.alignMenuRight ]
                    , toggleMsg = SplitMsg
                    , toggleButton =
                        Dropdown.splitToggle
                            { options =
                                [ Button.warning
                                , Button.attrs [ onClick SplitMainMsg ]
                                ]
                            , togglerOptions = [ Button.warning ]
                            , children = [ text "My split drop" ]
                            }
                    , items =
                        [ Dropdown.buttonItem
                            [ onClick SplitItem1Msg ]
                            [ text "SplitItem 1" ]
                        , Dropdown.buttonItem
                            [ onClick SplitItem2Msg ]
                            [ text "SplitItem 2" ]
                        , Dropdown.buttonItem
                            [ onClick SplitItem2Msg, class "disabled", disabled True ]
                            [ text "SplitItem 2" ]
                        ]
                    }
                ]
            , Grid.col
                [ Col.attrs [ colStyle ] ]
                [ text model.dummy ]
            ]
        , accordion model
        , tabs model
        , cards
        , tables
        , listGroup2
        , progressBars
        , modal model.modalState
        ]


navbar : Model -> Html Msg
navbar model =
    Navbar.config NavbarMsg
        |> Navbar.withAnimation
        |> Navbar.container
        |> Navbar.fixTop
        |> Navbar.darkCustom Color.brown
        |> Navbar.collapseMedium
        |> Navbar.brand [ href "#" ] [ text "Logo" ]
        |> Navbar.items
            [ Navbar.itemLink [ href "#" ] [ text "Page" ]
            , Navbar.itemLinkActive [ href "#" ] [ text "Another" ]
            , Navbar.itemLink [ href "#" ] [ text "More" ]
            , Navbar.dropdown
                { id = "navdropdown1"
                , toggle = Navbar.dropdownToggle [] [ text "Navdrop" ]
                , items =
                    [ Navbar.dropdownItem [ href "#meh" ] [ text "Menuitem1" ]
                    , Navbar.dropdownItem [ href "#meh" ] [ text "Menuitem2" ]
                    ]
                }
            ]
        |> Navbar.customItems
            [ Navbar.textItem [] [ text "Some text" ]
            , Navbar.formItem [ class "ml-lg-2" ]
                [ Input.text [ Input.small ]
                , Button.button
                    [ Button.success, Button.small ]
                    [ text "Submit" ]
                ]
            ]
        |> Navbar.view model.navbarState


simpleForm : Html Msg
simpleForm =
    Form.form
        []
        [ h1 [] [ text "Vertical Form" ]
        , Form.group [ Form.groupSuccess ]
            [ Form.label [ for "simpleInput" ] [ text "SimpleInput" ]
            , Input.text [ Input.id "simpleInput", Input.success ]
            , Form.validationText [] [ text "This went well !" ]
            , Form.help [] [ text "Something really helpful" ]
            ]
        , Form.group []
            [ Form.label [ for "simpleselect" ] [ text "Simple select" ]
            , Select.select [ Select.id "simpleselect" ]
                [ Select.item [] [ text "Option 1" ]
                , Select.item [] [ text "Option 2" ]
                ]
            ]
        , Form.group []
            [ Form.label [ for "customselect" ] [ text "Custom select" ]
            , Select.custom [ Select.id "customselect" ]
                [ Select.item [] [ text "Option 1" ]
                , Select.item [] [ text "Option 2" ]
                ]
            ]
        , Chk.checkbox [] "Lonely checker"
        , Form.group []
            [ Chk.custom [ Chk.attrs [ class "col-2" ] ] "Custom checker" ]
        , Form.group []
            [ Form.label [] [ text "A couple of radios" ]
            , div [ class "form-inline" ]
                [ Radio.radio [ Radio.inline, Radio.name "myradios" ] "Radio 1"
                , Radio.radio [ Radio.inline, Radio.name "myradios" ] "Radio 2"
                , Radio.radio [ Radio.inline, Radio.disabled True, Radio.name "myradios" ] "Radio 3"
                ]
            ]
        , Form.group []
            [ Form.label [] [ text "A group of sorts" ]
            , InputGrp.config
                (InputGrp.password [])
                |> InputGrp.small
                |> InputGrp.predecessors
                    [ InputGrp.span [] [ text "@" ] ]
                |> InputGrp.successors
                    [ InputGrp.button [ Button.outlinePrimary ] [ text "Do it!" ]
                    ]
                |> InputGrp.view
            ]
        ]


gridForm : Html Msg
gridForm =
    div []
        [ h1 [] [ text "Horizontal (grid) form" ]
        , Form.form [ class "container" ]
            [ h3 [] [ text "Header in form" ]
            , Form.row [ Form.rowSuccess ]
                [ Form.colLabel [ Col.xs4 ] [ text "Fill in:" ]
                , Form.col [ Col.xs8 ]
                    [ Input.text
                        [ Input.id "rowinput", Input.success ]
                    , Form.validationText [] [ text "This was cool !" ]
                    , Form.help [] [ text "Should be something..." ]
                    ]
                ]
            , Form.row []
                [ Form.colLabelSm [ Col.xs4 ] [ text "Postal" ]
                , Form.col [ Col.xs4 ]
                    [ Input.text [ Input.small ]
                    , Form.help [] [ text "5 digits" ]
                    ]
                , Form.col [ Col.xs4 ]
                    [ Input.text
                        [ Input.small, Input.attrs [ placeholder "Place" ] ]
                    ]
                ]
            , Form.row []
                [ Form.col [ Col.offsetXs4, Col.xs8 ]
                    [ Chk.custom [] "Lonely checker" ]
                ]
            , Form.row [ Form.rowWarning ]
                [ Form.colLabel [ Col.xs4 ] [ text "Row select:" ]
                , Form.col [ Col.xs8 ]
                    [ Select.custom [ Select.id "rowcustomselect" ]
                        [ Select.item [] [ text "Option 1" ]
                        , Select.item [] [ text "Option 2" ]
                        ]
                    , Form.validationText [] [ text "Can't select option 1 (:" ]
                    ]
                ]
            ]
        ]


modal : Modal.State -> Html Msg
modal modalState =
    Modal.config ModalMsg
        |> Modal.h5 [] [ text "Modal header" ]
        |> modalBody
        |> Modal.footer
            []
            [ Button.button
                [ Button.outlinePrimary
                , Button.attrs [ onClick <| ModalMsg Modal.hiddenState ]
                ]
                [ text "Close" ]
            ]
        |> Modal.small
        |> Modal.view modalState


modalBody : Modal.Config msg -> Modal.Config msg
modalBody =
    Modal.body []
        [ Grid.containerFluid []
            [ Grid.simpleRow
                [ Grid.col
                    [ Col.xs6 ]
                    [ text "Col 1" ]
                , Grid.col
                    [ Col.xs6 ]
                    [ text "Col 2" ]
                ]
            ]
        ]


tabs : Model -> Html Msg
tabs model =
    div []
        [ h1 [] [ text "Tabs" ]
        , Tab.config TabMsg
            |> Tab.withAnimation
            |> Tab.center
            |> Tab.pills
            |> Tab.items
                [ Tab.item
                    { id = "tabItem1"
                    , link = Tab.link [] [ text "Tab1" ]
                    , pane = Tab.pane [] [ listGroup ]
                    }
                , Tab.item
                    { id = "tabItem2"
                    , link = Tab.link [] [ text "Tab2" ]
                    , pane = Tab.pane [] [ listGroup2 ]
                    }
                ]
            |> Tab.view model.tabState
        ]


listGroup : Html Msg
listGroup =
    ListGroup.custom
        [ ListGroup.anchor
            [ ListGroup.success
            , ListGroup.attrs [ href "#" ]
            , ListGroup.attrs [ class "justify-content-between" ]
            ]
            [ text "Hello"
            , Badge.pill [] [ text "1" ]
            ]
        , ListGroup.anchor
            [ ListGroup.info
            , ListGroup.attrs [ href "#" ]
            , ListGroup.attrs [ class "justify-content-between" ]
            ]
            [ text "Aloha"
            , Badge.pillInfo [] [ text "2" ]
            ]
        ]


listGroup2 : Html Msg
listGroup2 =
    ListGroup.custom
        [ ListGroup.anchor
            [ ListGroup.active
            , ListGroup.attrs [ href "#" ]
            , ListGroup.attrs [ class "flex-column align-items-start" ]
            ]
            [ div [ class "d-flex w-100 justify-content-between" ]
                [ h5 [ class "mb-1" ] [ text "List group heading" ]
                , small [] [ text "3 days ago" ]
                ]
            , p [ class "mb-1" ] [ text "Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit." ]
            , small [] [ text "Oh yea that's neat" ]
            ]
        , ListGroup.anchor
            [ ListGroup.attrs [ href "#" ]
            , ListGroup.attrs [ class "flex-column align-items-start" ]
            ]
            [ div [ class "d-flex w-100 justify-content-between" ]
                [ h5 [ class "mb-1" ] [ text "List group heading" ]
                , small [] [ text "3 days ago" ]
                ]
            , p [ class "mb-1" ] [ text "Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit." ]
            , small [] [ text "Oh yea that's neat" ]
            ]
        , ListGroup.anchor
            [ ListGroup.attrs [ href "#" ]
            , ListGroup.attrs [ class "flex-column align-items-start" ]
            ]
            [ div [ class "d-flex w-100 justify-content-between" ]
                [ h5 [ class "mb-1" ] [ text "List group heading" ]
                , small [] [ text "3 days ago" ]
                ]
            , p [ class "mb-1" ] [ text "Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit." ]
            , small [] [ text "Oh yea that's neat" ]
            ]
        ]


accordion : Model -> Html Msg
accordion { accordionState } =
    div []
        [ h1 [] [ text "Accordion" ]
        , Accordion.config AccordionMsg
            |> Accordion.withAnimation
            |> Accordion.cards [ cardOne, cardTwo ]
            |> Accordion.view accordionState
        ]


cardOne : Accordion.Card Msg
cardOne =
    Accordion.card
        { id = "card1"
        , options = []
        , header =
            Accordion.headerH3 []
                (Accordion.toggle [] [ text " Card With container" ])
                |> Accordion.prependHeader [ span [ class "fa fa-car" ] [] ]
        , blocks =
            [ Accordion.block []
                [ Card.titleH4 [] [ text "Some title" ]
                , Card.text [] [ text "Some content, lorem ipsum etc" ]
                ]
            , Accordion.listGroup
                [ ListGroup.li [] [ text "List item 1" ]
                , ListGroup.li [] [ text "List item 2" ]
                ]
            ]
        }


cardTwo : Accordion.Card Msg
cardTwo =
    Accordion.card
        { id = "card2"
        , options = []
        , header =
            Accordion.header [] <|
                Accordion.toggle [] [ text "Card 2" ]
        , blocks =
            [ Accordion.block []
                [ Card.titleH4 [] [ text "Some other title" ]
                , Card.text [] [ text "Different content, lorem ipsum etc" ]
                ]
            , Accordion.block [ Card.blockAlign Text.alignXsCenter ]
                [ Card.titleH5 [] [ text "Another block title" ]
                , Card.text [] [ text "Even more content, lorem ipsum etc" ]
                ]
            ]
        }


cards : Html Msg
cards =
    div []
        [ h1 [] [ text "Cards" ]
        , Card.deck
            [ Card.config [ Card.outlinePrimary ]
                |> Card.headerH1 [] [ text "Primary" ]
                |> Card.footer [] [ text "Primary footer" ]
                |> Card.block []
                    [ Card.titleH4 [] [ text "Primary outlined" ]
                    , Card.text [] [ text "Outlined primary card. Cool." ]
                    ]
            , Card.config [ Card.outlineSuccess ]
                |> Card.headerH1 [] [ text "Success" ]
                |> Card.footer [] [ text "Success footer" ]
                |> Card.block
                    [ Card.blockAlign Text.alignXsLeft ]
                    [ Card.titleH4 [] [ text "Success outlined" ]
                    , Card.text [] [ text "The success of outlining cards is staggering" ]
                    , Card.link [ href "#" ] [ text "Link 1" ]
                    , Card.link [ href "#" ] [ text "Link 2" ]
                    ]
            ]
        , Card.group
            [ Card.config [ Card.danger, Card.align Text.alignXsCenter ]
                |> Card.block []
                    [ Card.titleH4 [] [ text "Danger inverse " ]
                    , Card.text [] [ text " A Simple card with a dangerous role" ]
                    , Card.link [ href "#" ] [ text "A Link !" ]
                    ]
            , Card.config [ Card.warning, Card.align Text.alignXsLeft ]
                |> Card.block []
                    [ Card.titleH4 [] [ text "Warning inverse " ]
                    , Card.text [] [ text " A Simple card with a warning role" ]
                    ]
            , Card.config [ Card.info, Card.align Text.alignXsRight ]
                |> Card.block []
                    [ Card.titleH4 [] [ text "Info inverse " ]
                    , Card.text [] [ text " A Simple card with a info role" ]
                    ]
            ]
        , Card.config [ Card.outlineDanger ]
            |> Card.block []
                [ Card.text [] [ text "Just some text you know" ] ]
            |> Card.view
        ]


tables : Html Msg
tables =
    div []
        [ h1 [] [ text "Simple Table" ]
        , Table.simpleTable
            ( Table.simpleThead
                [ Table.th [] [ text "Col 1" ]
                , Table.th [] [ text "Col 2" ]
                , Table.th [] [ text "Col 3" ]
                ]
            , Table.tbody []
                [ Table.tr []
                    [ Table.td [] [ text "Hello" ]
                    , Table.td [] [ text "Hello" ]
                    , Table.td [] [ text "Hello" ]
                    ]
                , Table.tr []
                    [ Table.td [] [ text "There" ]
                    , Table.td [] [ text "There" ]
                    , Table.td [] [ text "There" ]
                    ]
                , Table.tr []
                    [ Table.td [] [ text "Dude" ]
                    , Table.td [] [ text "Dude" ]
                    , Table.td [] [ text "Dude" ]
                    ]
                ]
            )
        , h1 [] [ text "Whacky Table" ]
        , Table.table
            { options =
                [ Table.hover
                , Table.bordered
                , Table.small
                , Table.attr <| onClick NoOp
                ]
            , thead =
                Table.thead
                    [ Table.inversedHead ]
                    [ Table.tr []
                        [ Table.th [ Table.cellWarning ] [ text "Col 1" ]
                        , Table.th [] [ text "Col 2" ]
                        , Table.th [] [ text "Col 3" ]
                        ]
                    ]
            , tbody =
                Table.tbody []
                    [ Table.tr
                        [ Table.rowSuccess ]
                        [ Table.th [] [ text "Hello" ]
                        , Table.td [] [ text "Hello" ]
                        , Table.td [] [ text "Hello" ]
                        ]
                    , Table.tr []
                        [ Table.th [ Table.cellInfo ] [ text "There" ]
                        , Table.td [] [ text "There" ]
                        , Table.td [] [ text "There" ]
                        ]
                    , Table.tr []
                        [ Table.th [] [ text "Dude" ]
                        , Table.td [] [ text "Dude" ]
                        , Table.td [] [ text "Dude" ]
                        ]
                    ]
            }
        ]


progressBars : Html msg
progressBars =
    div []
        [ h1 [] [ text "Progress bars" ]
        , Progress.progress [ Progress.label "Won't show..." ]
        , Progress.progress [ Progress.value 50 ]
        , Progress.progress [ Progress.value 30, Progress.striped ]
        , Progress.progress [ Progress.value 20, Progress.success, Progress.height 10 ]
        , Progress.progressMulti
            [ [ Progress.height 25, Progress.value 30 ]
            , [ Progress.value 100, Progress.info, Progress.striped ]
            , [ Progress.value 100, Progress.label "Silly" ]
            , [ Progress.value 100, Progress.danger, Progress.animated ]
            ]
        , Progress.progress
            [ Progress.value 50
            , Progress.customLabel
                [ span [ class "fa fa-car" ] [] ]
            ]
        ]


rowStyle : Attribute Msg
rowStyle =
    style
        [ ( "min-height", "8rem" )
        , ( "background-color", "rgba(255, 0, 0, 0.1)" )
        , ( "border", "1 px solid black" )
        ]


colStyle : Attribute Msg
colStyle =
    style
        [ ( "padding-top", ".75rem" )
        , ( "padding-bottom", ".75rem" )
        , ( "background-color", "rgba(86, 61, 124, 0.15)" )
        , ( "border", "1px solid rgba(86, 61, 124, 0.2)" )
        ]
