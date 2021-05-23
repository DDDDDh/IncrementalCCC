package HistoryProducer;

import java.util.HashMap;
import java.util.LinkedList;

public class LinProducer extends randomProducer{

    HashMap<String, Integer> kvMap;

    public LinProducer(){
        super();
        this.kvMap = new HashMap<>();
    }

    public LinProducer(int opNum, int processNum, int rRate, int wRate){
        super(opNum, processNum, rRate, wRate);
//        this.globalActiveVar = new HashMap<>();
        this.kvMap = new HashMap<>();



    }

    public void generateLinHistory(){

        //根据当前生成器的配置生成操作，随后用本地kvMap模拟得到返回值
        operationProducer oProducer = new operationProducer(this.getRRate(), this.getWRate());
        oProducer.setProcessRange(this.getProcessNum());
        generatedOperation curOp;
        for(int i = 0; i < this.getOpNum(); i++) {
            curOp = oProducer.nextOperation(); //先随机生成一个操作，再根据其具体情况调整
            this.opList.add(curOp);
            if(curOp.isWrite()){ //如果是写操作，写入kvMap
                this.kvMap.put(curOp.getVariable(), curOp.getValue());
            }
            else if(curOp.isRead()){ //如果是读操作，读到的应为kvMap中对应变量的值
                if(this.kvMap.containsKey(curOp.getVariable())) {
                    curOp.setValue(this.kvMap.get(curOp.getVariable()));
                }
                else{
                    curOp.setValue(-1);
                }
            }
            this.processOpList.get(curOp.getProcess()).add(curOp.getIndex());
        }

    }


}
