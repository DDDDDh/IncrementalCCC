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
        this.isWriteHBInitRead = this.hbo.checkWriteHBInitReadByProcess(opList);
    }

    public void checkCyclicHB(){
        this.isCyclicHB = this.hbo.cycleDetectionByProcess();
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
