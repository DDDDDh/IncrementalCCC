# IncrementalCCC
Incremental Causal Consistency Checking Algorithm in Java Implementation



## 输入格式

```
例：{:type :ok, :f :write, :value [b 1], :process 3, :time 19, :position 19, :link nil, :index 18}
```

- :type

  表示该操作的类型，ok为成功返回的操作，invoke为只调用未能正确返回的操作，info为返回系统信息的操作；

- :f

  表示执行的操作类型，write为写操作，read为读操作；（对于read-write history而言，只考虑这两种操作）

- :value

  表示操作的执行参数，value[b 1]表示对变量b执行了值为1的操作；

- :process

  表示操作执行的线程id；

- :time

  表示操作执行的时刻；

- :position

  时钟的一种，暂时用不到；

- :link

  表示在某个causal session里的上一个操作position；

- :index

  表示操作下标，用于唯一全局标识某个操作，类似于ar序的作用；





### 说明

IncrementalCausalOrder中的函数只支持输入为DAG图的情况，否则，即表示输入的运行记录中存在CyclicCO的情况，不符合Causal的规约，直接返回不满足，无需进行下一步计算。（也即，在这样的情况下，后续计算出的邻接矩阵与传递闭包计算的结果肯定是不等的）







