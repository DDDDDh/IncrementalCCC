import History.*;
import Relations.*;

import java.io.IOException;

public class CausalChecker {



    public static void main(String args[]) throws IOException {
        String url = "src/main/resources/hy_history.edn";
        HistoryReader reader = new HistoryReader(url);
//        LinkedList<Operation> opList = reader.readHistory();
//        for(int i = 0; i < opList.size(); i++){
//            System.out.println(i+opList.get(i).toString());
//        }
        History history = new History(reader.readHistory());
        history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...
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

        System.out.println("Total op num:" + reader.getTotalNum());

        long startTime = System.nanoTime();
        IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
        ico.incrementalCO(history, po, rf);
        long endTime = System.nanoTime();
        System.out.println("Running time of incremental computation of co:" + (endTime - startTime) + "ns");

        startTime = System.nanoTime();
        BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
        System.out.println("Finish initialization of bco");
        bco.computeCO(history, po, rf);
        endTime = System.nanoTime();
        System.out.println("Running time of brute-force computation of co:" + (endTime - startTime) + "ns");

        BasicCausalOrder bco1 = new BasicCausalOrder(history.getOpNum());
        bco1.computeCOBFS(history, po, rf);

        if(!bco1.checkEqualDebug(bco, history.getOperationList())){
            System.out.println("Error! The causal orders are not equal!");
        }

        ConflictRelation cf = new ConflictRelation(history.getOpNum());
        cf.caculateConflictRelation(history, bco);
    }
}