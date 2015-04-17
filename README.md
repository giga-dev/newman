[![Build Status](https://travis-ci.org/giga-dev/newman.svg?branch=master)](https://travis-ci.org/giga-dev/newman) 


# Hello, Newman...

Newman is a framework for distributing test load across multiple agents. It's named after [Newman](http://en.wikipedia.org/wiki/Newman_%28Seinfeld%29) from Seinfeld.

[online docs](http://giga-dev.github.io/newman/docs/index.html)


## Quick start.

1. git clone git@github.com:giga-dev/newman.git.
2. generate keys `(cd newman-server/bin/keysgen.sh)`.
3. run the server using maven, from newman-server type `mvn exec:java`.
4. run the client using maven, from newman-client type `mvn exec:exec`.


## Update the online docs.

1. git clone git@github.com:giga-dev/newman.git gh-pages.
2. cd gh-pages.
3. git branch gh-pages.
4. From there it is just simple [jekyll](http://jekyllrb.com/) project.
    * Run the preprocessor with `jekyll serve`.
    * Edit commit and push, Github will process the file and update the view.