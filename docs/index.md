---
layout: default
title:  async rmi docs
date:   2014-11-01 15:40:56
categories: doc
---

##Introduction

1. [why did i start this project](why-did-i-start-this-project.html).
2. [rmi pros](rmi-pros.html).

[Async RMI in 10 Minutes](10min-start.html).

## Async RMI Feature set.
<ul class="features">

    <li>Asynchronous calls [<a href="asynchronous-calls.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/futures">example</a>].</li>
    <ul class="features">
        <li>Java 8 <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html">CompletableFuture</a> as remote futures.</li>
        <li>Client timeout [<a href="client-timeout.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/timeout">example</a>].</li>
        <li>One way calls [<a href="oneway-calls.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/oneway">example</a>].</li>
        <li>Cancellation of remote future [<a href="cancellation-of-remote-future.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/blob/master/src/test/java/org/async/rmi/CancelingRemoteFutureTest.java">example</a>].</li>
    </ul>

    <li class="notready">Configurable thread policies [<a href="threads.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/oneway">example</a>].</li>

    <li>Dynamic class loading [<a href="dynamic-class-loading.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/dcl">example</a>].</li>

    <li>Ease of use.</li>
    <ul class="features">
        <li>Maven build [<a href="building.html">documentation</a>].</li>
        <li>Use Oracle RMI marker interfaces, Remote, RemoteException.</li>
        <li>Logging [<a href="logging.html">documentation</a>].</li>
        <li>Mo code generation.</li>
        <li>Support Closures and Streams.</li>
        <li class="notready">Work behind firewalls</li>
        <li>Automatic exporting remote objects [<a href="automatic-exporting.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/dcl">example</a>].</li>
    </ul>

    <li>Client connection pool</li>
    <li>Requests pipeline [<a href="request-pipeline.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/pipeline">example</a>].</li>
    <li>Configuration File[<a href="configuration.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/blob/master/example/ssl.server.config.yml">example</a>].</li>

    <li>Networking</li>
    <ul class="features">
        <li>Unpublish [<a href="unpublish.html">documentation</a>].</li>
        <li>Selecting network interface [<a href="selecting-network-interface.html">documentation</a>].</li>
        <li>Handshake [<a href="handshake.html">documentation</a>].</li>
        <li>Encryption [<a href="tls.html">documentation</a>] [<a href="https://github.com/barakb/asyncrmi/tree/master/example/src/main/java/org/async/example/ssl">example</a>].</li>
        <li>Closing proxy [<a href="closing-client.html">closing-client</a>] [<a href="https://github.com/barakb/asyncrmi/blob/master/src/test/java/org/async/rmi/CloseTest.java">example</a>].</li>
        <li>Redirect proxy [<a href="redirect-proxy.html">redirect client</a>] [<a href="https://github.com/barakb/asyncrmi/blob/master/src/test/java/org/async/rmi/RedirectTest.java">example</a>].</li>
    </ul>

</ul>
