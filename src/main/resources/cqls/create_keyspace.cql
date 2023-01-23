CREATE KEYSPACE IF NOT EXISTS mykeyspace WITH REPLICATION = {
   'class' : 'NetworkTopologyStrategy',
   'datacenter1' : 1
  } ;