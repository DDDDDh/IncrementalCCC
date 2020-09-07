package Checker;

import History.History;
import Relations.*;

import java.util.LinkedList;
import History.*;

public class CMChecker extends CCChecker {

    HappenBeforeOrder hbo;
    private boolean isWriteHBInitRead;
    private boolean isCyclicHB;

    private boolean isCM;

    public CMChecker(History history, ProgramOrder po, ReadFrom rf, CausalOrder co){
        super(history, po, rf, co);
        this.isWriteHBInitRead = false;
        this.isCyclicHB = false;
    }

    public CMChecker(History history, ProgramOrder po, ReadFrom rf, CausalOrder co, HappenBeforeOrder hbo){
        super(history, po, rf, co);
        this.hbo = hbo;
        this.isWriteHBInitRead = false;
        this.isCyclicHB = false;
        this.isCM = false;
        this.isChecked = false;
    }

    public void checkWriteHBInitRead(){
        LinkedList<Operation> opList = history.getOperationList();
        Operation curOp;
        Operation hboOp;
        int size = opList.size();
        for(int i = 0; i < size; i++){
            curOp = opList.get(i);
            if(curOp.isInitRead()){
                for(int j = 0; j < size; j++){
                    if(this.hbo.existEdge(j, i)){
                        hboOp = opList.get(j);
                        if(hboOp.isWrite() && hboOp.onSameKey(curOp)){
                            this.isWriteHBInitRead = true;
                        }
                    }
                }
            }
        }
    }

    public void checkCyclicHB(){
        this.isCyclicHB = this.hbo.cycleDetection();
    }

    public boolean checkCM(){

        this.isCC = checkCC();

        this.checkWriteHBInitRead();
        this.checkCyclicHB();
        this.isChecked = true;

        if((!this.isCC) || (!this.isChecked)){
            return false;
        }
        else if((!this.isWriteHBInitRead) && (!this.isCyclicHB)){
            this.isCM = true;
            return true;
        }
        return false;
    }

    public boolean getIsCM(){return this.isCM;}

}
