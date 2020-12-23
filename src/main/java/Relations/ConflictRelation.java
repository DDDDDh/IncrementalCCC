package Relations;

import History.*;

import java.util.BitSet;
import java.util.LinkedList;

public class ConflictRelation extends BasicRelation{

    public ConflictRelation(int size){
        super(size);
    }

    //注意，需要在CO计算完成之后才能调用此函数
    public void caculateConflictRelation(History history, CausalOrder co){

        assert (co.checkCalculated());
        if(!co.checkCalculated()){
            System.out.println("The input causal order has not been calculated!");
            return;
        }

        LinkedList<Operation> opList = history.getOperationList();
        Operation curOp;
        Operation correspondingWrite;
        BitSet curList;
        Operation visOp;
        for(int i = 0; i < opList.size(); i++){
            curOp = opList.get(i);
            if(curOp.isWrite() || curOp.isInitRead() || curOp.getCorrespondingWriteID() == -2){ //过滤掉写操作、读初值的操作与ThinAirRead
                continue;
            }
            else if(curOp.isRead()){

                int correspondingWriteID = curOp.getCorrespondingWriteID();
                if(correspondingWriteID != -1) { //只取存在对应写操作的读添加cf关系
                    correspondingWrite = opList.get(curOp.getCorrespondingWriteID());
                    curList = curOp.getCoList();
                    for (int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j + 1)) {
                        visOp = opList.get(j);
                        //取CO位于当前读操作之前，与其对应写操作写入同一变量，并且值不同的写操作
                        if (visOp.isWrite() && correspondingWrite.onSameKey(visOp) && (visOp.getValue() != correspondingWrite.getValue())) {
                            this.setTrue(visOp.getID(), correspondingWrite.getID());
//                            System.out.println("Adding edge:" + visOp.easyPrint() + " to " + correspondingWrite.easyPrint());
                        }
                    }
                }
            }
        }
//        System.out.println("Conflict Relation Matrix:");
//        this.printMatrix();
    }
}
