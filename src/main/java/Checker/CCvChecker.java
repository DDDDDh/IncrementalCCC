package Checker;

import History.History;
import Relations.*;

public class CCvChecker extends CCChecker{

    ConflictRelation cf;
    private boolean isCyclicCF;

    private boolean isCCv;

    public CCvChecker(History history, ProgramOrder po, ReadFrom rf, CausalOrder co){
        super(history, po, rf, co);
        this.isCyclicCF = false;
    }

    public CCvChecker(History history, ProgramOrder po, ReadFrom rf, CausalOrder co, ConflictRelation cf){
        super(history, po, rf, co);
        this.cf = cf;
        this.isCyclicCF = false;
        this.isCCv = false;
        this.isChecked = false;
    }

    public void checkCyclicCF(){
        BasicRelation COUnionCF = new BasicRelation(history.getOpNum());
        COUnionCF.union(co, cf);
//        COUnionCF.printMatrix();
        if(COUnionCF.cycleDetection()){
            this.isCyclicCF = true;
        }
        this.isCCv = false;
        this.isChecked = true;
        System.out.println("Chekcing CyclicCF, result:" + this.isCyclicCF);
    }

    public boolean checkCCv(){

        this.isCC = checkCC();

        checkCyclicCF();

        if((!this.isCC) || (!this.isChecked) ){
            return false;
        }

        if(this.isCC && !this.isCyclicCF){
            this.isCCv = true;
            return true;
        }

        return false;
    }


    public boolean getIsCCv(){return this.isCCv;}



}
