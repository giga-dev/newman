module Tests exposing (suite)

import Expect exposing (Expectation)
import Test exposing (..)
import Date exposing (Date)
import DateFormat
import Fuzzers


atLeast : Int -> DateFormat.Token -> Date -> Expectation
atLeast min token someDate =
    let
        formattedDate =
            DateFormat.format [ token ] someDate
    in
        stringAtLeast min formattedDate


withinRange : Int -> Int -> DateFormat.Token -> Date -> Expectation
withinRange min max token someDate =
    let
        formattedDate =
            DateFormat.format [ token ] someDate
    in
        stringWithinRange min max formattedDate


fixedWithinRange : Int -> Int -> Int -> DateFormat.Token -> Date -> Expectation
fixedWithinRange len min max token someDate =
    let
        formattedDate =
            DateFormat.format [ token ] someDate
    in
        Expect.all
            [ stringWithinRange min max
            , isLength len
            ]
            formattedDate


formatsWithLength : Int -> DateFormat.Token -> Date -> Expectation
formatsWithLength len token someDate =
    DateFormat.format [ token ] someDate
        |> isLength len


isLength : Int -> String -> Expectation
isLength len str =
    Expect.equal len (String.length str)


stringAtLeast : Int -> String -> Expectation
stringAtLeast min string =
    case String.toInt string of
        Ok int ->
            Expect.atLeast min int

        Err _ ->
            Expect.fail "Doesn't return a number"


stringWithinRange : Int -> Int -> String -> Expectation
stringWithinRange min max string =
    case String.toInt string of
        Ok int ->
            int
                |> Expect.all
                    [ Expect.atLeast min
                    , Expect.atMost max
                    ]

        Err _ ->
            Expect.fail "Doesn't return a number"


suffixWithinRange : Int -> Int -> DateFormat.Token -> Date -> Expectation
suffixWithinRange min max token someDate =
    let
        result =
            DateFormat.format [ token ] someDate
    in
        if endsInSuffix result then
            stringWithinRange min max (String.dropRight 2 result)
        else
            Expect.fail "Doesn't end in a suffix."


endsInSuffix : String -> Bool
endsInSuffix string =
    let
        lastTwoLetters =
            String.right 2 string
    in
        List.member lastTwoLetters [ "st", "nd", "rd", "th" ]


suite : Test
suite =
    describe "The DateFormat Module"
        [ -- Month
          describe "monthNumber"
            [ fuzz Fuzzers.datetime "Is between 1 and 12" <|
                withinRange 1 12 DateFormat.monthNumber
            ]
        , describe "monthSuffix"
            [ fuzz Fuzzers.datetime "Is between 1 and 12" <|
                suffixWithinRange 1 12 DateFormat.monthSuffix
            ]
        , describe "monthFixed"
            [ fuzz Fuzzers.datetime "Is between 1 and 12" <|
                fixedWithinRange 2 1 12 DateFormat.monthFixed
            ]
        , describe "monthNameFirstThree"
            [ fuzz Fuzzers.datetime "Is always three characters long" <|
                formatsWithLength 3 DateFormat.monthNameFirstThree
            ]
        , describe "monthNameFull"
            [ fuzz Fuzzers.datetime "Starts with first three" <|
                (\date ->
                    let
                        firstThree =
                            DateFormat.format
                                [ DateFormat.monthNameFirstThree ]
                                date

                        fullName =
                            DateFormat.format
                                [ DateFormat.monthNameFull ]
                                date
                    in
                        Expect.equal
                            (firstThree)
                            (String.left 3 fullName)
                )
            , fuzz Fuzzers.datetime "Is a valid month" <|
                (\date ->
                    let
                        validMonths =
                            [ "January"
                            , "February"
                            , "March"
                            , "April"
                            , "May"
                            , "June"
                            , "July"
                            , "August"
                            , "September"
                            , "October"
                            , "November"
                            , "December"
                            ]

                        monthName =
                            DateFormat.format [ DateFormat.monthNameFull ] date
                    in
                        Expect.equal
                            True
                            (List.member monthName validMonths)
                )
            ]

        -- Day of Month
        , describe "dayOfMonthNumber"
            [ fuzz Fuzzers.datetime "Is between 1 and 31" <|
                withinRange 1 31 DateFormat.dayOfMonthNumber
            ]
        , describe "dayOfMonthSuffix"
            [ fuzz Fuzzers.datetime "Is between 1 and 31" <|
                suffixWithinRange 1 31 DateFormat.dayOfMonthSuffix
            ]
        , describe "dayOfMonthFixed"
            [ fuzz Fuzzers.datetime "Is between 1 and 31" <|
                fixedWithinRange 2 1 31 DateFormat.dayOfMonthFixed
            ]

        -- Day of Year
        , describe "dayOfYearNumber"
            [ fuzz Fuzzers.datetime "Is between 1 and 365" <|
                withinRange 1 365 DateFormat.dayOfYearNumber
            ]
        , describe "dayOfYearSuffix"
            [ fuzz Fuzzers.datetime "Is between 1 and 365" <|
                suffixWithinRange 1 365 DateFormat.dayOfYearSuffix
            ]
        , describe "dayOfYearFixed"
            [ fuzz Fuzzers.datetime "Is between 1 and 365" <|
                fixedWithinRange 3 1 365 DateFormat.dayOfYearFixed
            ]

        -- Day of Week
        , describe "dayOfWeekNumber"
            [ fuzz Fuzzers.datetime "Is between 0 and 6" <|
                withinRange 0 6 DateFormat.dayOfWeekNumber
            ]
        , describe "dayOfWeekSuffix"
            [ fuzz Fuzzers.datetime "Is between 0 and 6" <|
                suffixWithinRange 0 6 DateFormat.dayOfWeekSuffix
            ]
        , describe "dayOfWeekNameFirstTwo"
            [ fuzz Fuzzers.datetime "Is always two characters long" <|
                formatsWithLength 2 DateFormat.dayOfWeekNameFirstTwo
            ]
        , describe "dayOfWeekNameFirstThree"
            [ fuzz Fuzzers.datetime "Is always three characters long" <|
                formatsWithLength 3 DateFormat.dayOfWeekNameFirstThree
            ]
        , describe "dayOfWeekNameFull"
            [ fuzz Fuzzers.datetime "Starts with first two" <|
                (\date ->
                    let
                        firstTwo =
                            DateFormat.format
                                [ DateFormat.dayOfWeekNameFirstTwo ]
                                date

                        fullName =
                            DateFormat.format
                                [ DateFormat.dayOfWeekNameFull ]
                                date
                    in
                        Expect.equal
                            (firstTwo)
                            (String.left 2 fullName)
                )
            , fuzz Fuzzers.datetime "Starts with first three" <|
                (\date ->
                    let
                        firstThree =
                            DateFormat.format
                                [ DateFormat.dayOfWeekNameFirstThree ]
                                date

                        fullName =
                            DateFormat.format
                                [ DateFormat.dayOfWeekNameFull ]
                                date
                    in
                        Expect.equal
                            (firstThree)
                            (String.left 3 fullName)
                )
            , fuzz Fuzzers.datetime "Is a valid month" <|
                (\date ->
                    let
                        validWeekdays =
                            [ "Monday"
                            , "Tuesday"
                            , "Wednesday"
                            , "Thursday"
                            , "Friday"
                            , "Saturday"
                            , "Sunday"
                            ]

                        dayOfWeekName =
                            DateFormat.format [ DateFormat.dayOfWeekNameFull ] date
                    in
                        Expect.equal
                            True
                            (List.member dayOfWeekName validWeekdays)
                )

            -- Year
            , describe "yearNumberLastTwo"
                [ fuzz Fuzzers.datetime "Is between 00 and 99" <|
                    fixedWithinRange 2 0 99 DateFormat.yearNumberLastTwo
                ]
            , describe "yearNumber"
                [ fuzz Fuzzers.datetime "Is at least 1970" <|
                    atLeast 1970 DateFormat.yearNumber
                ]

            -- Quarter
            , describe "quarterNumber"
                [ fuzz Fuzzers.datetime "Is between 1 and 4" <|
                    withinRange 1 4 DateFormat.quarterNumber
                ]
            , describe "quarterSuffix"
                [ fuzz Fuzzers.datetime "Is between 1 and 4" <|
                    suffixWithinRange 1 4 DateFormat.quarterSuffix
                ]

            -- Week of Year
            , describe "weekOfYearNumber"
                [ fuzz Fuzzers.datetime "Is between 1 and 52" <|
                    withinRange 1 52 DateFormat.weekOfYearNumber
                ]
            , describe "weekOfYearSuffix"
                [ fuzz Fuzzers.datetime "Is between 1 and 52" <|
                    suffixWithinRange 1 52 DateFormat.weekOfYearSuffix
                ]
            , describe "weekOfYearFixed"
                [ fuzz Fuzzers.datetime "Is between 1 and 52" <|
                    fixedWithinRange 2 1 52 DateFormat.weekOfYearFixed
                ]
            ]

        -- Week of Year
        , describe "amPmUppercase"
            [ fuzz Fuzzers.datetime "Is either AM or PM" <|
                (\date ->
                    let
                        value =
                            DateFormat.format [ DateFormat.amPmUppercase ] date
                    in
                        Expect.equal
                            True
                            (List.member
                                value
                                [ "AM", "PM" ]
                            )
                )
            ]
        , describe "amPmLowercase"
            [ fuzz Fuzzers.datetime "Is either am or pm" <|
                (\date ->
                    let
                        value =
                            DateFormat.format [ DateFormat.amPmLowercase ] date
                    in
                        Expect.equal
                            True
                            (List.member
                                value
                                [ "am", "pm" ]
                            )
                )
            ]

        -- Military Hour
        , describe "hourMilitaryNumber"
            [ fuzz Fuzzers.datetime "Is between 0 and 23" <|
                withinRange 0 23 DateFormat.hourMilitaryNumber
            ]
        , describe "hourMilitaryFixed"
            [ fuzz Fuzzers.datetime "Is between 0 and 23" <|
                fixedWithinRange 2 0 23 DateFormat.hourMilitaryFixed
            ]

        -- Hour
        , describe "hourNumber"
            [ fuzz Fuzzers.datetime "Is between 1 and 12" <|
                withinRange 1 12 DateFormat.hourNumber
            ]
        , describe "hourFixed"
            [ fuzz Fuzzers.datetime "Is between 1 and 12" <|
                fixedWithinRange 2 1 12 DateFormat.hourFixed
            ]

        -- Military Hour
        , describe "hourMilitaryFromOneNumber"
            [ fuzz Fuzzers.datetime "Is between 1 and 24" <|
                withinRange 1 24 DateFormat.hourMilitaryFromOneNumber
            ]
        , describe "hourMilitaryFromOneFixed"
            [ fuzz Fuzzers.datetime "Is between 1 and 24" <|
                fixedWithinRange 2 1 24 DateFormat.hourMilitaryFromOneFixed
            ]

        -- Hour
        , describe "minuteNumber"
            [ fuzz Fuzzers.datetime "Is between 0 and 59" <|
                withinRange 0 59 DateFormat.minuteNumber
            ]
        , describe "minuteFixed"
            [ fuzz Fuzzers.datetime "Is between 0 and 59" <|
                fixedWithinRange 2 0 59 DateFormat.minuteFixed
            ]

        -- Hour
        , describe "secondNumber"
            [ fuzz Fuzzers.datetime "Is between 0 and 59" <|
                withinRange 0 59 DateFormat.secondNumber
            ]
        , describe "secondFixed"
            [ fuzz Fuzzers.datetime "Is between 0 and 59" <|
                fixedWithinRange 2 0 59 DateFormat.secondFixed
            ]
        , describe "text"
            [ fuzz Fuzzers.datetimeAndString "Passes through any string" <|
                (\( date, someString ) ->
                    Expect.equal
                        someString
                        (DateFormat.format [ DateFormat.text someString ] date)
                )
            ]
        ]
