module Bootstrap.Form.InputGroup
    exposing
        ( view
        , config
        , predecessors
        , successors
        , text
        , password
        , datetimeLocal
        , date
        , month
        , time
        , week
        , number
        , email
        , url
        , search
        , tel
        , button
        , span
        , large
        , small
        , attrs
        , Config
        , Input
        , Addon
        )

{-| Easily extend form input controls by adding text and buttons.

    import Bootstrap.Form.InputGroup as InputGroup
    import Bootstrap.Form.Input as Input
    import Bootstrap.Button as Button


    InputGroup.config
        ( InputGroup.text [] )
        |> InputGroup.large
        |> InputGroup.predecessors
            [ InputGroup.span [] [ text "Stuff" ] ]
        |> InputGroup.successors
            [ InputGroup.button [] [ text "DoIt!"] ]
        |> InputGroup.view


# General
@docs view, config, predecessors, successors, text, button, span, Config, Input, Addon

# Additional input flavors
@docs password, datetimeLocal, date, month, time, week, number, email, url, search, tel


# Sizing
@docs large, small

# Further customization
@docs attrs

-}

import Html
import Html.Attributes as Attributes
import Bootstrap.Grid.Internal as GridInternal
import Bootstrap.Button as Button
import Bootstrap.Form.Input as Input


{-| Opaque representation of the view configuration for an input group.
-}
type Config msg
    = Config
        { input : Input msg
        , predecessors : List (Addon msg)
        , successors : List (Addon msg)
        , size : Maybe GridInternal.ScreenSize
        , attributes : List (Html.Attribute msg)
        }


{-| Opaque representation of an input element.
-}
type Input msg
    = Input (Html.Html msg)


{-| Opaque representation of an input-group add-on element.
-}
type Addon msg
    = Addon (Html.Html msg)


{-| Create initial view configuration for an input group.

* `input` - The input for the input group
-}
config : Input msg -> Config msg
config input =
    Config
        { input = input
        , predecessors = []
        , successors = []
        , size = Nothing
        , attributes = []
        }


{-| Create the view representation for an Input group based on
a [´configuration`](#Config)
-}
view : Config msg -> Html.Html msg
view (Config config) =
    let
        (Input input) =
            config.input
    in
        Html.div
            ([ Attributes.class "input-group" ]
                ++ ([ Maybe.andThen sizeAttribute config.size ]
                        |> List.filterMap identity
                   )
                ++ config.attributes
            )
            (List.map (\(Addon e) -> e) config.predecessors
                ++ [ input ]
                ++ List.map (\(Addon e) -> e) config.successors
            )


{-| Specify a list of add-ons to display before the input.

* `addons` List of add-ons
* `config` View configuration for Input group (so far)
-}
predecessors :
    List (Addon msg)
    -> Config msg
    -> Config msg
predecessors addons (Config config) =
    Config
        { config | predecessors = addons }


{-| Specify a list of add-ons to display after the input.

* `addons` List of add-ons
* `config` View configuration for Input group (so far)
-}
successors :
    List (Addon msg)
    -> Config msg
    -> Config msg
successors addons (Config config) =
    Config
        { config | successors = addons }


{-| Create a simple span add-on. Great for simple texts or font icons

* `attributes` - List of attributes
* `children` - List of child elements
-}
span : List (Html.Attribute msg) -> List (Html.Html msg) -> Addon msg
span attributes children =
    Html.span
        (Attributes.class "input-group-addon" :: attributes)
        children
        |> Addon


{-| Create a button add-on.

* `options` List of button options
* `children` LIst of child elements
-}
button :
    List (Button.Option msg)
    -> List (Html.Html msg)
    -> Addon msg
button options children =
    Html.span
        [ Attributes.class "input-group-btn" ]
        [ Button.button options children ]
        |> Addon


{-| Create an input add-on with type="text"
-}
text : List (Input.Option msg) -> Input msg
text =
    input Input.text


{-| Create an input add-on with type="password"
-}
password : List (Input.Option msg) -> Input msg
password =
    input Input.password


{-| Create an input add-on with type="datetime-local"
-}
datetimeLocal : List (Input.Option msg) -> Input msg
datetimeLocal =
    input Input.datetimeLocal


{-| Create an input add-on with type="date"
-}
date : List (Input.Option msg) -> Input msg
date =
    input Input.date


{-| Create an input add-on with type="month"
-}
month : List (Input.Option msg) -> Input msg
month =
    input Input.month


{-| Create an input add-on with type="time"
-}
time : List (Input.Option msg) -> Input msg
time =
    input Input.time


{-| Create an input add-on with type="week"
-}
week : List (Input.Option msg) -> Input msg
week =
    input Input.week


{-| Create an input add-on with type="number"
-}
number : List (Input.Option msg) -> Input msg
number =
    input Input.number


{-| Create an input add-on with type="email"
-}
email : List (Input.Option msg) -> Input msg
email =
    input Input.email


{-| Create an input add-on with type="url"
-}
url : List (Input.Option msg) -> Input msg
url =
    input Input.url


{-| Create an input add-on with type="search"
-}
search : List (Input.Option msg) -> Input msg
search =
    input Input.search


{-| Create an input add-on with type="tel"
-}
tel : List (Input.Option msg) -> Input msg
tel =
    input Input.tel


input :
    (List (Input.Option msg) -> Html.Html msg)
    -> List (Input.Option msg)
    -> Input msg
input inputFn options =
    inputFn options |> Input


{-| Make all controls in an input group large
-}
large : Config msg -> Config msg
large (Config config) =
    Config
        { config | size = Just GridInternal.LG }


{-| Make all controls in an input group small
-}
small : Config msg -> Config msg
small (Config config) =
    Config
        { config | size = Just GridInternal.SM }


{-| When you need to customize the input group container, use this function to provide customization attributes.
-}
attrs : List (Html.Attribute msg) -> Config msg -> Config msg
attrs attributes (Config config) =
    Config
        { config | attributes = attributes }


sizeAttribute : GridInternal.ScreenSize -> Maybe (Html.Attribute msg)
sizeAttribute size =
    Maybe.map
        (\s -> Attributes.class <| "input-group-" ++ s)
        (GridInternal.screenSizeOption size)
