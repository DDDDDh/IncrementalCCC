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
    private int index;
    private BitSet visList;


    Operation(String type, String key, int value, int process, long time, long position, int index){
        if(type.equals(":write")){
            this.setType(OpType.write);
        }
        else if(type.equals(":read")){
            this.setType(OpType.read);
        }
        else{
            this.setType(OpType.other);
        }
        this.setKey(key);
        this.setValue(value);
        this.setProcess(process);
        this.setTime(time);
        this.setPosition(position);
        this.setIndex(index);
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
    public void setIndex(int index){
        this.index = index;
    }
    public int getIndex(){
        return this.index;
    }
    //此函数需要在知道一共有多少个操作后再调用
    public void initialVisList(int size){
        this.visList = new BitSet(size);
    }
    public BitSet getVisList(){
        return this.visList;
    }

    @Override
    public String toString(){
        return "Type:"+ this.getType()+ " Key:" + this.getKey() +" Value:" + this.getValue();
    }


    public static void main(String args[]){

        System.out.println("Hello?");
    }


}
