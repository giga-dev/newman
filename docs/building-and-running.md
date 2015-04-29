---
layout: default
title:  Newman Testing Framework
date:   2014-11-01 15:40:56
categories: doc
---

###Get the sources from github.

- Run `git clone git@github.com:giga-dev/newman.git`.


###Prerequisets.

#### MongoDb.

####In case your machine is dockerized.
  - Run `docker pull mongo`
  - Run `newman-server/mongo-restart.sh
  
####Otherwise install [mongo](https://www.mongodb.org/downloads) db on your machine
  
### UI.
- Install [node and npm](https://nodejs.org/)
- Install twitter [bower](http://bower.io/)
- Install Newman web packages: from cmd opened in the newman-server/web run `bower update`.
  
### Keys for jetty SSL.
- From cmd opened in the newman-server/bin run `keysgen.sh`.

###Compile the Java sources.
- From cmd opened in the root of the project run `mvn install`

###Running the Newman server.
- From newman-server folder run the cmd `mvn exec:java`
- Open browser at https://localhost:8443/
- Login with barak:barak or root:root
