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

        this.initialCOMatrixWithList(po, rf, opList);
        this.computeTransitiveClosure();

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
