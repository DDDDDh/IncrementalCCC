import Checker.CCChecker;
import Checker.CCvChecker;
import Checker.CMChecker;
import History.History;
import History.HistoryReader;
import HistoryProducer.CCProducer;
import Relations.*;

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

        String stressTestOut = "target/RandomHistories/StressTestLogfile.txt";
        String logPath = "target/RandomHistories/CheckingLogfile.txt";
        File outfile = new File(stressTestOut);
        PrintWriter output = new PrintWriter(outfile);
        output.println("-----Begin Stress Test-----");
        System.out.println("-----Begin Stress Test-----");
        boolean ccResult;
        boolean ccvResult;
        boolean cmResult;
        int countCC = 0;
        int countCCv = 0;
        int countCM = 0;

        int opNum = 1;

        for (int k = 1; k < 11; k++) {

            opNum = k*100;
            System.out.println("Round: " + k);

            for (int i = 0; i < 100; i++) {
                System.out.println("No." + i);
                output.println("No. " + i);
                CCProducer ccProducer = new CCProducer(opNum, 5, 3, 1);
                ccProducer.generatePath();
                ccProducer.generateCCHistory();
                ccProducer.printToFile();
                ccProducer.printToFileDebug();

                String url = ccProducer.getOutputPath();
                HistoryReader reader = new HistoryReader(url);

                History history = new History(reader.readHistory());
                history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...

                if (!history.isDifferentiated()) {
                    System.out.println("Detected not differentiated.");
                    output.println("Detected not differentiated.");
                    continue;
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


                CCChecker ccChecker = new CCChecker(history, po, rf, bco);
                ccResult = ccChecker.checkCC();
                output.println("Chekcing CC, result:" + ccResult);
                System.out.println("Chekcing CC, result:" + ccResult);
                if (ccResult) {
                    countCC++;
                } else {
                    if (ccChecker.isCyclicCO()) {
                        output.println("CyclicCO detected!");
                    } else if (ccChecker.isThinAirRead()) {
                        output.println("ThinAirRead detected!");
                    } else if (ccChecker.isWriteCOInitRead()) {
                        output.println("WriteCOInitRead detected!");
                    } else if (ccChecker.isWriteCORead()) {
                        output.println("WriteCORead detected!");
                    }
                    output.close();
                    System.exit(-1);
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
                if (ccvResult) {
                    countCCv++;
                }

                startTime = System.nanoTime();
                BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
//            System.out.println("");
                hbo.calculateHBo(history, po, rf, bco);
                endTime = System.nanoTime();
                long bhboTime = endTime - startTime;

                startTime = System.nanoTime();
                IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
                ihbo.incrementalHBO(history,po, rf);
                endTime = System.nanoTime();
                long ihboTime = endTime - startTime;

                CMChecker cmChecker = new CMChecker(history, po, rf, bco, hbo);
                cmResult = cmChecker.checkCM();
                output.println("Checking CM, result:" + cmResult);
                System.out.println("Checking CM, result:" + cmResult);
                if (cmResult) {
                    countCM++;
                }


                //根据判定的结果挪到不同的文件夹中
                String newPath = ccProducer.getOutputPath();
                File oldFile = new File(newPath);
                String dic;

                if (!ccResult) {
                    System.out.println("Error! not a cc history!");
                    System.exit(-1);
                } else if (!cmResult && !ccvResult) { //cc but not cm&ccv
                    newPath = newPath.replace("RandomHistories/", "RandomHistories/CC/" + ccProducer.getOpNum() + "/");
                    dic = newPath.substring(0, newPath.lastIndexOf("/"));
                    File newDic = new File(dic);
                    if (!newDic.exists()) {
                        newDic.mkdirs();
                    }

                } else if (cmResult && !ccvResult) { //cm but not ccv
                    newPath = newPath.replace("RandomHistories/", "RandomHistories/CMNotCCv/" + ccProducer.getOpNum() + "/");
                    dic = newPath.substring(0, newPath.lastIndexOf("/"));
                    File newDic = new File(dic);
                    if (!newDic.exists()) {
                        newDic.mkdirs();
                    }
                } else if (ccvResult && !cmResult) { //ccv but not cm
                    newPath = newPath.replace("RandomHistories/", "RandomHistories/CCvNotCM/" + ccProducer.getOpNum() + "/");
                    dic = newPath.substring(0, newPath.lastIndexOf("/"));
                    File newDic = new File(dic);
                    if (!newDic.exists()) {
                        newDic.mkdirs();
                    }
                } else { //三种一致性全都符合
                    newPath = newPath.replace("RandomHistories/", "RandomHistories/AllSatisfied/" + ccProducer.getOpNum() + "/");
                    dic = newPath.substring(0, newPath.lastIndexOf("/"));
                    File newDic = new File(dic);
                    if (!newDic.exists()) {
                        newDic.mkdirs();
                    }
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
                oldFile.renameTo(newFile);
                appendLog(logPath, "----------------------------------------------------------");
                appendLog(logPath, "Checking " + newPath);
                appendLog(logPath, "CC Result: " + ccResult);
                appendLog(logPath, "CCv Result: " + ccvResult);
                appendLog(logPath, "CM Result: " + cmResult);
                appendLog(logPath, "BCO time: " + bcoTime);
                appendLog(logPath, "ICO time: " + icoTime);
                appendLog(logPath, "CF time: " + cfTime);
                appendLog(logPath, "Basic HBo time: " + bhboTime);
                appendLog(logPath, "Incremental HBo time: " + ihboTime);

            }

        }

        output.println("-----End Stress Test-----");

        output.println("CC: " + countCC + " CCv: " + countCCv + " CM: " + countCM);
        System.out.println("CC: " + countCC + " CCv: " + countCCv + " CM: " + countCM);
        output.close();

    }

}
