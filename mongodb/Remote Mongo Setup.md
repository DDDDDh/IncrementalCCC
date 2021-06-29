Mongo Setup(in 114.212.82.99)

Part One：配置单机副本集

1、下载好对应系统及合适版本的安装包；

​	(https://www.mongodb.com/try/download/community)

2、修改.bashrc文件：

​		export PATH=<mongo-install-directory>/bin:$PATH

​		（远程登录可能不会自动加载该文件，可以手动输入bash）

3、创建数据及日志目录：

​		mkdir -p /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/node1

​		mkdir -p /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/node2

​		mkdir -p /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/node3

4、创建keyfile

​		mkdir -p /home/oy/DH/mongoDB/MongoInstance/var/mongodb/pki	

​		openssl rand -base64 741 > /home/oy/DH/mongoDB/MongoInstance/var/mongodb/pki/dh-keyfile

​		修改权限：chmod 400 /home/oy/DH/mongoDB/MongoInstance/var/mongodb/pki/dh-keyfile

5、创建config文件，并复制到node2.conf、node3.conf

​		/home/oy/DH/mongoDB/MongoInstance/configs/node1.conf

storage:

  dbPath: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/node1

net:

  bindIp: 114.212.82.99,localhost

  port: 27011

security:

  authorization: enabled

  keyFile: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/pki/dh-keyfile

systemLog:

  destination: file

  path: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/node1/mongod.log

  logAppend: true

processManagement:

  fork: true

replication:

  replSetName: dh-example



6、使用config文件启动mongod:

​		mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/node1.conf (19453)

​		mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/node2.conf (19662)

​		mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/node3.conf (19724)



7、连接node1并初始化副本集：

​		mongo --port 27011 

​		>rs.initiate()



8、创建用户

​		>use admin

​		>db.createUser({user:"dh",pwd:"dh",roles:[{role:"root",db:"admin"}]})



9、退出后以新建的用户身份登入mongo shell

​		>exit

​		>mongo --host "dh-example/114.212.82.99:27011" -u "dh" -p "dh" --authenticationDatabase "admin"



10、添加其他副本节点

​		>rs.add("114.212.82.99:27012")

​		>rs.add("114.212.82.99:27013")



11、使用mongo shell远程登录测试：

​		mongo admin --username dh --host 114.212.82.99 --port 27011 --password dh

​		或者可以使用--eval 运行一段mongodb脚本：

​		mongo admin --username dh --host 114.212.82.99 --port 27011 --password dh --eval 'rs.status()'

​		(--eval <javascript>)

​		当指定了replica set name时，不论指定那个端口，都会自动连上当前的primary节点：

​		例：当primary为27011时，执行mongo admin --username dh --host "dh-example/114.212.82.99:27013" --password dh后，仍会连接上27011。如果需要连接指定的secondary node，则不能指定replica set name。



Part Two：配置单机分片集群

1、按照上一步的模式，建立好CSRS(config server replica set)：

​		配置文件：csrs1.conf:

sharding:

  clusterRole: configsvr

replication:

  replSetName: dh-csrs

security:

  keyFile: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/pki/dh-keyfile

net:

  bindIp: localhost,114.212.82.99

  port: 26011

systemLog:

  destination: file

  path: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/csrs1/csrs1.log

  logAppend: true

processManagement:

  fork: true

storage:

  dbPath: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/csrs1

1.1 启动线程

​		mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/csrs1.conf (7031)

​		mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/csrs2.conf (7106)

​		mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/csrs3.conf (7173)

1.2 连接csrs1并初始化副本集：

​		mongo --port 26011 

​		>rs.initiate()

1.3 创建用户

​		>use admin

​		>db.createUser({user:"dh",pwd:"dh",roles:[{role:"root",db:"admin"}]})

1.4 用刚创建的用户认证登陆

​		>db.auth("dh", "dh")

1.5 添加其他节点

​		>rs.add("114.212.82.99:26012")

​		>rs.add("114.212.82.99:26013")

2、启动mongos；

2.1 准备好mongos配置文件mongos.conf:

sharding:

  configDB: dh-csrs/114.212.82.99:26011,114.212.82.99:26012,114.212.82.99:26013

security:

  keyFile: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/pki/dh-keyfile

net:

  bindIp: localhost,114.212.82.99

  port: 26000

systemLog:

  destination: file

  path: /home/oy/DH/mongoDB/MongoInstance/var/mongodb/db/mongos/mongos.log

  logAppend: true

processManagement:

  fork: true

2.2 启动mongos线程

​	> mongos -f /home/oy/DH/mongoDB/MongoInstance/configs/mongos.conf（9387）

2.3 连接到mongos：

​	>mongo --port 26000 --username dh --password dh --authenticationDatabase admin

2.4 检查分片集群状态：

​	>sh.status()

3、将原有副本集设置为分片

3.1 连接到任一secondary node：

​	>mongo --port 27012 -u "dh" -p "dh" --authenticationDatabase "admin"

​	>use admin

​	>db.shutdownServer()

3.2 修改配置文件node2.conf，添加如下语句：

​		sharding:

​			clusterRole: shardsvr

3.3 重新启动该节点

​	>mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/node2.conf（17094）

3.4 类似重启node3：

​	>mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/node3.conf (11158)

3.5 连接主节点：

​	>mongo --port 27011 -u "dh" -p "dh" --authenticationDatabase "admin"

3.6 使其退化为从节点：

​	>rs.stepDown()

3.7 修改配置并重启node1

​	>mongod -f /home/oy/DH/mongoDB/MongoInstance/configs/node1.conf (11544)

3.8 重新连接到mongos：

​	>mongo --port 26000 --username dh --password dh --authenticationDatabase admin

3.9 添加分片：

​	>sh.addShard("dh-example/114.212.82.99:27011")

3.10 检查分片集群状态：

​	>sh.status()

3.11 开启负载均衡：

​	>sh.startBalancer()
