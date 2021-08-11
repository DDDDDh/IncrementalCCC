set timeout 15 #设置超时，单位秒
#Usage: ./ShutdownTestShardingCluster.sh 需配合参数设置在.conf文件中
#为了配合java调用，此处使用绝对路径
csrs_config="/Users/yi-huang/Project/IncrementalCCC/mongodb/configs/alicloud_mongodb_csrs.conf"
mongos_config="/Users/yi-huang/Project/IncrementalCCC/mongodb/configs/alicloud_mongodb_mongos.conf"
shards_config="/Users/yi-huang/Project/IncrementalCCC/mongodb/configs/alicloud_mongodb_shards.conf"


#先关闭shards
bak=$IFS
if [[ ! -f $shards_config ]];then               #判断位置参数是否为文件
  echo "the $shards_config is not a file"
  exit
fi
IFS=$'\n'                    #将环境变量IFS的值修改为换行符
for i in `cat $shards_config`                #逐行读取文件内容并打印到屏幕
do
  IFS=$' '
  echo "Current Parameter:" $i
  arrary=($i)
  address=${arrary[0]}
  port=${arrary[1]}
  expect ShutdownServer.sh $address $port
  IFS=$'\n'
done
IFS=$bak                    #将环境变量IFS的值改回原值

#随后关闭mongos
bak=$IFS #
if [[ ! -f $mongos_config ]];then              #判断位置参数是否为文件
  echo "the $mongos_config is not a file"
  exit
fi
IFS=$'\n'                    #将环境变量IFS的值修改为换行符
for i in `cat $mongos_config`                #逐行读取文件内容并打印到屏幕
do
  IFS=$' '
  echo "Current Parameter:" $i
  arrary=($i)
  address=${arrary[0]}
  port=${arrary[1]}
  expect ShutdownServer.sh $address $port
  IFS=$'\n'
done
IFS=$bak                    #将环境变量IFS的值改回原值


#最后关闭csrs
bak=$IFS
if [[ ! -f $csrs_config ]];then               #判断位置参数是否为文件
  echo "the $csrs_config is not a file"
  exit
fi
IFS=$'\n'                    #将环境变量IFS的值修改为换行符
for i in `cat $csrs_config`                #逐行读取文件内容并打印到屏幕
do
  IFS=$' '
  echo "Current Parameter:" $i
  arrary=($i)
  address=${arrary[0]}
  port=${arrary[1]}
  expect ShutdownServer.sh $address $port
  IFS=$'\n'
done
IFS=$bak                    #将环境变量IFS的值改回原值

echo "The cluster is shut down."
