package HistoryProducer;

import java.util.*;
import lombok.*;

//主要负责生成随机操作

//
//    int type; //0表示初始状态，1表示ok，2表示invoke
//            methods method;
//            String variable;
//            int value;
//            int process;
//            int time;
//            int position;
//            String link;
//            int index;
//

@Data
public class operationProducer {


    int globalIndex;
    int writeRate;
    int readRate;
    LinkedList<Integer> weightList; //存储读写比率的权重列表
    LinkedList<methods> ops;
    int weightSum; //按权重生成随机数辅助变量
    LinkedList<Integer> weightTemp; //按权重生成随机数辅助数据结构
    int varRange; //用数字表示生成的变量范围（默认从'a'开始，顺序往后)
    HashMap<String, Integer> varValueMap;
    int valueRange; //生成值的范围
    HashMap<String, Set<Integer>> usedValue; //标记每个变量上已经写入的值
    int processRange;
    int globalTime;
    int globalPosition;

    //默认的读写比例是1：1
    operationProducer(){

        //globalIndex从0开始计数，globalTime和globalPosition从1开始计数
        this.setGlobalIndex(0);
        this.setGlobalTime(1);
        this.setGlobalPosition(1);

        this.setWriteRate(1);
        this.setReadRate(1);
        this.weightList = new LinkedList<Integer>();
        this.setWeightSum(0);
        this.ops = new LinkedList<>();
        this.ops.add(methods.Read);
        this.ops.add(methods.Write);
        this.weightTemp = new LinkedList<>();
        this.varRange = 10; //默认生成5个变量
        this.updateWeightList();
        this.varValueMap = new HashMap<>();
        this.valueRange = 1000; //默认生成0-100的值
        this.usedValue = new HashMap<>();
        this.processRange = 5; //默认有5个线程

    }

    operationProducer(int rRate, int wRate){
        this.setGlobalIndex(0);
        this.setGlobalTime(1);
        this.setGlobalPosition(1);

        this.setWriteRate(wRate);
        this.setReadRate(rRate);
        this.weightList = new LinkedList<Integer>();
        this.setWeightSum(0);
        this.ops = new LinkedList<>();
        this.ops.add(methods.Read);
        this.ops.add(methods.Write);
        this.weightTemp = new LinkedList<>();
        this.varRange = 10; //默认生成5个变量
        this.updateWeightList();
        this.varValueMap = new HashMap<>();
        this.valueRange = 1000; //默认生成0-100的值
        this.usedValue = new HashMap<>();
        this.processRange = 5; //默认有5个线程
    }

    void updateWeightList(){
        weightList.clear();
        this.setWeightSum(0);
        weightTemp.clear();
        weightTemp.add(0);
        weightList.add(readRate);
        weightSum += readRate;
        weightTemp.add(Integer.valueOf(this.weightSum));
        weightList.add(writeRate);
        weightSum += writeRate;
        weightTemp.add(Integer.valueOf(this.weightSum));
    }


    int randomType(){ //暂时只生成type ok的操作
        return 1;
    }

    //根据制定的读写比率生成操作类型
    methods randomMethod(){
        Random random = new Random();
        int rand = random.nextInt(this.weightSum);
        int index = 0;
        for(int i = this.weightTemp.size()-1; i > 0; i--){
            if(rand >= this.weightTemp.get(i)){
                index = i;
                break;
            }
        }
        return this.ops.get(index);
    }

    String randomVar(methods type){
        String temp = "";
        Random random = new Random();
        int rand;
        /* 此部分代码：根据读写操作的不同随机生成访问的变量
        if(type == methods.Write) { //如果是写操作，根据限制的变量范围随机生成变量
            long a = 97;
            rand = random.nextInt(this.varRange);
            a = a + rand;
            temp = String.valueOf((char) a);
            if(!this.varValueMap.containsKey(temp)){ //如果没有访问过该变量，则将其放入map中
                this.varValueMap.put(temp, -1); //初始时把-1暂存入数据结构，待之后更新
            }

        }
        else if(type == methods.Read){ //如果是读操作，从已经写入的变量集合里随机挑一个读
            Set usedVar = this.varValueMap.keySet();
            rand = random.nextInt(usedVar.size());
            Iterator it = usedVar.iterator();
            int i = 0;
            while(i <= rand){
                temp = (String)it.next();
                i++;
            }
        }
        */
        long a = 97;
        rand = random.nextInt(this.varRange);
        a = a + rand;
        temp = String.valueOf((char) a);
        if(!this.varValueMap.containsKey(temp) && type == methods.Write){ //如果写操作没有访问过该变量，则将其放入map中
            this.varValueMap.put(temp, -1); //初始时把-1暂存入数据结构，待之后更新
            Set tempSet = new HashSet();
            this.usedValue.put(temp, tempSet);
        }
        return temp;
    }

    int randomValue(generatedOperation op){
        int temp = 0;
        Random random = new Random();
        if(op.getMethod() == methods.Write){ //如果是写操作，随机写入一个范围内的值
            temp = random.nextInt(this.valueRange);
            while(this.usedValue.get(op.getVariable()).contains(temp)){ //如果对同一变量写入了重复的值，则重新生成
                temp = random.nextInt(this.valueRange);
            }
            this.varValueMap.put(op.getVariable(), temp);
            Set tempSet = this.usedValue.get(op.getVariable());
            tempSet.add(temp);
            this.usedValue.put(op.getVariable(), tempSet);
        }
        else if(op.getMethod() == methods.Read){ //如果是读操作，返回该变量被写入的最新值
            if(this.varValueMap.containsKey(op.getVariable())) {
                temp = this.varValueMap.get(op.getVariable());
            }
            else{ //如果没写入过该变量，读到-1（初值）
                temp = -1;
            }
        }
        return temp;
    }

    int randomProcess(){ //将该操作随机分配到一个线程上
        int temp = 0;
        Random random = new Random();
        temp = random.nextInt(this.processRange);
        return temp;
    }


    public generatedOperation nextOperation(){

        generatedOperation op = new generatedOperation();
        op.setType(this.randomType());
        op.setIndex(this.globalIndex++);
        op.setMethod(this.randomMethod());
        op.setVariable(this.randomVar(op.getMethod()));
        op.setValue(this.randomValue(op));
        op.setProcess(this.randomProcess());
        op.setPosition(this.globalPosition++);
        op.setTime(this.globalTime++);

        return op;
    }
}
