This plugin is under development. The build is still failing. I try to fix this in the next days or so.
=======================================================================================================

[![Build Status](https://travis-ci.org/martinpallmann/akka-persistence-riak.svg?branch=master)](https://travis-ci.org/martinpallmann/akka-persistence-riak)

Riak Plugin for Akka Persistence
================================

[Akka Persistence](http://doc.akka.io/docs/akka/2.3.6/scala/persistence.html) journal store backed by [Riak](http://basho.com/riak/).

Dependency
----------

This version of `akka-persistence-riak` depends on Riak 2.0.1 and Akka 2.4.1.


Journal plugin
--------------

### Activation 

To activate the journal plugin, add the following lines to `application.conf`:

    akka.persistence.journal.plugin = "akka-persistence-riak-async.journal"
    akka-persistence-riak-async.journal.class = "docs.persistence.MyJournal"

You also need to configure the riak hosts:

    akka-persistence-riak-async.nodes = ["127.0.0.1"]
