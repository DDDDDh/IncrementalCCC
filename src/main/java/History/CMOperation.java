package History;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import lombok.*;

@Data
public class CMOperation extends Operation{

    int lastRead; //即reachable read
    int processReadID; //当前线程读操作全序的编号;
    int earlistRead;
    int masterPid; //当前考虑的线程id
    LinkedList<Integer> rrList; //Reachable Read List, 后向链表，标示当前写操作可达的读操作集合
    HashMap<String, Integer> precedingWrite; //标示对当前操作的可见写操作集合而言，每个变量上最近一个写操作
    LinkedList<Integer> preList; //前驱列表
    LinkedList<Integer> sucList; //后继列表
    boolean hasDictatedRead; //拥有对应读操作，也即对应读操作在主线程，也就表明该写操作存在于全序中
    int lastSameProcess; //同一线程上的前一个操作
    boolean inWWGroup; //标记当前操作是否位于某个读操作的wwGroup中，用以初始化可达性

    //以下变量为topo-Schedule会用到的辅助变量
    int iCount;
    LinkedList<Integer> iSucList;
    LinkedList<Integer> iPreList;
    boolean iDone;
    int lastRR;

    public CMOperation(){
        this.setType(OpType.other);
        this.setKey("null");
        this.setValue(-1);
        this.setProcess(-1);
        this.setTime(-1);
        this.setPosition(-1);
        this.setID(-1);
        this.setVisList(null);
        this.setCorrespondingWriteID(-1);
        this.setTopoID(-1);
        this.setLastOpID(-1);
        this.setCoList(null);
        this.lastRead = -1;
        this.processReadID = -1;
        this.earlistRead = -1;
        this.masterPid = masterPid;
        this.rrList = new LinkedList<>();
        this.precedingWrite = new HashMap<>();
        this.preList = new LinkedList<>();
        this.sucList = new LinkedList<>();
        this.hasDictatedRead = false;
        this.lastSameProcess = -1;
        this.inWWGroup = false;

        this.iCount = 0;
        this.iSucList = new LinkedList<>();
        this.iPreList = new LinkedList<>();
        this.iDone = false;
        this.lastRR = -1;
    }

    public void initCMOperation(Operation otherOp, int masterPid){
        this.copyOperation(otherOp);
        this.lastRead = -1;
        this.processReadID = -1;
        this.earlistRead = -1;
        this.masterPid = masterPid;
        this.rrList = new LinkedList<>();
        this.precedingWrite = new HashMap<>();
        this.preList = new LinkedList<>();
        this.sucList = new LinkedList<>();
        this.hasDictatedRead = false;
        this.lastSameProcess = -1;
        this.inWWGroup = false;

        this.iCount = 0;
        this.iSucList = new LinkedList<>();
        this.iPreList = new LinkedList<>();
        this.iDone = false;
        this.lastRR = -1;

    }

    public void updatePrecedingWrite(CMOperation lastOp, LinkedList<CMOperation> opList){
        int preWriteID;
        int curWriteID;
        CMOperation preWrite;
        CMOperation curWrite;

        //根据前一个操作的preceding write更新自己的，总是保持最新的那个写
        if(!lastOp.precedingWrite.isEmpty()) {
            for (String key : lastOp.precedingWrite.keySet()) {
                if (lastOp.precedingWrite.get(key) != null && lastOp.precedingWrite.get(key) != -3) { //只有前一个操作的precedingWrite不为空时才用来更新
                    preWriteID = lastOp.precedingWrite.get(key);
                    preWrite = opList.get(preWriteID);
                    if(this.precedingWrite.get(key) == -3){
                        this.precedingWrite.put(key, preWriteID);
                    }
                    else {
                        curWriteID = this.precedingWrite.get(key);
                        curWrite = opList.get(curWriteID);
                        if (curWrite.compareTotalOrderLessThan(preWrite)) {
//                        System.out.println("PW of " + this.easyPrint() + " is change from " + curWriteID + " to " + preWriteID);
                            this.precedingWrite.put(key, preWriteID);
                        }
                    }
                }
            }
        }
    }

    public void initPrecedingWrite(HashSet<String> keySet){
        for(String key: keySet){
            this.precedingWrite.put(key, -3);
        }
    }

    //潜在要求：每个线程上的操作按id从小到大排列，并且processList中的操作有序
    public void initLastSameProcess(LinkedList<Operation> opList, LinkedList<Integer> processList, int processID){
        Operation curOp;
        for(Integer i: processList){
            if(processID == this.getProcess()) { //如果为主线程上的操作，正常设置前一操作
                if (i < this.getID()) {
                    this.setLastSameProcess(i);
                }
            }
            else{ //如果不为主线程上的操作，只考虑写操作
                if(i < this.getID()){
                    curOp = opList.get(i);
                    if(curOp.isWrite()){
                        this.setLastSameProcess(i);
                    }
                }
            }
        }
    }

    /*
    如果另一操作的全序值大于当前操作，返回true
     */
    public boolean compareTotalOrderLessThan(CMOperation otherOp){
        return this.getProcessReadID() < otherOp.getProcessReadID();
    }

    public void initInducedSubGraph(){
        this.setICount(0);
        this.getISucList().clear();
        this.getIPreList().clear();
        this.setIDone(false);
    }

}
