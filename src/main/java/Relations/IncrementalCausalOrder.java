package Relations;

//注意，这个类的调用会对每个操作的coList带来不可逆的变化...

import History.*;

import java.util.BitSet;
import java.util.LinkedList;

public class IncrementalCausalOrder extends CausalOrder{

    public IncrementalCausalOrder(int size){
        super(size);
    }

    public LinkedList<Integer> topoSort(History history){
        LinkedList<Operation> opList = history.getOperationList();
        LinkedList<Integer> topoList = new LinkedList<>();
        LinkedList<Integer> stack = new LinkedList<>();
        int size = this.getMatrixSize();
        int[] inDegree = new int[size];
        int tempDegree = 0;

        //根据邻接矩阵为每个点初始化入度
        for(int j = 0; j < size; j++){
            tempDegree = 0;
            for(int i = 0; i < size; i++){
                if(this.existEdge(i,j)){
                    tempDegree++;
                }
            }
            inDegree[j] = tempDegree;
        }

        int count = 0; //判环辅助变量
        for(int i = 0; i < size; i++){
            if(inDegree[i] == 0){ //找到入度为0的点，入栈
                stack.addFirst(i);
                inDegree[i] = -1;
            }
        }
        int curID;
        Operation curOp;
        while(!stack.isEmpty()){
            curID = stack.removeFirst();
            curOp = opList.get(curID);
            topoList.add(curID);
            curOp.setTopoID(count++);
            for(int i = 0; i < size; i++){
                if(this.existEdge(curID, i)){
                    inDegree[i]--;
                    if(inDegree[i] == 0){
                        stack.addFirst(i);
                        inDegree[i] = -1;
                    }
                }
            }
        }
        if(count < size){
            this.isCyclicCO = true;
            System.out.println("Detected CyclicCO!");
        }
//        System.out.println("Count: " + count + " Size:" + size);
        return topoList;
    }

    public void incrementalCO(History history, ProgramOrder po, ReadFrom rf){

        assert (!rf.checkThinAirRead());
        if(rf.checkThinAirRead()){
            System.out.println("Contain ThinAirRead!");
            return;
        }

        LinkedList<Operation> opList = history.getOperationList();
//        System.out.println("PO Matrix:");
//        po.printMatrix();
//        System.out.println("RF Matrix:");
//        rf.printMatrix();
        this.initialCOMatrixWithList(po, rf, opList);
//        System.out.println("Initial CO Matrix:");
//        this.printMatrix();
//        this.initialCOMatrix(po, rf);
        LinkedList<Integer> topoList = this.topoSort(history);

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
                    correspondingWrite = opList.get(curOp.getCorrespondingWriteID());
                    curList.and(correspondingWrite.getCoList());
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
                    correspondingWrite = opList.get(curOp.getCorrespondingWriteID());
                    curList.or(correspondingWrite.getCoList());
                    curList.or(lastOp.getCoList());
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
