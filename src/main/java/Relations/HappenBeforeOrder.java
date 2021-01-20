package Relations;

import History.*;

import java.util.HashMap;
import java.util.LinkedList;

import java.util.BitSet;

public class HappenBeforeOrder extends BasicRelation{

    //HBo关系的集合为每个线程计算得到的HBo邻接矩阵
    HashMap<Integer, BasicRelation> processMatrix;

    public HappenBeforeOrder(int size){
        super(size);
        processMatrix = new HashMap<>();
    }

    public boolean checkEqual(HappenBeforeOrder hbo) {
        boolean isEqual = true;
//        System.out.println("test override");
        BasicRelation curMatrix;
        BasicRelation targetMatrix;
        for(Integer curProcess: this.processMatrix.keySet()){
            curMatrix = this.processMatrix.get(curProcess);
            targetMatrix = hbo.processMatrix.get(curProcess);
            if(!curMatrix.checkEqual(targetMatrix)){
                System.out.println("Detected not equal at process " + curProcess);
                System.out.println("Matrix 1:");
                curMatrix.printMatrix();
                System.out.println("Matrix 2:");
                targetMatrix.printMatrix();
                isEqual = false;
            }
        }
        return isEqual;
    }

    public boolean cycleDetectionByProcess(){

        BasicRelation curMatrix;
        boolean isCycle = false;
        for(Integer curProcess: processMatrix.keySet()){
//            System.out.println("Cycle detecting for process:" + curProcess);
            curMatrix = processMatrix.get(curProcess);
//            curMatrix.printMatrix();
            boolean temp = curMatrix.cycleDetection();
            isCycle  = isCycle || temp;
        }
        return isCycle;
    }

    public boolean checkWriteHBInitReadByProcess(LinkedList<Operation> opList){

        boolean isWriteHBInitRead = false;
        BasicRelation curMatrix;
//        System.out.println("Process");
        for(Integer curProcess: processMatrix.keySet()){
//            System.out.println("Checking WriteHBInitRead for process " + curProcess + "!!!!!!!!!!!!!!!!!!!!!!!!");
            curMatrix = processMatrix.get(curProcess);
//            curMatrix.printMatrix();
            Operation curOp;
            Operation hboOp;
            int size = opList.size();
            for(int i = 0; i < size; i++){
                curOp = opList.get(i);
//                System.out.println("Checking Op " +curOp.easyPrint() + " i:" + i);
                if(curOp.isInitRead() && curOp.getProcess() == curProcess){
//                    System.out.println("CurOp:" + curOp.easyPrint() + "on process");
                    for(int j = 0; j < size; j++){
                        if(curMatrix.existEdge(j, i)){
//                            System.out.print("from " + j +" to " + i);
                            hboOp = opList.get(j);
//                            System.out.print(" HBo op " + hboOp.easyPrint() + " ");
                            if(hboOp.isWrite() && hboOp.onSameKey(curOp)){
//                                System.out.println("WriteHBInitRead Detected!!!");
                                isWriteHBInitRead = true;
                            }
                        }
//                        System.out.println();
                    }
                }
            }
        }
        return isWriteHBInitRead;
    }

}
