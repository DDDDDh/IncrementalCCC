package History;

import java.util.BitSet;

public class Operation {

    public enum OpType{
        write, read, other
    }

    private OpType type;
    private String key;
    private int value;
    private int process;
    private long time;
    private long position;
    private int id; //index等价于每个操作的全局标识符，很重要
    private BitSet visList;
    private int correspondingWriteIndex; //只有该操作是读操作时，对应的写操作下标才有意义，初值为-1；


    Operation(){
        this.setType(OpType.other);
        this.setKey("null");
        this.setValue(-1);
        this.setProcess(-1);
        this.setTime(-1);
        this.setPosition(-1);
        this.setID(-1);
        this.setVisList(null);
        this.setCorrespondingWriteIndex(-1);
    }

    Operation(String type, String key, int value, int process, long time, long position, int index){
        if(type.equals(":write")){
            this.setType(OpType.write);
        }
        else if(type.equals(":read")){
            this.setType(OpType.read);
            if(value == -1){ //读入初值的读操作，对应写操作下标初始化为-2；
                this.setCorrespondingWriteIndex(-2);
            }
            else {
                this.setCorrespondingWriteIndex(-1); //否则，将读操作的对应写操作下标初始化为-1；
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
    }

    public void copyOperation(Operation otherOp){
        this.setType(otherOp.getType());
        this.setKey(otherOp.getKey());
        this.setValue(otherOp.getValue());
        this.setProcess(otherOp.getProcess());
        this.setTime(otherOp.getTime());
        this.setPosition(otherOp.getPosition());
        this.setID(otherOp.getID());
        this.setVisList(otherOp.getVisList());
        this.setCorrespondingWriteIndex(otherOp.getCorrespondingWriteIndex());
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
    public void setValue(int value){
        this.value = value;
    }
    public int getValue(){
        return this.value;
    }
    public void setProcess(int process){
        this.process = process;
    }
    public int getProcess(){
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
    public void setID(int index){
        this.id = index;
    }
    public void setCorrespondingWriteIndex(int index){this.correspondingWriteIndex = index;}
    public int getCorrespondingWriteIndex(){return this.correspondingWriteIndex;}
    public int getID(){
        return this.id;
    }
    public void setVisList(BitSet bitSet){this.visList = bitSet;}

    //此函数需要在知道一共有多少个操作后再调用
    public void initialVisList(int size){
        this.visList = new BitSet(size);
    }

    public BitSet getVisList(){
        return this.visList;
    }

    public boolean isWrite(){
        if(this.type == OpType.write){
            return true;
        }
        return false;
    }

    public boolean isInitWrite(){
        if(this.type == OpType.write && this.value == 0){
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


    public static void main(String args[]){

        System.out.println("Hello?");
    }


}
