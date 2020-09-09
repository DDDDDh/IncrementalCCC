package Relations;

import History.*;

import java.util.BitSet;
import java.util.LinkedList;

public class BasicCausalOrder extends CausalOrder {

    public BasicCausalOrder(int size){
        super(size);
    }

    public void computeCO(History history, ProgramOrder po, ReadFrom rf){

        assert (!rf.checkThinAirRead());
//        if(rf.checkThinAirRead()){
//            System.out.println("Contain ThinAirRead!");
//            return;
//        }

        LinkedList<Operation> opList = history.getOperationList();

        int size = opList.size();

        Operation curOp;

        //重置每个操作的coList，用于随后计算
        for(int i = 0; i < size; i++){
            curOp = opList.get(i);
            curOp.flushCoList();
        }


        this.initialCOMatrixWithList(po, rf, opList);

        System.out.println("Begin to compute Transitive Closure");
        this.computeTransitiveClosure();
        System.out.println("Finish computing of Transitive Closure");
        //传递闭包计算完成后更新每个操作的coList
        this.updateCoList(opList);

//        System.out.println("Full CO Matrix after transitive closure computing:");
//        this.printMatrix();
        this.isCalculated = true;
    }

    public void computeCOBFS(History history, ProgramOrder po, ReadFrom rf){

        assert (!rf.checkThinAirRead());
//        if(rf.checkThinAirRead()){
//            System.out.println("Contain ThinAirRead!");
//            return;
//        }

        LinkedList<Operation> opList = history.getOperationList();

        int size = opList.size();

        Operation curOp;

        //重置每个操作的coList，用于随后计算
        for(int i = 0; i < size; i++){
            curOp = opList.get(i);
            curOp.flushCoList();
        }


        this.initialCOMatrixWithList(po, rf, opList);

        System.out.println("Begin to compute Transitive Closure");
        this.computeTransitiveClosureByBFS();
        System.out.println("Finish computing of Transitive Closure");
        //传递闭包计算完成后更新每个操作的coList
        this.updateCoList(opList);

//        System.out.println("Full CO Matrix after transitive closure computing:");
//        this.printMatrix();
        this.isCalculated = true;
    }




//    public boolean checkCalculated(){
//        return this.isCalculated;
//    }



}
