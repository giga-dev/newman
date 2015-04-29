[![Build Status](https://travis-ci.org/giga-dev/newman.svg?branch=master)](https://travis-ci.org/giga-dev/newman) 


# Hello, Newman...

Newman is a framework for distributing test load across multiple agents. It's named after [Newman](http://en.wikipedia.org/wiki/Newman_%28Seinfeld%29) from Seinfeld.

[online docs](http://giga-dev.github.io/newman/docs/index.html)

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