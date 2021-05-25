package Relations;

import History.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.*;
import java.util.BitSet;
import java.util.concurrent.CompletionService;

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

        //当包含ThinAirRead与CyclicCO的时候均不能用增量算法计算Happen-before关系
        if(rf.checkThinAirRead()){
            this.setThinAirRead(true);
            return;
        }
        if(co.isCyclicCO()){
            this.setCyclicCO(true);
            return;
        }

//        LinkedList<Operation> opList = history.getOperationList();
//        this.unionAndSetVis(po, rf, opList);
//        System.out.println("Initial Matrix:");
//        this.printMatrix();

        int processNum = history.getProcessOpList().keySet().size();

        //把history中操作的可见集合更新为co
        co.updateListByMatrix(history.getOperationList());

//        //返回结果为每个线程按照read-centric方法计算得到的HBo邻接矩阵
//        HashMap<Integer, BasicRelation> processMatrix = new HashMap<>();

        ExecutorService executorService = Executors.newCachedThreadPool();
        CompletionService<BasicRelation> completionService = new ExecutorCompletionService<BasicRelation>(executorService);
        for(Integer processID: history.getProcessOpList().keySet()){
            Callable<BasicRelation> proc = new incrementalProcess(history, processID, po);
            completionService.submit(proc);
//
//            procMap.put(processID, submit);
//            processMatrix.put(processID, submit.get());
//            System.out.println("Check no" + processID + " Result:");
//            processMatrix.get(processID).printMatrix();
//            this.union(this, processMatrix.get(processID));
        }


        //把每个线程上计算得到的hbo关系存入线程-hbo关系矩阵中
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
            System.out.println("Loop time for readinc alg: " + tempMatrix.getLoopTime() + " process:" + tempMatrix.getProcessID() );
            if(tempMatrix.getLoopTime() > this.maxLoop){
                this.maxLoop = tempMatrix.getLoopTime();
            }
        }
        this.setLoopTime(this.maxLoop);

//        System.out.println("Finally we get a matrix:");
//        this.printMatrix();

        executorService.shutdown();


    }
}

class incrementalProcess implements Callable<BasicRelation>{

    History history;
    int processID; //当前计算的线程id
    LinkedList<CMOperation> opList;
    LinkedList<Integer> curReadList; //当前线程上的读操作列表
    boolean isCaculated;
    int size;
    ProgramOrder po;
    BasicRelation matrix; //用来存储最后的关系矩阵
    int loopTime;
    Operation lastOp;

    //debug
    String processLog = "target/RandomHistories/StressTestOriginalCheckLog0319_processLog.txt";
    long lastTime;
    long curTime;
    public static void appendLog(String fileName, String content) throws IOException {
        try {
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public incrementalProcess (History history, int processID, ProgramOrder po) throws Exception{
        this.history = history;
        this.processID = processID;
        this.size = history.getOpNum();
        this.opList = new LinkedList<CMOperation>();
        this.curReadList = new LinkedList<Integer>();
        this.lastTime = System.nanoTime();
        this.initProcessInfo();
        this.curTime = System.nanoTime();
//        appendLog(this.processLog, "Init Time for process " + this.processID + ":" + (curTime - lastTime));
        this.lastTime = this.curTime;
        this.po = po;
        this.matrix = new BasicRelation(this.size);
        this.matrix.updateMatrixByList(history.getOperationList()); //用history中的操作列表更新当前可达性矩阵（默认包含了co信息)
        this.matrix.setProcessID(processID);
        this.loopTime = 0;

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
//            System.out.println("p" + this.processID + " CM operation coList:" + originalList.get(i).getCoList());
            op.initCMOperation(originalList.get(i), processID);
//            System.out.println("p" + this.processID + " op:" + op.easyPrint() + " OP coList:" + op.getCoList());
            op.initPrecedingWrite(history.getKeySet());
//            op.initLastSameProcess(history.getOperationList(), history.getProcessOpList().get(op.getProcess()), op.getProcess());
            //Update0308:应该传入正确的主线程id以筛去其他线程的操作
            op.initLastSameProcess(history.getOperationList(), history.getProcessOpList().get(op.getProcess()), processID);
            this.opList.add(op);
//            System.out.println(op.easyPrint() + " process:" + op.getProcess() + " processID" + processID);
            if(op.isRead() && op.getProcess() == processID){ //是当前进程上的读操作
                this.curReadList.add(op.getID());
            }
            if(op.getProcess() == this.processID){
                lastOp = op;
            }
        }

        ignoreInvisibleOps();

//        CMOperation op;
//        for(int i = 0; i < this.size; i++){
//            op = this.opList.get(i);
//            System.out.println(op.easyPrint() + " index:" + op.getID() + " colist:" + op.getCoList());
//        }


        //为当前线程上的读操作更新lastRead，同时更新读全序编号
//        int lastReadID = -2; // 设为-2是为了与初值-1区分开
        int curID;
        CMOperation curOp;
        CMOperation cWrite;
        int readSize = this.curReadList.size();
        for(int i = 0; i < readSize; i++){
            curID = this.curReadList.get(i);
            curOp = this.opList.get(curID);
//            curOp.setLastRead(lastReadID); //由此，每线程的第一个操作对应的lastRead为-2
            curOp.setLastRead(curID); //更正：每个读操作的lastRead值应为它本身
            curOp.setProcessReadID(i); //读全序编号
            if(curOp.getCorrespondingWriteID() != -1) { //如果存在对应写操作，则一同更新
                cWrite = this.opList.get(curOp.getCorrespondingWriteID());
                cWrite.setProcessReadID(i); //写全序编号
//                System.out.println("Set process readID of " + cWrite.easyPrint() +  " to " + i);
                cWrite.setHasDictatedRead(true);
                cWrite.getPrecedingWrite().put(cWrite.getKey(), cWrite.getID()); //为每一有效力的写操作更新自身的PW
            }
//            lastReadID = curID;
        }

        this.ignoreOtherRead();

        //为每个写操作更新preceding write
        BitSet curCoList;
        CMOperation visOp;
        String visKey;
        HashMap<String, Integer> tempPW;
        CMOperation tempPWOp;

        //第一轮，先将每个有效写操作的对应PW值设置为自身
        for(int i = 0; i < this.size; i++){
            curOp = this.opList.get(i);
            if(curOp.isWrite() && curOp.isHasDictatedRead()){
                curOp.getPrecedingWrite().put(curOp.getKey(), curOp.getID());
            }
        }

        //第二轮，每个线程从前往后根据可见操作集合更新PW
        for(int i = 0; i < this.size; i++){
            curOp = this.opList.get(i);
//            if(curOp.isWrite() && curOp.isHasDictatedRead()){
//                curCoList = curOp.getCoList();
//                tempPW = curOp.getPrecedingWrite();
//                for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
//                    visOp = this.opList.get(j);
//                    if(visOp.isWrite() && visOp.isHasDictatedRead()){ //考虑对其可见的有效写操作
//                        visKey = visOp.getKey();
//                        if(!visKey.equals(curOp.getKey())){ //如果可见写操作并非写入同一变量，直接更新
//                            if(tempPW.containsKey(visKey)){ //如果已经包含有对该变量的信息，选择较新的更新
//                                if(tempPW.get(visKey) == null){
//                                    tempPW.put(visKey, j);
//                                }
//                                else if(j > tempPW.get(visKey)){
//                                    tempPW.put(visKey, j);
//                                }
//                            }
//                            else{  //否则直接更新
//                                tempPW.put(visKey, j);
//                            }
//                        }
//                        else{//如果写入同一变量，那么强制更新为当前写操作
//                            tempPW.put(visKey, curOp.getID());
//                        }
//                    }
//                }
//            }
//            if(curOp.isWrite() && curOp.isHasDictatedRead()){ //更新PW不需要当前操作一定有对应读，且不一定需要非得是写操作
            curCoList = curOp.getCoList();
            tempPW = curOp.getPrecedingWrite();
            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                visOp = this.opList.get(j);
                if(visOp.isWrite() && visOp.isHasDictatedRead()){ //考虑对其可见的有效写操作
                    visKey = visOp.getKey();
                    if(!visKey.equals(curOp.getKey())){ //如果可见写操作并非写入同一变量，直接更新
                        if(tempPW.containsKey(visKey)){ //如果已经包含有对该变量的信息，选择较新的更新
                            if(tempPW.get(visKey) == null){ //其实应该不会出现这种case，因为每一个key对应的pw初始值都是-3
                                System.out.println("Is this case possible???");
                                tempPW.put(visKey, j);
                            }
//                            else if(j > tempPW.get(visKey)){ //to fix:更新的时候比较的应该是写全序编号
                            else if(tempPW.get(visKey) == -3){
                                tempPW.put(visKey, j);
                            }
                            else{
                                tempPWOp = this.opList.get(tempPW.get(visKey)); //找到提供PW的操作
                                if(tempPWOp.compareTotalOrderLessThan(visOp)) {
                                    tempPW.put(visKey, j);
                                }
                            }
                        }
                        else{  //否则直接更新
                            tempPW.put(visKey, j);
                        }
                    }
                    else if(curOp.isWrite() && curOp.isHasDictatedRead()){//如果写入同一变量，那么强制更新为当前有效写操作
                        tempPW.put(visKey, curOp.getID());
                    }
                    else{ //否则还是按照可见操作更新
                        if(tempPW.get(visKey) == -3){
                            tempPW.put(visKey, j);
                        }
                        else {
                            tempPWOp = this.opList.get(tempPW.get(visKey));
//                        if(j > tempPW.get(visKey)) {
                            if (tempPWOp.compareTotalOrderLessThan(visOp)) {
                                tempPW.put(visKey, j);
                            }
                        }
                    }
                }
            }
//            }
        }

        //为每个操作更新前驱和后继链表
        this.initPreAndSucList();

        //为每个写操作更新自己的RR值
        int firstRead;
        LinkedList<Integer> sucList;
        CMOperation sucOp;
        for(int i = 0 ; i < this.size; i++){
            firstRead = this.size+1;
            curOp = this.opList.get(i);
//            System.out.println("CurOp: " + curOp.easyPrint() + "process:" + this.processID);
            if(curOp.isWrite()) { //只更新写操作：因为读操作的之前已经更新了
                sucList = curOp.getSucList();
                for (int j = 0; j < sucList.size(); j++) {
                    sucOp = this.opList.get(sucList.get(j));
//                    System.out.println("Suc Op:" + sucOp.easyPrint() + " process" + this.processID);
                    if (sucOp.isRead() && sucOp.getID() < firstRead) {
                        firstRead = sucOp.getID();
                        curOp.setLastRead(firstRead);
//                        System.out.println("Set LastRead of " + curOp.easyPrint() + "to" + firstRead + " process" + this.processID);
                    }
                }
            }
//            System.out.println("Operation:" + curOp.easyPrint() + " lastread:" + curOp.getLastRead() + " process" + this.processID);
        }

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

//        //针对本线程上的读操作忽略其他线程上的可见读操作
//        for(int i = 0; i < this.curReadList.size(); i++){
//            curOp = this.opList.get(curReadList.get(i));
//            curCoList = curOp.getCoList();
//            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
//                visOp = this.opList.get(j);
//                if(visOp.isRead() && visOp.getProcess() != this.processID){ //找到位于其他process上的读操作
//                    curCoList.set(j, false);
//                }
//            }
//        }


        //0523fix: 要针对所有操作把其他线程上的读忽略，否则在后面更新wwGroup信息时会出错
        //由这些读操作传递而来的可达性信息已经由co保存在每个写操作的colist中了
        //针对所有操作忽略其他线程上的读操作
        for(int i = 0; i < size; i++){
            curOp = this.opList.get(i);
            curCoList = curOp.getCoList();
            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                visOp = this.opList.get(j);
                if(visOp.isRead() && visOp.getProcess() != this.processID){ //找到位于其他process上的读操作
                    curCoList.set(j, false);
                }
            }
        }

    }

    public boolean readCentric(ProgramOrder po) throws Exception{

        int curID;
        int correspondingWriteID;
        int readSize = this.curReadList.size();
        Operation curOp;
//        System.out.println("Read size:" + readSize);

        this.lastTime = System.nanoTime();

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

        this.curTime = System.nanoTime();
//        appendLog(this.processLog, "Detected time for process " + this.processID + ":" + (curTime - lastTime));

        int r, rPrime;
        CMOperation rOp;
        CMOperation wPrime;
        CMOperation wOp;
        BitSet curCoList;

        for(int i = 0; i < readSize; i++){

            r = this.curReadList.get(i);
            rOp = this.opList.get(r);
            if(i == 0){
                rPrime = -2;
            }
            else{
                rPrime = this.curReadList.get(rOp.getProcessReadID()-1);
            }


            if(rOp.isInitRead()){//跳过读初值的操作
                continue;
            }
//            rPrime = rOp.getLastRead();
            wOp = this.opList.get(rOp.getCorrespondingWriteID());
//            if(rPrime != -2) {
//                System.out.println("Dealing with read:" + rOp.easyPrint() + "position:" + rOp.getPosition() + " at process " + this.processID + " corresponding write:" + wOp.easyPrint() + "rPrime:" + this.opList.get(rPrime).easyPrint() + "position:" + wOp.getPosition() + "process:" + this.processID);
//            }
//            else{
////                System.out.println("Dealing with read:" + rOp.easyPrint() + "position:" + rOp.getPosition() + " at process " + this.processID + " corresponding write:" + wOp.easyPrint() + "rPrime:null " + "position:" + wOp.getPosition() + "process:" + this.processID);
//            }
            initReachability(rPrime, r);
            //此处应该添加：读操作按照
//            System.out.println("r:" + rOp.easyPrint() + "process" + this.processID);
            curCoList = rOp.getCoList();
            for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                wPrime = this.opList.get(j);
//                System.out.println("wPrime:" + wPrime.easyPrint() + "index:" + j);
                if(wPrime.isWrite() && wPrime.getKey().equals(rOp.getKey()) && wPrime.getID() != wOp.getID()){
//                    System.out.println("Applying rule C to add edge:" + wPrime.easyPrint() + " to " + wOp.easyPrint());
                    if(applyRuleC(wPrime, wOp, rOp)){
                        this.matrix.setCyclicHB(true);
//                        System.out.printf("Rule C caused cycle here!");
                        return false;
                    }
                }
            }

            if(i == 0){ //跳过当r为第一个操作的情况
//                System.out.println(rOp.easyPrint() + "is the First read at process " + this.processID +", continue...");
//                System.out.println("do not need to reschedule by cur_rriop:" + rOp.easyPrint() + " dw:" + wOp.easyPrint() + " process" + this.processID);
                continue;
            }

            //如果w对r'不可见的话，直接跳过
            if(!this.opList.get(rPrime).getCoList().get(wOp.getID())){ //2020.12.23 modified here...//2021.01.05 remodify
//                System.out.println(wOp.easyPrint() + "is not visible to r':" + this.opList.get(rPrime).easyPrint() + " continue...");
//                System.out.println("do not need to reschedule by cur_rriop:" + rOp.easyPrint() + " dw:" + wOp.easyPrint() + " process" + this.processID);
                continue;
            }
            else{
                this.loopTime++;
//                System.out.println("loop inc for r':" + this.opList.get(rPrime).easyPrint() + " r:" + rOp.easyPrint());
//                System.out.println("wOp:" + wOp.easyPrint() + "is visible to rPrime:" + this.opList.get(rPrime).easyPrint() +", begin to topoSchedule...looptime:" + this.loopTime);
                if(topoSchedule(rOp)){
//                    System.out.println("Cycle detected when topo scheduling..."); //在逆拓扑排序中成环，意味着包含CyclicHB
                    this.matrix.setLoopTime(this.loopTime);
                    this.matrix.setCyclicHB(true);
                    return false;
                }
            }
        }

//        System.out.println("Now looptime:" +loopTime);
        //完成关系计算之后，把操作的可见集合整合到邻接矩阵中
        this.matrix.updateMatrixByCMList(this.opList);
        this.matrix.computeTransitiveClosure();  //因为incremental算法省略了一些边，所以要得到完整的HBo矩阵，需要通过传递闭包计算

//        ignoreInvisibleOps();

        this.matrix.setLoopTime(this.loopTime);

        //将不在本线程的读操作相关矩阵元素全部置零
        for(Operation o: this.opList){
            int oID = o.getID();
            if(o.getProcess() != this.processID && o.isRead()){
                for(int i = 0; i < this.size; i++){
                    this.matrix.setFalse(i, oID);
                    this.matrix.setFalse(oID, i);
                }
            }
        }
//        System.out.println("Finish computation of HBo at process " + this.processID);
        return true;
    }

    public void ignoreInvisibleOps(){
        BitSet lastCoList = this.lastOp.getCoList();
        Operation tempOp;
        for (int i = lastCoList.nextClearBit(0); i >= 0 && i < this.size; i = lastCoList.nextClearBit(i + 1)) { //注意这里用的是next clear bit
            if(i!= lastOp.getID()) { //不能把自己忽略
                tempOp = this.opList.get(i); //tempOp为对该线程不可见的操作
                tempOp.flushCoList();
                for (int j = 0; j < this.opList.size(); j++) {
                    this.opList.get(j).getCoList().set(j, false);
                }
            }
        }
    }


    //对于每个读操作的downset，这里用该操作o的coList标示
    public void initReachability(int rPrime, int r){
//        System.out.println("R" + r + "R'" + rPrime);
        CMOperation rOp = this.opList.get(r);
        CMOperation correspondingWrite = this.opList.get(rOp.getCorrespondingWriteID());

        if(rPrime == -2){ //r'为-2，标示r为当前线程第一个读操作
            //当r为当前线程的第一个读操作时，只需要从它的对应写操作更新
            //->原因：由于co=(po u rf)+, 因此该操作的可见操作只有1、其对应写操作；2、po位于它之前的其他写操作；而2这一部分在initProcessInfo里已经更新完成了
//            System.out.println("First read at process " + this.processID + " is " + rOp.easyPrint());
            rOp.updatePrecedingWrite(correspondingWrite, this.opList);
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
                            else if(correspondingWrite.getID() == visOp.getID()){ //就是D(r)本身
                                visOp.setLastRead(rOp.getID());
                                //wwGroup.add(i);
//                                System.out.println("Ignore corresponding write.");
                            }
//                        else if(visOp.getProcess() == correspondingWrite.getProcess()){ //在D(r)同一线程上的写操作
                            else if (correspondingWrite.getCoList().get(i)) { //更新：co在D(r)之前的写操作
                                visOp.setInWWGroup(true);
                                wwGroup.add(i);
                            }
                            else {
//                                System.out.println("r:" + curOp.easyPrint() + "r'" + this.opList.get(rPrime).easyPrint() + "visOp:" + visOp.easyPrint());
                                System.out.println("Error!Impossible1");
                            }
                        } else { //在r与r'之间不可能有其他读操作(因为其他线程上的读操作现在已经被剔除
                            System.out.println("Error!Impossible2");
                        }
                    }
                    else{
                        //在r-Delta中去除r和r' //不存在此种情况
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
                    curOp.updatePrecedingWrite(lastOp, this.opList);
                }
            }
            //先根据rr组的最后一个操作更新r操作的PW
            rOp.updatePrecedingWrite(curOp,this.opList);

            //0523fix：采用新策略更新
//            //此处有问题 在wwGroup中，所有操作不一定是在同一个线程。
//            //暂时策略：r操作的PW跟着每个操作更新
//            for(int i = 1; i < wwGroup.size(); i++){
//                curOp = this.opList.get(wwGroup.get(i));
//                if(curOp.getLastSameProcess() != -1) {
//                    lastOp = this.opList.get(curOp.getLastSameProcess());
//                    curOp.updatePrecedingWrite(lastOp, this.opList);
//                    rOp.updatePrecedingWrite(lastOp, this.opList);
//                }
//            }
//            //添加：更新完之后读操作要根据最后一个操作更新自身的PW
//            rOp.updatePrecedingWrite(curOp,this.opList);


            //备选策略：对wwGroup中的操作进行拓扑排序，然后按照拓扑序更新
            BitSet curCoList;
            CMOperation curCoOp;
            LinkedList<Integer> wwList = topoSortCoDr(correspondingWrite, this.opList);
            for(int i = 0; i < wwList.size(); i++){
                curOp = this.opList.get(wwList.get(i));
                if(curOp.isInWWGroup()) { //只为wwGroup中的操作更新，因为其余操作的信息在之前已经更新过了
                    curCoList = curOp.getCoList();
                    for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)){
                        curCoOp = this.opList.get(j);
                        curCoOp.updatePrecedingWrite(curCoOp, this.opList);
                        correspondingWrite.updatePrecedingWrite(curCoOp, this.opList);
                    }
                }
            }

            //添加：更新完之后读操作要根据最后一个操作更新自身的PW
            rOp.updatePrecedingWrite(correspondingWrite,this.opList);


        }

    }

    //对dR的coList中的操作进行拓扑排序
    public LinkedList<Integer> topoSortCoDr(CMOperation dR, LinkedList<CMOperation> opList){

        BitSet dRcoList = dR.getCoList();
        int totalSize = opList.size();
        int[] inDegree = new int[totalSize];
        CMOperation visOp;
        BitSet visCoList;
        LinkedList<CMOperation> coOpSet = new LinkedList<CMOperation>();
        int tempDegree;
        LinkedList<Integer> stack = new LinkedList<Integer>();
        BasicRelation adjMatrix = new BasicRelation(totalSize);

        //初始化入度
        for (int i = dRcoList.nextSetBit(0); i >= 0; i = dRcoList.nextSetBit(i + 1)) {
            visOp = opList.get(i);
            coOpSet.add(visOp);
            visCoList = visOp.getCoList();
            tempDegree = 0;
            for(int j = visCoList.nextSetBit(0); j >= 0; j = visCoList.nextSetBit(j+1)){
                adjMatrix.setTrue(j,i);
                tempDegree++;
            }
            inDegree[i] = tempDegree;
        }

        //找到入度为0的点，入栈
        CMOperation coOp;
        int coOpID;
        for(int i = 0; i < coOpSet.size(); i++){
            coOp = coOpSet.get(i);
            coOpID = coOp.getID();
            if(inDegree[coOpID] == 0){
                stack.addFirst(coOpID);
                inDegree[coOpID] = -1;
            }
        }

        LinkedList<Integer> topoList = new LinkedList<Integer>();
        int curID;
        while(!stack.isEmpty()){
            curID = stack.removeFirst();
            topoList.add(curID);
            for(int i = 0; i < totalSize; i++){
                if(adjMatrix.existEdge(curID, i)){
                    inDegree[i]--;
                    if(inDegree[i] == 0){
                        stack.addFirst(i);
                        inDegree[i] = -1;
                    }
                }
            }
        }

        return topoList;
    }



    public CMOperation identifyRuleC(CMOperation wPrime){

//        System.out.println("Identifying rule C for " + wPrime.easyPrint());

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

//        System.out.println("Cycle detection between " + wPrime.easyPrint() +"and " + w.easyPrint());

        if(!wPrime.getKey().equals(w.getKey())){ //wPrime与w必须作用于同一变量
//            System.out.println("Error! Not in same variable!");
            return false;
        }
        String v = w.getKey();

        if(wPrime.getPrecedingWrite().get(v) != -3) {
            CMOperation wPrimePWv = this.opList.get(wPrime.getPrecedingWrite().get(v));
            if (w.getProcessReadID() <= wPrimePWv.getProcessReadID()) { //作用于同一变量的写操作之间具有全序关系
//                System.out.println("Detected a cycle between w:" + w.easyPrint() +"and wPrime:" + wPrime.easyPrint());
//                System.out.println("w processReadID:" + w.getProcessReadID() + " w' PMv:" + wPrimePWv.getProcessReadID());
//                CMOperation wprocessRead = this.opList.get(this.curReadList.get(w.getProcessReadID()));
//                CMOperation wPrimeProcessRead = this.opList.get(this.curReadList.get(wPrimePWv.getProcessReadID()));
//                System.out.println("w processRead:" + wprocessRead.easyPrint() +"at position" + wprocessRead.getPosition() + "wPrime processRead:" + wPrimeProcessRead.easyPrint() + "at position " + wPrimeProcessRead.getPosition());


                return true;
            }
        }
        else{ //wPrime对应的PW(v)为-3，证明在w'之前没有过对v的写操作，且w'本身没有对应读操作
//            System.out.println("-3 detected...wPrime:" + wPrime.easyPrint());
        }
        return false;
    }

    public void updateReachability(CMOperation wPrime, CMOperation w, CMOperation r){
//        System.out.println("===module: updateReachability===");


//        if(w.getLastRead()!= -1 && wPrime.getLastRead()!= -1) {  //lastRead = -1表明在其之前没有read操作
//            CMOperation rrW = this.opList.get(w.getLastRead());
//            CMOperation rrWPrime = this.opList.get(wPrime.getLastRead());
//            if (rrW.getProcessReadID() < rrWPrime.getProcessReadID()) {
//                System.out.println("Update Reachability: RR of " + wPrime.easyPrint() + "from " + rrWPrime.easyPrint() + "to " +rrW.easyPrint() );
//                wPrime.setLastRead(w.getLastRead());
//                wPrime.getRrList().add(w.getLastRead());
//            }
//        }
//        else{
//
//        }
//        System.out.println("w lastRead:" + w.getLastRead() + "wPrime lastRead:" + wPrime.getLastRead());
        if(w.getLastRead() < wPrime.getLastRead()){
//            System.out.println("Update Reachability: RR of " + wPrime.easyPrint() + "from " + wPrime.getLastRead() + "to " + w.getLastRead() );
            wPrime.setLastRR(wPrime.getLastRead()); //存储RR的旧值
            wPrime.setLastRead(w.getLastRead());
            wPrime.getRrList().add(w.getLastRead());
        }

        //更新w的PW
        w.updatePrecedingWrite(wPrime, this.opList);
//        System.out.println("Update Reachability: PW of " +w.easyPrint() +  "by " + wPrime.easyPrint());

        CMOperation curOp;
        for (Integer i : w.getSucList()) {
            curOp = this.opList.get(i);
            curOp.updatePrecedingWrite(wPrime, this.opList);
//            System.out.println("Update Reachability: PW of " +curOp.easyPrint() +  "by " + wPrime.easyPrint());
        }
    }

    public boolean applyRuleC(CMOperation wPrime, CMOperation w, CMOperation r){
//        System.out.println("===module: applyRuleC===");
        //add edge w'->w
//        System.out.println("Adding edge " + wPrime.getID() + " to " + w.getID() + "!!!!!!!!!!!!!!!!!!!!!!");
//        System.out.println("That is: " + wPrime.easyPrint() + " to " + w.easyPrint() + " by " +r.easyPrint() + "at position " + r.getPosition());
        BitSet wCoList = w.getCoList();
        wCoList.set(wPrime.getID());

//        //加边后计算矩阵的传递闭包 -> 其实没必要，将wPrime的可见操作集合继承给w即可
//        this.matrix.setTrue(wPrime.getID(), w.getID());
//        this.matrix.computeTransitiveClosure();
//        this.matrix.updateCMListByMatrix(this.opList);
        BitSet wPrimeCoList = wPrime.getCoList();

        for (int i = wPrimeCoList.nextSetBit(0); i >= 0; i = wPrimeCoList.nextSetBit(i + 1)) {
            wCoList.set(i);
        }


        if(wPrime.getSucList().contains(w.getID())){
//            System.out.println("Already exist, do not need to add.");
        }
        else{
            wPrime.getSucList().add(w.getID());
        }
//        wPrime.getISucList().add(w.getID());

        if(w.getPreList().contains(wPrime.getID())){
//            System.out.println("Already exist, do not need to add.");
        }
        else{
            w.getPreList().add(wPrime.getID());
        }
//        w.getIPreList().add(wPrime.getID());

        if(cycleDetection(wPrime, w)){
//            System.out.println("Exit computation by a cycle between " + wPrime.easyPrint() +"and " + w.easyPrint());
            return true;
        }
        //如果没有检测到环，接着更新可达关系
        updateReachability(wPrime, w, r);
        return false;
    }

    public boolean topoSchedule(CMOperation r){

//        System.out.println("===module: topoSchedule===");

        if(!r.isRead() || r.getCorrespondingWriteID() == -1) {
//            System.out.println("Error when calling topo-schedule");
            return false;
        }
        CMOperation curOp;
        CMOperation visOp;
        BitSet curOpList;

        //初始化诱导子图

        HashSet<Integer> inducedSubgraph = new HashSet<>();
        CMOperation dR = this.opList.get(r.getCorrespondingWriteID());
//        System.out.println("r:" + r.easyPrint() + "D(r)" + dR.easyPrint());
        BitSet drCoList = dR.getCoList();
        BitSet rCoList = r.getCoList();
        for(int i = drCoList.nextSetBit(0); i >= 0; i = drCoList.nextSetBit(i+1)){
            this.opList.get(i).initInducedSubGraph(); //加入时初始化相关信息
            inducedSubgraph.add(i);
        }
        dR.initInducedSubGraph();
        inducedSubgraph.add(dR.getID()); //将D(r)也加入到D(r)-downset中

        //根据诱导子图初始化信息
        for(Integer i: inducedSubgraph){
            curOp = this.opList.get(i);
//            System.out.println("CurOp:" + curOp.easyPrint() + "initial prelist:" + curOp.getIPreList());
            curOpList = curOp.getCoList();
            for(int j = curOpList.nextSetBit(0); j >= 0; j = curOpList.nextSetBit(j+1)){
                visOp = this.opList.get(j);
                if(inducedSubgraph.contains(j)){ //能加入信息的前提是可见操作也在诱导子图中
                    if(!visOp.getISucList().contains(i)) { //避免重复添加
                        visOp.getISucList().add(i); //将i加入j的后继链表
                        visOp.setICount(visOp.getICount()+1);
                    }
                    if(!curOp.getIPreList().contains(j)) {
                        curOp.getIPreList().add(j); //将j加入i的前驱链表
                    }
                }
                visOp.setIDone(false);
            }
        }

//        System.out.println("Induced sub graph node:" + inducedSubgraph);


        Queue<Integer> QZERO = new LinkedList<>();
        QZERO.offer(dR.getID());

        CMOperation wPrime;
        CMOperation o;
        CMOperation rReturn;
        CMOperation w;
        while(!QZERO.isEmpty()){

            wPrime = this.opList.get(QZERO.poll());
//            System.out.println("Dealing with wprime:" + wPrime.easyPrint());
//            System.out.println("wPrime prelist:" + wPrime.getIPreList());
            if(wPrime.isWrite() && !wPrime.isIDone()){
                rReturn = this.identifyRuleC(wPrime); //先进行一次识别，处理之前加边的时候就update了RR的情况
                if(rReturn != null){
                    w = this.opList.get(rReturn.getCorrespondingWriteID());
                    if(applyRuleC(wPrime, w, rReturn)){
//                        System.out.println("Exit computation by a cycle between " + wPrime.easyPrint() +"and " + w.easyPrint());
                        return true;
                    }
                    if(dR.getCoList().get(w.getID())&& !w.isIDone()){
//                        System.out.println("add " + wPrime.easyPrint() + "to " + w.easyPrint() +"'s prelist");
                        w.getIPreList().add(wPrime.getID());
                        wPrime.getISucList().add(w.getID());
                        wPrime.setICount(wPrime.getICount()+1);
                    }
                }
                else{ //之前没update，那么从后继节点中update
                    wPrime.setLastRR(wPrime.getLastRead()); //将w'的旧rr值保存，随后根据其后继节点更新
//                System.out.println("wPrime successor list:" + wPrime.getISucList());
                    for(Integer i: wPrime.getISucList()){
                        o = this.opList.get(i);
                        if(o.getLastRead() < wPrime.getLastRead() && o.getLastRead() > 0){
                            wPrime.setLastRead(o.getLastRead());
//                            System.out.println("Set LastRead of " + wPrime.easyPrint() + " by " + o.easyPrint() + " to" + o.getLastRead());
                        }
                    }
                    rReturn = this.identifyRuleC(wPrime);
                    if(rReturn != null){
                        w = this.opList.get(rReturn.getCorrespondingWriteID());
                        if(applyRuleC(wPrime, w, rReturn)){
//                            System.out.println("Exit computation by a cycle between " + wPrime.easyPrint() +"and " + w.easyPrint());
                            return true;
                        }
                        if(dR.getCoList().get(w.getID())&& !w.isIDone()){
//                        System.out.println("add " + wPrime.easyPrint() + "to " + w.easyPrint() +"'s prelist");
                            w.getIPreList().add(wPrime.getID());
                            wPrime.getISucList().add(w.getID());
                            wPrime.setICount(wPrime.getICount()+1);
                        }
                    }
                }
//                System.out.println("Hello?");
            }

            if(wPrime.getICount() == 0){
//                System.out.println("pull " + wPrime.easyPrint());
                wPrime.setIDone(true);
//                System.out.println(wPrime.getIPreList());
                for(Integer i: wPrime.getIPreList()){
                    o = this.opList.get(i);
                    o.setICount(o.getICount()-1);
//                    System.out.println("now " + o.easyPrint() +" icout:" + o.getICount() + " process" + this.processID);
                    if(o.getICount() == 0){
                        QZERO.offer(i);
                    }
                }
            }
//            System.out.println("Stack size:" + QZERO.size());
        }
//        System.out.println("exit topo-schedule");
        return false;
    }

    public void ignoreOtherReadFinal(){
        for(Operation o: this.opList){
            int oID = o.getID();
            if(o.getProcess() != this.processID && o.isRead()){
                for(int i = 0; i < this.size; i++){
                    this.matrix.setFalse(i, oID);
                    this.matrix.setFalse(oID, i);
                }
            }
        }
    }

    public BasicRelation getMatrix(){return this.matrix;}


    public BasicRelation call() throws Exception{
//        LinkedList<Integer> thisOpList = history.getProcessOpList().get(this.processID);
//        LinkedList<Operation> opList = history.getOperationList();

        if(this.readCentric(po)){
//            System.out.println("no cycle in HBo of process" + this.processID);
        }
        else{
//            System.out.println("Cycle detected! process " + this.processID);
        }

        BasicRelation matrix = this.getMatrix();
//        matrix.updateMatrixByCMList(this.opList);
//        matrix.computeTransitiveClosure(); //因为incremental算法省略了一些边，所以要得到完整的HBo矩阵，需要通过传递闭包计算
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