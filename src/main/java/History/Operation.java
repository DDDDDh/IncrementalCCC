package History;

import DataStructure.JsonLine;

import java.util.BitSet;
import java.util.LinkedList;

public class Operation {

    public enum OpType{
        write, read, other
    }

    private OpType type;
    private String key;
    private long value;
    private long process;
    private long time;
    private long position;
    private long id; //index等价于每个操作的全局标识符，很重要
    private BitSet visList; //暂时等价于poList
    private int correspondingWriteID; //只有该操作是读操作时，对应的写操作下标才有意义，初值为-1(读入初值为-2); ->在ReadFrom里初始化
    private int topoID; //拓扑序号，从0开始; ->在IncrementalCausalOrder里初始化
    private int lastOpID; //同一线程位于前一位的操作ID，如果该操作为某线程第一个操作，该值为-1; ->在ProgramOrder里初始化
    private BitSet coList;
    private BitSet predecessors; //直接前驱列表
    private BitSet successors; //直接后继列表
    private int nextWrite; //为读操作存储同线程上的下一个写操作（用于将其在别的线程上忽略时把对应写连接过去）



    public Operation(){
        this.setType(OpType.other);
        this.setKey("null");
        this.setValue(-1);
        this.setProcess(-1);
        this.setTime(-1);
        this.setPosition(-1);
        this.setID(-1);
        this.setVisList(null);
        this.setCorrespondingWriteID(-1);
        this.setTopoID(-1);
        this.setLastOpID(-1);
        this.setCoList(null);
        this.setPredecessors(null);
        this.setSuccessors(null);
        this.setNextWrite(-1);
    }

    Operation(JsonLine jsonLine){
        if(jsonLine.getType().equals("write")){
            this.setType(OpType.write);
        }
        else if(jsonLine.getType().equals("read")){
            this.setType(OpType.read);
            if(jsonLine.getValue() == -1){
                this.setCorrespondingWriteID(-2);
            }
            else{
                this.setCorrespondingWriteID(-1);
            }
        }
        else{
            this.setType(OpType.other);
        }
        this.setKey(jsonLine.getKey());
        this.setValue(jsonLine.getValue());
        this.setProcess(jsonLine.getProcess());
        this.setTime(jsonLine.getTime());
        this.setPosition(jsonLine.getPosition());
        this.setID(jsonLine.getIndex());
        this.setVisList(null);
        this.setCorrespondingWriteID(-1);
        this.setTopoID(-1);
        this.setLastOpID(-1);
        this.setCoList(null);
        this.setPredecessors(null);
        this.setSuccessors(null);
        this.setNextWrite(-1);
    }


    Operation(String type, String key, int value, int process, long time, long position, int index){
        if(type.equals(":write")){
            this.setType(OpType.write);
        }
        else if(type.equals(":read")){
            this.setType(OpType.read);
            if(value == -1){ //读入初值的读操作，对应写操作下标初始化为-2；
                this.setCorrespondingWriteID(-2);
            }
            else {
                this.setCorrespondingWriteID(-1); //否则，将读操作的对应写操作下标初始化为-1；
            }
        }
        else{
            this.setType(OpType.other);
        }
        this.setKey(key);
        this.setValue(value);
        this.setProcess(process);
        this.setTime(time);
        this.setPosition(position);
        this.setID(index);
        this.setVisList(null);
        this.setCorrespondingWriteID(-1);
        this.setTopoID(-1);
        this.setLastOpID(-1);
        this.setCoList(null);
        this.setPredecessors(null);
        this.setSuccessors(null);
        this.setNextWrite(-1);
    }

    public void copyOperation(Operation otherOp){
        this.setType(otherOp.getType());
        this.setKey(otherOp.getKey());
        this.setValue(otherOp.getValue());
        this.setProcess(otherOp.getProcess());
        this.setTime(otherOp.getTime());
        this.setPosition(otherOp.getPosition());
        this.setID(otherOp.getID());
        this.copyVisList(otherOp.getVisList());
//        this.setVisList(otherOp.getVisList());
        this.setCorrespondingWriteID(otherOp.getCorrespondingWriteID());
        this.setTopoID(otherOp.getTopoID());
        this.setLastOpID(otherOp.getLastOpID());
        this.copyCoList(otherOp.getCoList());
//        this.setCoList(otherOp.getVisList());
        this.copyPredecessors(otherOp.getPredecessors());
        this.copySuccessors(otherOp.getSuccessors());
        this.setNextWrite(otherOp.getNextWrite());
    }

    public void setKey(String key) {
        this.key = key;
    }
    public String getKey(){
        return this.key;
    }
    public void setType(OpType type){
        this.type = type;
    }
    public OpType getType() {
        return this.type;
    }
    public void setValue(long value){
        this.value = value;
    }
    public long getValue(){
        return this.value;
    }
    public void setProcess(long process){
        this.process = process;
    }
    public long getProcess(){
        return this.process;
    }
    public void setTime(long time){
        this.time = time;
    }
    public long getTime(){
        return this.time;
    }
    public void setPosition(long position){
        this.position = position;
    }
    public long getPosition(){
        return this.position;
    }
    public void setID(long index){
        this.id = index;
    }
    public void setCorrespondingWriteID(int index){this.correspondingWriteID = index;}
    public int getCorrespondingWriteID(){return this.correspondingWriteID;}
    public long getID(){
        return this.id;
    }
    public void setVisList(BitSet bitSet){this.visList = bitSet;}
    public void setTopoID(int topoID){
        this.topoID = topoID;
    }
    public int getTopoID(){return this.topoID;}
    public void setLastOpID(int lastOpID){
        this.lastOpID = lastOpID;
    }
    public int getLastOpID(){return this.lastOpID;}
    public void setCoList(BitSet bitSet){this.coList = bitSet;}
    public void setPredecessors(BitSet bitSet){this.predecessors = bitSet;}
    public BitSet getPredecessors() {
        return predecessors;
    }
    public void setSuccessors(BitSet bitSet){this.successors = bitSet;}
    public BitSet getSuccessors() {
        return successors;
    }
    public void setNextWrite(int nextWriteId){this.nextWrite = nextWriteId;}
    public int getNextWrite(){return this.nextWrite;}

    //用于拷贝构造
    public void copyVisList(BitSet bitSet){
        if(bitSet != null) {
            this.visList = new BitSet(bitSet.size());
            for (int j = bitSet.nextSetBit(0); j >= 0; j = bitSet.nextSetBit(j + 1)) {
                this.visList.set(j);
            }
        }
    }

    public void copyCoList(BitSet bitSet){
        if(bitSet!= null) {
            this.coList = new BitSet(bitSet.size());
            for (int j = bitSet.nextSetBit(0); j >= 0; j = bitSet.nextSetBit(j + 1)) {
                this.coList.set(j);
            }
        }
    }

    public void copyPredecessors(BitSet bitSet){
        if(bitSet!= null) {
            this.predecessors = new BitSet(bitSet.size());
            for (int j = bitSet.nextSetBit(0); j >= 0; j = bitSet.nextSetBit(j + 1)) {
                this.predecessors.set(j);
            }
        }
    }

    public void copySuccessors(BitSet bitSet){
        if(bitSet!= null) {
            this.successors = new BitSet(bitSet.size());
            for (int j = bitSet.nextSetBit(0); j >= 0; j = bitSet.nextSetBit(j + 1)) {
                this.successors.set(j);
            }
        }
    }


    //此函数需要在知道一共有多少个操作后再调用
    public void initialVisList(int size){
        this.visList = new BitSet(size);
    }

    public BitSet getVisList(){
        return this.visList;
    }

    public BitSet getCoList(){ return this.coList;}

    public boolean isWrite(){
        if(this.type == OpType.write){
            return true;
        }
        return false;
    }

    public boolean isInitWrite(){
        if(this.type == OpType.write && this.value == -1){
            return true;
        }
        return false;
    }

    public boolean isRead(){
        if(this.type == OpType.read){
            return true;
        }
        return false;
    }

    public boolean isInitRead(){
        if(this.isRead() && (this.getValue() == -1)){
            return true;
        }
        return false;
    }

    public boolean onSameKey(Operation otherOp){
        if(this.getKey().equals(otherOp.getKey())){
            return true;
        }
        return false;
    }

    public boolean notEqual(Operation otherOp){
        if(this.getID() != otherOp.getID()){
            return true;
        }
        return false;
    }

    public void flushCoList(){
        if(this.getCoList()!= null) {
            BitSet tempList = new BitSet(this.getCoList().size());
            this.setCoList(tempList);
        }
    }

    @Override
    public String toString(){
        return "Type:"+ this.getType()+ " Key:" + this.getKey() +" Value:" + this.getValue();
    }

    public String easyPrint(){
        String tempString = "";
        if(this.isWrite()){
            tempString += "w("+this.getKey()+")"+this.getValue()+"; ";
        }
        else if(this.isRead()){
            tempString += "r("+this.getKey()+")"+this.getValue()+"; ";
        }
        else{
            tempString = "err";
        }

        return tempString;
    }

    public String printOpInfo(){
        return ":type " + this.getType() +", :key " + this.getKey() + ", :value " + this.getValue() + ", :process " + this.getProcess() + ", :time " + this.getTime() + ", :position " + this.getPosition() +", :index " + this.getID();
    }

    public static void main(String args[]){

        System.out.println("Hello?");
    }


}
