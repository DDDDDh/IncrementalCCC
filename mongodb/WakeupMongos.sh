set address [lindex $argv 0]
set filePath [lindex $argv 1]

spawn ssh $address

expect ""
send "bash\n"
#为了保证能够进入bash而确认一遍(n6需要手动输入)

expect ""
send "mongos -f $filePath\n"

expect ""
send "exit\n"
expect ""
send "exit\n"

expect eof