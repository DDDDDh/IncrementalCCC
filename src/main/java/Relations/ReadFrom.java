package Relations;

import History.*;

import java.util.HashMap;
import java.util.LinkedList;

public class ReadFrom extends BasicRelation{

    private boolean isThinAirRead;

    public ReadFrom (int size){
        super (size);
        this.isThinAirRead = false;
    }

    public void caculateReadFrom(History history){
        assert (history.isDifferentiated()); //一定要是differentiated history
        LinkedList<Operation> opList = history.getOperationList();
        HashMap<String, LinkedList<Integer>> keyOpList = history.getKeyOpList();
        HashMap<Integer, Integer> valueToWriteID = new HashMap<Integer, Integer>();
        LinkedList<Integer> curOpList;
        int listSize;
        Operation curOp;
        int curValue;
        int correspondingWriteID;
        for(String key: keyOpList.keySet()){
//            System.out.println("Working on key["+key+"]...");
            curOpList = keyOpList.get(key);
            listSize = curOpList.size();
            for(int i = 0; i < listSize; i++){ //遍历一次该key上面的所有写操作，将写入每个值的写操作标记
                curOp = opList.get(curOpList.get(i));
                if(curOp.isWrite()){
                    valueToWriteID.put(curOp.getValue(), curOp.getID());
                }
            }
            for(int i = 0; i < listSize; i++){ //再次遍历，为读操作找到对应写操作，并标记
                curOp = opList.get(curOpList.get(i));
                if(curOp.isRead()){
                    curValue = curOp.getValue();
                    if(curValue == -1){ //读到初值的操作，不设置对应写操作
                        break;
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
                        this.isThinAirRead = true;
                    }
                    else {
                        this.setTrue(correspondingWriteID, curOp.getID());
                        curOp.setCorrespondingWriteID(correspondingWriteID);
//                        System.out.println("Adding read from relation:" + opList.get(correspondingWriteID).easyPrint() + "to " + curOp.easyPrint());
//                        System.out.println("That is, " + correspondingWriteID + " to " + curOp.getID());
                    }
                }
            }
        }
    }

    public boolean checkThinAirRead(){
        return this.isThinAirRead;
    }
}
