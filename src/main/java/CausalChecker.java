import Checker.CCChecker;
import Checker.CCvChecker;
import Checker.CMChecker;
import History.*;
import Relations.*;

import java.io.IOException;

public class CausalChecker {



    public static void main(String args[]) throws Exception {
//        String url = "src/main/resources/hy_history.edn";
//        String url = "src/main/resources/SpecialCases/CCvNotCM_history.edn";
//        String url = "src/main/resources/BadPatternExamples/CyclicHB2_history.edn";
        String url = "src/main/resources/RandomHistories/Running_202011518_opNum100_processNum5_rRate3_wRate1.edn";

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
            System.exit(-1);
        }
//        history.testProtection();
        ProgramOrder po = new ProgramOrder(history.getOpNum());
        po.calculateProgramOrder(history);
//        po.printMatrx();
        ReadFrom rf = new ReadFrom(history.getOpNum());
        rf.calculateReadFrom(history);
//        rf.printMatrx();

        System.out.println("Total op num:" + reader.getTotalNum());

        long startTime = System.nanoTime();
        IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
        ico.incrementalCO(history, po, rf);
        long endTime = System.nanoTime();
        System.out.println("Running time of incremental computation of co:" + (endTime - startTime) + "ns");
//        System.out.println("ico Matrix:");
//        ico.printMatrix();

        startTime = System.nanoTime();
        BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
        System.out.println("Finish initialization of bco");
        bco.computeCO(history, po, rf);
        endTime = System.nanoTime();
        System.out.println("Running time of brute-force computation of co:" + (endTime - startTime) + "ns");
//        System.out.println("bco Matrix:");
//        bco.printMatrix();

        System.out.println("---Begin to check CC---");
        CCChecker ccChecker = new CCChecker(history, po, rf, ico);
        System.out.println("Chekcing CC, result:" + ccChecker.checkCC());

//        BasicCausalOrder bco1 = new BasicCausalOrder(history.getOpNum());
//        bco1.computeCOBFS(history, po, rf);
//
//        if(!bco1.checkEqualDebug(bco, history.getOperationList())){
//            System.out.println("Error! The causal orders are not equal!");
//        }


        System.out.println("Begin to compute conflict relation");
        ConflictRelation cf = new ConflictRelation(history.getOpNum());
        cf.caculateConflictRelation(history, bco);
//        System.out.println("Finish computation of conflict relation");
//        cf.printMatrix();
        System.out.println("---Begin to check CCv---");
        CCvChecker ccvChecker = new CCvChecker(history, po, rf, ico, cf);
        System.out.println("Chekcing CCv, result:" + ccvChecker.checkCCv());

        System.out.println("Begin to compute happen-before relation");
        BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
        hbo.calculateHBo(history, po, rf, ico);
        System.out.println("Finish computation of happen-before relation");


//        IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
//        ihbo.incrementalHBO(history, po, rf);
        System.out.println("---Begin to check CM---");
        CMChecker cmChecker = new CMChecker(history, po, rf, ico, hbo);
        System.out.println("Checking CM, result:" + cmChecker.checkCM());


    }
}
