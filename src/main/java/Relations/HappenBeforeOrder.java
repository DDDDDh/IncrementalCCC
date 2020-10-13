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

    public boolean cycleDetectionByProcess(){

        BasicRelation curMatrix;
        boolean isCycle = false;
        for(Integer curProcess: processMatrix.keySet()){
            curMatrix = processMatrix.get(curProcess);
            isCycle  = isCycle && curMatrix.cycleDetection();
        }
        return isCycle;
    }

    public boolean checkWriteHBInitReadByProcess(LinkedList<Operation> opList){

        boolean isWriteHBInitRead = false;
        BasicRelation curMatrix;
        for(Integer curProcess: processMatrix.keySet()){
            curMatrix = processMatrix.get(curProcess);
            Operation curOp;
            Operation hboOp;
            int size = opList.size();
            for(int i = 0; i < size; i++){
                curOp = opList.get(i);
                if(curOp.isInitRead()){
                    for(int j = 0; j < size; j++){
                        if(curMatrix.existEdge(j, i)){
                            hboOp = opList.get(j);
                            if(hboOp.isWrite() && hboOp.onSameKey(curOp)){
                                isWriteHBInitRead = true;
                            }
                        }
                    }
                }
            }
        }
        return isWriteHBInitRead;
    }

}
