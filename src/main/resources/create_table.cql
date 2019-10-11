CREATE KEYSPACE mykeyspace WITH REPLICATION = {
   'class' : 'NetworkTopologyStrategy',
   'datacenter1' : 1
  } ;

CREATE TABLE IF NOT EXISTS mykeyspace.person (
    email text,
    firstName text,
    lastName text,
    yearBirth int,
    PRIMARY KEY ((email))
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };