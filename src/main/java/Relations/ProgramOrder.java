package Relations;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import History.*;


public class ProgramOrder extends BasicRelation{

    public ProgramOrder(int size){
        super(size);
    }

    public void calculateProgramOrder(History history){
        LinkedList<Operation> opList = history.getOperationList();
        HashMap<Integer, LinkedList<Integer>> processOpList = history.getProcessOpList();
        LinkedList<Integer> tempOpList;
        for(Integer i: processOpList.keySet()){
            tempOpList = processOpList.get(i);
//            System.out.println("Processing process" + i );
            for(int j = 0; j < tempOpList.size(); j++){
                Operation curOp = opList.get(tempOpList.get(j));
//                System.out.print("Op:" + curOp.easyPrint() + " ");
                BitSet tempList;
                if(j == 0){ //为每个线程的第一个操作初始化po
                    tempList = new BitSet(history.getOpNum());
                    curOp.setLastOpID(-1);
                }
                else{
                    int lastID = tempOpList.get(j-1);
                    tempList = (BitSet)opList.get(lastID).getVisList().clone(); //继承前一操作的program order
                    tempList.set(lastID, true); //设置前一操作可见
                    curOp.setLastOpID(lastID);
                }
                curOp.setVisList(tempList);
//                System.out.println("List:" + tempList);
            }
//            System.out.println();
        }

        //利用每个操作的program order集合初始化program order矩阵
        for(Operation op: opList){
            int curID = op.getID();
            BitSet visList = op.getVisList();
            for(int i = visList.nextSetBit(0); i >= 0; i = visList.nextSetBit(i+1)){
                this.setTrue(i, curID);
            }
        }
    }

    public void calculateProgramOrderByProcess(History history, int processID){
        LinkedList<Operation> opList = history.getOperationList();
        HashMap<Integer, LinkedList<Integer>> processOpList = history.getProcessOpList();
        LinkedList<Integer> tempOpList;
        for(Integer i: processOpList.keySet()){
            tempOpList = processOpList.get(i);
            if(i == processID) { //为当前process正常设置program order
                for (int j = 0; j < tempOpList.size(); j++) {
                    Operation curOp = opList.get(tempOpList.get(j));
                    BitSet tempList;
                    if (j == 0) { //为每个线程的第一个操作初始化po
                        tempList = new BitSet(history.getOpNum());
                        curOp.setLastOpID(-1);
                    } else {
                        int lastID = tempOpList.get(j - 1);
                        tempList = (BitSet) opList.get(lastID).getVisList().clone(); //继承前一操作的program order
                        tempList.set(lastID, true); //设置前一操作可见
                        curOp.setLastOpID(lastID);
                    }
                    curOp.setVisList(tempList);
                }
            }
            else{ //若为其他process，则只考虑写操作
                int lastWrite = -1;
                for(int j = 0; j < tempOpList.size(); j++){
                    Operation curOp = opList.get(tempOpList.get(j));
                    if(curOp.isWrite()){
                        BitSet tempList;
                        if (j == 0 || lastWrite == -1) { //初始化第一个写操作
                            tempList = new BitSet(history.getOpNum());
                            curOp.setLastOpID(-1);
                            lastWrite = j;
                        }
                        else{
                            tempList = (BitSet) opList.get(lastWrite).getVisList().clone();
                            tempList.set(lastWrite, true);
                            curOp.setLastOpID(lastWrite);
                            lastWrite = j;
                        }
                        curOp.setVisList(tempList);
                    }
                }
            }
//            System.out.println();
        }

        //利用每个操作的program order集合初始化program order矩阵
        for(Operation op: opList){
            int curID = op.getID();
            BitSet visList = op.getVisList();
            for(int i = visList.nextSetBit(0); i >= 0; i = visList.nextSetBit(i+1)){
                this.setTrue(i, curID);
            }
        }

    }

}
