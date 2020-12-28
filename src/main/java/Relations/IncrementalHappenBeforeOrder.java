package Relations;

import History.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.*;
import java.util.BitSet;


/*
假设：所有操作的id是一个全序，在某一进程上，操作id也从小到大增长
     curReadList里的操作也是按照po序由小到大排列
 */

public class IncrementalHappenBeforeOrder extends HappenBeforeOrder{

    public IncrementalHappenBeforeOrder(int size){
        super(size);
    }

    /*
    接收输入的历史记录，按照process的情况分发给子线程计算各自的HBO
     */

    public void incrementalHBO(History history, ProgramOrder po, ReadFrom rf, CausalOrder co) throws Exception{

//        if(rf.checkThinAirRead()){
//            return;
//        }

//        LinkedList<Operation> opList = history.getOperationList();
//        this.unionAndSetVis(po, rf, opList);
//        System.out.println("Initial Matrix:");
//        this.printMatrix();

        int processNum = history.getProcessOpList().keySet().size();

        //利用增量算法计算CO关系，并以此更新（初始化）每个操作的co列表
//        IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
//        ico.incrementalCO(history, po, rf);
//        System.out.println("CO Matrix after ico:");
//        ico.printMatrix();
//        ico.updateListByMatrix(history.getOperationList());

//        for(int i = 0; i < history.getOperationList().size(); i++){
//            System.out.println("No." + i + history.getOperationList().get(i).getCoList());
//        }
        //把history中操作的可见集合更新为co
        co.updateListByMatrix(history.getOperationList());

//        this.union(this, ico);

//        this.printMatrix();

//        此处初始化挪动到HappeneforeOrder本身去进行
//        //返回结果为每个线程按照read-centric方法计算得到的HBo邻接矩阵
//        HashMap<Integer, BasicRelation> processMatrix = new HashMap<>();

        ExecutorService executorService = Executors.newCachedThreadPool();

        for(Integer processID: history.getProcessOpList().keySet()){
            Callable<BasicRelation> proc = new incrementalProcess(history, processID, po);
            Future<BasicRelation> submit = executorService.submit(proc);
            processMatrix.put(processID, submit.get());
//            System.out.println("Check no" + processID + " Result:");
//            processMatrix.get(processID).printMatrix();
//            this.union(this, processMatrix.get(processID));
        }

//        System.out.println("Finally we get a matrix:");
//        this.printMatrix();

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


    public incrementalProcess(History history, int processID, ProgramOrder po){
        this.history = history;
        this.processID = processID;
        this.size = history.getOpNum();
        this.opList = new LinkedList<CMOperation>();
        this.curReadList = new LinkedList<Integer>();
        this.initProcessInfo();
        this.po = po;


    }

    public void initProcessInfo(){

        //根据history初始化操作信息
        LinkedList<Operation> originalList = history.getOperationList();
//        System.out.println("Located!!!!!!!!!!!!!!");
//        for(int i = 0; i < history.getOperationList().size(); i++){
//            System.out.println("No." + i + originalList.get(i).getCoList());
//        }


        //为CM的计算初始化额外的操作信息
        for(int i = 0; i < this.size; i++){
            CMOperation op = new CMOperation();
//            System.out.println("CM operation coList:" + originalList.get(i).getCoList());
            op.initCMOperation(originalList.get(i), processID);
//            System.out.println("OP coList:" + op.getCoList());
            op.initPrecedingWrite(history.getKeySet());
            op.initLastSameProcess(history.getOperationList(), history.getProcessOpList().get(op.getProcess()), op.getProcess());
            this.opList.add(op);
//            System.out.println(op.easyPrint() + " process:" + op.getProcess() + " processID" + processID);
            if(op.isRead() && op.getProcess() == processID){ //是当前进程上的读操作
                this.curReadList.add(op.getID());
            }
        }

//        CMOperation op;
//        for(int i = 0; i < this.size; i++){
//            op = this.opList.get(i);
//            System.out.println(op.easyPrint() + " index:" + op.getID() + " colist:" + op.getCoList());
//        }


        //为当前线程上的读操作更新lastRead，同时更新读全序编号
        int lastReadID = -2; // 设为-2是为了与初值-1区分开
        int curID;
        CMOperation curOp;
        CMOperation cWrite;
        int readSize = this.curReadList.size();
        for(int i = 0; i < readSize; i++){
            curID = this.curReadList.get(i);
            curOp = this.opList.get(curID);
            curOp.setLastRead(lastReadID); //由此，每线程的第一个操作对应的lastRead为-2
            curOp.setProcessReadID(i);
            if(curOp.getCorrespondingWriteID() != -1 && curOp.getCorrespondingWriteID()!= -2) { //如果存在对应写操作，则一同更新
                cWrite = this.opList.get(curOp.getCorrespondingWriteID());
                cWrite.setProcessReadID(i);
                cWrite.setHasDictatedRead(true);
                cWrite.getPrecedingWrite().put(cWrite.getKey(), cWrite.getID()); //为每一有效力的写操作更新自身的PW
            }
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
                                if(tempPW.get(visKey) == null){
                                    tempPW.put(visKey, j);
                                }
                                else if(j > tempPW.get(visKey)){
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
        Operation curOp;
//        System.out.println("Read size:" + readSize);
        for(int i = 0; i < readSize; i++){
            curID = this.curReadList.get(i);
            curOp = this.opList.get(curID);
            if(curOp.isInitRead()){ //跳过读初值的操作
                continue;
            }
            correspondingWriteID = curOp.getCorrespondingWriteID();
            if( correspondingWriteID < 0 || po.existEdge(curID, correspondingWriteID)){
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
            if(rOp.isInitRead()){//跳过读初值的操作
                continue;
            }
            rPrime = rOp.getLastRead();
            wOp = this.opList.get(rOp.getCorrespondingWriteID());
            initReachability(rPrime, r);
            //此处应该添加：读操作按照
//            System.out.println("r:" + rOp.easyPrint());
            curCoList = rOp.getCoList();
            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                wPrime = this.opList.get(j);
//                System.out.println("wPrime:" + wPrime.easyPrint() + "index:" + j);
                if(wPrime.isWrite() && wPrime.getKey() == rOp.getKey() && wPrime.getID() != wOp.getID()){
//                    System.out.println("Applying rule C to add edge:" + wPrime.easyPrint() + " to " + wOp.easyPrint());
                    applyRuleC(wPrime, wOp, rOp);
                }
            }

            if(rPrime == -2){ //当r为第一个操作的情况
                continue;
            }
            //如果w对r'不可见的话，直接跳过
            else if(this.opList.get(rPrime).getCoList().get(wOp.getID())){ //2020.12.23 modifited here...
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
    public void initReachability(int rPrime, int r){
//        System.out.println("R" + r + "R'" + rPrime);
        CMOperation rOp = this.opList.get(r);
        CMOperation correspondingWrite = this.opList.get(rOp.getCorrespondingWriteID());

        if(rPrime == -2){ //r'为-2，标示r为当前线程第一个读操作
            //当r为当前线程的第一个读操作时，只需要从它的对应写操作更新
            rOp.updatePrecedingWrite(correspondingWrite);
            CMOperation curOp = rOp;
            CMOperation lastOp;
            while(curOp.getLastSameProcess()!= -1){ //把同线程之前所有写操作的lastRead值设置为r的下标
               lastOp = this.opList.get(curOp.getLastSameProcess());
               lastOp.setLastRead(rOp.getID());
               curOp = lastOp;

            }
            correspondingWrite.setLastRead(rOp.getID()); //把对应写操作的lastRead值设置为r的下标
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
                    if(i != r && i != rPrime) {
                        visOp = this.opList.get(i);
                        visOp.getRrList().add(r); // 将r加入r-delta的写操作的reachable read列表中
                        rDelta.add(i);
                        if (visOp.isWrite()) {
                            visOp.setLastRead(rOp.getID()); //将r-delta中每个写操作的rr值初始化为r
                            if (i > rPrime && i < r && visOp.getProcess() == this.processID) { //在r与r'中间的写操作
                                rrGroup.add(i);
                            }
//                        else if(visOp.getProcess() == correspondingWrite.getProcess()){ //在D(r)同一线程上的写操作
                            else if (correspondingWrite.getCoList().get(i)) { //更新：co在D(r)之前的写操作
                                wwGroup.add(i);
                            }
                            else if(correspondingWrite.getID() == visOp.getID()){ //就是D(r)本身
                                //wwGroup.add(i);
//                                System.out.println("Ignore corresponding write.");
                            }
                            else {
//                                System.out.println("r:" + curOp.easyPrint() + "r'" + this.opList.get(rPrime).easyPrint() + "visOp:" + visOp.easyPrint());
                                System.out.println("Error!Impossible1");
                            }
                        } else { //在r与r'之间不可能有其他写操作
                            System.out.println("Error!Impossible2");
                        }
                    }
                    else{
                        //在r-Delta中去除r和r'
                    }
                }
            }

            //在rr组内的操作都是在同一线程，因此按照顺序更新即可
            CMOperation curOp = new CMOperation();
            CMOperation lastOp;
            for(int i = 0; i < rrGroup.size(); i++){
                curOp = this.opList.get(rrGroup.get(i));
                if(curOp.getLastSameProcess() != -1) { //若前一操作下标为-1，则为该线程第一个操作，无需更新
                    lastOp = this.opList.get(curOp.getLastSameProcess());
                    curOp.updatePrecedingWrite(lastOp);
                }
            }
            //先根据rr组的最后一个操作更新r操作的PW
            rOp.updatePrecedingWrite(curOp);

            //此处有问题 在wwGroup中，所有操作不一定是在同一个线程。
            //暂时策略：r操作的PW跟着每个操作更新
            for(int i = 1; i < wwGroup.size(); i++){
                curOp = this.opList.get(wwGroup.get(i));
                if(curOp.getLastSameProcess() != -1) {
                    lastOp = this.opList.get(curOp.getLastSameProcess());
                    curOp.updatePrecedingWrite(lastOp);
                    rOp.updatePrecedingWrite(lastOp);
                }
            }

            //添加：更新完之后读操作要根据最后一个操作更新自身的PW
            rOp.updatePrecedingWrite(curOp);


        }

    }



    public CMOperation identifyRuleC(CMOperation wPrime){

        int rOld = wPrime.getLastRR();
        int rNew = wPrime.getLastRead();
//        System.out.println("rOld:" + rOld + " rNew:" + rNew);
        if(rNew == -1 || rOld == -1){
            return null;
        }
        LinkedList<Integer> newToOld = new LinkedList<>(); //存储位于[rNew...rOld)之间的所有读操作
        CMOperation curOp;
        for(int i = rNew; i < rOld; i++){
            curOp = this.opList.get(i);
            if(curOp.isRead() && curOp.getProcess() == this.processID){
                newToOld.add(i);
            }
        }

        //返回new to old中第一个有对应写操作的读 （用于后续apply-rule-c)
        for(int i = 0; i < newToOld.size(); i++){
            curOp = this.opList.get(newToOld.get(i));
            if(curOp.getKey().equals(wPrime.getKey())){
                if(curOp.getCorrespondingWriteID() != -1 && curOp.getCorrespondingWriteID() != -2) { //*********

                    return curOp;
                }
            }
        }
        return null;

    }

    public boolean cycleDetection(CMOperation wPrime, CMOperation w){

        if(wPrime.getKey() != w.getKey()){ //wPrime与w必须作用于同一变量
//            System.out.println("Error! Not in same variable!");  //*****************
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
        if(w.getLastRead()!= -1 && wPrime.getLastRead()!= -1) { //*******************
            CMOperation rrW = this.opList.get(w.getLastRead());
            CMOperation rrWPrime = this.opList.get(wPrime.getLastRead());
            if (rrW.getProcessReadID() < rrWPrime.getProcessReadID()) {
                wPrime.setLastRead(w.getLastRead());
                wPrime.getRrList().add(w.getLastRead());
            }

            CMOperation curOp;
            for (Integer i : w.getSucList()) {
                curOp = this.opList.get(i);
                curOp.updatePrecedingWrite(wPrime);
            }
        }
    }

    public boolean applyRuleC(CMOperation wPrime, CMOperation w, CMOperation r){

        //add edge w'->w
//        System.out.println("Adding edge " + wPrime.getID() + " to " + w.getID() + "!!!!!!!!!!!!!!!!!!!!!!");
        w.getCoList().set(wPrime.getID());
        wPrime.getSucList().add(w.getID());
        wPrime.getISucList().add(w.getID());
        w.getPreList().add(wPrime.getID());
        w.getIPreList().add(wPrime.getID());

        if(cycleDetection(wPrime, w)){
            return true;
        }
        //如果没有检测到环，接着更新可达关系
        updateReachability(wPrime, w, r);
        return false;
    }

    public boolean topoSchedule(CMOperation r){

        if(!r.isRead() || r.getCorrespondingWriteID() == -1) {
            System.out.println("Error when calling topo-schedule");
            return false;
        }
        CMOperation curOp;
        CMOperation visOp;
        BitSet curOpList;

        //初始化诱导子图
        HashSet<Integer> inducedSubgraph = new HashSet<>();
        CMOperation dR = this.opList.get(r.getCorrespondingWriteID());
        BitSet drCoList = dR.getCoList();
        BitSet rCoList = r.getCoList();
        for(int i = drCoList.nextSetBit(0); i >= 0; i = drCoList.nextSetBit(i+1)){
            inducedSubgraph.add(i);
        }
        inducedSubgraph.add(dR.getID()); //将D(r)也加入到D(r)-downset中

        //根据诱导子图初始化信息
        for(Integer i: inducedSubgraph){
            curOp = this.opList.get(i);
            curOpList = curOp.getCoList();
            for(int j = curOpList.nextSetBit(0); j >= 0; j = curOpList.nextSetBit(j+1)){
                visOp = this.opList.get(j);
                if(inducedSubgraph.contains(j)){ //能加入信息的前提是可见操作也在诱导子图中
                    visOp.setICount(visOp.getICount()+1);
                    visOp.getISucList().add(i); //将i加入j的后继链表
                    curOp.getIPreList().add(j); //将j加入i的前驱链表
                }
                visOp.setIDone(false);
            }
        }

        Queue<Integer> QZERO = new LinkedList<>();
        QZERO.offer(dR.getID());

        CMOperation wPrime;
        CMOperation o;
        CMOperation rReturn;
        CMOperation w;
        while(!QZERO.isEmpty()){
            wPrime = this.opList.get(QZERO.poll());
            if(wPrime.isWrite() && !wPrime.isIDone()){
                wPrime.setLastRR(wPrime.getLastRead()); //将w'的旧rr值保存，随后根据其后继节点更新
                for(Integer i: wPrime.getISucList()){
                    o = this.opList.get(i);
                    if(o.getLastRead() < wPrime.getLastRead() && o.getLastRead() > 0){
                        wPrime.setLastRead(o.getLastRead());
                    }
                }
                rReturn = this.identifyRuleC(wPrime);
                if(rReturn != null){
                    w = this.opList.get(rReturn.getCorrespondingWriteID());
                    if(applyRuleC(wPrime, w, rReturn)){
                        return true;
                    }
                    if(dR.getCoList().get(w.getID())&& !w.isIDone()){
                        w.getIPreList().add(wPrime.getID());
                        wPrime.getISucList().add(w.getID());
                        wPrime.setICount(wPrime.getICount()+1);
                    }
                }
            }

            if(wPrime.getICount() == 0){
                wPrime.setIDone(true);
                for(Integer i: wPrime.getIPreList()){
                    o = this.opList.get(i);
                    o.setICount(o.getICount()-1);
                    if(o.getICount() == 0){
                        QZERO.offer(i);
                    }
                }
            }
        }
        return false;
    }


    public BasicRelation call(){
//        LinkedList<Integer> thisOpList = history.getProcessOpList().get(this.processID);
//        LinkedList<Operation> opList = history.getOperationList();
        BasicRelation matrix = new BasicRelation(this.size);
        if(this.readCentric(po)){
//            System.out.println("no cycle in HBo of process" + this.processID);
        }
        else{
            System.out.println("Cycle detected! process " + this.processID);
        }
        matrix.updateMatrixByCMList(this.opList);
        matrix.computeTransitiveClosure(); //因为incremental算法省略了一些边，所以要得到完整的HBo矩阵，需要通过传递闭包计算
//        System.out.println("HBo Matrix for process" + this.processID +":");
//        matrix.printMatrix();
//        System.out.println("Info of process" + this.processID + "??:" + this.curReadList.size());
//        for(int i = 0; i < this.curReadList.size(); i++){
//            System.out.println("Op" + i + ":" + this.opList.get(curReadList.get(i)).easyPrint());
//        }
        isCaculated = true;
        return matrix;
    }
}