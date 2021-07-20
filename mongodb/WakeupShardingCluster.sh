set timeout 15 #设置超时，单位秒
#Usage: ./WakeupShardingCluster.sh 需配合参数设置在alicloud_mongodb.conf中
csrs_config="configs/alicloud_mongodb_csrs.conf"
mongos_config="configs/alicloud_mongodb_mongos.conf"
shards_config="configs/alicloud_mongodb_shards.conf"

#先启动csrs
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
  filePath=${arrary[2]}
  expect WakeupServer.sh $address $filePath
  IFS=$'\n'
done
IFS=$bak                    #将环境变量IFS的值改回原值

#随后启动mongos
bak=$IFS #
if [[ ! -f $mongos_config ]];then              
  echo "the $mongos_config is not a file"
  exit
fi

IFS=$'\n'                    
for i in `cat $mongos_config`                
do
  IFS=$' '
  echo "Current Parameter:" $i
  arrary=($i)
  address=${arrary[0]}
  filePath=${arrary[2]}
  expect WakeupMongos.sh $address $filePath
  IFS=$'\n'
done
IFS=$bak                    

#最后启动shards
bak=$IFS
if [[ ! -f $shards_config ]];then               
  echo "the $shards_config is not a file"
  exit
fi

IFS=$'\n'                    
for i in `cat $shards_config`               
do
  IFS=$' '
  echo "Current Parameter:" $i
  arrary=($i)
  address=${arrary[0]}
  filePath=${arrary[2]}
  expect WakeupServer.sh $address $filePath
  IFS=$'\n'
done
IFS=$bak                    

echo "The sharding cluster is running."

