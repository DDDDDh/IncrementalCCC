import Checker.*;
import History.*;
import Relations.*;

import java.util.*;
import java.io.*;


public class Debugger {

    public static void main (String args[]) throws Exception{

//      String curFile = "/Users/yi-huang/Project/MongoTrace/store/test2_majority_majority_no-nemesis_2000-5000/Part1/history2000.edn";
//        String curFile = "/Users/yi-huang/Project/MongoTrace/store/test3_majority_majority_node-failure_2000-5000/history2000.edn";
//        String curFile = "/Users/yi-huang/Project/IncrementalCCC/src/main/resources/BadPatternExamples/CyclicHB_history.edn";
        String curFile = "/Users/yi-huang/Project/MongoTrace/1227/no-memesis/majority-linearizable/100-3000/history200.edn";
//        String curFile = "/Users/yi-huang/Project/MongoTrace/mongo-causal-register-wc-:majority-rc-:majority-ti-180-sd-2-cry-10-wp-0.25-rp-0.75-ops-600-no-nemesis_202012225/history600_0.edn";
        String logPath = "/Users/yi-huang/Project/MongoTrace/store/DebuggerLogfile.txt";
        File outfile = new File(logPath);
        PrintWriter output = new PrintWriter(outfile);

        System.out.println("Checking file:" + curFile);
        output.println("Checking file:" + curFile);
        HistoryReader reader = new HistoryReader(curFile);
        History history = new History(reader.readHistory());
        history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后设置总操作数
        if (!history.isDifferentiated()) {
            System.out.println("Detected not differentiated.");
            System.exit(-1);
        }

        ProgramOrder po = new ProgramOrder(history.getOpNum());
        po.calculateProgramOrder(history);
        ReadFrom rf = new ReadFrom(history.getOpNum());
        rf.calculateReadFrom(history);

        System.out.println("Total op num:" + reader.getTotalNum());
        System.out.println("Process num:" + history.getProcessOpList().keySet().size());
        output.println("Total op num:" + reader.getTotalNum());
        output.println("Process num:" + history.getProcessOpList().keySet().size());

        long startTime = System.nanoTime();
        BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
//            System.out.println("Finish initialization of bco");
        bco.computeCO(history, po, rf);
        long endTime = System.nanoTime();
        System.out.println("bco Time:" + (endTime - startTime) + "ns");
        output.println("bco Time:" + (endTime - startTime) + "ns");

        startTime = System.nanoTime();
        IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
        ico.incrementalCO(history, po, rf);
        endTime = System.nanoTime();
        System.out.println("ico Time:" + (endTime - startTime) + "ns");
        output.println("ico Time:" + (endTime - startTime) + "ns");



        boolean coEquality = bco.checkEqualDebug(ico, history.getOperationList());
        if(!coEquality){
            System.out.println("ico is not equal to bco???");
            output.println("ico is not equal to bco???");
            System.out.println("ico matrix:");
            ico.printMatrix();
            System.out.println("bco matrix:");
            bco.printMatrix();
        }
        else{
            System.out.println("ico is equal to bco ^.^");
        }


        CCChecker ccChecker = new CCChecker(history, po, rf, bco);

        System.out.println("Begin to compute conflict relation");
        startTime = System.nanoTime();
        ConflictRelation cf = new ConflictRelation(history.getOpNum());
        cf.caculateConflictRelation(history, bco);
        endTime = System.nanoTime();
        System.out.println("cf Time:" + (endTime - startTime) + "ns");
        output.println("cf Time:" + (endTime - startTime) + "ns");

        CCvChecker ccvChecker = new CCvChecker(history, po, rf, ico, cf);


        System.out.println("Begin to compute happen-before relation");

        startTime = System.nanoTime();
        IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
        ihbo.incrementalHBO(history, po, rf, ico);
        endTime = System.nanoTime();
        System.out.println("ihbo Time:" + (endTime - startTime) + "ns");
        output.println("ihbo Time:" + (endTime - startTime) + "ns");


        startTime = System.nanoTime();
        BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
        hbo.calculateHBo(history, po, rf, ico);
        endTime = System.nanoTime();
        System.out.println("bhbo Time:" + (endTime - startTime) + "ns");
        output.println("bhbo Time:" + (endTime - startTime) + "ns");
//        System.out.println("Finish computation of happen-before relation");


        boolean hboEquality = hbo.checkEqual(ihbo);
        if(!hboEquality){
            System.out.println("ihbo is not equal to bhbo???");
            output.println("ihbo is not equal to bhbo???");
        }
        else{
            System.out.println("ihbo is equal to bhbo ^.^");
        }
//
//
        CMChecker cmChecker = new CMChecker(history, po, rf, ico, ihbo);

        if (ccChecker.checkCC()) {
            System.out.println("Chekcing CC, result:true");
            output.println("Chekcing CC, result:true");
        } else {
            System.out.println("Chekcing CC, result:false");
            System.out.println("Fail Reason:" + ccChecker.failReason());
            output.println("Chekcing CC, result:false");
            output.println("Fail Reason:" + ccChecker.failReason());
        }

        if (ccvChecker.checkCCv()) {
            System.out.println("Chekcing CCv, result:true");
            output.println("Chekcing CCv, result:true");
        } else {
            System.out.println("Chekcing CCv, result:false");
            System.out.println("Fail Reason:" + ccvChecker.failReason());
            output.println("Chekcing CCv, result:false");
            output.println("Fail Reason:" + ccvChecker.failReason());
        }

        if (cmChecker.checkCM()) {
            System.out.println("Checking CM, result:true");
            output.println("Checking CM, result:true");
        } else {
            System.out.println("Chekcing CM, result:false");
            System.out.println("Fail Reason:" + cmChecker.failReason());
            output.println("Chekcing CM, result:false");
            output.println("Fail Reason:" + cmChecker.failReason());
        }

        System.out.println("---------------------------------------------------------");
        output.println("---------------------------------------------------------");
    }
}
