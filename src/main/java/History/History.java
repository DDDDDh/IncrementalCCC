
/*
整理读入的操作，按照访问的变量分组，同时按照线程分组
 */

package History;


import java.util.*;

public class History {

    LinkedList<Operation> originalHistory;
    LinkedList<Operation> operationList; //初始化完成之后，访问操作列表均访问此列表（用于保护originalHistory
    LinkedList<Integer> writeHistory;
    LinkedList<Integer> readHistory;
    HashMap<String, LinkedList<Integer>> keyOpList; //作用于每个key上的操作(下标)列表
    HashMap<Integer, LinkedList<Integer>> processOpList; //每个线程维护自己的操作(下标)列表 [用于：后续计算program order]
    HashSet<String> keySet;
    int opNum;

    public History(LinkedList<Operation> operations) {
        this.originalHistory = operations;
        this.initOperations();
        this.initWriteReadHistories();
        this.initOpGroupByKey();
//        this.printOpGroupByKey();
        this.initOpGroupByProcess();
//        this.lastIndex = histories.get(histories.size() - 1).getIndex(); // the max index in the  histories
        this.initPresAndSucs();
    }

    public void initOperations(){
        this.operationList = new LinkedList<Operation>();
        Operation tempOp;
        for(Operation op: this.originalHistory){
            tempOp = new Operation();
            tempOp.copyOperation(op);
            this.operationList.add(tempOp);
        }
    }

    private void initWriteReadHistories() {
        writeHistory= new LinkedList<Integer>();
        readHistory = new LinkedList<Integer>();
        for (Operation op : this.operationList) {
            if (op.isWrite()) {
                writeHistory.add(op.getID());
            }
            if (op.isRead()) {
                readHistory.add(op.getID());
            }
        }
    }

    public void printWriteReadHistories(){
        System.out.println("-----------------------------------");
        System.out.println("Write Operations:");
        String tempString = "";
        for(Integer i : this.writeHistory){
            tempString += this.operationList.get(i.intValue()).easyPrint();
        }
        System.out.println(tempString);
        tempString = "";
        System.out.println("Read Operations:");
        for(Integer i : this.readHistory){
            tempString += this.operationList.get(i.intValue()).easyPrint();
        }
        System.out.println(tempString);
        System.out.println("-----------------------------------");
    }

    private void initOpGroupByKey(){
        this.keyOpList = new HashMap<String, LinkedList<Integer>>();
        this.keySet = new HashSet<>();
        String curKey;
        for (Operation op : this.operationList) {
            curKey = op.getKey();
            // Add curKey to keySet
            this.keySet.add(curKey);

            if (!this.keyOpList.containsKey(curKey)) {
                this.keyOpList.put(curKey, new LinkedList<Integer>());
            }
            this.keyOpList.get(curKey).add(op.getID());
        }
    }

    public void printOpGroupByKey(){
        System.out.println("-----------------------------------");
        String tempString = "";
        int listSize;
        LinkedList<Integer> tempList;
        for(String key: this.keySet){
            tempString = "";
            System.out.println("Operations on Key[" + key + "]:");
            tempList = this.keyOpList.get(key);
            listSize = tempList.size();
            for(int i = 0; i < listSize; i++){
                tempString += this.operationList.get(tempList.get(i).intValue()).easyPrint();
            }
            System.out.println(tempString);
        }
        System.out.println("-----------------------------------");
    }

    private void initOpGroupByProcess(){
        this.processOpList = new HashMap<>();
        int curProcess;
        for(Operation op: this.operationList){
            curProcess = op.getProcess();
            if(!this.processOpList.containsKey(curProcess)){
                this.processOpList.put(curProcess, new LinkedList<>());
            }
            this.processOpList.get(curProcess).add(op.getID());
        }
    }

    public void generateProcessOpList(HashMap<Integer, LinkedList<Operation>> pOpList){
//        HashMap pOpList = new HashMap<Integer, LinkedList<Operation>>();
        int curProcess;
        for(Operation op: this.operationList){
//            System.out.println("Dealing with op:" + op.easyPrint());
            curProcess = op.getProcess();
            if(!this.processOpList.containsKey(curProcess)){
                System.out.println("impossible!");
                this.processOpList.put(curProcess, new LinkedList<>());
            }
            LinkedList<Operation> tList = (LinkedList<Operation>)pOpList.get(curProcess);
            if(tList == null){
                System.out.println("impossible too");
                tList = new LinkedList<Operation>();
            }
            tList.add(op);
        }
    }

    public void printOpGroupByProcess(){
        System.out.println("-----------------------------------");
        Set processes = this.processOpList.keySet();
        int curProcess;
        String tempString = "";
        LinkedList<Integer> tempList;
        for(Object i: processes){
            tempString = "";
            curProcess = (int)i;
            System.out.println("Operations on Process[" + curProcess + "]:");
            tempList = this.processOpList.get(curProcess);
            for(int j = 0; j < tempList.size(); j++){
                tempString += this.operationList.get(tempList.get(j).intValue()).easyPrint();
            }
            System.out.println(tempString);
        }
        System.out.println("-----------------------------------");

    }

    //需要在initOpGroupByKey()之后调用
    public boolean isDifferentiated(){
        HashSet<Long> valueSet;
        Operation tempOp;
        for(String key: this.keySet){  //遍历每个变量，筛查写入重复值的操作
            LinkedList<Integer> tempOpList = this.keyOpList.get(key);
            valueSet = new HashSet<>();
            for(int i = 0; i < tempOpList.size(); i++){
                tempOp = this.operationList.get(tempOpList.get(i));
                if(tempOp.isWrite()){ //只需考虑写操作
                    if(tempOp.isInitWrite()){
                        System.out.println("Init Write Detected!");
                        System.out.println(tempOp.easyPrint());
                        System.out.println(tempOp.printOpInfo());
                        return false;
                    }

                    if(valueSet.contains(tempOp.getValue())){ //写入了重复值
                        System.out.println("Duplicated Date Detected!");
                        System.out.println(tempOp.easyPrint());
                        System.out.println(tempOp.printOpInfo());
                        return false;
                    }
                    valueSet.add(tempOp.getValue());
                }
            }
        }
        return true;
    }

    public void initialReadFrom(){

    }

    public LinkedList<Operation> getOperationList() {
        return this.operationList;
    }

    public LinkedList<Integer> getWriteHistory() {
        return this.writeHistory;
    }

    public LinkedList<Integer> getReadHistory() {
        return this.readHistory;
    }

    public HashMap<String, LinkedList<Integer>> getKeyOpList() {
        return this.keyOpList;
    }

    public HashMap<Integer, LinkedList<Integer>> getProcessOpList() {
        return this.processOpList;
    }

    public void testProtection(){

        this.operationList.remove(5);

        System.out.println("The original history is:");
        for(int i = 0; i < this.originalHistory.size(); i++){
            System.out.print(this.originalHistory.get(i).easyPrint());
        }
        System.out.println();

        System.out.println("The operation list is:");
        for(int i = 0; i < this.operationList.size(); i++){
            System.out.print(this.operationList.get(i).easyPrint());
        }

    }

    public void initPresAndSucs(){

        int size = this.getOperationList().size();
        for(Operation op: this.getOperationList()){
            op.setPredecessors(new BitSet(size));
            op.setSuccessors(new BitSet(size));
        }
    }

    public HashSet<String> getKeySet() {
        return this.keySet;
    }

    public void setOpNum(int n){
        this.opNum = n;
    }

    public int getOpNum(){
        return this.opNum;
    }

}
