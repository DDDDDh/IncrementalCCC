import Checker.*;
import History.*;
import Relations.*;

import java.util.*;
import java.io.*;

//批量检测文件夹中的历史记录

public class BatchChecking {

    String parseFiles(String path){

        String filePath = "";
        File file = new File(path);
        if(file.exists() && file.isDirectory()){
            parseFiles(file.getAbsolutePath());
        }
        else if (file.isFile()){
            String filename = file.getName();
            System.out.println();
        }

        return filePath;

    }

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

//        String path = "/Users/yi-huang/Project/MongoTrace/store/test2_majority_majority_no-nemesis_2000-5000/Part1/";
        String path = "/Users/yi-huang/Project/IncrementalCCC/target/Data/Running_8_15/RemainPart/";
//        String logPath = "/Users/yi-huang/Project/MongoTrace/store/test2Part1CheckingLogfile.txt";
        String logPath = "/Users/yi-huang/Project/IncrementalCCC/target/Data/Running_8_15/RemainPart/CheckingLogFile.txt";
        String globalLog = "/Users/yi-huang/Project/IncrementalCCC/target/Data/Running_8_15/RemainPart/GlobalLog.txt";
        File outfile = new File(logPath);
        PrintWriter output = new PrintWriter(outfile);

        List<String> files = new ArrayList<String>();
        File file = new File(path);
        File[] tempList = file.listFiles();

        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                files.add(tempList[i].toString());
                //文件名，不包含路径
                //String fileName = tempList[i].getName();
            }
        }

        for (String filename: files) {

            String curFile = filename;
            System.out.println("Checking file:" + curFile);
            output.println("Checking file:" + curFile);
            HistoryReader reader = new HistoryReader(curFile);
            History history = new History(reader.readHistory());
            history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后设置总操作数
            if(!history.isDifferentiated()){
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
            IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
            ico.incrementalCO(history, po, rf);
            long endTime = System.nanoTime();
            long icoTime = endTime - startTime;
            System.out.println("ico Time:" + icoTime + "ns");
            output.println("ico Time:" + icoTime + "ns");

            startTime = System.nanoTime();
            BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
//            System.out.println("Finish initialization of bco");
            bco.computeCO(history, po, rf);
            endTime = System.nanoTime();
            long bcoTime = endTime - startTime;
            System.out.println("bco Time:" + bcoTime + "ns");
            output.println("bco Time:" + bcoTime + "ns");

//            boolean coEquality = bco.checkEqual(ico);
//            if(coEquality){
//                System.out.println();
//            }

            boolean coEquality = bco.checkEqualDebug(ico, history.getOperationList());
            if(!coEquality){
                System.out.println("ico is not equal to bco???");
                output.println("ico is not equal to bco???");
            }
            else{
                System.out.println("ico is equal to bco ^.^");
                output.println("ico is equal to bco ^.^");
            }


            CCChecker ccChecker = new CCChecker(history, po, rf, ico);

//            System.out.println("Begin to compute conflict relation");
            startTime = System.nanoTime();
            ConflictRelation cf = new ConflictRelation(history.getOpNum());
            cf.caculateConflictRelation(history, bco);
            endTime = System.nanoTime();
            long cfTime = endTime - startTime;
            System.out.println("cf Time:" + cfTime + "ns");
            output.println("cf Time:" + cfTime + "ns");

            CCvChecker ccvChecker = new CCvChecker(history, po, rf, ico, cf);


//            System.out.println("Begin to compute happen-before relation");

            startTime = System.nanoTime();
            BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
            hbo.calculateHBo(history, po, rf, ico);
            endTime = System.nanoTime();
            long bhboTime = endTime - startTime;
            System.out.println("bhbo Time:" + bhboTime + "ns");
            output.println("bhbo Time:" + bhboTime + "ns");

            startTime = System.nanoTime();
            IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum(),1);
            ihbo.incrementalHBO(history,po, rf, ico);
            endTime = System.nanoTime();
            long ihboTime = endTime - startTime;
            System.out.println("ihbo Time:" + ihboTime + "ns");
            output.println("ihbo Time:" + ihboTime + "ns");

//            System.out.println("Finish computation of happen-before relation");

            boolean hboEquality = hbo.checkEqual(ihbo);
            if(!hboEquality){
                System.out.println("ihbo is not equal to bhbo???");
                output.println("ihbo is not equal to bhbo???");
            }
            else{
                System.out.println("ihbo is equal to bhbo ^.^");
                output.println("ihbo is equal to bhbo -.-");
            }


            CMChecker cmChecker = new CMChecker(history, po, rf, ico, hbo);

            boolean ccResult = ccChecker.checkCC();
            boolean ccvResult = ccvChecker.checkCCv();
            boolean cmResult = cmChecker.checkCM();

            if(ccChecker.checkCC()){
                System.out.println("Chekcing CC, result:true");
                output.println("Chekcing CC, result:true" );
            }
            else{
                System.out.println("Chekcing CC, result:false");
                System.out.println("Fail Reason:" + ccChecker.failReason());
                output.println("Chekcing CC, result:false" );
                output.println("Fail Reason:" + ccChecker.failReason());
            }

            if(ccvChecker.checkCCv()){
                System.out.println("Chekcing CCv, result:true");
                output.println("Chekcing CCv, result:true");
            }
            else{
                System.out.println("Chekcing CCv, result:false");
                System.out.println("Fail Reason:" + ccvChecker.failReason());
                output.println("Chekcing CCv, result:false");
                output.println("Fail Reason:" + ccvChecker.failReason());
            }

            if(cmChecker.checkCM()){
                System.out.println("Checking CM, result:true");
                output.println("Checking CM, result:true");
            }
            else{
                System.out.println("Chekcing CM, result:false");
                System.out.println("Fail Reason:" + cmChecker.failReason());
                output.println("Chekcing CM, result:false");
                output.println("Fail Reason:" + cmChecker.failReason());
            }

            System.out.println("---------------------------------------------------------");
            output.println("---------------------------------------------------------");

            appendLog(globalLog, "----------------------------------------------------------");
            appendLog(globalLog, "File Location:" + curFile);
            appendLog(globalLog, "Checking " + curFile);
            appendLog(globalLog, "CC Result: " + ccResult);
            appendLog(globalLog, "CCv Result: " + ccvResult);
            appendLog(globalLog, "CM Result: " + cmResult);
            appendLog(globalLog, "BCO time: " + bcoTime);
            appendLog(globalLog, "ICO time: " + icoTime);
            appendLog(globalLog, "CF time: " + cfTime);
            appendLog(globalLog, "Basic HBo time: " + bhboTime);
            appendLog(globalLog, "Incremental HBo time: " + ihboTime);

        }
        output.close();
    }


}
