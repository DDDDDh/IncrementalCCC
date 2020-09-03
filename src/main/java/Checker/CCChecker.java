package Checker;
import Relations.*;
import History.*;

import java.util.BitSet;
import java.util.LinkedList;

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
        int size = this.co.getMatrixSize();
        for(int i = 0; i < size; i++){
            if(this.co.existEdge(i,i)){
                this.isCyclicCO = true;
            }
        }
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
                        this.isWriteCOInitRead = true;
                    }
                }
            }
        }
    }

    public void checkWriteCORead(){
        LinkedList<Operation> opList = history.getOperationList();
        Operation curOp;
        Operation correspondingWrite;
        BitSet curCoList;
        Operation visOp;
        for(int i = 0; i < opList.size(); i++) {
            curOp = opList.get(i);
            if (curOp.isRead() && (!curOp.isInitRead())) {      //只针对没有读入初值的读操作
                curCoList = curOp.getCoList();
                correspondingWrite = opList.get(curOp.getCorrespondingWriteID());
                for (int j = curCoList.nextSetBit(0); j >= 0; j = curCoList.nextSetBit(j + 1)) {
                    visOp = opList.get(j);
                    //co位于当前读操作之前的其他写入同一变量的操作
                    if (visOp.isWrite() && visOp.notEqual(correspondingWrite) && visOp.onSameKey(curOp)) {
                        if(visOp.getCoList().get(correspondingWrite.getID())) {     //corresponding write对其可见
                            this.isWriteCORead = true;
                        }
                    }
                }
            }
        }
    }

    public void checkThinAirRead(){
        this.isThinAirRead = this.rf.checkThinAirRead();
    }


    public boolean checkCC(){

        this.checkThinAirRead();
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
    
}
