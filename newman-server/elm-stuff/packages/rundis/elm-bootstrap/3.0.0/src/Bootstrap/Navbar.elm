module Bootstrap.Navbar
    exposing
        ( view
        , config
        , initialState
        , subscriptions
        , withAnimation
        , fixTop
        , fixBottom
        , faded
        , primary
        , success
        , info
        , warning
        , danger
        , inverse
        , lightCustom
        , darkCustom
        , lightCustomClass
        , darkCustomClass
        , brand
        , items
        , customItems
        , itemLink
        , itemLinkActive
        , textItem
        , formItem
        , customItem
        , container
        , collapseSmall
        , collapseMedium
        , collapseLarge
        , collapseExtraLarge
        , attrs
        , dropdown
        , dropdownItem
        , dropdownDivider
        , dropdownHeader
        , dropdownToggle
        , Item
        , CustomItem
        , Brand
        , State
        , Config
        , DropdownToggle
        , DropdownItem
        )

{-| The navbar is a wrapper that positions branding, navigation, and other elements in a concise header.
The navbar is designed to be responsive by default and made interactive with a tiny sprinkle of Elm.





    import Bootstrap.Navbar as Navbar

    -- You need to keep track of the view state for the navbar in your model

    type alias Model =
        { navbarState : Navbar.State }


    -- The navbar needs to know the initial window size, so the inital state for a navbar requires a command to be run by the Elm runtime

    initialState : ( Model, Cmd Msg )
    initialState =
        let
            ( navbarState, navbarCmd ) =
                Navbar.initialState NavbarMsg
        in
            ( { navbarState = navbarState }, navbarCmd )


    -- Define a message for the navbar

    type Msg
        = NavbarMsg Navbar.State


    -- You need to handle navbar messages in your update function to step the navbar state forward

    update : Msg -> Model -> (Model, Cmd Msg)
    update msg model =
        case msg of
            NavbarMsg state ->
                ( { model | navbarState = state }, Cmd.none )


    view : Model -> Html Msg
    view model =
        Navbar.config NavbarMsg
            |> Navbar.withAnimation
            |> Navbar.brand [ href "#"] [ text "Brand"]
            |> Navbar.items
                [ Navbar.itemLink [href "#"] [ text "Item 1"]
                , Navbar.itemLink [href "#"] [ text "Item 2"]
                ]
            |> Navbar.view model.navbarState

    -- If you use animations as above or you use dropdowns in your navbar you need to configure subscriptions too

    subscriptions : Model -> Sub Msg
    subscriptions model =
        Navbar.subscriptions model.navbarState NavbarMsg



# Navbar
@docs view, config, Config


## Options
@docs withAnimation, primary, success, info, warning, danger, inverse, faded, fixTop, fixBottom, lightCustom, darkCustom, lightCustomClass, darkCustomClass, collapseSmall, collapseMedium, collapseLarge, collapseExtraLarge, container, attrs


## Brand
@docs brand, Brand

## Menu items
@docs items, itemLink, itemLinkActive, Item

## Dropdown menu
@docs dropdown, dropdownToggle, dropdownItem, dropdownDivider, dropdownHeader, DropdownToggle, DropdownItem


## Custom items
@docs customItems, textItem, formItem, customItem, CustomItem



# State
@docs initialState, State


# Interactive elements and subscriptions
@docs subscriptions


-}

import Html
import Html.Attributes exposing (class, classList, style, type_, id, href)
import Html.Events exposing (onClick, on, onWithOptions)
import Bootstrap.Grid.Internal as GridInternal
import Color
import Dict
import Json.Decode as Json
import DOM
import AnimationFrame
import Window
import Task
import Mouse


{-| Opaque type representing the view state of the navbar and any navbar dropdown menus
-}
type State
    = State VisibilityState


type alias VisibilityState =
    { visibility : Visibility
    , height : Maybe Float
    , windowSize : Maybe Window.Size
    , dropdowns : Dict.Dict String DropdownStatus
    }


type Visibility
    = Hidden
    | StartDown
    | AnimatingDown
    | StartUp
    | AnimatingUp
    | Shown


type DropdownStatus
    = Open
    | ListenClicks
    | Closed


{-| Configuration information for describing the view of the Navbar

* `options` List of [`configuration options`](#options)
* `toMsg` Message function used for stepping the viewstate of the navbar forward
* `withAnimation` Set to True if you wish the menu to slide up/down with an animation effect
* `brand` Optional [`brand`](#brand) element (typically a logo)
* `items` List of menu items that the user can select from
* `customItems` List of custom (inline) items that you may place to the right of the std. navigation items
-}
type Config msg
    = Config (ConfigRec msg)


type alias ConfigRec msg =
    { options : Options msg
    , toMsg : State -> msg
    , withAnimation : Bool
    , brand : Maybe (Brand msg)
    , items : List (Item msg)
    , customItems : List (CustomItem msg)
    }


type alias Options msg =
    { fix : Maybe Fix
    , isContainer : Bool
    , scheme : Maybe Scheme
    , toggleAt : GridInternal.ScreenSize
    , attributes : List (Html.Attribute msg)
    }


type Fix
    = Top
    | Bottom


type alias Scheme =
    { modifier : LinkModifier
    , bgColor : BackgroundColor
    }


type LinkModifier
    = Dark
    | Light


type BackgroundColor
    = Faded
    | Primary
    | Success
    | Info
    | Warning
    | Danger
    | Inverse
    | Custom Color.Color
    | Class String


{-| Opaque type representing a selectable menu item
-}
type Item msg
    = Item
        { attributes : List (Html.Attribute msg)
        , children : List (Html.Html msg)
        }
    | NavDropdown (Dropdown msg)


{-| Opaque type representing a custom (inline) navbar item
-}
type CustomItem msg
    = CustomItem (Html.Html msg)


{-| Opaque type representing a brand element
-}
type Brand msg
    = Brand (Html.Html msg)


type Dropdown msg
    = Dropdown
        { id : String
        , toggle : DropdownToggle msg
        , items : List (DropdownItem msg)
        }


{-| Opaque type representing the toggle element for a dropdown menu
-}
type DropdownToggle msg
    = DropdownToggle
        { attributes : List (Html.Attribute msg)
        , children : List (Html.Html msg)
        }


{-| Opaque type representing an item in a dropdown menu
-}
type DropdownItem msg
    = DropdownItem (Html.Html msg)


{-| You need to call this function to initialize the view state for the navbar
and store the state in your main model.

    init : ( Model, Cmd Msg )
    init =
        let
            (navbarState, navCmd) =
                Navbar.initialState NavbarMsg
        in
            ( { navbarState = navbarState }
            , navCmd
            )

The Cmd part is needed, because the navbar as implemented currently needs the window size.
Hopefully a smoother solution can be devised in the future.


-}
initialState : (State -> msg) -> ( State, Cmd msg )
initialState toMsg =
    let
        state =
            State
                { visibility = Hidden
                , height = Nothing
                , windowSize = Nothing
                , dropdowns = Dict.empty
                }
    in
        ( state, initWindowSize toMsg state )


initWindowSize : (State -> msg) -> State -> Cmd msg
initWindowSize toMsg state =
    Window.size
        |> Task.perform
            (\size ->
                toMsg <|
                    mapState (\s -> { s | windowSize = Just size }) state
            )


{-| To support animations and managing the state of dropdown you need to wire up this
function in your main subscriptions function.

    subscriptions : Model -> Sub Msg
    subscriptions model =
        Navbar.subscriptions model.navbarState NavbarMsg


**Note: ** If you are NOT using dropdowns in your navbar AND you are using a navbar without animation
you can skip this. But it's not that much work, so maybe you are better off doing it anyway.
-}
subscriptions : State -> (State -> msg) -> Sub msg
subscriptions ((State { visibility }) as state) toMsg =
    let
        updState v =
            mapState
                (\s -> { s | visibility = v })
                state
    in
        Sub.batch
            [ case visibility of
                StartDown ->
                    AnimationFrame.times
                        (\_ -> toMsg <| updState AnimatingDown)

                StartUp ->
                    AnimationFrame.times
                        (\_ -> toMsg <| updState AnimatingUp)

                _ ->
                    Sub.none
            , Window.resizes
                (\size ->
                    mapState (\s -> { s | windowSize = Just size }) state
                        |> toMsg
                )
            , dropdownSubscriptions state toMsg
            ]


dropdownSubscriptions : State -> (State -> msg) -> Sub msg
dropdownSubscriptions ((State { dropdowns }) as state) toMsg =
    let
        updDropdowns =
            Dict.map
                (\_ status ->
                    case status of
                        Open ->
                            ListenClicks

                        ListenClicks ->
                            Closed

                        Closed ->
                            Closed
                )
                dropdowns

        updState =
            mapState (\s -> { s | dropdowns = updDropdowns }) state

        needsSub s =
            Dict.toList dropdowns
                |> List.any (\( _, status ) -> status == s)
    in
        Sub.batch
            [ if needsSub Open then
                AnimationFrame.times
                    (\_ -> toMsg updState)
              else
                Sub.none
            , if needsSub ListenClicks then
                Mouse.clicks
                    (\_ -> toMsg updState)
              else
                Sub.none
            ]


{-| Creates a default navbar view configuration. Providing a starting point
to set up your navbar how you'd like.
-}
config : (State -> msg) -> Config msg
config toMsg =
    Config
        { toMsg = toMsg
        , withAnimation = False
        , brand = Nothing
        , items = []
        , customItems = []
        , options =
            { fix = Nothing
            , isContainer = False
            , scheme = Just { modifier = Light, bgColor = Faded }
            , toggleAt = GridInternal.XS
            , attributes = []
            }
        }


updateConfig : (ConfigRec msg -> ConfigRec msg) -> Config msg -> Config msg
updateConfig mapper (Config config) =
    Config <| mapper config


updateOptions : (Options msg -> Options msg) -> Config msg -> Config msg
updateOptions mapper (Config config) =
    Config { config | options = mapper config.options }


{-| The main view function for displaying a navbar.

    Navbar.config NavbarMsg
        |> Navbar.withAnimation
        |> Navbar.brand [ href "#"] [ text "Brand"]
        |> Navbar.items
            [ Navbar.itemLink [href "#"] [ text "Item 1"]
            , Navbar.itemLink [href "#"] [ text "Item 2"]
            ]
        |> Navbar.customItems
            [ Navbar.textItem [] [ text "Some text" ] ]
        |> Navbar.view model.navbarState

* `state` Required view state the navbar uses to support interactive behavior
* `config` The view [`configuration`](#Configuration) that determines to look and feel of the navbar

-}
view :
    State
    -> Config msg
    -> Html.Html msg
view state ((Config { options, brand, items, customItems }) as config) =
    Html.nav
        (navbarAttributes options)
        ([ Html.button
            [ class <|
                "navbar-toggler"
                    ++ (Maybe.map (\_ -> " navbar-toggler-right") brand
                            |> Maybe.withDefault ""
                       )
              -- navbar-toggler-right"
            , type_ "button"
            , toggleHandler state config
            ]
            [ Html.span [ class "navbar-toggler-icon" ] [] ]
         ]
            ++ maybeBrand brand
            ++ [ Html.div
                    (menuAttributes state config)
                    [ Html.div (menuWrapperAttributes state config)
                        ([ renderNav state config items ] ++ renderCustom customItems)
                    ]
               ]
        )


{-| Use a slide up/down animation for toggling the navbar menu when collapsed.

**NOTE: ** Do remember to set up the subscriptions function when using this option.
-}
withAnimation : Config msg -> Config msg
withAnimation config =
    updateConfig (\conf -> { conf | withAnimation = True }) config


{-| Option to fix the menu to the top of the viewport

**Note: You probably need to add some margin-top to the content element following the navbar when using this option **
-}
fixTop : Config msg -> Config msg
fixTop config =
    updateOptions (\opts -> { opts | fix = Just Top }) config



--NavbarFix Top


{-| Option to fix the menu to the bottom of the viewport
-}
fixBottom : Config msg -> Config msg
fixBottom config =
    updateOptions (\opts -> { opts | fix = Just Bottom }) config


{-| Use this option when you want a fixed width menu (typically because you're main content is also confgirued to be fixed width)
-}
container : Config msg -> Config msg
container config =
    updateOptions (\opts -> { opts | isContainer = True }) config


{-| Inverse the colors of your menu. Dark background and light text
-}
inverse : Config msg -> Config msg
inverse =
    scheme Dark Inverse


{-| Give your menu a light faded gray look
-}
faded : Config msg -> Config msg
faded =
    scheme Light Faded


{-| Option to color menu using the primary color
-}
primary : Config msg -> Config msg
primary =
    scheme Dark Primary


{-| Option to color menu using the success color
-}
success : Config msg -> Config msg
success =
    scheme Dark Success


{-| Option to color menu using the info color
-}
info : Config msg -> Config msg
info =
    scheme Dark Info


{-| Option to color menu using the warning color
-}
warning : Config msg -> Config msg
warning =
    scheme Dark Warning


{-| Option to color menu using the danger color
-}
danger : Config msg -> Config msg
danger =
    scheme Dark Danger


{-| Option to color menu using a dark custom background color
-}
darkCustom : Color.Color -> Config msg -> Config msg
darkCustom color =
    scheme Dark <| Custom color


{-| Option to color menu using a light custom background color
-}
lightCustom : Color.Color -> Config msg -> Config msg
lightCustom color =
    scheme Light <| Custom color


{-| Option to color menu using a dark custom background color defined by css class(es)
-}
darkCustomClass : String -> Config msg -> Config msg
darkCustomClass classString =
    scheme Dark <| Class classString


{-| Option to color menu using a light custom background color defined by css class(es)
-}
lightCustomClass : String -> Config msg -> Config msg
lightCustomClass classString =
    scheme Light <| Class classString


scheme : LinkModifier -> BackgroundColor -> Config msg -> Config msg
scheme modifier bgColor config =
    updateOptions
        (\opt ->
            { opt
                | scheme =
                    Just
                        { modifier = modifier
                        , bgColor = bgColor
                        }
            }
        )
        config


{-| Collapse the menu at the small media breakpoint
-}
collapseSmall : Config msg -> Config msg
collapseSmall =
    toggleAt GridInternal.SM


{-| Collapse the menu at the medium media breakpoint
-}
collapseMedium : Config msg -> Config msg
collapseMedium =
    toggleAt GridInternal.MD


{-| Collapse the menu at the large media breakpoint
-}
collapseLarge : Config msg -> Config msg
collapseLarge =
    toggleAt GridInternal.LG


{-| Collapse the menu at the extra large media breakpoint
-}
collapseExtraLarge : Config msg -> Config msg
collapseExtraLarge =
    toggleAt GridInternal.XL


toggleAt : GridInternal.ScreenSize -> Config msg -> Config msg
toggleAt size config =
    updateOptions (\opt -> { opt | toggleAt = size }) config


{-| Add a custom Html.Attribute to the navbar element using this function
-}
attrs : List (Html.Attribute msg) -> Config msg -> Config msg
attrs attrs config =
    updateOptions (\opt -> { opt | attributes = opt.attributes ++ attrs }) config


{-| Create a brand element for your navbar

    Navbar.brand
        [ href "#" ] -- (and perhaps use onWithOptions for custom handling of clicks !)
        [ img [src "assets/logo.svg" ] [ text "MyCompany" ] ]
        config

* `attributes` List of attributes
* `children` List of children
* `config` Navbar config record to add/modify brand for
-}
brand :
    List (Html.Attribute msg)
    -> List (Html.Html msg)
    -> Config msg
    -> Config msg
brand attributes children config =
    updateConfig
        (\conf ->
            { conf
                | brand =
                    Html.a
                        ([ class "navbar-brand" ] ++ attributes)
                        children
                        |> Brand
                        |> Just
            }
        )
        config


{-| Configure your navbar with a list of navigation links and/or dropdowns.

**NOTE** If you call this function several times, the last time "wins".
-}
items : List (Item msg) -> Config msg -> Config msg
items items config =
    updateConfig (\conf -> { conf | items = items }) config


{-| You can add custom items to a navbar too. These are placed after any navigation items.

**NOTE** If you call this function several times, the last time "wins".
-}
customItems : List (CustomItem msg) -> Config msg -> Config msg
customItems items config =
    updateConfig (\conf -> { conf | customItems = items }) config


{-| Create a menu item (as an `a` element)

* `attributes` List of attributes
* `children` List of children

-}
itemLink : List (Html.Attribute msg) -> List (Html.Html msg) -> Item msg
itemLink attributes children =
    Item
        { attributes = attributes
        , children = children
        }


{-| Create a menu item that is styled as active (as an `a` element)

* `attributes` List of attributes
* `children` List of children

-}
itemLinkActive : List (Html.Attribute msg) -> List (Html.Html msg) -> Item msg
itemLinkActive attributes =
    itemLink (class "active" :: attributes)


{-| Create a custom inline text element, which will float to the right when the menu isn't collapsed

* `attributes` List of attributes
* `children` List of children


**Note: If you have multiple custom items you will need to provide spacing between them yourself **
-}
textItem : List (Html.Attribute msg) -> List (Html.Html msg) -> CustomItem msg
textItem attributes children =
    Html.span
        (class "navbar-text" :: attributes)
        children
        |> CustomItem


{-| Create a custom inline form element, which will float to the right when the menu isn't collapsed

    Navbar.formItem []
        [ TextInput.text
            [ TextInput.small ]
        , Button.button
            [ Button.roleSuccess, Button.small]
            [ text "Submit"]]
        ]

* `attributes` List of attributes
* `children` List of children


**Note: If you have multiple custom items you will need to provide spacing between them yourself **
-}
formItem : List (Html.Attribute msg) -> List (Html.Html msg) -> CustomItem msg
formItem attributes children =
    Html.form
        (class "form-inline" :: attributes)
        children
        |> CustomItem


{-| Create a completely custom item, which will float to the right when the menu isn't collapsed. You should ensure that you create inline elements or else your menu will break in unfortunate ways !

* `attributes` List of attributes
* `children` List of children


**Note: If you have multiple custom items you will need to provide spacing between them yourself **
-}
customItem : Html.Html msg -> CustomItem msg
customItem elem =
    CustomItem elem


toggleHandler : State -> Config msg -> Html.Attribute msg
toggleHandler ((State { height }) as state) (Config { withAnimation, toMsg }) =
    let
        updState h =
            mapState
                (\s ->
                    { s
                        | height = Just h
                        , visibility =
                            visibilityTransition withAnimation s.visibility
                    }
                )
                state
    in
        heightDecoder
            |> Json.andThen
                (\v ->
                    Json.succeed <|
                        toMsg <|
                            (if v > 0 then
                                updState v
                             else
                                updState <| Maybe.withDefault 0 height
                            )
                )
            |> on "click"


heightDecoder : Json.Decoder Float
heightDecoder =
    let
        tagDecoder =
            Json.map2 (\tag val -> ( tag, val ))
                (Json.field "tagName" Json.string)
                (Json.value)

        fromNavDec =
            Json.oneOf
                [ Json.at [ "childNodes", "2", "childNodes", "0", "offsetHeight" ] Json.float
                , Json.at [ "childNodes", "1", "childNodes", "0", "offsetHeight" ] Json.float
                ]

        fromButtonDec =
            DOM.parentElement <| fromNavDec

        resToDec res =
            case res of
                Result.Ok v ->
                    Json.succeed v

                Result.Err err ->
                    Json.fail err
    in
        (DOM.target <|
            DOM.parentElement <|
                tagDecoder
        )
            |> Json.andThen
                (\( tag, val ) ->
                    case tag of
                        "NAV" ->
                            Json.decodeValue
                                fromNavDec
                                val
                                |> resToDec

                        "BUTTON" ->
                            Json.decodeValue
                                fromButtonDec
                                val
                                |> resToDec

                        _ ->
                            Json.succeed 0
                )


menuAttributes : State -> Config msg -> List (Html.Attribute msg)
menuAttributes ((State { visibility, height }) as state) ((Config { withAnimation, toMsg, options }) as config) =
    let
        defaults =
            [ class "collapse navbar-collapse" ]
    in
        case visibility of
            Hidden ->
                case height of
                    Nothing ->
                        if not withAnimation || shouldHideMenu state config then
                            defaults
                        else
                            [ style
                                [ ( "display", "block" )
                                , ( "height", "0" )
                                , ( "overflow", "hidden" )
                                ]
                            ]

                    Just _ ->
                        defaults

            StartDown ->
                [ transitionStyle Nothing ]

            AnimatingDown ->
                [ transitionStyle height
                , on "transitionend" <|
                    transitionHandler state config
                ]

            AnimatingUp ->
                [ transitionStyle Nothing
                , on "transitionend" <|
                    transitionHandler state config
                ]

            StartUp ->
                [ transitionStyle height ]

            Shown ->
                defaults ++ [ class "show" ]


menuWrapperAttributes : State -> Config msg -> List (Html.Attribute msg)
menuWrapperAttributes ((State { visibility, height }) as state) ((Config { withAnimation }) as config) =
    let
        styleBlock =
            [ style [ ( "display", "block" ) ] ]

        display =
            case height of
                Nothing ->
                    if not withAnimation || shouldHideMenu state config then
                        "flex"
                    else
                        "block"

                Just _ ->
                    "flex"
    in
        case visibility of
            Hidden ->
                [ style [ ( "display", display ), ( "width", "100%" ) ] ]

            StartDown ->
                styleBlock

            AnimatingDown ->
                styleBlock

            AnimatingUp ->
                styleBlock

            StartUp ->
                styleBlock

            Shown ->
                if not withAnimation || shouldHideMenu state config then
                    [ class "collapse navbar-collapse show" ]
                else
                    [ style [ ( "display", "block" ) ] ]


shouldHideMenu : State -> Config msg -> Bool
shouldHideMenu (State { windowSize }) (Config { options }) =
    let
        winMedia =
            case windowSize of
                Just s ->
                    toScreenSize s

                Nothing ->
                    GridInternal.XS
    in
        sizeToComparable winMedia > sizeToComparable options.toggleAt


sizeToComparable : GridInternal.ScreenSize -> number
sizeToComparable size =
    case size of
        GridInternal.XS ->
            1

        GridInternal.SM ->
            2

        GridInternal.MD ->
            3

        GridInternal.LG ->
            4

        GridInternal.XL ->
            5


toScreenSize : Window.Size -> GridInternal.ScreenSize
toScreenSize { width } =
    if width <= 576 then
        GridInternal.XS
    else if width <= 768 then
        GridInternal.SM
    else if width <= 992 then
        GridInternal.MD
    else if width <= 1200 then
        GridInternal.LG
    else
        GridInternal.XL


transitionHandler : State -> Config msg -> Json.Decoder msg
transitionHandler state (Config { toMsg, withAnimation }) =
    mapState
        (\s ->
            { s
                | visibility =
                    visibilityTransition withAnimation s.visibility
            }
        )
        state
        |> toMsg
        |> Json.succeed


transitionStyle : Maybe Float -> Html.Attribute msg
transitionStyle maybeHeight =
    let
        pixelHeight =
            Maybe.map (\v -> (toString v) ++ "px") maybeHeight
                |> Maybe.withDefault "0"
    in
        style
            [ ( "position", "relative" )
            , ( "height", pixelHeight )
            , ( "overflow", "hidden" )
            , ( "-webkit-transition-timing-function", "ease" )
            , ( "-o-transition-timing-function", "ease" )
            , ( "transition-timing-function", "ease" )
            , ( "-webkit-transition-duration", "0.35s" )
            , ( "-o-transition-duration", "0.35s" )
            , ( "transition-duration", "0.35s" )
            , ( "-webkit-transition-property", "height" )
            , ( "-o-transition-property", "height" )
            , ( "transition-property", "height" )
            ]


mapState : (VisibilityState -> VisibilityState) -> State -> State
mapState mapper (State state) =
    State <| mapper state


maybeBrand : Maybe (Brand msg) -> List (Html.Html msg)
maybeBrand brand =
    case brand of
        Just (Brand b) ->
            [ b ]

        Nothing ->
            []


renderNav :
    State
    -> Config msg
    -> List (Item msg)
    -> Html.Html msg
renderNav state config navItems =
    Html.ul
        [ class "navbar-nav mr-auto" ]
        (List.map
            (\item ->
                case item of
                    Item item ->
                        renderItemLink item

                    NavDropdown dropdown ->
                        renderDropdown state config dropdown
            )
            navItems
        )


renderItemLink :
    { attributes : List (Html.Attribute msg)
    , children : List (Html.Html msg)
    }
    -> Html.Html msg
renderItemLink { attributes, children } =
    Html.li
        [ class "nav-item" ]
        [ Html.a
            ([ class "nav-link" ] ++ attributes)
            children
        ]


renderCustom : List (CustomItem msg) -> List (Html.Html msg)
renderCustom items =
    List.map (\(CustomItem item) -> item) items



{- navbarAttributes : List (Option msg) -> List (Html.Attribute msg)
   navbarAttributes options =
       class "navbar" :: List.concatMap navbarAttribute options
-}


navbarAttributes : Options msg -> List (Html.Attribute msg)
navbarAttributes options =
    [ classList
        [ ( "navbar", True )
        , ( "container", options.isContainer )
        ]
    , class <|
        "navbar-toggleable"
            ++ (Maybe.map (\s -> "-" ++ s) (GridInternal.screenSizeOption options.toggleAt)
                    |> Maybe.withDefault ""
               )
    ]
        ++ (case options.scheme of
                Just scheme ->
                    schemeAttributes scheme

                Nothing ->
                    []
           )
        ++ (case options.fix of
                Just fix ->
                    [ class <| fixOption fix ]

                Nothing ->
                    []
           )
        ++ options.attributes


fixOption : Fix -> String
fixOption fix =
    case fix of
        Top ->
            "fixed-top"

        Bottom ->
            "fixed-bottom"


schemeAttributes : Scheme -> List (Html.Attribute msg)
schemeAttributes { modifier, bgColor } =
    [ linkModifierClass modifier
    , backgroundColorOption bgColor
    ]


linkModifierClass : LinkModifier -> Html.Attribute msg
linkModifierClass modifier =
    class <|
        case modifier of
            Dark ->
                "navbar-inverse"

            Light ->
                "navbar-light"


backgroundColorOption : BackgroundColor -> Html.Attribute msg
backgroundColorOption bgClass =
    case bgClass of
        Faded ->
            class "bg-faded"

        Primary ->
            class "bg-primary"

        Success ->
            class "bg-success"

        Info ->
            class "bg-info"

        Warning ->
            class "bg-warning"

        Danger ->
            class "bg-danger"

        Inverse ->
            class "bg-inverse"

        Custom color ->
            style [ ( "background-color", toRGBString color ) ]

        Class classString ->
            class classString


toRGBString : Color.Color -> String
toRGBString color =
    let
        { red, green, blue } =
            Color.toRgb color
    in
        "RGB(" ++ toString red ++ "," ++ toString green ++ "," ++ toString blue ++ ")"


visibilityTransition : Bool -> Visibility -> Visibility
visibilityTransition withAnimation visibility =
    case ( withAnimation, visibility ) of
        ( True, Hidden ) ->
            StartDown

        ( True, StartDown ) ->
            AnimatingDown

        ( True, AnimatingDown ) ->
            Shown

        ( True, Shown ) ->
            StartUp

        ( True, StartUp ) ->
            AnimatingUp

        ( True, AnimatingUp ) ->
            Hidden

        ( False, Hidden ) ->
            Shown

        ( False, Shown ) ->
            Hidden

        _ ->
            Hidden



--- NAV DROPDOWNS


{-| Create a dropdown menu for use in a navbar

* `config` A record with the following properties
    * `id` A unique id for your dropdown. It's important, because it's used to keep track of the state of the dropdown !
    * `toggle` The main item ([`toggle`](#dropdownToggle)) that toggles the dropdown menu up or down
-}
dropdown :
    { id : String
    , toggle : DropdownToggle msg
    , items : List (DropdownItem msg)
    }
    -> Item msg
dropdown config =
    Dropdown config |> NavDropdown


{-| Function to construct a toggle for a [`dropdown`](#dropdown)

* attributes List of attributes
* children List of child elements
-}
dropdownToggle :
    List (Html.Attribute msg)
    -> List (Html.Html msg)
    -> DropdownToggle msg
dropdownToggle attributes children =
    DropdownToggle
        { attributes = attributes
        , children = children
        }


{-| Creates an `a` element appropriate for use in a nav dropdown

* `attributes` List of attributes
* `children` List of child elements
-}
dropdownItem : List (Html.Attribute msg) -> List (Html.Html msg) -> DropdownItem msg
dropdownItem attributes children =
    Html.a ([ class "dropdown-item" ] ++ attributes) children
        |> DropdownItem


{-| Creates a divider element appropriate for use in dropdowns.
Handy when you want to visually separate groups of menu items in a dropdown menu

* `attributes` List of attributes
* `children` List of child elements
-}
dropdownDivider : DropdownItem msg
dropdownDivider =
    Html.div [ class "dropdown-divider" ] []
        |> DropdownItem


{-| Creates an header element appropriate for use in dropdowns.
Handy when you want to provide a heading for a group of menu items in a dropdown menu

* `attributes` List of attributes
* `children` List of child elements
-}
dropdownHeader : List (Html.Html msg) -> DropdownItem msg
dropdownHeader children =
    Html.h6
        [ class "dropdown-header" ]
        children
        |> DropdownItem


renderDropdown :
    State
    -> Config msg
    -> Dropdown msg
    -> Html.Html msg
renderDropdown state ((Config { options }) as config) (Dropdown { id, toggle, items }) =
    let
        needsDropup =
            Maybe.map
                (\fix ->
                    case fix of
                        Bottom ->
                            True

                        Top ->
                            False
                )
                options.fix
                |> Maybe.withDefault False
    in
        Html.li
            [ classList
                [ ( "nav-item", True )
                , ( "dropdown", True )
                , ( "dropup", needsDropup )
                , ( "show", getOrInitDropdownStatus id state /= Closed )
                ]
            ]
            [ renderDropdownToggle state id config toggle
            , Html.div
                [ class "dropdown-menu" ]
                (List.map (\(DropdownItem item) -> item) items)
            ]


renderDropdownToggle :
    State
    -> String
    -> Config msg
    -> DropdownToggle msg
    -> Html.Html msg
renderDropdownToggle state id config (DropdownToggle { attributes, children }) =
    Html.a
        ([ class "nav-link dropdown-toggle"
         , href "#"
         , onWithOptions
            "click"
            { stopPropagation = False
            , preventDefault = True
            }
           <|
            Json.succeed (toggleOpen state id config)
         ]
            ++ attributes
        )
        children


toggleOpen :
    State
    -> String
    -> Config msg
    -> msg
toggleOpen state id (Config { toMsg }) =
    let
        currStatus =
            getOrInitDropdownStatus id state

        newStatus =
            case currStatus of
                Open ->
                    Closed

                ListenClicks ->
                    Closed

                Closed ->
                    Open
    in
        mapState
            (\s ->
                { s | dropdowns = Dict.insert id newStatus s.dropdowns }
            )
            state
            |> toMsg


getOrInitDropdownStatus : String -> State -> DropdownStatus
getOrInitDropdownStatus id (State { dropdowns }) =
    Dict.get id dropdowns
        |> Maybe.withDefault Closed
