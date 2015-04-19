---
layout: default
title:  Newman Testing Framework
date:   2014-11-01 15:40:56
categories: doc
---

## Hello, Newman...

Newman is a framework for distributing test load across multiple agents. It's named after [Newman](http://en.wikipedia.org/wiki/Newman_%28Seinfeld%29) from Seinfeld.

## How to build and run.

1. Run `git clone git@github.com:giga-dev/newman.git`.
2. Generate the server private/public keys with the command `(cd newman-server/bin/keysgen.sh)`.
3. Run the server using maven, from newman-server type `mvn exec:java`.
4. Run the client using maven, from newman-client type `mvn exec:exec`.

## Newman High Level Diagram.

![Newman-High-Level.jpg](/newman/images/Newman-High-Level.jpg)

## Newman Agent Flow.

![Newman-Agent-State-Machine.jpg](/newman/images/Newman-Agent-State-Machine.jpg)
