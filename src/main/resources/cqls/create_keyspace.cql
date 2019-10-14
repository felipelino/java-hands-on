CREATE KEYSPACE mykeyspace WITH REPLICATION = {
   'class' : 'NetworkTopologyStrategy',
   'datacenter1' : 1
  } ;