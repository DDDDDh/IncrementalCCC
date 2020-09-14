package Relations;

import History.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.*;

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
    boolean isCaculated;

    public incrementalProcess(History history, int processID){
        this.history = history;
        this.processID = processID;
    }

    public void initReachability(Operation r, Operation rPrime){

    }

    public void identifyRuleC(Operation w){

    }

    public boolean cycleDetection(Operation wPrime, Operation w){

        return false;
    }

    public void updateReachability(Operation wPrime, Operation w, Operation r){


    }

    public boolean applyRuleC(Operation wPrime, Operation w, Operation r, Operation rCur){

        return false;
    }

    public boolean topoSchedule(Operation r){
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