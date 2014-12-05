#! /bin/sh

sudo riak-admin bucket-type create journal '{"props": {}}'
sudo riak-admin bucket-type activate journal
sudo riak-admin bucket-type create journal-seqnrs '{"props":{"datatype":"set"}}'
sudo riak-admin bucket-type activate journal-seqnrs 

