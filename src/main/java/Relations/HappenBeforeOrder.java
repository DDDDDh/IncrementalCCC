package Relations;

import History.*;

import java.util.LinkedList;

import java.util.BitSet;

public class HappenBeforeOrder extends BasicRelation{

    HappenBeforeOrder(int size){
        super(size);
    }

    public void calculateHBo(History history, ProgramOrder po, ReadFrom rf){

        LinkedList<Operation> opList = history.getOperationList();
        int size = history.getOpNum();
        Operation curOp;

        //重置每个操作的coList，用于随后计算
        for(int i = 0; i < size; i++){
            curOp = opList.get(i);
            curOp.flushCoList();
        }

        this.union(po, rf);
        this.updateListByMatrix(opList);
        boolean forward = true;
        Operation correspondingWrite;
        Operation wPrime;
        BitSet curList;
        while(forward) {
            forward = false;
            //Step 3 of PRAM
            this.computeTransitiveClosure();
            this.updateListByMatrix(opList);
            //Step 4 of PRAM
            for(Operation o: opList){
                if (o.isWrite() || o.isInitRead()){
                    continue;
                }
                else if(o.isRead()){
                    correspondingWrite = opList.get(o.getCorrespondingWriteID());
                    curList = o.getCoList();
                    for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
                        wPrime = opList.get(j);
                        if(wPrime.isWrite() && wPrime.onSameKey(o) && wPrime.notEqual(correspondingWrite)){
                            correspondingWrite.getCoList().set(j, true);
                            this.setTrue(j, o.getCorrespondingWriteID());
                            forward = true;
                        }
                    }
                }
            }
        }
//        System.out.println("HBo Matrix:");
//        this.printMatrix();
    }

}
