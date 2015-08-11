#!/bin/bash
cd $1
newest=$(ls -t | grep 1 | head -1)
echo "${newest}"