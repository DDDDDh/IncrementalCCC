import Checker.CCChecker;
import Checker.CCvChecker;
import Checker.CMChecker;
import History.History;
import History.HistoryReader;
import HistoryProducer.CCProducer;
import HistoryProducer.LinProducer;
import Relations.*;

import java.io.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class generateAndTest {

    public static void main(String args[]) throws Exception {

        String stressTestOut = "target/RandomHistories/StressTestLogfile.txt";
        String logPath = "target/RandomHistories/CheckingLogfile.txt";
        File outfile = new File(stressTestOut);
        PrintWriter output = new PrintWriter(outfile);
        output.println("-----Begin Stress Test-----");
        System.out.println("-----Begin Stress Test-----");
        boolean ccResult;
        boolean ccvResult;
        boolean cmResult;

//        int opNum = 100;
//        CCProducer ccProducer = new CCProducer(opNum, 5, 3, 1, 10, 100);
//        ccProducer.generatePath();
//        ccProducer.generateCCHistory();
//        ccProducer.printToFile();
//        ccProducer.printToFileDebug(1);

        int opNum = 1000;
        LinProducer linProducer = new LinProducer(opNum, 10, 3,1);
        linProducer.generatePath();
        linProducer.generateLinHistory();
        linProducer.printToFile();
        linProducer.printToFileDebug(2);
        String url = linProducer.getOutputPath();

//        String url = "/Users/yi-huang/Project/IncrementalCCC/target/RandomHistories/Running_202111020_opNum20_processNum5_rRate3_wRate1.edn";
//        String url = ccProducer.getOutputPath();
        HistoryReader reader = new HistoryReader(url);
//
        History history = new History(reader.readHistory());
        history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...

        if (!history.isDifferentiated()) {
            System.out.println("Detected not differentiated.");
            output.println("Detected not differentiated.");
        }

        ProgramOrder po = new ProgramOrder(history.getOpNum());
        po.calculateProgramOrder(history);

        ReadFrom rf = new ReadFrom(history.getOpNum());
        rf.calculateReadFrom(history);

        long startTime = System.nanoTime();
        BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
        bco.computeCO(history, po, rf);
        long endTime = System.nanoTime();
        long bcoTime = endTime - startTime;


        startTime = System.nanoTime();
        IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
        ico.incrementalCO(history, po, rf);
        endTime = System.nanoTime();
        long icoTime = endTime - startTime;

        if(ico.isCyclicCO()){
            System.out.println("Cannot compare matrix! Reason: isCyclicCO:" + ico.isCyclicCO());
        }
        else {
            boolean coEquality = bco.checkEqualDebug(ico, history.getOperationList());
            if (!coEquality) {
                System.out.println("ico is not equal to bco???");
                output.println("ico is not equal to bco???");
                System.out.println("ico matrix:");
                ico.printMatrix();
                System.out.println("bco matrix:");
                bco.printMatrix();
            } else {
                System.out.println("ico is equal to bco ^.^");
            }
        }

        CCChecker ccChecker = new CCChecker(history, po, rf, bco);
        ccResult = ccChecker.checkCC();
        output.println("Chekcing CC, result:" + ccResult);
        System.out.println("Chekcing CC, result:" + ccResult);

        if (ccChecker.isCyclicCO()) {
            output.println("CyclicCO detected!");
        } else if (ccChecker.isThinAirRead()) {
            output.println("ThinAirRead detected!");
        } else if (ccChecker.isWriteCOInitRead()) {
            output.println("WriteCOInitRead detected!");
        } else if (ccChecker.isWriteCORead()) {
            output.println("WriteCORead detected!");
        }


        startTime = System.nanoTime();
        ConflictRelation cf = new ConflictRelation(history.getOpNum());
        cf.caculateConflictRelation(history, bco);
        endTime = System.nanoTime();
        long cfTime = endTime - startTime;

        CCvChecker ccvChecker = new CCvChecker(history, po, rf, bco, cf);
        ccvResult = ccvChecker.checkCCv();
        output.println("Chekcing CCv, result:" + ccvResult);
        System.out.println("Chekcing CCv, result:" + ccvResult);


        startTime = System.nanoTime();
        IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
        ihbo.incrementalHBO(history,po, rf, bco);
        endTime = System.nanoTime();
        long ihboTime = endTime - startTime;

        startTime = System.nanoTime();
        BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
        hbo.calculateHBo(history, po, rf, bco);
        endTime = System.nanoTime();
        long bhboTime = endTime - startTime;



        //如果包含这三种非法模式，ihbo不能完整计算得到每个线程hbo的关系矩阵
        if(ihbo.isCyclicCO() || ihbo.isThinAirRead() || ihbo.isCyclicHB()){
            System.out.println("Cannot compare matrix! Reason: isCyclicCO:" + ihbo.isCyclicCO() + " isThinAirRead:" + ihbo.isThinAirRead() + " isCyclicHB:" + ihbo.isCyclicHB());
        }
        else {
            boolean hboEquality = hbo.checkEqual(ihbo);
            if (!hboEquality) {
                System.out.println("ihbo is not equal to bhbo???");
                output.println("ihbo is not equal to bhbo???");
            } else {
                System.out.println("ihbo is equal to bhbo ^.^");
            }
        }

        CMChecker cmChecker = new CMChecker(history, po, rf, bco, hbo);
        cmResult = cmChecker.checkCM();
        output.println("Checking CM, result:" + cmResult);
        System.out.println("Checking CM, result:" + cmResult);
        System.out.println("ihboTime:" + ihboTime);
        System.out.println("bhboTime:" + bhboTime);
        output.close();
    }
}
