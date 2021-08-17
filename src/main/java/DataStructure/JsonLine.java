package DataStructure;
import HistoryProducer.GeneratedOperation;
import lombok.*;
import History.*;

//Define the content of each line in logfile

@Data
public class JsonLine {
    private String type;
    private Operation.OpType opType;
    private String key;
    private long value;
    private long process;
    private long time;
    private long position;
    private long index;

    public JsonLine(String type, String opType, String key, int value, int process, long time, long position, int index){
        this.setType(type.substring(1)); //去掉一开始的冒号
        if(opType.equals(":write")){
            this.setOpType(Operation.OpType.write);
        }
        else if(opType.equals(":read")){
            this.setOpType(Operation.OpType.read);
        }
        else{
            this.setOpType(Operation.OpType.other);
        }
        this.setKey(key);
        this.setValue(value);
        this.setProcess(process);
        this.setTime(time);
        this.setPosition(position);
        this.setIndex(index);
    }

    public JsonLine(GeneratedOperation operation){
        this.setType("ok");
        if(operation.isWrite()){
            this.setOpType(Operation.OpType.write);
        }
        else if(operation.isRead()){
            this.setOpType(Operation.OpType.read);
        }
        this.setKey(operation.getVariable());
        this.setValue(operation.getValue());
        this.setProcess(operation.getProcess());
        this.setPosition(operation.getPosition());
        this.setIndex(operation.getIndex());
    }

    public JsonLine(Operation op, int index){
        this.setOpType(op.getType());
        this.setType("ok");
        this.setKey(op.getKey());
        this.setValue(op.getValue());
        this.setProcess(op.getProcess());
        this.setPosition(op.getPosition());
        this.setIndex(index);
    }

}
