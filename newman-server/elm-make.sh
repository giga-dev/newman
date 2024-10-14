#!/usr/bin/env bash
elm make --yes

elm make web/elm/src/Main.elm --output web/elm/Main.js --yes

