#!/bin/bash
#Usage: expect LogClear.sh $address $port

set timeout 15
# set n6 n6.disalg.cn
# 两种传参的方式
# spawn ssh [lrange $argv 0 0]
# expect ""
# send "mongo --port [lrange $argv 1 1]\n"
set address [lindex $argv 0]
set port [lindex $argv 1]


spawn ssh $address
expect ""
send "bash\n" #为了保证能够进入bash而确认一遍(n6需要手动输入)

#连接mongodb并认证
expect ""
send "mongo --port $port\n"
expect ""
send "use admin\n"
expect ""
send "db.auth(\"dh\",\"dh\")\n"

#将旧log另存
expect ""
send "db.runCommand({logRotate:1})\n"
expect ""
send "exit\n"

#找到目录下新生成的历史log文件并输出
expect ""
send "log=\$(find /home/huangyi/mongoDB/var/mongodb/ -name *.log.*)\n"
expect ""
send "echo \$log\n"
# expect ""
# send "rs.status()\n"

#逐个删除找到的历史log文件
# expect ""
# send "for line in \$log \n"
# expect ""
# send "do\n"
# expect ""
# send "rm -rf \$line\n"
# expect ""
# send "done\n"
expect ""
send "for line in \$log; do rm -rf \$line; done;\n"


#退出连接 结束任务
expect ""
send "exit\n"
#为了保证正常退出而多退一次(n6需要退出bash)
expect ""
send "exit\n"
expect eof


