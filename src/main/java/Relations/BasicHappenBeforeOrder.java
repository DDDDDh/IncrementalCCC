package Relations;

import History.*;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BasicHappenBeforeOrder extends HappenBeforeOrder{

    public BasicHappenBeforeOrder (int size){
        super(size);
    }

    public void calculateHBo(History history, ProgramOrder po, ReadFrom rf, CausalOrder co) throws Exception{

        LinkedList<Operation> opList = history.getOperationList();

        co.updateListByMatrix(history.getOperationList());

//        for(int i = 0; i < history.getOperationList().size(); i++){
//            System.out.println("No." + i + history.getOperationList().get(i).getCoList());
//        }

//        this.union(this, co);

//        this.printMatrix();

//        此处初始化挪动到HappeneforeOrder本身去进行
//        //返回结果为每个线程按照read-centric方法计算得到的HBo邻接矩阵
//        HashMap<Integer, BasicRelation> processMatrix = new HashMap<>();

        ExecutorService executorService = Executors.newCachedThreadPool();

        for(Integer processID: history.getProcessOpList().keySet()){
            Callable<BasicRelation> proc = new basicProcess(history, processID, po, co);
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

class basicProcess implements Callable<BasicRelation> {


    History history;
    int processID;
    LinkedList<Operation> opList;
    LinkedList<Integer> curReadList;
    boolean isCaculated;
    int size;
    ProgramOrder po;
    CausalOrder co;
    BasicRelation matrix;


    public basicProcess(History history, int processID, ProgramOrder po, CausalOrder co){
        this.history = history;
        this.processID = processID;
        this.size = history.getOpNum();
        this.opList = new LinkedList<Operation>();
        this.curReadList = new LinkedList<Integer>();
        this.po = po;
        this.co = co;
        this.init();
    }

    //复制history中的操作，并初始化自身操作列表
    public void initOpList(){
        for(int i = 0; i < this.size; i++){
            Operation tempOp = new Operation();
            tempOp.copyOperation(history.getOperationList().get(i));
            tempOp.flushCoList(); //归零后将由下一步用matrix再初始化
            this.opList.add(tempOp);
        }
    }

    public void init(){
        this.initOpList();
        this.matrix = new BasicRelation(this.size);
        //利用causao order初始化可达性矩阵
        this.matrix.union(this.matrix, this.co);
        this.matrix.updateListByMatrix(this.opList);
        this.ignoreOhterRead();

    }

    //把其他线程上的读操作隐去
    //注意，但是由其传递的co关系得以保留，如：若有w1->r1->w2，将r1隐去后，w1现在仍然可达w2
    public void ignoreOhterRead(){
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
        boolean forward = true;
        Operation correspondingWrite;
        Operation wPrime;
        BitSet curList;
        int loop = 0;
        while(forward) {
//            System.out.println("Loop " + loop);
            forward = false;
            //Step 3 of PRAM
            this.matrix.computeTransitiveClosure();
            this.matrix.updateListByMatrix(opList);
            //Step 4 of PRAM
            for(Operation o: opList){
                if (o.isWrite() || o.isInitRead()){
                    continue;
                }
                else if(o.isRead()){
                    if(o.getProcess() != this.processID){ //略去不在本线程的读
                        continue;
                    }

                    int correspodingWriteID = o.getCorrespondingWriteID();
                    if(correspodingWriteID!= -1) { //略去没有对应写操作的读
                        correspondingWrite = opList.get(o.getCorrespondingWriteID());
                        curList = o.getCoList();
                        for (int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j + 1)) {
                            wPrime = opList.get(j);
                            if (wPrime.isWrite() && wPrime.onSameKey(o) && wPrime.notEqual(correspondingWrite)) {
                                if (!this.matrix.existEdge(j, o.getCorrespondingWriteID())) {
                                    forward = true;
                                }
                                correspondingWrite.getCoList().set(j, true);
                                this.matrix.setTrue(j, o.getCorrespondingWriteID());
                            }
                        }
                    }
                }
            }
            loop++;
        }
    }

    public BasicRelation getMatrix() {
        return this.matrix;
    }

    public BasicRelation call() {
//        LinkedList<Integer> thisOpList = history.getProcessOpList().get(this.processID);
//        LinkedList<Operation> opList = history.getOperationList();

        caculateHBoProcess();
        BasicRelation matrix = this.getMatrix();
//        System.out.println("Basic HBo Matrix for process" + this.processID + ":               ********************" );
//        matrix.printMatrix();
//        System.out.println("Info of process" + this.processID + "??:" + this.curReadList.size());
//        for(int i = 0; i < this.curReadList.size(); i++){
//            System.out.println("Op" + i + ":" + this.opList.get(curReadList.get(i)).easyPrint());
//        }
        isCaculated = true;
        return matrix;
    }

}
