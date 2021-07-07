import Checker.CCChecker;
import Checker.CCvChecker;
import Checker.CMChecker;
import DBConnector.MyMongoClient;
import History.History;
import History.HistoryReader;
import HistoryProducer.CCProducer;
import HistoryProducer.YCSBProducer;
import Relations.*;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;

import java.io.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;



public class Main {

    public static void appendLog(String fileName, String content) throws IOException{
        try {
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception{

        System.out.println("-----------------Begin Workload Part-----------------");

        int opNum = 500;

        YCSBProducer producer = new YCSBProducer(200,0.75,0.25,10,100,5,"uniform","uniform");
        System.out.println("Begin to produce history...");
        producer.generatePath();
        producer.generateHistory();
        producer.printToFile();
        System.out.println("History is printed to file, log path:" + producer.getOutputPath());
        System.out.println("Total Read:" + producer.getReadCount() + " Total Write:" + producer.getWriteCount());
        String workloadPath = producer.getOutputPath();

        System.out.println("-----------------End Workload Part-------------------");

        System.out.println("----------------Begin Running Part-------------------");
        String mongoLog = "target/ParameterChoosing/debug/mongoLogfile_0703" + "_opNum" + opNum + "_processNum" + producer.getProcessNum() + ".edn";
        MyMongoClient client = new MyMongoClient("mongodb://dh:dh@n3.disalg.cn:26011/?maxIdleTimeMS=60000", "test_mongo", "original_data",
        50, mongoLog, ReadConcern.MAJORITY, WriteConcern.MAJORITY);
        client.loadAndRun(workloadPath);
        client.closeConnection();
        System.out.println("Run with mongo, logfile: " + mongoLog);
        System.out.println("----------------End Running Part-------------------");

        System.out.println("----------------Begin Checking Part-------------------");
        System.out.println("Checking causal consistency for:" + mongoLog);

        boolean ccResult;
        boolean ccvResult;
        boolean cmResult;

        HistoryReader reader = new HistoryReader(mongoLog);
        History history = new History(reader.readHistory());
        history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...

        if (!history.isDifferentiated()) {
            System.out.println("Detected not differentiated.");
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

        if (ico.isCyclicCO()) {
            System.out.println("Cannot compare matrix! Reason: isCyclicCO:" + ico.isCyclicCO());
        } else {
            boolean coEquality = bco.checkEqualDebug(ico, history.getOperationList());
            if (!coEquality) {
                System.out.println("ico is not equal to bco???");
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
        System.out.println("Chekcing CC, result:" + ccResult);
        System.out.println("bco time:" + bcoTime);
        System.out.println("ico time:" + icoTime);

        if (ccChecker.isCyclicCO()) {
            System.out.println("CyclicCO detected!");
        } else if (ccChecker.isThinAirRead()) {
            System.out.println("ThinAirRead detected!");
        } else if (ccChecker.isWriteCOInitRead()) {
            System.out.println("WriteCOInitRead detected!");
        } else if (ccChecker.isWriteCORead()) {
            System.out.println("WriteCORead detected!");
        }


        startTime = System.nanoTime();
        ConflictRelation cf = new ConflictRelation(history.getOpNum());
        cf.caculateConflictRelation(history, bco);
        endTime = System.nanoTime();
        long cfTime = endTime - startTime;

        CCvChecker ccvChecker = new CCvChecker(history, po, rf, bco, cf);
        ccvResult = ccvChecker.checkCCv();
        System.out.println("Chekcing CCv, result:" + ccvResult);
        System.out.println("cf time:" + cfTime);

        startTime = System.nanoTime();
        IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
        ihbo.incrementalHBO(history, po, rf, ico);
        endTime = System.nanoTime();
        long ihboTime = endTime - startTime;

        startTime = System.nanoTime();
        BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
        hbo.calculateHBo(history, po, rf, ico);
        endTime = System.nanoTime();
        long bhboTime = endTime - startTime;


        //如果包含这三种非法模式，ihbo不能完整计算得到每个线程hbo的关系矩阵
        if (ihbo.isCyclicCO() || ihbo.isThinAirRead() || ihbo.isCyclicHB()) {
            System.out.println("Cannot compare matrix! Reason: isCyclicCO:" + ihbo.isCyclicCO() + " isThinAirRead:" + ihbo.isThinAirRead() + " isCyclicHB:" + ihbo.isCyclicHB());
            boolean hboEquality = hbo.checkEqual(ihbo);
            if (!hboEquality) {
                System.out.println("ihbo is not equal to bhbo???");
                System.exit(-1);
            } else {
                System.out.println("ihbo is equal to bhbo ^.^");
            }
        }

        CMChecker cmChecker = new CMChecker(history, po, rf, bco, hbo);
        cmResult = cmChecker.checkCM();
        System.out.println("Checking CM, result:" + cmResult);
        System.out.println("ihbo time:" + ihboTime/100000000 +"." + ihboTime%100000000+ "s");
        System.out.println("bhbo time:" + bhboTime/100000000 +"." + bhboTime%100000000+ "s");


        System.out.println("----------------End Checking Part-------------------");

    }

}
