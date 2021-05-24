package IncrementalCCC.Relations;

import IncrementalCCC.History.Operation;

import java.util.BitSet;
import java.util.LinkedList;

public class CausalOrder extends BasicRelation{

    boolean isCalculated;

    public CausalOrder(int size){
        super(size);
        this.isCalculated = false;
    }

    public void initialCOMatrix(ProgramOrder po, ReadFrom rf){
        this.union(po, rf);
    }
    public void initialCOMatrixWithList(ProgramOrder po, ReadFrom rf, LinkedList<Operation> opList){
        //初始化每个操作的coList
        for(int i = 0; i < opList.size(); i++){
            opList.get(i).setCoList(new BitSet(opList.size()));
        }
        this.unionAndSetVis(po, rf, opList);
    }

    public boolean checkCalculated(){
        return this.isCalculated;
    }
}
