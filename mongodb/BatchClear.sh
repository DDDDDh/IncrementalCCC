set timeout 15 #设置超时，单位秒
#Usage: ./BatchClear.sh 需配合参数设置在alicloud_mongodb.conf中
config_file="alicloud_mongodb.conf"

bak=$IFS

# if [[ $config_file -ne 1 ]]; then             #判断位置参数是否为1
#   echo "Usage $0 filename"
#   exit
# fi

if [[ ! -f $config_file ]];then               #判断位置参数是否为文件
  echo "the $config_file is not a file"
  exit
fi

IFS=$'\n'                    #将环境变量IFS的值修改为换行符
for i in `cat $config_file`                #逐行读取文件内容并打印到屏幕
do
  IFS=$' '
  echo "Current Parameter:" $i
  arrary=($i)
  address=${arrary[0]}
  port=${arrary[1]}
  expect LogClear.sh $address $port
  IFS=$'\n'
done
IFS=$bak                    #将环境变量IFS的值改回原值
