package IncrementalCCC.Relations;

//注意，这个类的调用会对每个操作的coList带来不可逆的变化...

//注意，当输入history不为DAG图，即包含CyclicCO时，增量计算得到的邻接矩阵与传递闭包计算得到的是肯定不同的

import IncrementalCCC.History.*;

import java.util.BitSet;
import java.util.LinkedList;

public class IncrementalCausalOrder extends CausalOrder{

    LinkedList<Operation> opList;
    LinkedList<Integer> topoList;
    boolean isDAG;

    public IncrementalCausalOrder(int size){
        super(size);
    }

    public boolean DAGDetection(History history, ProgramOrder po, ReadFrom rf){

        opList = history.getOperationList();
        int size = opList.size();
        Operation curOp;

        //重置每个操作的coList，用于随后计算
        for(int i = 0; i < size; i++){
            curOp = opList.get(i);
            curOp.flushCoList();
        }

        this.initialCOMatrixWithList(po, rf, opList);
//        System.out.println("Initial CO Matrix:");
//        this.printMatrix();
//        this.initialCOMatrix(po, rf);
        topoList = this.topoSort(history);

        if(topoList.size() != this.getMatrixSize()){
            System.out.println("Not a DAG");
            this.setCyclicCO(true);
            return false;
        }
        return true;
    }



    public void incrementalCO(History history, ProgramOrder po, ReadFrom rf){

//        assert (!rf.checkThinAirRead());
//        if(rf.checkThinAirRead()){
//            System.out.println("Contain ThinAirRead!");
//            this.setThinAirRead(true);
//            return;
//        }

        isDAG = DAGDetection(history, po, rf);

        if(!isDAG){ //如果不是DAG图，不能用增量算法计算
            return;
        }

//        System.out.println("PO Matrix:");
//        po.printMatrix();
//        System.out.println("RF Matrix:");
//        rf.printMatrix();

//        for(int i = 0; i < topoList.size(); i++){
//            System.out.println("TopoID:" + i + " Op:" + opList.get(topoList.get(i)).easyPrint());
//        }

        //Begin incremental part
        Operation curOp;
        Operation lastOp;
        Operation correspondingWrite;
        BitSet curList;
        int curID;
        for(int i = 0; i < topoList.size(); i++){ //按拓扑序遍历

            curOp = opList.get(topoList.get(i));
            curList = curOp.getCoList();
            curID = curOp.getID();
            if(curOp.getLastOpID() == -1){ //curOp为某线程的第一个操作
                if(curOp.isRead() && !curOp.isInitRead()){//如果为读操作，那么向对应写操作继承vis集合

                    if(curOp.getCorrespondingWriteID() == -2){ //ThinAirRead
                        continue;
                    }
                    else {
                        correspondingWrite = opList.get(curOp.getCorrespondingWriteID());
                        curList.or(correspondingWrite.getCoList());
                    }
                }
                else if(curOp.isWrite() || curOp.isInitRead()){ //如果为写操作或读初值，那么不会看到任何操作
                    continue;
                }
            }
            else{
                lastOp = opList.get(curOp.getLastOpID());
                if(curOp.isWrite() || curOp.isInitRead()){ //写操作和读初值只需向前一个操作继承
                    curList.or(lastOp.getCoList());
                }
                else if(curOp.isRead() && !curOp.isInitRead()){ //否则向前和向对应写继承
//                    System.out.println("CurOp:" + curOp.easyPrint() + "CWID:" + curOp.getCorrespondingWriteID());

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
//            System.out.println("curList:" + curList);
            for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
                this.setTrue(j, curID);
            }
        }
//        System.out.println("Full CO Matrix after incremental computing:");
//        this.printMatrix();
        this.isCalculated = true;
    }

//    public boolean checkCalculated(){
//        return this.isCalculated;
//    }
}