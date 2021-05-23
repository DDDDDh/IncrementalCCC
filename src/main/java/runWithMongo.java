import Checker.CCChecker;
import Checker.CCvChecker;
import Checker.CMChecker;
import DBConnector.mongoConnector;
import History.History;
import History.HistoryReader;
import HistoryProducer.CCProducer;
import HistoryProducer.LinProducer;
import Relations.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class runWithMongo {

    public static void appendLog(String fileName, String content) throws IOException {
        try {
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.write("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {

//        for(int k = 1; k <= 5; k++) {

//        int k = 1;
            int processNum = 5;
            int opNum = 1000;
//

//            for (int j = 5; j >= 1; j--) {

//                int varRange = j*5;
//                if(k == 3 && varRange > 10){
//                    continue;
//                }
            int varRange = 10;


//                for (int i = 10; i >= 1; i--) {

//                    int valRange = i*50;
            int valRange = 50;

//                    if(varRange*valRange <= opNum/4){
//                        continue;
//                    }
//



                    String stressTestOut = "target/ParameterChoosing/debug/StressTestLogfile0512_process"+ processNum + "_var"+ varRange + "_val" + valRange  +"_opNum"+ opNum +".txt";
                    File outfile = new File(stressTestOut);
                    PrintWriter logout = new PrintWriter(outfile);
                    String mongoCheckLog = "target/ParameterChoosing//debug/StressTestMongoCheckLog0512_process"+ processNum + "_var"+ varRange + "_val" + valRange  +"_opNum"+ opNum +".txt";

                    logout.println("-----Begin Stress Test-----Round OpNum: "+opNum);
                    System.out.println("-----Begin Stress Test-----Round OpNum: "+ opNum);


                    CCProducer ccProducer = new CCProducer(opNum, processNum, 3, 1, varRange, valRange);

                    ccProducer.generatePath();

                    ccProducer.generateCCHistory();

                    ccProducer.printToFile();

                    ccProducer.printToFileDebug(2);

//                    LinProducer linProducer = new LinProducer(opNum, 5,3,1);
//                    linProducer.generatePath();
//                    linProducer.generateLinHistory();
//                    linProducer.printToFile();
//                    LinProducer ccProducer = linProducer; //取个别名，避免修改下文中的变量名

                    String url = ccProducer.getOutputPath();
//                    String url = linProducer.getOutputPath();
                    System.out.println("generated history in: " + url);
                    logout.println("generated history in: " + url);

                    boolean ccResult;
                    boolean ccvResult;
                    boolean cmResult;

//
//                    logout.println("begin to check causal for original history");
//                    //begin
//                    HistoryReader reader = new HistoryReader(url);
//
//                    History history = new History(reader.readHistory());
//                    history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...
//
//                    if (!history.isDifferentiated()) {
//                        System.out.println("Detected not differentiated.");
//                        logout.println("Detected not differentiated.");
//                        continue;
//                    }
//
//                    ProgramOrder po = new ProgramOrder(history.getOpNum());
//                    po.calculateProgramOrder(history);
//
//                    ReadFrom rf = new ReadFrom(history.getOpNum());
//                    rf.calculateReadFrom(history);
//
//                    long startTime = System.nanoTime();
//                    BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
//                    bco.computeCO(history, po, rf);
//                    long endTime = System.nanoTime();
//                    long bcoTime = endTime - startTime;
//
//
//                    startTime = System.nanoTime();
//                    IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
//                    ico.incrementalCO(history, po, rf);
//                    endTime = System.nanoTime();
//                    long icoTime = endTime - startTime;
//
//                    boolean coEquality = bco.checkEqualDebug(ico, history.getOperationList());
//                    if(!coEquality){
//                        System.out.println("ico is not equal to bco???");
//                        logout.println("ico is not equal to bco???");
//                    }
//                    else{
//                        System.out.println("ico is equal to bco ^.^ ");
//                        logout.println("ico is equal to bco ^.^ ");
//                    }
//
//                    CCChecker ccChecker = new CCChecker(history, po, rf, bco);
//                    ccResult = ccChecker.checkCC();
//                    logout.println("Chekcing CC, result:" + ccResult);
//                    System.out.println("Chekcing CC, result:" + ccResult);
//
//
//                    if (ccResult) {
//                    } else {
//                        if (ccChecker.isCyclicCO()) {
//                            logout.println("CyclicCO detected!");
//                        } else if (ccChecker.isThinAirRead()) {
//                            logout.println("ThinAirRead detected!");
//                        } else if (ccChecker.isWriteCOInitRead()) {
//                            logout.println("WriteCOInitRead detected!");
//                        } else if (ccChecker.isWriteCORead()) {
//                            logout.println("WriteCORead detected!");
//                        }
//                        logout.close();
//                        System.exit(-1);
//                    }
//
//                    startTime = System.nanoTime();
//                    ConflictRelation cf = new ConflictRelation(history.getOpNum());
//                    cf.caculateConflictRelation(history, bco);
//                    endTime = System.nanoTime();
//                    long cfTime = endTime - startTime;
//
//
//                    CCvChecker ccvChecker = new CCvChecker(history, po, rf, bco, cf);
//                    ccvResult = ccvChecker.checkCCv();
//                    logout.println("Chekcing CCv, result:" + ccvResult);
//                    System.out.println("Chekcing CCv, result:" + ccvResult);
//                    if (ccvResult) {
//                    }
//                    else{
//                        logout.println("Fail Reason:" + ccvChecker.failReason());
//                    }
//
//                    startTime = System.nanoTime();
//                    BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
////            System.out.println("");
//                    hbo.calculateHBo(history, po, rf, bco);
//                    endTime = System.nanoTime();
//                    long bhboTime = endTime - startTime;
//
//                    startTime = System.nanoTime();
//                    IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
//                    ihbo.incrementalHBO(history,po, rf, bco);
//                    endTime = System.nanoTime();
//                    long ihboTime = endTime - startTime;
//
//                    if(ihbo.isCyclicCO() || ihbo.isThinAirRead() || ihbo.isCyclicHB()){
//                        System.out.println("Cannot compare matrix! Reason: isCyclicCO:" + ihbo.isCyclicCO() + " isThinAirRead:" + ihbo.isThinAirRead() + " isCyclicHB:" + ihbo.isCyclicHB());
//                        logout.println("Cannot compare matrix! Reason: isCyclicCO:" + ihbo.isCyclicCO() + " isThinAirRead:" + ihbo.isThinAirRead() + " isCyclicHB:" + ihbo.isCyclicHB());
//                    }
//                    else {
//                        boolean hboEquality = hbo.checkEqual(ihbo);
//                        if (!hboEquality) {
//                            System.out.println("ihbo is not equal to bhbo???");
//                            logout.println("ihbo is not equal to bhbo???");
//                        } else {
//                            System.out.println("ihbo is equal to bhbo ^.^");
//                            logout.println("ihbo is equal to bhbo -.-");
//                        }
//                    }
//
//                    CMChecker cmChecker = new CMChecker(history, po, rf, bco, hbo);
//                    cmResult = cmChecker.checkCM();
//                    logout.println("Checking CM, result:" + cmResult);
//                    System.out.println("Checking CM, result:" + cmResult);
//                    if (cmResult) {
//                    }
//                    else{
//                        logout.println("Fail Reason:" + cmChecker.failReason());
//                    }
//
//                    //根据判定的结果挪到不同的文件夹中
//                    String newPath = ccProducer.getOutputPath();
//                    File oldFile = new File(newPath);
//                    String dic;
//
//                    if (!ccResult) {
//                        System.out.println("Error! not a cc history!");
//                        System.exit(-1);
//                    } else if (!cmResult && !ccvResult) { //cc but not cm&ccv
//                        newPath = newPath.replace("RandomHistories/", "RandomHistories/CC/" + ccProducer.getOpNum() + "/");
//                        dic = newPath.substring(0, newPath.lastIndexOf("/"));
//                        File newDic = new File(dic);
//                        if (!newDic.exists()) {
//                            newDic.mkdirs();
//                        }
//
//                    } else if (cmResult && !ccvResult) { //cm but not ccv
//                        newPath = newPath.replace("RandomHistories/", "RandomHistories/CMNotCCv/" + ccProducer.getOpNum() + "/");
//                        dic = newPath.substring(0, newPath.lastIndexOf("/"));
//                        File newDic = new File(dic);
//                        if (!newDic.exists()) {
//                            newDic.mkdirs();
//                        }
//                    } else if (ccvResult && !cmResult) { //ccv but not cm
//                        newPath = newPath.replace("RandomHistories/", "RandomHistories/CCvNotCM/" + ccProducer.getOpNum() + "/");
//                        dic = newPath.substring(0, newPath.lastIndexOf("/"));
//                        File newDic = new File(dic);
//                        if (!newDic.exists()) {
//                            newDic.mkdirs();
//                        }
//                    } else { //三种一致性全都符合
//                        newPath = newPath.replace("RandomHistories/", "RandomHistories/AllSatisfied/" + ccProducer.getOpNum() + "/");
//                        dic = newPath.substring(0, newPath.lastIndexOf("/"));
//                        File newDic = new File(dic);
//                        if (!newDic.exists()) {
//                            newDic.mkdirs();
//                        }
//                    }
//                    File newFile = new File(newPath);
//                    if (newFile.exists()) {
//                        newPath = newPath.replace(".edn", "_1.edn");
//                        newFile = new File(newPath);
//                        String version;
//                        int v;
//                        while (newFile.exists()) { //如果不是第一次创建同名文件，则迭代号进行更新即可
//                            version = newPath.substring(newPath.lastIndexOf("_") + 1, newPath.lastIndexOf("."));
//                            v = Integer.valueOf(version) + 1;
//                            newPath = newPath.replace("_" + version + ".edn", "_" + v + ".edn");
//                            newFile = new File(newPath);
//                        }
//                    }
//                    System.out.println("File Location:" + newPath);
//                    logout.println("File Location:" + newPath);
//
//                    oldFile.renameTo(newFile);
//
//                    appendLog(originalCheckLog, "----------------------------------------------------------");
//                    appendLog(originalCheckLog, "Checking " + newPath);
//                    appendLog(originalCheckLog, "CC Result: " + ccResult);
//                    appendLog(originalCheckLog, "CCv Result: " + ccvResult);
//                    appendLog(originalCheckLog, "CM Result: " + cmResult);
//                    appendLog(originalCheckLog, "BCO time: " + bcoTime);
//                    appendLog(originalCheckLog, "ICO time: " + icoTime);
//                    appendLog(originalCheckLog, "CF time: " + cfTime);
//                    appendLog(originalCheckLog, "Basic HBo time: " + bhboTime);
//                    appendLog(originalCheckLog, "Incremental HBo time: " + ihboTime);
//
//                    //end

                    mongoConnector connector = new mongoConnector();
                    String mongoLog = "target/ParameterChoosing/debug/mongoLogfile_0512" + "_opNum" + opNum + "_processNum" + ccProducer.getProcessNum() + ".edn";
//                    connector.mongoRun("mongodb+srv://m220student:m220password@mflix.9pm5g.mongodb.net/test?maxIdleTimeMS=3000&connectTimeoutMS=5000&socketTimeoutMS=5000", "test_mongo", "original_data", newPath, mongoLog);
//                    connector.mongoRun("mongodb://127.0.0.1:27013", "test_mongo", "original_data", url, mongoLog);
                    connector.mongoRun("mongodb://dh:dh@n3.disalg.cn:26011/?maxIdleTimeMS=60000", "test_mongo", "original_data", url, mongoLog);

                    System.out.println("run with mongo, logfile: " + mongoLog);
                    logout.println("run with mongo, logfile: " + mongoLog);
                    logout.println("begin to check causal for mongo history");

                    HistoryReader reader = new HistoryReader(mongoLog);
                    History history = new History(reader.readHistory());
                    history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...

                    if (!history.isDifferentiated()) {
                        System.out.println("Detected not differentiated.");
                        logout.println("Detected not differentiated.");
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
                            logout.println("ico is not equal to bco???");
                            System.out.println("ico matrix:");
                            ico.printMatrix();
                            System.out.println("bco matrix:");
                            bco.printMatrix();
                        } else {
                            System.out.println("ico is equal to bco ^.^");
                            logout.println("ico is equal to bco ^.^");
                        }
                    }

                    CCChecker ccChecker = new CCChecker(history, po, rf, bco);
                    ccResult = ccChecker.checkCC();
                    logout.println("Chekcing CC, result:" + ccResult);
                    System.out.println("Chekcing CC, result:" + ccResult);
                    System.out.println("bco time:" + bcoTime);
                    logout.println("bco time:" + bcoTime);
                    System.out.println("ico time:" + icoTime);
                    logout.println("ico time:" + icoTime);

                    if (ccChecker.isCyclicCO()) {
                        logout.println("CyclicCO detected!");
                    } else if (ccChecker.isThinAirRead()) {
                        logout.println("ThinAirRead detected!");
                    } else if (ccChecker.isWriteCOInitRead()) {
                        logout.println("WriteCOInitRead detected!");
                    } else if (ccChecker.isWriteCORead()) {
                        logout.println("WriteCORead detected!");
                    }


                    startTime = System.nanoTime();
                    ConflictRelation cf = new ConflictRelation(history.getOpNum());
                    cf.caculateConflictRelation(history, bco);
                    endTime = System.nanoTime();
                    long cfTime = endTime - startTime;

                    CCvChecker ccvChecker = new CCvChecker(history, po, rf, bco, cf);
                    ccvResult = ccvChecker.checkCCv();
                    logout.println("Chekcing CCv, result:" + ccvResult);
                    System.out.println("Chekcing CCv, result:" + ccvResult);
                    System.out.println("cf time:" + cfTime);
                    logout.println("cf time:" + cfTime);


                    startTime = System.nanoTime();
                    IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
                    ihbo.incrementalHBO(history, po, rf, ico);
                    endTime = System.nanoTime();
                    long ihboTime = endTime - startTime;
                    logout.println("ihbo max loop: " + ihbo.getLoopTime());

                    startTime = System.nanoTime();
                    BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
                    hbo.calculateHBo(history, po, rf, ico);
                    endTime = System.nanoTime();
                    long bhboTime = endTime - startTime;
                    logout.println("bhbo max loop: " + hbo.getLoopTime() );


                    //如果包含这三种非法模式，ihbo不能完整计算得到每个线程hbo的关系矩阵
                    if (ihbo.isCyclicCO() || ihbo.isThinAirRead() || ihbo.isCyclicHB()) {
                        System.out.println("Cannot compare matrix! Reason: isCyclicCO:" + ihbo.isCyclicCO() + " isThinAirRead:" + ihbo.isThinAirRead() + " isCyclicHB:" + ihbo.isCyclicHB());
                        logout.println("Cannot compare matrix! Reason: isCyclicCO:" + ihbo.isCyclicCO() + " isThinAirRead:" + ihbo.isThinAirRead() + " isCyclicHB:" + ihbo.isCyclicHB());
                    } else {
                        boolean hboEquality = hbo.checkEqual(ihbo);
                        if (!hboEquality) {
                            System.out.println("ihbo is not equal to bhbo???");
                            logout.println("ihbo is not equal to bhbo???");
                            System.exit(-1);
                        } else {
                            System.out.println("ihbo is equal to bhbo ^.^");
                            logout.println("ihbo is equal to bhbo ^.^");
                        }
                    }

                    CMChecker cmChecker = new CMChecker(history, po, rf, bco, hbo);
                    cmResult = cmChecker.checkCM();
                    logout.println("Checking CM, result:" + cmResult);
                    System.out.println("Checking CM, result:" + cmResult);
                    System.out.println("ihbo time:" + ihboTime);
                    System.out.println("bhbo time:" + bhboTime);
                    logout.println("ihbo time:" + ihboTime);
                    logout.println("bhbo time:" + bhboTime);


                    String newPath = mongoLog;
//                    String dic;
                    File oldFile = new File(newPath);

                    newPath = newPath.replace("ParameterChoosing/", "ParameterChoosing/mongoSpecial/" + ccProducer.getOpNum() + "/");
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
                    System.out.println("File Location:" + newPath);
                    logout.println("File Location:" + newPath);
                    oldFile.renameTo(newFile);

                    appendLog(mongoCheckLog, "----------------------------------------------------------");
                    appendLog(mongoCheckLog, "Checking " + newPath);
                    appendLog(mongoCheckLog, "CC Result: " + ccResult);
                    appendLog(mongoCheckLog, "CCv Result: " + ccvResult);
                    appendLog(mongoCheckLog, "CM Result: " + cmResult);
                    appendLog(mongoCheckLog, "BCO time: " + bcoTime);
                    appendLog(mongoCheckLog, "ICO time: " + icoTime);
                    appendLog(mongoCheckLog, "CF time: " + cfTime);
                    appendLog(mongoCheckLog, "Basic HBo time: " + bhboTime);
                    appendLog(mongoCheckLog, "Incremental HBo time: " + ihboTime);


                    logout.close();
//                }
//            }


//        }

    }
}