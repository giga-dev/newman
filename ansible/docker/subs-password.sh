#!/usr/bin/env bash
# usage $0 password file
#echo "calling $0 $1 $2"
sed -i "s/<ENTER PASSWORD>/$1/g" "$2"
#grep $1 $2
