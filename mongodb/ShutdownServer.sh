#!/bin/bash
#Usage: expect LogClear.sh $address $port
set address [lindex $argv 0]
set port [lindex $argv 1]

spawn ssh $address

expect ""
send "bash\n"
#为了保证能够进入bash而确认一遍(n6需要手动输入)

expect ""
send "mongo --port $port\n"

expect ""
send "use admin\n"
expect ""
send "db.auth(\"dh\",\"dh\")\n"

expect ""
send "db.shutdownServer({\"force\":true})\n"

expect ""
send "exit\n"
expect ""
send "exit\n"
expect ""
send "exit\n"

expect eof