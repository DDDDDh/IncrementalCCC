package Relations;

import History.*;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.concurrent.*;

public class BasicHappenBeforeOrder extends HappenBeforeOrder{

    public BasicHappenBeforeOrder (int size){
        super(size);
    }

    public void calculateHBo(History history, ProgramOrder po, ReadFrom rf, CausalOrder co) throws Exception{

        if(rf.checkThinAirRead()){
            this.setThinAirRead(true);
            return;
        }
        if(co.isCyclicCO()) {
            this.setCyclicCO(true);
            return;
        }

        LinkedList<Operation> opList = history.getOperationList();

        //把history中操作的可见集合更新为co
        co.updateListByMatrix(history.getOperationList());

//        //返回结果为每个线程计算得到的HBo邻接矩阵

        ExecutorService executorService = Executors.newCachedThreadPool();
        CompletionService<BasicRelation> completionService = new ExecutorCompletionService<BasicRelation>(executorService);
        for(Integer processID: history.getProcessOpList().keySet()){
//            Callable<BasicRelation> proc = new basicProcess(history, processID, po, co);
//            Future<BasicRelation> submit = executorService.submit(proc);
//            processMatrix.put(processID, submit.get());
            Callable<BasicRelation> proc = new basicProcess(history, processID, po, co);
            completionService.submit(proc);

        }

        for(int i = 0; i < history.getProcessOpList().keySet().size(); i++){
            Future<BasicRelation> submit = completionService.take();
            BasicRelation processResult = submit.get();
            processMatrix.put(processResult.getProcessID(), processResult);
            System.out.println("Loop time for closure alg: " + processResult.getLoopTime() + " process:" + processResult.getProcessID() );
            if(processResult.getLoopTime() > this.maxLoop){
                this.maxLoop = processResult.getLoopTime();
            }
//            System.out.println("process " + processResult.getProcessID() + " is completed~");
        }
        this.setLoopTime(this.maxLoop);

//        System.out.println("Finally we get a matrix:");
//        this.printMatrix();

        executorService.shutdown();
    }


}

class basicProcess implements Callable<BasicRelation> {


    History history;
    int processID;
    LinkedList<Operation> opList;
    LinkedList<Operation> curReadList;
    boolean isCaculated;
    int size;
    ProgramOrder po;
    CausalOrder co;
    BasicRelation matrix;
    int loopTime;
    Operation lastOp; //当前处理线程的最后一个操作


    public basicProcess(History history, int processID, ProgramOrder po, CausalOrder co){
        this.history = history;
        this.processID = processID;
        this.size = history.getOpNum();
        this.opList = new LinkedList<Operation>();
        this.curReadList = new LinkedList<Operation>();
        this.po = po;
        this.co = co;
        this.loopTime = 0;
        this.init();
    }

    //复制history中的操作，并初始化自身操作列表
    //0524 fix:同时把需要处理的读操作加入curReadList
    public void initOpList(){
        for(int i = 0; i < this.size; i++){
            Operation tempOp = new Operation();
            tempOp.copyOperation(history.getOperationList().get(i));
            tempOp.flushCoList(); //归零后将由下一步用matrix再初始化
            this.opList.add(tempOp);
            if(tempOp.getProcess() == this.processID){
                lastOp = tempOp;
                if(tempOp.isRead() && !tempOp.isInitRead()) {
                    this.curReadList.add(tempOp);
                }
            }
        }
    }

    //根据当前线程最后一个操作的coList，把对本线程不可见的操作忽略
    public void ignoreInvisibleOps(){
        BitSet lastCoList = this.lastOp.getCoList();
        Operation tempOp;
        for (int i = lastCoList.nextClearBit(0); i >= 0 && i < this.size; i = lastCoList.nextClearBit(i + 1)) { //注意这里用的是next clear bit
            if(i != lastOp.getID()) { //不能把自己删掉
                tempOp = this.opList.get(i); //tempOp为对该线程不可见的操作
                tempOp.flushCoList();
                this.matrix.ignoreOp(i); //随后在邻接矩阵中把该操作忽略
            }
        }
    }

    public void init(){
        this.initOpList();
        this.matrix = new BasicRelation(this.size);
        this.matrix.setProcessID(this.processID);
        //利用causal order初始化可达性矩阵
        this.matrix.union(this.matrix, this.co);
//        System.out.println("last op in process" + this.processID +":" + lastOp.toString() + "id:" + lastOp.getID());
        this.matrix.updateListByMatrix(this.opList);
        this.ignoreInvisibleOps(); //在利用co初始化完成后，需要将对此线程不可见的操作忽略
        this.ignoreOtherRead();

    }

    //把其他线程上的读操作隐去
    //注意，但是由其传递的co关系得以保留，如：若有w1->r1->w2，将r1隐去后，w1现在仍然可达w2
    public void ignoreOtherRead(){
        Operation tempOp;
        for(int i = 0; i < this.size; i++){
            tempOp = this.opList.get(i);
            if(tempOp.isRead() && tempOp.getProcess() != this.processID){ //不在当前线程上的读操作
                tempOp.flushCoList(); //先把该读操作的coList清空;
                //随后在可达性矩阵中设置所有操作不可见该操作
                for(int j = 0; j < this.size; j++){
                    this.matrix.setFalse(i,j);
                }
            }
        }
    }

    public void caculateHBoProcess(){
//       System.out.println("Basic happen before for process " + this.processID);
        boolean forward = true;
        Operation correspondingWrite;
        Operation wPrime;
        BitSet curList;
//        cont:
        while(forward) {
//            System.out.println("Loop " + loop);
            this.loopTime++;
            forward = false;
            //Step 3 of PRAM
            this.matrix.computeTransitiveClosure();
            this.matrix.updateListByMatrix(this.opList);
            //Step 4 of PRAM
            for(Operation o: this.curReadList){
//              System.out.println("Dealing with " + o.easyPrint() +" for basic happen before.");
//              System.out.println("Get " + o.easyPrint() +"process:" + o.getProcess() + "corresponding write id:" + o.getCorrespondingWriteID());
                int correspondingWriteID = o.getCorrespondingWriteID();
                if(correspondingWriteID >= 0) { //略去没有对应写操作的读
//                        System.out.println("Dealing with " + o.easyPrint() +" for basic happen before....process" + this.processID);
                    correspondingWrite = this.opList.get(o.getCorrespondingWriteID());
                    curList = o.getCoList();
                    for (int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j + 1)) {
                        wPrime = this.opList.get(j);
                        if (wPrime.isWrite() && wPrime.onSameKey(o) && wPrime.notEqual(correspondingWrite)) {
                            if (!this.matrix.existEdge(j, correspondingWriteID)) { //因为已经计算过传递闭包，所以M[i][j]为1即为存在i->j的路径
                                correspondingWrite.getCoList().set(j, true);
                                this.matrix.setTrue(j, correspondingWriteID);
                                forward = true;
//                                    break cont;
                            }
//                                System.out.println("Add an edge from " + j + " to " + o.getCorrespondingWriteID() + "process" + this.processID);
                        }
                    }
                }
            }
        }

        this.matrix.setLoopTime(this.loopTime);

//        System.out.println("basic hbo Matrix for process" + this.processID);
//        this.matrix.printMatrix();

        //返回之前将不在本线程的读操作相关矩阵元素全部置零
        for(Operation o: this.opList){
            int oID = o.getID();
            if(o.getProcess() != this.processID && o.isRead()){
                for(int i = 0; i < this.size; i++){
                    this.matrix.setFalse(i, oID);
                    this.matrix.setFalse(oID, i);
                }
            }
        }

//        System.out.println("basic hbo Matrix for process" + this.processID);
//        this.matrix.printMatrix();

    }

    public BasicRelation getMatrix() {
        return this.matrix;
    }

    public BasicRelation call() throws Exception{
//        LinkedList<Integer> thisOpList = history.getProcessOpList().get(this.processID);
//        LinkedList<Operation> opList = history.getOperationList();
//        System.out.println("Here we are running process:" + this.processID);

        caculateHBoProcess();
        BasicRelation matrix = this.getMatrix();
//        if(this.processID == 1){
//           Thread.sleep(1000);
//        }
//        System.out.println("Basic HBo Matrix for process" + this.processID + ":               ********************" );
//        matrix.printMatrix();
//        System.out.println("Info of process" + this.processID + "??:" + this.curReadList.size());
//        for(int i = 0; i < this.curReadList.size(); i++){
//            System.out.println("Op" + i + ":" + this.opList.get(curReadList.get(i)).easyPrint());
//        }
        isCaculated = true;
//        System.out.println("End of process" + this.processID);
        return matrix;
    }

}
