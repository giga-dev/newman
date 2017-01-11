#!/bin/bash
export module=server
# password for user xap
export ANSIBLE_PASS_1=PASSWORD_HERE
# password for user tgrid
export ANSIBLE_PASS_2=PASSWORD_HERE
./run.sh $ANSIBLE_PASS_1 $ANSIBLE_PASS_2