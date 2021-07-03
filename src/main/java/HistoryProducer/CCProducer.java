package HistoryProducer;

import java.util.*;
import lombok.*;

@Data
public class CCProducer extends RandomProducer {

//    HashMap<String, LinkedList> globalActiveVar;
    HashMap<Integer, HashMap<String, Integer>> processVarValue; //存储每个线程上为某个变量最新写入的写操作index


    public CCProducer(){
        super();
//        this.globalActiveVar = new HashMap<>();
        this.processVarValue = new HashMap<>();
    }

    public CCProducer(int opNum, int processNum, int rRate, int wRate, int varRange, int valRange){
        super(opNum, processNum, rRate, wRate, varRange, valRange);
//        this.globalActiveVar = new HashMap<>();
        this.processVarValue = new HashMap<>();
        this.setProcessNum(processNum);
        this.setVarRange(varRange);
        this.setValRange(valRange);
        HashMap<String, Integer> tempMap;
        LinkedList<Integer> tempList;
        for(int i = 0; i < this.getProcessNum(); i++){
            tempMap = new HashMap<>();
            tempList = new LinkedList<Integer>();
            this.getProcessVarValue().put(i, tempMap);
            this.getProcessOpList().put(i, tempList);
        }
    }

    public int maxWriteIndexOfCausalPast(HashSet<Integer> causalPast, String var){
        int maxIndex = -2;
        GeneratedOperation tempOp;
        GeneratedOperation maxWrite;
        for(Integer i : causalPast){
            tempOp = this.opList.get(i);
            if(tempOp.getVariable().equals(var) && tempOp.isWrite()){ //在causalPast中有对应变量的写操作
                if(maxIndex == -2){ //如果没更新过，则直接更新
                    maxIndex = tempOp.getIndex();
                }
                else{ //否则比较找到"最新"的那一个
                    maxWrite = this.opList.get(maxIndex);
                    if(tempOp.causallyLaterThan(maxWrite)){ //如果tempOp causal order位于maxWrite之后，直接更新
                        maxIndex = tempOp.getIndex();
                    }
                    else if(maxWrite.causallyLaterThan(tempOp)){
                        //如果maxWrite位于tempOp之后，无需更新
                    }
                    else{//当两个操作无causal order联系的时候，暂时的策略是取index更大的那一个
                        if(tempOp.getIndex() > maxIndex){
                            maxIndex = tempOp.getIndex();
                        }
                    }
                }

            }
        }
        return  maxIndex;
    }

    public boolean InCausalPast(HashSet<Integer> causalPast, int targetIndex){

        GeneratedOperation tempOp;
        for(Integer i: causalPast){
            tempOp = this.opList.get(i);
            if(tempOp.isWrite() && tempOp.getCausalPast().contains(targetIndex)){ //不能在causalPast中任一写操作的causalPast里，否则会引发WriteCORead
                return true;
            }
        }
        return false;
    }

//    public void printToFileDebug(int mode)throws FileNotFoundException {
//        String debugPath = this.getOutputPath().replace(".edn", "_debug.edn");
//        File outfile = new File(debugPath);
//        PrintWriter output = new PrintWriter(outfile);
//        generatedOperation tempOp;
//        LinkedList<Integer> tempOpList;
//        String tempStr = "";
//        if(mode == 1) {
//            for (Integer i : this.processOpList.keySet()) {
//                tempStr = "Process " + i + ": ";
//                tempOpList = this.processOpList.get(i);
//                for (int j = 0; j < tempOpList.size(); j++) {
//                    tempOp = this.opList.get(tempOpList.get(j));
//                    tempStr += tempOp.easyPrint() + "; ";
//                }
//                output.println(tempStr);
//            }
//        }
//        else if(mode == 2){ //用于输出PRAM算法能够处理的trace
//            for(Integer i : this.processOpList.keySet()){
//                tempStr = "";
//                tempOpList = this.processOpList.get(i);
//                for(int j = 0; j < tempOpList.size(); j++){
//                    tempOp = this.opList.get(tempOpList.get(j));
//                    if(tempOp.getValue()!=-1){ //忽略读初值
//                        tempStr += tempOp.printBare() + " ";
//                    }
//                }
//                output.println(tempStr);
//            }
//
//        }
//        output.close();
//
//    }

    public void generateCCHistory(){

        //根据当前生成器的配置生成操作
        OperationProducer oProducer = new OperationProducer(this.getRRate(), this.getWRate(), this.getVarRange(), this.getValRange(), this.getProcessNum());
//        oProducer.setProcessRange(this.getProcessNum());

        GeneratedOperation curOp;
        int processID;
        GeneratedOperation lastSameProcessOp;
        int processOpNum;
        LinkedList<Integer> tempList = new LinkedList<Integer>();
        GeneratedOperation latestWrite;
        GeneratedOperation tempOp;

        for(int i = 0; i < this.getOpNum(); i++){

            curOp = oProducer.nextOperation(); //先随机生成一个操作，再根据其具体情况调整
            this.opList.add(curOp);
            processID = curOp.getProcess();
            if(curOp.isWrite()){ //如果生成的是一个写操作，则根据其同线程前一操作更新因果历史，并放入
//                if(!this.globalActiveVar.containsKey(curOp.getVariable())) {
//                    tempList = new LinkedList<Integer>();
//                    tempList.add(processID);
//                    this.globalActiveVar.put(curOp.getVariable(), tempList);
//                }
//                else{
//                    this.globalActiveVar.get(curOp.getVariable()).add(processID);
//                }

                if(processOpList.get(processID).isEmpty()){ //如果该写操作是对应线程的第一个操作，直接加入
                    this.processVarValue.get(processID).put(curOp.getVariable(), curOp.getIndex());
                    this.processOpList.get(processID).add(curOp.getIndex());
                    curOp.updateCausalPast(curOp); //将其本身加入其causalPast中

                }
                else{ //否则根据前一操作更新causalHist之后加入
                    processOpNum = processOpList.get(processID).size();
                    lastSameProcessOp = this.opList.get((int)processOpList.get(processID).get(processOpList.get(processID).size()-1));
                    curOp.updateCausalPast(lastSameProcessOp);
                    this.processVarValue.get(processID).put(curOp.getVariable(), curOp.getIndex());
                    this.processOpList.get(processID).add(curOp.getIndex());
                }

            }
            //重点在于，如何给读操作生成一个合理的符合co的随机返回值
            else if(curOp.isRead()){
                curOp.setValue(-2); //-2表示未经处理的读入值
                Random random = new Random();
                int randProcess = random.nextInt(this.getProcessNum());
                int maxWriteIndex= -2; //表示causalPast中关于同一变量的最后一个写的下标
                //如果不是该线程的第一个操作，先根据lastSameProcessOp更新当前操作的causalPast
                if(!processOpList.get(processID).isEmpty()) {
                    lastSameProcessOp = this.opList.get((int) processOpList.get(processID).getLast());
                    curOp.updateCausalPast(lastSameProcessOp);
                    maxWriteIndex = this.maxWriteIndexOfCausalPast(curOp.getCausalPast(), curOp.getVariable());
                }


                //到随机生成的Process里找对应变量的写操作
                if(this.processVarValue.get(randProcess).containsKey(curOp.getVariable())) { //有相关的写操作
                    latestWrite = this.opList.get(this.processVarValue.get(randProcess).get(curOp.getVariable()));
                    //如果CausalPast中有相关变量的写操作，则视情况更新
//                    System.out.println("latestWrite:" + latestWrite);
                    if(maxWriteIndex != -2) {
                        GeneratedOperation maxWrite = this.opList.get(maxWriteIndex);
//                        System.out.println("maxWrite:" + maxWrite);
                        if (InCausalPast(curOp.getCausalPast(), latestWrite.getIndex())){ //如果随机生成的写操作在当前操作的causalPast中,则不能读到该值，改为读到maxWrite
                            latestWrite = maxWrite;
//                            System.out.println("Now latestWrite:" + latestWrite);
                        }
                    }
                    curOp.setValue(latestWrite.getValue());
                    curOp.updateCausalPast(latestWrite); //根据对应写操作更新causalPast
//                    this.globalActiveVar.get(curOp.getVariable()).add(latestWrite.getProcess());
                    if(this.processVarValue.get(processID).containsKey(curOp.getVariable())) {
                        if (latestWrite.getIndex() > this.processVarValue.get(processID).get(curOp.getVariable())) {
                            this.processVarValue.get(processID).put(curOp.getVariable(), latestWrite.getIndex()); //更新为lastWrite
                        }
                    }
                    else{
                        this.processVarValue.get(processID).put(curOp.getVariable(), latestWrite.getIndex());
                    }
                    this.processOpList.get(processID).add(curOp.getIndex());
                }
                else{ //如果没有相关的写操作，转入CausalPast找
                    int tempIndex = maxWriteIndex;
//                    for(Integer j : curOp.getCausalPast()){
//                        if(j != curOp.getIndex()) { //排除该操作本身
//                            tempOp = this.opList.get(j);
//                            if (tempOp.isWrite() && tempOp.getVariable().equals(curOp.getVariable())) {
//                                System.out.println("Visible Write:" + tempOp.easyPrint());
////                                if (tempOp.getIndex() > tempIndex) {
////                                    tempIndex = tempOp.getIndex();
////                                }
//                                if(tempIndex == -1){ //如果tempIndex没更新过，直接更新
//                                    tempIndex = tempOp.getIndex();
//                                }
//                                else if(!this.opList.get(tempIndex).getCausalPast().contains(j) && tempOp.getIndex() >tempIndex){
//                                    System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//                                    tempIndex = tempOp.getIndex();
//                                }
//                            }
//                        }
//                    }
                    if(tempIndex == -2){ //也没找到，则可以返回读入空值
//                        System.out.println("Can return a read nil...");
//                        System.out.println("Causal Past:");
//                        String causalStr = "";
//                        for(Integer j: curOp.getCausalPast()){
//                            causalStr += this.opList.get(j).easyPrint() + "; ";
//                        }
//                        System.out.println(causalStr);
                        curOp.setValue(-1);
                    }
                    else{
                        latestWrite = this.opList.get(tempIndex);
//                        System.out.println("Last Write:" + latestWrite.easyPrint());
                        curOp.setValue(latestWrite.getValue()); //否则返回causalPast中最新的同变量的值
                        curOp.updateCausalPast(latestWrite);
//                        this.globalActiveVar.get(curOp.getVariable()).add(latestWrite.getProcess());
                        if(this.processVarValue.get(processID).containsKey(curOp.getVariable())) {
                            if (latestWrite.getIndex() > this.processVarValue.get(processID).get(curOp.getVariable())) {
                                this.processVarValue.get(processID).put(curOp.getVariable(), latestWrite.getIndex()); //更新为lastWrite
                            }
                        }
                        else{
                            this.processVarValue.get(processID).put(curOp.getVariable(), latestWrite.getIndex());
                        }
                    }
                    this.processOpList.get(processID).add(curOp.getIndex());
                }
            }

//            System.out.println("CurOp:"+ curOp.easyPrint() + " index:" + curOp.getIndex() + " process:" + curOp.getProcess());
//            System.out.println("CausalPast of curOp:" + curOp.getCausalPast());
        }
    }
}
