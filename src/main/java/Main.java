import Checker.CCChecker;
import Checker.CCvChecker;
import Checker.CMChecker;
import DBConnector.MyMongoClient;
import History.History;
import History.HistoryReader;
import HistoryProducer.YCSBProducer;
import Relations.*;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;

import java.io.*;
import java.io.IOException;
import java.util.Calendar;


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

    public static void main(String[] args) throws Exception{
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String globalLog = "target/Data/GlobalLog_"+month+"_"+day+".txt";
//        for(int i = 1; i <= 10; i++) { //控制轮数
        for(int i = 1; i <= 1; i++) { //控制轮数
            System.out.println("Round " + i);
            appendLog(globalLog, "Round" + i);
//            for (int k = 1; k <= 30; k++) { //控制操作数
            for (int k = 1; k <= 5; k++) { //控制操作数
                int opNum = 0;
                if (0 < k && k <= 15) {
                    opNum = 100 * k;
                } else if (k % 5 == 0) {
                    opNum = 100 * k;
                } else {
                    continue;
                }
//                for(int j = 1; j <=4; j++) {
                for(int j = 1; j <=1; j++) {
                    int processNum = j*5;

                    System.out.println("-----------------Begin Workload Part-----------------");

                    YCSBProducer producer = new YCSBProducer(opNum, 0.75, 0.25, 10, 100, processNum, "uniform", "uniform");
                    System.out.println("Begin to produce history...");
                    producer.generatePath();
                    producer.generateHistory();
                    producer.printToFile();
                    System.out.println("History is printed to file, log path:" + producer.getOutputPath());
                    System.out.println("Total Read:" + producer.getReadCount() + " Total Write:" + producer.getWriteCount());
                    String workloadPath = producer.getOutputPath();

                    System.out.println("-----------------End Workload Part-------------------");

                    System.out.println("----------------Begin Running Part-------------------");

                    String mongoLog = "target/ParameterChoosing/debug/mongoLogfile_" + month + "_" + day + "_opNum" + producer.getOpNum() + "_processNum" + producer.getProcessNum() + ".edn";
                    MyMongoClient client = new MyMongoClient("mongodb://dh:dh@n1.disalg.cn:26011,n3.disalg.cn:26011,n6.disalg.cn:26011/?maxIdleTimeMS=60000", "test_mongo", "original_data",
                            50, mongoLog, ReadConcern.MAJORITY, WriteConcern.MAJORITY);
//                    MyMongoClient client = new MyMongoClient("mongodb://dh:dh@n0.disalg.cn:29004,n2.disalg.cn:29004,n4.disalg.cn:29004/?maxIdleTimeMS=60000", "test_mongo", "original_data",
//                            50, mongoLog, ReadConcern.MAJORITY, WriteConcern.MAJORITY);
                    //访问分片集群时，要确保MongoDB URI里包含2个及以上的mongos地址，来实现负载均衡及高可用

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
                    System.out.println("Checking CC, result:" + ccResult);
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
                    System.out.println("Checking CCv, result:" + ccvResult);
                    System.out.println("cf time:" + cfTime);

                    startTime = System.nanoTime();
                    IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum(), 2);
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
//        System.out.println("ihbo time:" + ihboTime/100000000 +"." + ihboTime%100000000+ "s");
//        System.out.println("bhbo time:" + bhboTime/100000000 +"." + bhboTime%100000000+ "s");
                    System.out.println("ihbo time:" + ihboTime);
                    System.out.println("bhbo time:" + bhboTime);


                    String newPath = mongoLog;
//                    String dic;
                    File oldFile = new File(newPath);

                    newPath = newPath.replace("ParameterChoosing/", "ParameterChoosing/mongoSpecial/" + producer.getOpNum() + "/");
                    String dic = newPath.substring(0, newPath.lastIndexOf("/"));
                    File newDic = new File(dic);
                    if (!newDic.exists()) {
                        newDic.mkdirs();
                    }

                    File newFile = new File(newPath);
                    if (newFile.exists()) {
                        newPath = newPath.replace(".edn", "_1.edn");
                        newFile = new File(newPath);
                        String version;
                        int v;
                        while (newFile.exists()) { //如果不是第一次创建同名文件，则迭代号进行更新即可
                            version = newPath.substring(newPath.lastIndexOf("_") + 1, newPath.lastIndexOf("."));
                            v = Integer.valueOf(version) + 1;
                            newPath = newPath.replace("_" + version + ".edn", "_" + v + ".edn");
                            newFile = new File(newPath);
                        }
                    }
                    appendLog(globalLog, "----------------------------------------------------------");
                    System.out.println("File Location:" + newPath);
                    appendLog(globalLog,"File Location:" + newPath);
                    appendLog(globalLog, "Checking " + newPath);
                    appendLog(globalLog, "CC Result: " + ccResult);
                    appendLog(globalLog, "CCv Result: " + ccvResult);
                    appendLog(globalLog, "CM Result: " + cmResult);
                    appendLog(globalLog, "BCO time: " + bcoTime);
                    appendLog(globalLog, "ICO time: " + icoTime);
                    appendLog(globalLog, "CF time: " + cfTime);
                    appendLog(globalLog, "Basic HBo time: " + bhboTime);
                    appendLog(globalLog, "Incremental HBo time: " + ihboTime);

                    System.out.println("----------------End Checking Part-------------------");
                }
            }
        }

    }

}
