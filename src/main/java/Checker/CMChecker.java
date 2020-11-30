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
//        System.out.println("???");
        this.isWriteHBInitRead = this.hbo.checkWriteHBInitReadByProcess(opList);
//        System.out.println("Checking WriteHBInitRead, result:"+this.isWriteHBInitRead);
    }

    public void checkCyclicHB(){
        this.isCyclicHB = this.hbo.cycleDetectionByProcess();
//        System.out.println("Checking CyclicHB, result:"+this.isCyclicHB);
    }

    public boolean checkCM(){

        this.isCC = checkCC();

        if(!this.isCC){
            return false;
        }

        this.checkWriteHBInitRead();
        this.checkCyclicHB();
        this.isChecked = true;

        if(!this.isChecked){
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
