#! /bin/sh

riak-admin bucket-type create journal '{"props": {}}'
riak-admin bucket-type activate journal
riak-admin bucket-type create journal-seqnrs '{"props":{"datatype":"set"}}'
riak-admin bucket-type activate journal-seqnrs 

