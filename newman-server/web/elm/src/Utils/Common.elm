module Utils.Common exposing (..)

import DateFormat


dayTimeStringFormat : String
dayTimeStringFormat =
    "%b %d, %H:%M:%S"


dateTimeDateFormat : List DateFormat.Token
dateTimeDateFormat =
    [ DateFormat.monthNameFirstThree
    , DateFormat.text " "
    , DateFormat.dayOfMonthNumber
    , DateFormat.text ", "
    , DateFormat.hourMilitaryNumber
    , DateFormat.text ":"
    , DateFormat.minuteFixed
    , DateFormat.text ":"
    , DateFormat.secondFixed
    ]


hourStringFormat : String
hourStringFormat =
    "%H:%M:%S"
