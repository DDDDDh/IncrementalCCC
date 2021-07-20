##Aliyun sharding cluster setup



username: dh

password: dh



Config Server Replica Set (CSRS)：

CSRS-1: n0.disalg.cn:26005

CSRS-2: n2.disalg.cn:26005

CSRS-3: n4.disalg.cn:26005



Sharding Clusters:

Shard1

Shard1-1: n1.disalg.cn: 27001

Shard1-2: n2.disalg.cn: 27001

Shard1-3: n3.disalg.cn: 27001

Shard2

Shard2-1: n2.disalg.cn: 27002

Shard2-2: n3.disalg.cn: 27002

Shard2-3: n4.disalg.cn: 27002

Shard3

Shard3-1: n4.disalg.cn: 27003

Shard3-2: n6.disalg.cn: 27003

Shard3-3: n0.disalg.cn: 27003

Shard4

Shard3-1: n6.disalg.cn: 27004

Shard3-2: n0.disalg.cn: 27004

Shard3-3: n1.disalg.cn: 27004



Mongos:

Mongos_0: n0.disalg.cn: 26011

Mongos_1: n1.disalg.cn: 26011

Mongos_2: n2.disalg.cn: 26011

Mongos_3: n3.disalg.cn: 26011

Mongos_4: n4.disalg.cn: 26011

Mongos_6: n6.disalg.cn: 26011