#!/bin/bash

rm -rf elm-stuff
elm-package install --yes
elm-make

