package Relations;

import History.*;
import sun.awt.image.ImageWatched;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.*;
import java.util.BitSet;


/*
假设：所有操作的id是一个全序，在某一进程上，操作id也从小到大增长

 */

public class IncrementalHappenBeforeOrder extends HappenBeforeOrder{

    public IncrementalHappenBeforeOrder(int size){
        super(size);
    }

    /*
    接收输入的历史记录，按照process的情况分发给子线程计算各自的HBO
     */

    public void incrementalHBO(History history, ProgramOrder po, ReadFrom rf) throws Exception{

        LinkedList<Operation> opList = history.getOperationList();
        this.unionAndSetVis(po, rf, opList);
        System.out.println("Initial Matrix:");
        this.printMatrix();

        int processNum = history.getProcessOpList().keySet().size();

        //利用增量算法计算CO关系，并以此更新（初始化）每个操作的co列表
        IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
        ico.incrementalCO(history, po, rf);
        ico.updateListByMatrix(history.getOperationList());

        //返回结果为每个线程按照read-centric方法计算得到的HBo邻接矩阵
        HashMap<Integer, BasicRelation> processMatrix = new HashMap<>();

        ExecutorService executorService = Executors.newCachedThreadPool();

        for(Integer processID: history.getProcessOpList().keySet()){
            Callable<BasicRelation> proc = new incrementalProcess(history, processID);
            Future<BasicRelation> submit = executorService.submit(proc);
            System.out.println("Check no" + processID + " Result:");
            processMatrix.put(processID, submit.get());
            processMatrix.get(processID).printMatrix();
            this.union(this, processMatrix.get(processID));
        }

        System.out.println("Finally we get a matrix:");
        this.printMatrix();

        executorService.shutdown();


    }


}

class incrementalProcess implements Callable<BasicRelation>{

    History history;
    int processID;
    LinkedList<CMOperation> opList;
    LinkedList<Integer> curReadList;
    boolean isCaculated;
    int size;
    ProgramOrder po;
    ReadFrom rf;

    public incrementalProcess(History history, int processID){
        this.history = history;
        this.processID = processID;
        this.size = history.getOpNum();
        this.initProcessInfo();
        this.po = po;
        this.rf = rf;
    }

    public void initProcessInfo(){

        //根据history初始化操作信息
        LinkedList<Operation> originalList = history.getOperationList();
        for(int i = 0; i < this.size; i++){
            CMOperation op = new CMOperation();
            op.initCMOperation(originalList.get(i), processID);
            op.initPrecedingWrite(history.getKeySet());
            op.initLastSameProcess(history.getOperationList(), history.getProcessOpList().get(op.getProcess()), op.getProcess());
            this.opList.add(op);
            if(op.isRead() && op.getProcess() == processID){ //是当前进程上的读操作
                this.curReadList.add(op.getID());
            }
        }

        //更新lastRead，同时更新读全序编号
        int lastReadID = -2; // 设为-2是为了与初值-1区分开
        int curID;
        CMOperation curOp;
        CMOperation cWrite;
        int readSize = this.curReadList.size();
        for(int i = 0; i < readSize; i++){
            curID = this.curReadList.get(i);
            curOp = this.opList.get(curID);
            cWrite = this.opList.get(curOp.getCorrespondingWriteID());
            curOp.setLastRead(lastReadID); //由此，每线程的第一个操作对应的lastRead为-2
            curOp.setProcessReadID(i);
            cWrite.setProcessReadID(i);
            cWrite.setHasDictatedRead(true);
            cWrite.getPrecedingWrite().put(cWrite.getKey(), cWrite.getID()); //为每一有效力的写操作更新自身的PW
            lastReadID = curID;
        }

        this.ignoreOtherRead();

        //为每个写操作更新preceding write
        BitSet curCoList;
        CMOperation visOp;
        String visKey;
        HashMap<String, Integer> tempPW;
        for(int i = 0; i < this.size; i++){
            curOp = this.opList.get(i);
            if(curOp.isWrite() && curOp.isHasDictatedRead()){
                curCoList = curOp.getCoList();
                tempPW = curOp.getPrecedingWrite();
                for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                    visOp = this.opList.get(j);
                    if(visOp.isWrite() && visOp.isHasDictatedRead()){ //考虑对其可见的有效写操作
                        visKey = visOp.getKey();
                        if(!visKey.equals(curOp.getKey())){ //如果可见写操作并非写入同一变量，直接更新
                            if(tempPW.containsKey(visKey)){ //如果已经包含有对该变量的信息，选择较新的更新
                                if(j > tempPW.get(visKey)){
                                    tempPW.put(visKey, j);
                                }
                            }
                            else{  //否则直接更新
                                tempPW.put(visKey, j);
                            }
                        }
                        else{//如果写入同一变量，那么强制更新为当前写操作
                            tempPW.put(visKey, curOp.getID());
                        }
                    }
                }
            }
        }

        //为每个操作更新前驱和后继链表
        this.initPreAndSucList();

    }

    //尽量在ignoreOtherRead之后执行
    public void initPreAndSucList(){

        CMOperation curOp,visOp;
        BitSet curCoList;
        for(int i = 0; i < this.size; i++){
            curOp = this.opList.get(i);
            if(curOp.isWrite() || curOp.getProcess() == this.processID) { //只为写操作与本线程的操作更新信息
                curCoList = curOp.getCoList(); //每个操作的coList里已经存储了相关的可见操作信息
                for (int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)) {
                    visOp = this.opList.get(j);
                    if(visOp.isWrite() || visOp.getProcess() == this.processID){
                        curOp.getPreList().add(j);
                        visOp.getSucList().add(i);
                    }
                }
            }
        }
    }

    public void ignoreOtherRead(){
        CMOperation curOp;
        BitSet curCoList;
        CMOperation visOp;

        //针对本线程上的读操作忽略其他线程上的可见读操作
        for(int i = 0; i < this.curReadList.size(); i++){
            curOp = this.opList.get(curReadList.get(i));
            curCoList = curOp.getCoList();
            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                visOp = this.opList.get(j);
                if(visOp.isRead() && visOp.getProcess() != this.processID){ //找到位于其他process上的读操作
                    curCoList.set(j, false);
                }
            }
        }


//        //针对所有操作忽略其他线程上的读操作
//        for(int i = 0; i < size; i++){
//            curOp = this.opList.get(i);
//            curCoList = curOp.getCoList();
//            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
//                visOp = this.opList.get(j);
//                if(visOp.isRead() && visOp.getProcess() != this.processID){ //找到位于其他process上的读操作
//                    curCoList.set(j, false);
//                }
//            }
//        }

    }

    public boolean readCentric(ProgramOrder po){

        int curID;
        int correspondingWriteID;
        int readSize = this.curReadList.size();
        for(int i = 0; i < readSize; i++){
            curID = this.curReadList.get(i);
            correspondingWriteID = this.opList.get(curID).getCorrespondingWriteID();
            if(correspondingWriteID == -1 || po.existEdge(curID, correspondingWriteID)){
                return false;
            }
        }

        int r, rPrime;
        CMOperation rOp;
        CMOperation wPrime;
        CMOperation wOp;
        BitSet curCoList;
        for(int i = 0; i < readSize; i++){
            r = this.curReadList.get(i);
            rOp = this.opList.get(r);
            rPrime = rOp.getLastRead();
            wOp = this.opList.get(rOp.getCorrespondingWriteID());
            initReachability(rPrime, r);

            curCoList = rOp.getCoList();
            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                wPrime = this.opList.get(j);
                if(wPrime.isWrite() && wPrime.getKey() == rOp.getKey()){
                    applyRuleC(wPrime, wOp, rOp, rOp);
                }
            }

            if(this.opList.get(rPrime).getCoList().get(wOp.getID())){
                continue;
            }
            else{
                if(topoSchedule(rOp)){
                    return false;
                }
            }
        }
        return true;
    }

    //对于每个读操作的downset，这里用该操作o的coList标示
    public void initReachability(int r, int rPrime){
        CMOperation curOp = this.opList.get(r);
        CMOperation correspondingWrite = this.opList.get(curOp.getCorrespondingWriteID());

        if(rPrime == -2){ //r'为-2，标示r为当前线程第一个读操作

        }
        else {
            HashSet<Integer> rDelta = new HashSet();
            BitSet rCoList = this.opList.get(r).getCoList();
            BitSet rPrimeCoList = this.opList.get(rPrime).getCoList();
            LinkedList<Integer> rrGroup = new LinkedList<>();
            LinkedList<Integer> wwGroup = new LinkedList<>();
            CMOperation visOp;
            for (int i = rCoList.nextSetBit(0); i >= 0; i = rCoList.nextSetBit(i + 1)) {
                if (!rPrimeCoList.get(i)) { //找到在对r可见但是对rPrime不可见的操作
                    visOp = this.opList.get(i);
                    visOp.getRrList().add(r); // 将r加入r-delta的写操作的reachable read列表中
                    rDelta.add(i);
                    if(visOp.isWrite()){
                        if(i > rPrime && i < r && visOp.getProcess() == this.processID){ //在r与r'中间的写操作
                            rrGroup.add(i);
                        }
//                        else if(visOp.getProcess() == correspondingWrite.getProcess()){ //在D(r)同一线程上的写操作
                        else if(correspondingWrite.getCoList().get(i)){ //更新：co在D(r)之前的写操作
                            wwGroup.add(i);
                        }
                        else{
                            System.out.println("Error!Impossible");
                        }
                    }
                    else{
                        System.out.println("Error!Impossible");
                    }
                }
            }

            //在每组内的操作都是在同一进程，因此按照顺序更新即可
            CMOperation lastOp;
            for(int i = 0; i < rrGroup.size(); i++){
                curOp = this.opList.get(rrGroup.get(i));
                if(curOp.getLastSameProcess() != -1) { //若前一操作下标为-1，则为该线程第一个操作，无需更新
                    lastOp = this.opList.get(curOp.getLastSameProcess());
                    curOp.updatePrecedingWrite(lastOp);
                }
            }
            for(int i = 1; i < wwGroup.size(); i++){
                curOp = this.opList.get(wwGroup.get(i));
                if(curOp.getLastSameProcess() != -1) {
                    lastOp = this.opList.get(curOp.getLastSameProcess());
                    curOp.updatePrecedingWrite(lastOp);
                }
            }
        }
    }



    public void identifyRuleC(CMOperation wPrime){

    }

    public boolean cycleDetection(CMOperation wPrime, CMOperation w){

        if(wPrime.getKey() != w.getKey()){ //wPrime与w必须作用于同一变量
            System.out.println("Error! Not in same variable!");
            return false;
        }
        String v = w.getKey();
        if(wPrime.getPrecedingWrite().get(v) != null) {
            CMOperation wPrimePWv = this.opList.get(wPrime.getPrecedingWrite().get(v));
            if (w.getProcessReadID() <= wPrimePWv.getProcessReadID()) {
                return true;
            }
        }
        return false;
    }

    public void updateReachability(CMOperation wPrime, CMOperation w, CMOperation r){
        CMOperation rrW = this.opList.get(w.getLastRead());
        CMOperation rrWPrime = this.opList.get(wPrime.getLastRead());
        if(rrW.getProcessReadID() < rrWPrime.getProcessReadID()){
            wPrime.setLastRead(w.getLastRead());
            wPrime.getRrList().add(w.getLastRead());
        }

        CMOperation curOp;
        for(Integer i: w.getSucList()){
            curOp = this.opList.get(i);
            curOp.updatePrecedingWrite(wPrime);
        }


    }

    public boolean applyRuleC(CMOperation wPrime, CMOperation w, CMOperation r, CMOperation rCur){

        //add edge w'->w
        w.getCoList().set(wPrime.getID());
        wPrime.getSucList().add(w.getID());
        w.getPreList().add(wPrime.getID());

        if(cycleDetection(wPrime, w)){
            return true;
        }
        //如果没有检测到环，接着更新可达关系
        updateReachability(wPrime, w, r);
        return false;
    }

    public boolean topoSchedule(CMOperation r){




        return false;


    }


    public BasicRelation call(){
        LinkedList<Integer> thisOpList = history.getProcessOpList().get(this.processID);
        LinkedList<Operation> opList = history.getOperationList();
        BasicRelation matrix = new BasicRelation(opList.size());
        int size = thisOpList.size();
        int curID;
        for(int i = 0 ; i < size; i++){
            curID = thisOpList.get(i);
            System.out.println("Process" + this.processID + " No." + i + ": " + opList.get(thisOpList.get(i)).easyPrint());
            matrix.setTrue(curID, curID);
        }
        isCaculated = true;
        return matrix;
    }
}