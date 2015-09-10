---
layout: default
title:  How to clone mongo db
date:   2014-11-01 15:40:56
categories: doc
---
## How to clone mongodb to your local machine.

1. access the docker of the mongo
docker exec -i -t my-mongo /bin/bash

2. access mongo shell
> mongo
> use admin

3. type in the command to copy the remote database to your own (new database)

> db.copyDatabase("newman-db","my-copy-of-newman-db", "192.168.50.66");

Some Mongo UIs allow to do this using a built-in command line shell or from the menus.
e.g. robomongo has a "open shell" where you can type the above command and press Ctrl+Enter.