module Bootstrap.ListGroup
    exposing
        ( ul
        , keyedUl
        , li
        , custom
        , keyedCustom
        , anchor
        , button
        , success
        , info
        , warning
        , danger
        , active
        , disabled
        , attrs
        , ItemOption
        , Item
        , CustomItem
        )

{-| List groups are a flexible and powerful component for displaying a series of content. List group items can be modified and extended to support just about any content within. They can also be used as navigation with the right modifier class

# Simple lists
@docs ul, li, keyedUl, Item


# Custom lists
@docs custom, keyedCustom, anchor, button, CustomItem


# Options
@docs success, info, warning, danger, active, disabled, attrs, ItemOption


-}

import Html
import Html.Attributes as Attr exposing (class, classList, type_)
import Html.Keyed as Keyed
import Bootstrap.Internal.ListGroup as Internal


{-| Opaque type representing configuration options for a list item
-}
type alias ItemOption msg =
    Internal.ItemOption msg


{-| Opaque type representing a list item in a ul based list group
-}
type alias Item msg =
    Internal.Item msg


{-| Opaque type representing an item in a custom list group
-}
type alias CustomItem msg =
    Internal.CustomItem msg


{-| A simple list group based on a ul element

    ListGroup.ul
        [ ListGroup.li [ ListGroup.active ] [ text "Item 1"]
        , ListGroup.li [ ] [ text "Item 2" ]
        ]
-}
ul : List (Item msg) -> Html.Html msg
ul items =
    Html.ul
        [ class "list-group" ]
        (List.map Internal.renderItem items)


{-| Create a ul list group with keyed children.
-}
keyedUl : List ( String, Item msg ) -> Html.Html msg
keyedUl keyedItems =
    Keyed.ul
        [ class "list-group" ]
        (List.map (\( key, item ) -> ( key, Internal.renderItem item )) keyedItems)


{-| Composable li element for a ul based list group
-}
li : List (ItemOption msg) -> List (Html.Html msg) -> Item msg
li options children =
    Internal.Item
        { itemFn = Html.li
        , children = children
        , options = options
        }


{-| Create a custom List group

    ListGroup.custom
        [ ListGroup.button
            [ ListGroup.attr <| onClick "MyItem1Msg"
            , ListGroup.info
            ]
            [ text "List item 1" ]
        , ListGroup.button
            [ ListGroup.attr <| onClick "MyItem2Msg"
            , ListGroup.warning
            ]
            [ text "List item 2" ]
        ]

-}
custom : List (CustomItem msg) -> Html.Html msg
custom items =
    Html.div
        [ class "list-group" ]
        (List.map Internal.renderCustomItem items)


{-| Create a custom list group with keyed children.
-}
keyedCustom : List ( String, CustomItem msg ) -> Html.Html msg
keyedCustom items =
    Keyed.node "div"
        [ class "list-group" ]
        (List.map (\( key, item ) -> ( key, Internal.renderCustomItem item )) items)


{-| Create a composable anchor list item for use in a custom list

* `options` List of options to configure the list item
* `children` List of child elements
-}
anchor :
    List (ItemOption msg)
    -> List (Html.Html msg)
    -> CustomItem msg
anchor options children =
    let
        updOptions =
            if List.any ((==) Internal.Disabled) options then
                options ++ [ Internal.Attrs [ Internal.preventClick ] ]
            else
                options
    in
        Internal.CustomItem
            { itemFn = Html.a
            , children = children
            , options = Internal.Action :: updOptions
            }


{-| Create a composable button list item for use in a custom list

* `options` List of options to configure the list item
* `children` List of child elements
-}
button :
    List (ItemOption msg)
    -> List (Html.Html msg)
    -> CustomItem msg
button options children =
    Internal.CustomItem
        { itemFn = Html.button
        , children = children
        , options = Internal.Action :: (options ++ [ Internal.Attrs [ type_ "button" ] ])
        }


{-| Option to style a list item with success colors
-}
success : ItemOption msg
success =
    Internal.Roled Internal.Success


{-| Option to style a list item with info colors
-}
info : ItemOption msg
info =
    Internal.Roled Internal.Info


{-| Option to style a list item with warning colors
-}
warning : ItemOption msg
warning =
    Internal.Roled Internal.Warning


{-| Option to style a list item with danger colors
-}
danger : ItemOption msg
danger =
    Internal.Roled Internal.Danger


{-| Option to mark a list item as active
-}
active : ItemOption msg
active =
    Internal.Active


{-| Option to disable a list item
-}
disabled : ItemOption msg
disabled =
    Internal.Disabled


{-| Use this function to supply any additional Hmtl.Attribute you need for your list items
-}
attrs : List (Html.Attribute msg) -> ItemOption msg
attrs attrs =
    Internal.Attrs attrs
