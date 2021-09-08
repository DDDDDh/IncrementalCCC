package Relations;

import History.*;

import java.util.HashMap;
import java.util.LinkedList;
import static org.junit.Assert.assertTrue;

public class ReadFrom extends BasicRelation{

    private boolean isThinAirRead;

    public ReadFrom (int size){
        super (size);
        this.isThinAirRead = false;
    }

    public void calculateReadFrom(History history){
        assertTrue ("The history must be differentiated for read-from computation",history.isDifferentiated()); //一定要是differentiated history
        LinkedList<Operation> opList = history.getOperationList();
        HashMap<String, LinkedList<Integer>> keyOpList = history.getKeyOpList();
        HashMap<Long, Integer> valueToWriteID = new HashMap<Long, Integer>();
        LinkedList<Integer> curOpList;
        int listSize;
        Operation curOp;
        Long curValue;
        int correspondingWriteID;
        for(String key: keyOpList.keySet()){

            curOpList = keyOpList.get(key);
            listSize = curOpList.size();
//            System.out.println("Working on key["+key+"]...List size:" + listSize);
            for(int i = 0; i < listSize; i++){ //遍历一次该key上面的所有写操作，将写入每个值的写操作标记
                curOp = opList.get(curOpList.get(i));
                if(curOp.isWrite()){
                    valueToWriteID.put(curOp.getValue(), curOp.getID());
                }
            }

            for(int i = 0; i < listSize; i++){ //再次遍历，为读操作找到对应写操作，并标记
                correspondingWriteID = -1;
                curOp = opList.get(curOpList.get(i));
                if(curOp.isRead()){
                    curValue = curOp.getValue();

                    if(curValue == -1){ //读到初值的操作，不设置对应写操作
                        continue;
                    }
                    if(valueToWriteID.containsKey(curValue)) {
                        correspondingWriteID = valueToWriteID.get(curValue);
                    }
                    else{
                        System.out.println("do not contain value:" + curValue);
                        correspondingWriteID = -1;
                    }
                    if(correspondingWriteID == -1){ //没有找到对应写操作
//                        System.out.println("Error! ");
                        System.out.println("ThinAirRead:" + curOp.easyPrint());
                        curOp.setCorrespondingWriteID(-2); //把thin air read的对应写操作下标标注为-2
                        this.isThinAirRead = true;
                    }
                    else {
                        this.setTrue(correspondingWriteID, curOp.getID());
                        curOp.setCorrespondingWriteID(correspondingWriteID);

                        //为读操作与对应写操作加上直接前驱（后继）关系
                        curOp.getPredecessors().set(correspondingWriteID, true);
                        opList.get(correspondingWriteID).getSuccessors().set(curOpList.get(i), true);
//                        System.out.println("Adding read from relation:" + opList.get(correspondingWriteID).easyPrint() + "to " + curOp.easyPrint());
//                        System.out.println("That is, " + correspondingWriteID + " to " + curOp.getID());
                    }
                }
            }
            valueToWriteID.clear();
        }
    }

    public boolean checkThinAirRead(){
        return this.isThinAirRead;
    }
}
