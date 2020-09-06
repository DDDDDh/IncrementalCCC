
/*
从存储历史记录的文件中读取每个操作，并且返回一个操作的列表opList
 */



package History;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import Relations.*;
import org.apache.commons.lang3.StringUtils;


public class HistoryReader {

    final String KEY_TYPE = ":type ";
    final String KEY_F = " :f ";
    final String KEY_VALUE = " :value ";
    final String KEY_PROCESS = " :process ";
    final String KEY_TIME = " :time ";
    final String KEY_POSITION = " :position ";
    final String KEY_LINK = " :link ";

    private String url;
    private int idx;

    public HistoryReader(String url){
        this.url = url;
        this.idx = 0;
    }

    public LinkedList<Operation> readHistory() throws IOException{
        System.out.println("Reading history in " + url);
        LinkedList<Operation> opList = new LinkedList<Operation>();
        BufferedReader in = new BufferedReader(new FileReader(this.url));
        String line;
        while ((line = in.readLine()) != null) {
            line = StringUtils.strip(line, "{}");
            Operation op = this.getOperation(line);
            if (op != null) {
                opList.add(op);
            }
        }
        in.close();
        return opList;
    }

    public Operation getOperation(String line) {
        String[] subs = line.split(",");
        String type = StringUtils.remove(subs[0], KEY_TYPE);
        if (!type.equals(":ok")) { //如果不是正常返回的操作，则忽略
            return null;
        }
        String f = StringUtils.remove(subs[1], KEY_F);
        String val = StringUtils.remove(subs[2], KEY_VALUE);
        String[] kv = StringUtils.strip(val, "[]").split(" ");
        String key = kv[0];
        int value;
        if(kv[1].equals("nil")){
            value = -1;
        }
        else{
            value = Integer.parseInt(kv[1]);
        }
        int process = Integer.parseInt(StringUtils.remove(subs[3], KEY_PROCESS));
        long time = Long.parseLong(StringUtils.remove(subs[4], KEY_TIME));
        long position = Long.parseLong(StringUtils.remove(subs[5], KEY_POSITION));
        int index = idx++; //注意，这个index是读入正常操作之后进行的另外编号，与历史记录里本身的index无关
        return new Operation(f, key, value, process, time, position, index);
    }

    public static void main(String args[]) throws IOException{
        String url = "src/main/resources/small_history.edn";
        HistoryReader reader = new HistoryReader(url);
//        LinkedList<Operation> opList = reader.readHistory();
//        for(int i = 0; i < opList.size(); i++){
//            System.out.println(i+opList.get(i).toString());
//        }
        History history = new History(reader.readHistory());
        history.setOpNum(reader.idx); //读取完一个历史记录之后，一定一定要记得设置总操作数...
//        history.printOpGroupByKey();
//        history.printWriteReadHistories();
//        history.printOpGroupByProcess();
        if(!history.isDifferentiated()){
            System.out.println("Detected not differentiated.");
        }
//        history.testProtection();
        ProgramOrder po = new ProgramOrder(history.getOpNum());
        po.caculateProgramOrder(history);
//        po.printMatrx();
        ReadFrom rf = new ReadFrom(history.getOpNum());
        rf.caculateReadFrom(history);
//        rf.printMatrx();


        long startTime1 = System.nanoTime();
        IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
        ico.incrementalCO(history, po, rf);
        long endTime1 = System.nanoTime();
        System.out.println("Running time of incremental computation of co:" + (endTime1 - startTime1) + "ns");

        long startTime2 = System.nanoTime();
        BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
        bco.computeCO(history, po, rf);
        long endTime2 = System.nanoTime();
        System.out.println("Running time of brute-force computation of co:" + (endTime2 - startTime2) + "ns");

        if(!ico.checkEqual(bco)){
            System.out.println("Error! The causal orders are not equal!");
        }

        ConflictRelation cf = new ConflictRelation(history.getOpNum());
        cf.caculateConflictRelation(history, bco);


    }

}
