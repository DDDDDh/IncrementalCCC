package Relations;

import History.*;

import java.util.BitSet;
import java.util.LinkedList;

public class BasicCausalOrder extends CausalOrder {

    public BasicCausalOrder(int size){
        super(size);
    }

    public void computeTransitiveClosure(){
        int size = this.getMatrixSize();
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                for( int k = 0; k < size; k++){
                    if(!existEdge(i,j)) { //如果i->j本身就有边，不需要更新
                        if (existEdge(i, k) && existEdge(k, j)) {
                            this.setTrue(i,j);
                        }
                    }
                }
            }
        }
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

    public void updateCoList(LinkedList<Operation> opList){

        int size = this.getMatrixSize();
        Operation curOp;
        BitSet curList;
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(existEdge(i, j)){
                    curOp = opList.get(j);
                    curList = curOp.getCoList();
                    curList.set(i, true);
                }
            }
        }
    }


//    public boolean checkCalculated(){
//        return this.isCalculated;
//    }



}
