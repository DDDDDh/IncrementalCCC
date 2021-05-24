package IncrementalCCC.Relations;

import IncrementalCCC.History.*;

import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.BitSet;
import java.util.concurrent.CompletionService;

public class IncrementalHappenBeforeOrderV2 extends HappenBeforeOrder {

    public IncrementalHappenBeforeOrderV2(int size){super (size);}

    public void incrementalHBO(History history, ProgramOrder po, ReadFrom rf, CausalOrder co) throws Exception{

        if(rf.checkThinAirRead()){
            this.setThinAirRead(true);
            return;
        }

        if(co.isCyclicCO()){
            this.setCyclicCO(true);
            return;
        }

        //已经把co信息保存在oplist中
        co.updateListByMatrix(history.getOperationList());

        ExecutorService executorService = Executors.newCachedThreadPool();
        CompletionService<BasicRelation> completionService = new ExecutorCompletionService<BasicRelation>(executorService);
        for(Integer processID: history.getProcessOpList().keySet()){
            Callable<BasicRelation> proc = new incrementalProcessV2(history, processID, po);
            completionService.submit(proc);

        }

        for(int i = 0; i <  history.getProcessOpList().keySet().size(); i++){
            Future<BasicRelation> submit = completionService.take();
            BasicRelation tempMatrix = submit.get();
            //如果某个线程上的标志位为1，则总体的标志位也为1
            if(tempMatrix.isCyclicCO()){
                this.setCyclicCO(true);
            }
            else if(tempMatrix.isThinAirRead()){
                this.setThinAirRead(true);
            }
            else if(tempMatrix.isCyclicHB()){
                this.setCyclicHB(true);
            }
            processMatrix.put(tempMatrix.getProcessID(), tempMatrix);
            if(tempMatrix.getLoopTime() > this.maxLoop){
                this.maxLoop = tempMatrix.getLoopTime();
            }
        }
        this.setLoopTime(this.maxLoop);

        executorService.shutdown();
    }
}

class incrementalProcessV2 implements Callable<BasicRelation>{

    History history;
    int processID;
    LinkedList<CMOperation> opList;
    LinkedList<Integer> curReadList; //当前线程上的读操作列表
    boolean isCaculated;
    int size;
    ProgramOrder po;
    BasicRelation matrix; //用来存储最后的关系矩阵
    int loopTime;
    long lastTime;
    long curTime;
    LinkedList<Integer> topoList;


    public incrementalProcessV2(History history, int processID, ProgramOrder po) throws Exception{

        this.history = history;
        this.processID = processID;
        this.size = history.getOpNum();
        this.opList = new LinkedList<CMOperation>();
        this.curReadList = new LinkedList<Integer>();
        this.lastTime = System.nanoTime();
        this.curTime = System.nanoTime();
//        appendLog(this.processLog, "Init Time for process " + this.processID + ":" + (curTime - lastTime));
        this.lastTime = this.curTime;
        this.po = po;
        this.matrix = new BasicRelation(this.size);
        this.matrix.updateMatrixByList(history.getOperationList()); //用history中的操作列表更新当前可达性矩阵（默认包含了co信息)
        this.matrix.setProcessID(processID);
        this.loopTime = 0;
        this.topoList = new LinkedList<>();
        this.initProcessInfo();
    }

    public void initProcessInfo(){
        LinkedList<Operation> originalList = history.getOperationList();
        for(int i = 0; i < this.size; i++){
            CMOperation op = new CMOperation();
            op.initCMOperation(originalList.get(i), processID);
            op.initLastSameProcess(history.getOperationList(), history.getProcessOpList().get(op.getProcess()),processID);
            this.opList.add(op);
            if(op.isRead() && op.getProcess() == processID){ //是当前进程上的读操作
                this.curReadList.add(op.getID());
            }
        }

        //为读操作及对应写操作更新读全序编号
        int curID;
        CMOperation curOp;
        CMOperation cWrite;
        int readSize = this.curReadList.size();
        for(int i = 0; i < readSize; i++){
            curID = this.curReadList.get(i);
            curOp = this.opList.get(curID);
            curOp.setProcessReadID(i);
            if(curOp.getCorrespondingWriteID() != -1){
                cWrite = this.opList.get(curOp.getCorrespondingWriteID());
                cWrite.setProcessReadID(i);
                cWrite.setHasDictatedRead(true);
            }
        }

        //初始化完成后忽略其他线程上的读操作
//        this.ignoreOtherRead();

    }


//    public void ignoreOtherRead() {
//        CMOperation curOp;
//        BitSet curCoList;
//        CMOperation visOp;
//
////        //针对本线程上的读操作忽略其他线程上的可见读操作
////        for (int i = 0; i < this.curReadList.size(); i++) {
////            curOp = this.opList.get(curReadList.get(i));
////            curCoList = curOp.getCoList();
////            for (int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)) {
////                visOp = this.opList.get(j);
////                if (visOp.isRead() && visOp.getProcess() != this.processID) { //找到位于其他process上的读操作
////                    curCoList.set(j, false);
////                    this.matrix.setFalse(j, curOp.getID());
////                }
////            }
////        }
//
//        //针对所有操作忽略其他线程上的可见读操作
//        for(int i = 0; i < this.opList.size(); i++){
//            curOp = this.opList.get(i);
//            curCoList = curOp.getCoList();
//            //如果是其他线程上的读操作，将其可见操作集合置空
//            if(curOp.isRead() && curOp.getProcess() != this.processID){
//                for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)){
//                    curCoList.set(j, false);
//                    this.matrix.setFalse(j, i);
//                }
//            }
//            for (int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)) {
//                visOp = this.opList.get(j);
//                if (visOp.isRead() && visOp.getProcess() != this.processID) { //找到位于其他process上的读操作
//                    curCoList.set(j, false);
//                    this.matrix.setFalse(j, curOp.getID());
//                }
//            }
//        }
//
//    }

    public void ignoreOtherRead(){
        Operation tempOp;
        BitSet curCoList;
        for(int i = 0; i < this.size; i++){
            tempOp = this.opList.get(i);
            curCoList = tempOp.getCoList();
            if(tempOp.isRead() && tempOp.getProcess() != this.processID){ //不在当前线程上的读操作
                //先把该读操作的coList清空;
                for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)){
                    this.matrix.setFalse(j, i);
                }
                tempOp.flushCoList();
                //随后在可达性矩阵中设置所有操作不可见该操作
                for(int j = 0; j < this.size; j++){
                    this.matrix.setFalse(i,j);
                }
            }
        }
    }

    public boolean incremental_HBo_v2(){

        this.matrix.updateMatrixByCMList(this.opList); //利用oplist初始化可达性矩阵

        int curID;
        CMOperation curRead;
        CMOperation curWrite;
        CMOperation wPrime;
        int readSize = this.curReadList.size();
        Operation curOp;
        int correspondingWriteID;
        BitSet curCoList;
        LinkedList<Integer> wPreSet = new LinkedList<Integer>();

        for(int i = 0; i < readSize; i++){
            curID = this.curReadList.get(i);
            curOp = this.opList.get(curID);
            if(curOp.isInitRead()){ //跳过读初值的操作
                continue;
            }
            correspondingWriteID = curOp.getCorrespondingWriteID();
            if(correspondingWriteID < 0){
                System.out.println("Contain ThinAirRead!");
                this.matrix.setThinAirRead(true);
                return false;
            }

            if(po.existEdge(curID, correspondingWriteID)){
                System.out.println("There is a read: " + curOp.easyPrint() +"precede its correspnding write:" + this.opList.get(correspondingWriteID).easyPrint());
                this.matrix.setCyclicCO(true);
                return false;
            }
        }

//        System.out.println("Hi process" + this.processID);
//        System.out.println(this.curReadList);

        for(int i = 0; i < this.curReadList.size(); i++){
            curRead = this.opList.get(this.curReadList.get(i));
//            System.out.println("Dealing with " + curRead.easyPrint() + "process" + this.processID);
            if(curRead.isInitRead()){ //跳过读空值的操作
                continue;
            }

            curWrite = this.opList.get(curRead.getCorrespondingWriteID());
            curCoList = curRead.getCoList();
//            System.out.println("CoList of the read:" + curCoList + "process" + this.processID);

            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                wPrime = this.opList.get(j);
//                System.out.println("wPrime of read " + curRead.easyPrint() + ":" + wPrime.easyPrint() + " process:" + this.processID );
                if(wPrime.isWrite() && wPrime.getKey().equals(curRead.getKey()) && wPrime.getID() != curWrite.getID()){
                    this.matrix.setTrue(wPrime.getID(), curWrite.getID());
                    curWrite.getCoList().set(wPrime.getID(),true);
                    System.out.println("Setting "+ wPrime.easyPrint() + " to " + curWrite.easyPrint() + " true by read:" + curRead.easyPrint() + " process" + this.processID);
                    wPreSet.add(wPrime.getID());
                }
            }
            if(updateRech(wPreSet, curWrite)){
                System.out.println("Hello?" + this.processID);
                this.matrix.setCyclicHB(true); //如果成环，设置标志位
                return false;
            }
//            System.out.println("finish " + curRead.easyPrint()+ "process" + this.processID);
            wPreSet.clear();//用完wPreSet要清空！！！以备后续使用
        }

//        System.out.println("COlist of R(d)46:" + this.opList.get(37).getCoList() + " process:" + this.processID);
        return true;
    }

    public boolean updateRech(LinkedList<Integer> wPreSet, CMOperation curWrite){

//        System.out.println("Update Rech by curWrite:" + curWrite.easyPrint() + " process:" + this.processID);
//        System.out.println("Update with wPreSet:" + wPreSet + " process" + this.processID);

        if(this.matrix.cycleDetectionByMatrix()){
            return true;
        }
        this.topoList = this.matrix.topoSort(this.history);
        if(topoList.size() != this.size){
            System.out.println("Not a DAG");
            this.matrix.setCyclicCO(true);
            return true;
        }
        int wTopoOrder = 0; //当前写操作的拓扑序号
        for(int i = 0; i < topoList.size(); i++){
            if(topoList.get(i) == curWrite.getID()){
                wTopoOrder = i;
            }
        }

        CMOperation curOp;
        int curID;
        CMOperation lastOp;
        CMOperation correspondingWrite;
        BitSet curList;
        CMOperation wPrime;

        for(int i = wTopoOrder; i < topoList.size(); i++){
            curID = topoList.get(i);
            curOp = this.opList.get(curID);
            curList = curOp.getCoList();
//            System.out.println("Updating op:" + curOp.easyPrint()+ "process" + this.processID);

            if(curOp.getLastOpID() == -1){ //该线程的第一个操作
                if(curOp.isRead() && !curOp.isInitRead()){
                    if(curOp.getCorrespondingWriteID() == -2){
                        continue;
                    }
                    else{
                        correspondingWrite = this.opList.get(curOp.getCorrespondingWriteID());
                        curList.or(correspondingWrite.getCoList());
                    }

                }
                else if(curOp.isWrite() || curOp.isInitRead()){ //如果为写操作或读初值，那么不会看到任何操作
                    if(curOp.getID() == curWrite.getID()){ //除非是当前处理的写操作，那么则需要从可见写操作集合继承可见操作集合
                        for(Integer j: wPreSet){
                            wPrime = this.opList.get(j);
//                            System.out.println("wPrime:" + wPrime.easyPrint() + "process" + this.processID);
//                            System.out.println("wPime coList:" + wPrime.getCoList() + "process" + this.processID);
                            curList.or(wPrime.getCoList());
                        }
                    }
                    else {
                        continue;
                    }
                }
            }
            else{
                lastOp = this.opList.get(curOp.getLastOpID());
                if(curOp.isInitRead()){//读初值只需向前一个操作继承
                    curList.or(lastOp.getCoList());
                }
                else if(curOp.isWrite()){
                    curList.or(lastOp.getCoList());
                    if(curOp.getID() == curWrite.getID()){ //如果是当前写操作，额外向新的可见写操作集合继承可见操作集合
                        for(Integer j: wPreSet){
                            wPrime = this.opList.get(j);
//                            System.out.println("wPrime:" + wPrime.easyPrint() + "process" + this.processID);
//                            System.out.println("wPime coList:" + wPrime.getCoList() + "process" + this.processID);
                            curList.or(wPrime.getCoList());
                        }
                    }
                    else{ //否则正常

                    }
                }
                else if(curOp.isRead()){ //否则向前和向对应写继承
                    if(curOp.getCorrespondingWriteID() == -2){ //ThinAirRead也只向前一个操作继承
                        curList.or(lastOp.getCoList());
                    }
                    else {
                        correspondingWrite = opList.get(curOp.getCorrespondingWriteID());
                        curList.or(correspondingWrite.getCoList());
                        curList.or(lastOp.getCoList());
                    }
                }
            }
            //最后根据每个操作的可见操作集合更新可达性矩阵
            for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
                this.matrix.setTrue(j, curID);
            }
//            System.out.println("Now " + curOp.easyPrint() + "colist:" + curOp.getCoList() + " process:" + this.processID);
        }

        if(this.matrix.cycleDetectionByMatrix()){
            return true;
        }
//        System.out.println("Finish Rech of curWrite:" + curWrite.easyPrint() + " process:" + this.processID);
//        System.out.println("Finish op:" + curWrite.easyPrint() + "coList:" + curWrite.getCoList() + " process:" + this.processID);
        return false;
    }


    public BasicRelation getMatrix() {
        return matrix;
    }

    public BasicRelation call() throws Exception {

        if(this.incremental_HBo_v2()){

        }
        else{

        }

        this.ignoreOtherRead();

        BasicRelation matrix = this.getMatrix();
        matrix.computeTransitiveClosure();

        return matrix;
    }

}
