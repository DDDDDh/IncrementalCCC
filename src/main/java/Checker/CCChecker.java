package Checker;
import Relations.*;
import History.*;
import lombok.*;

import java.util.BitSet;
import java.util.LinkedList;

@Data
public class CCChecker {

    History history;
    ProgramOrder po;
    ReadFrom rf;
    CausalOrder co;


    boolean isCyclicCO;
    boolean isWriteCOInitRead;
    boolean isWriteCORead;
    boolean isThinAirRead;

    boolean isChecked;
    boolean isCC;

    public CCChecker(History history, ProgramOrder po, ReadFrom rf, CausalOrder co){
        this.history = history;
        this.po = po;
        this.rf = rf;
        this.co = co;

        this.isCyclicCO = false;
        this.isWriteCOInitRead = false;
        this.isWriteCORead = false;
        this.isThinAirRead = false;

        this.isChecked = false;
        this.isCC = false;
    }

    public void checkCyclicCO(){
        BasicRelation POUnionRF = new BasicRelation(history.getOpNum());
        POUnionRF.union(this.po, this.rf);
//        System.out.println("Matrix of PO union RF:");
//        POUnionRF.printMatrix();

        if(POUnionRF.cycleDetection()){
            this.isCyclicCO = true;
        }
        System.out.println("Chekcing CyclicCO, result:" + this.isCyclicCO);

    }

    public void checkWriteCOInitRead(){
        LinkedList<Operation> opList = history.getOperationList();
        Operation curOp;
        BitSet curCoList;
        Operation visOp;
        for(int i = 0; i < opList.size(); i++){
            curOp = opList.get(i);
            if(curOp.isInitRead()){
                curCoList = curOp.getCoList();
                for(int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j+1)){
                    visOp = opList.get(j);
                    if(visOp.isWrite() && visOp.onSameKey(curOp)){
                        System.out.println("Debug...visOp:" + visOp.easyPrint() + " curOp:" + curOp.easyPrint());
                        this.isWriteCOInitRead = true;
                    }
                }
            }
        }
//        System.out.println("Chekcing WriteCOInitRead, result:" + this.isWriteCOInitRead);
    }

    public void checkWriteCORead(){


        LinkedList<Operation> opList = history.getOperationList();
        Operation curOp;
        Operation correspondingWrite;
        BitSet curCoList;
        Operation visOp;
        for(int i = 0; i < opList.size(); i++) {
            curOp = opList.get(i);
//            System.out.println("Checking " + curOp.easyPrint() + " now...");
            if (curOp.isRead() && (!curOp.isInitRead())) {      //只针对没有读入初值的读操作
                int correspondingWriteID = curOp.getCorrespondingWriteID();
                if(correspondingWriteID != -1 && correspondingWriteID != -2) { //筛去没有对应写操作的读操作及ThinAirRead
                    curCoList = curOp.getCoList();
                    correspondingWrite = opList.get(curOp.getCorrespondingWriteID());
                    for (int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)) {
                        visOp = opList.get(j);
//                        System.out.print("Visible op:" + visOp.easyPrint() + " ");
                        //co位于当前读操作之前的其他写入同一变量的操作
                        if (visOp.isWrite() && visOp.notEqual(correspondingWrite) && visOp.onSameKey(curOp)) {
                            if (visOp.getCoList().get(correspondingWrite.getID())) {     //corresponding write对其可见
                                System.out.println("CorrespondingWrite:" + correspondingWrite.easyPrint() + " visOp:" + visOp.easyPrint() +"[id:" +visOp.getID() +"]"+ " curOp:" + curOp.easyPrint() + "[id:"+ curOp.getID()+"]");
                                this.isWriteCORead = true;
                            }
                        }
                    }
//                    System.out.println();
                }
            }
        }
//        System.out.println("Chekcing WriteCORead, result:" + this.isWriteCORead);
    }

    public void checkThinAirRead(){
        this.isThinAirRead = this.rf.checkThinAirRead();
//        System.out.println("Chekcing ThinAirRead, result:" + this.isThinAirRead);
    }


    public boolean checkCC(){

        this.checkThinAirRead();
//        if(this.isThinAirRead){
//            return false;
//        }

        this.checkCyclicCO();
        this.checkWriteCOInitRead();
        this.checkWriteCORead();
        this.isChecked = true;

        //必须计算过所有非法模式才会返回判断
        if(!isChecked){
            return false;
        }
        if((!this.isThinAirRead) && (!this.isCyclicCO) && (!this.isWriteCORead) && (!this.isWriteCOInitRead)){
            this.isCC = true;
            return true;
        }
        return false;
    }

    public boolean getIsCC(){return this.isCC;}

    public String failReason(){
        String reason = "";
        reason = "isThinAirRead:" + this.isThinAirRead + " isCyclicCO:" + this.isCyclicCO + " isWriteCORead:" + this.isWriteCORead + " isWriteCOInitRead:" + this.isWriteCOInitRead;
        return reason;
    }
}
