[![Build Status](https://travis-ci.org/giga-dev/newman.svg?branch=master)](https://travis-ci.org/giga-dev/newman) 


# Hello, Newman...

Newman is a framework for distributing test load across multiple agents. It's named after [Newman](http://en.wikipedia.org/wiki/Newman_%28Seinfeld%29) from Seinfeld.

[online docs](http://giga-dev.github.io/newman/docs/index.html)


## How to build and run (only 7 very small and very simple steps :worried: ).

1. Run `git clone git@github.com:giga-dev/newman.git`.
2. Generate the server private/public keys with the command `(cd newman-server/bin/keysgen.sh)`.
3. Download the web dependencies:
    - The web dependencies are managed using [bower](http://bower.io/) a package manager for the web download and install it.
    - open cmd at the newman-server/web folder and type `bower update`
4. Install and run [Mongodb](https://www.mongodb.org/) on your local machine.
   Alternatively you can install mongo in a docker container using `docker pull mongo` and then use the `newman-server/mongo-restart.sh` file to start it. 
5. Run the server using maven, from newman-server type `mvn exec:java`.
6. Run the client using maven, from newman-client type `mvn exec:exec`.
7. Open browser at https://localhost:8443/

## Update the online docs.

1. Download the docs using the cmd `git clone git@github.com:giga-dev/newman.git gh-pages`.
2. Change directory to the docs dir with the command `cd gh-pages`.
3. Change the branch to the docs branch dir with the command `git branch gh-pages`.
4. From there it is just simple [jekyll](http://jekyllrb.com/) project.
    * run the preprocessor with `jekyll serve`.
    * edit commit and push, Github will process the file and update the view.
    
## Newman High Level Diagram.

![Newman-High-Level.jpg](/docs/diagrams/Newman-High-Level.jpg)

## Newman Agent Flow.

![Newman-Agent-State-Machine.jpg](/docs/diagrams/Newman-Agent-State-Machine.jpg)