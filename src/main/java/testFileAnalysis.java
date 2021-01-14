import Checker.*;
import History.*;
import Relations.*;

import java.util.*;
import java.io.*;




public class testFileAnalysis {

    static LinkedList<File> parseFiles(File file){
        File[] files = file.listFiles();
        LinkedList<File> validFiles = new LinkedList<File>();
        if(files == null || files.length == 0){
            System.out.println("null");
            return null;
        }

        for(File f: files){
            if(f.isDirectory()){
//                System.out.println("Dir==>" + f.getAbsolutePath());
                LinkedList<File> tempList = parseFiles(f);
                for(int i = 0; i < tempList.size(); i++){
                    validFiles.add(tempList.get(i));
                }
            }
            else if (f.isFile()){
                String filename = f.getName();
                if(filename.endsWith(".edn")) {
//                    System.out.println(f.getAbsolutePath());
                    validFiles.add(f);
//                    return f.getName();
                }
            }
        }
        return validFiles;
    }
    public static void checkCC(PrintWriter output, long initialTime, History history, ProgramOrder po, ReadFrom rf, CausalOrder co){
        System.out.println("Begin to check CC:");
        output.println("Begin to check CC:");
        CCChecker ccChecker = new CCChecker(history, po, rf, co);
        boolean ccResult = ccChecker.checkCC();
        System.out.println("CC Result: " + ccResult);
        System.out.println("Contain ThinAirRead: " + ccChecker.isThinAirRead());
        System.out.println("Contain CyclicCO: " + ccChecker.isCyclicCO());
        System.out.println("Contain WriteCORead: " + ccChecker.isWriteCORead());
        System.out.println("Contain WriteCOInitRead: " + ccChecker.isWriteCOInitRead());
        output.println("CC Result: " + ccResult);
        output.println("Contain ThinAirRead: " + ccChecker.isThinAirRead());
        output.println("Contain CyclicCO: " + ccChecker.isCyclicCO());
        output.println("Contain WriteCORead: " + ccChecker.isWriteCORead());
        output.println("Contain WriteCOInitRead: " + ccChecker.isWriteCOInitRead());
        long currentTime = System.nanoTime();
        System.out.println("Finish checking of CC. Total time: " + (currentTime - initialTime) + "ns");
        output.println("Finish checking of CC. Total time: " + (currentTime - initialTime) + "ns");
        output.println();
    }

    //此处传入的是basic causal order
    public static void checkCM(PrintWriter output, long initialTime, History history, ProgramOrder po, ReadFrom rf, CausalOrder co, String arg) throws Exception{

        System.out.println("Begin to check CM:");
        output.println("Begin to check CM:");


        System.out.println("Begin to calculate HBo.");
        output.println("Begin to calculate HBo.");
        CMChecker cmChecker;

        if(arg.equals("I")){
            System.out.println("Incremental Computation of HBo");
            output.println("Incremental Computation of HBo");
            long startTime = System.nanoTime();
            IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
            ihbo.incrementalHBO(history,po, rf, co);
            long endTime = System.nanoTime();
            System.out.println("ihbo Time:" + (endTime - startTime) + "ns");
            output.println("ihbo Time:" + (endTime - startTime) + "ns");

            cmChecker = new CMChecker(history, po, rf, co, ihbo); //在此种情况下，用ihbo计算
        }
        else if(arg.equals("B")){
            System.out.println("Basic Computation of HBo");
            output.println("Basic Computation of HBo");
            long startTime = System.nanoTime();
            BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
            hbo.calculateHBo(history, po, rf, co);
            long endTime = System.nanoTime();
            System.out.println("bhbo Time:" + (endTime - startTime) + "ns");
            output.println("bhbo Time:" + (endTime - startTime) + "ns");

            cmChecker = new CMChecker(history, po, rf, co, hbo); //在此种情况下，用basic hbo计算

        }
//        else if(arg.equals("A")){
//            System.out.println("Using both methods to compute HBo");
//            output.println("Using both methods to compute HBo");
//        }
        else{
            System.out.println("Using both methods to compute HBo");
            output.println("Using both methods to compute HBo");
            System.out.println("Incremental Computation of HBo");
            output.println("Incremental Computation of HBo");
            long startTime = System.nanoTime();
            IncrementalHappenBeforeOrder ihbo = new IncrementalHappenBeforeOrder(history.getOpNum());
            ihbo.incrementalHBO(history,po, rf, co);
            long endTime = System.nanoTime();
            System.out.println("ihbo Time:" + (endTime - startTime) + "ns~");
            output.println("ihbo Time:" + (endTime - startTime) + "ns~");
            System.out.println("Basic Computation of HBo");
            output.println("Basic Computation of HBo");
            startTime = System.nanoTime();
            BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
            hbo.calculateHBo(history, po, rf, co);
            endTime = System.nanoTime();
            System.out.println("bhbo Time:" + (endTime - startTime) + "ns");
            output.println("bhbo Time:" + (endTime - startTime) + "ns");

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
                    output.println("ihbo is equal to bhbo -.-");
                }
            }


            cmChecker = new CMChecker(history, po, rf, co, ihbo); //都存在的情况下，保守起见用basic hbo计算

        }

        System.out.println("Finish calculation of HBo.");
        output.println("Finish calculation of HBo.");

        boolean cmResult = cmChecker.checkCM();
        System.out.println("CM Result: " + cmResult);
        System.out.println("Contain WriteHBInitRead: " + cmChecker.isWriteHBInitRead());
        System.out.println("Contain CyclicHB: " + cmChecker.isCyclicHB());
        output.println("CM Result: " + cmResult);
        output.println("Contain WriteHBInitRead: " + cmChecker.isWriteHBInitRead());
        output.println("Contain CyclicHB: " + cmChecker.isCyclicHB());
        long currentTime = System.nanoTime();
        System.out.println("Finish checking of CM. Total time: " + (currentTime - initialTime) + "ns");
        output.println("Finish checking of CM. Total time: " + (currentTime - initialTime) + "ns");
        output.println();
    }


    public static void checkCCv(PrintWriter output, long initialTime, History history, ProgramOrder po, ReadFrom rf, CausalOrder co){
        System.out.println("Begin to check CCv:");
        output.println("Begin to check CCv:");


        System.out.println("Begin to calculate cf.");
        output.println("Begin to calculate cf.");
        long startTime = System.nanoTime();
        ConflictRelation cf = new ConflictRelation(history.getOpNum());
        cf.caculateConflictRelation(history, co);
        long endTime = System.nanoTime();
        System.out.println("cf Time:" + (endTime - startTime) + "ns");
        output.println("cf Time:" + (endTime - startTime) + "ns");
        System.out.println("Finish calculation of cf.");
        output.println("Finish calculation of cf.");

        CCvChecker ccvChecker = new CCvChecker(history, po, rf, co, cf);
        boolean ccvResult = ccvChecker.checkCCv();
        System.out.println("CCv Result: " + ccvResult);
        System.out.println("Contain CyclicCF: " + ccvChecker.isCyclicCF());
        output.println("CCv Result: " + ccvResult);
        output.println("Contain CyclicCF: " + ccvChecker.isCyclicCF());
        long currentTime = System.nanoTime();
        System.out.println("Finish checking of CCv. Total time: " + (currentTime - initialTime) + "ns");
        output.println("Finish checking of CCv. Total time: " + (currentTime - initialTime) + "ns");
        output.println();
    }

    public static void main(String args[]) throws Exception{

//        File file = new File("/Users/yi-huang/Project/MongoTrace/1227/no-memesis/majority-linearizable/100-3000_test/");
        File file = new File(args[0]);
        long initialTime = System.nanoTime();

        LinkedList<File> validFiles = new LinkedList<>();
        if(file.exists() && file.isDirectory()) {
            validFiles = parseFiles(file);
        }
        System.out.println("Begin to print valid files:");
        for(File f: validFiles){
            initialTime = System.nanoTime();
            String fileName = f.getAbsolutePath();

//            System.out.println(fileName);
            String newFilename = fileName.replace("history", "hy_checking_" + args[1] + "_Results_");
            newFilename = newFilename.replace("edn", "log");
//            System.out.println("Now:" + newFilename);
            File outfile = new File(newFilename);
            PrintWriter output = new PrintWriter(outfile);
            String curFile = fileName;

            System.out.println("Checking file:" + curFile);
            output.println("Checking file:" + curFile);
            HistoryReader reader = new HistoryReader(curFile);
            History history = new History(reader.readHistory());
            history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后设置总操作数
            if(!history.isDifferentiated()){
                System.out.println("Detected not differentiated.");
                output.println("Detected not differentiated.");
                output.close();
                continue;
            }

            System.out.println("Total op num:" + reader.getTotalNum());
            System.out.println("Process num:" + history.getProcessOpList().keySet().size());
            output.println("Total op num:" + reader.getTotalNum());
            output.println("Process num:" + history.getProcessOpList().keySet().size());



            System.out.println("Begin to calculate po.");
            output.println("Begin to calculate po.");
            ProgramOrder po = new ProgramOrder(history.getOpNum());
            po.calculateProgramOrder(history);
            System.out.println("Finish calculation of po.");
            output.println("Finish calculation of po.");

            System.out.println("Begin to calculate rf.");
            output.println("Begin to calculate rf.");
            ReadFrom rf = new ReadFrom(history.getOpNum());
            rf.calculateReadFrom(history);
            System.out.println("Finish calculation of rf.");
            output.println("Finish calculation of rf.");
            output.println();

            output.println("Counted operations:");
            for(int i = 0; i < history.getOperationList().size(); i++){
                output.println(history.getOperationList().get(i).printOpInfo() + " corresponding write id:" + history.getOperationList().get(i).getCorrespondingWriteID());
            }
            output.println();

            //ico
            System.out.println("Begin to calculate co.");
            output.println("Begin to calculate co.");
            long startTime = 0;
            long endTime = 0;
            IncrementalCausalOrder ico = new IncrementalCausalOrder(history.getOpNum());
            if(!ico.DAGDetection(history,po,rf)){
                System.out.println("Not a DAG, cannot use incremental CO computation.");
                output.println("Not a DAG, cannot use incremental CO computation.");
            }
            else{
                startTime = System.nanoTime();
                ico.incrementalCO(history, po, rf);
                endTime = System.nanoTime();
                System.out.println("ico Time:" + (endTime - startTime) + "ns");
                output.println("ico Time:" + (endTime - startTime) + "ns");
            }

            //bco
            startTime = System.nanoTime();
            BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
//            System.out.println("Finish initialization of bco");
            bco.computeCO(history, po, rf);
            endTime = System.nanoTime();
            System.out.println("bco Time:" + (endTime - startTime) + "ns");
            output.println("bco Time:" + (endTime - startTime) + "ns");

            System.out.println("Finish calculation of co.");
            output.println("Finish calculation of co.");


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
                System.out.println("ico is equal to bco ^.^ ");
                output.println("ico is equal to bco ^.^ ");
            }
            output.println();

            if(args[1].equals("CC")){
                checkCC(output, initialTime, history, po, rf, bco);
            }
            else if(args[1].equals("CM")){
                checkCC(output, initialTime, history, po, rf, bco);
                if(args.length < 3) {
                    checkCM(output, initialTime, history, po, rf, bco, "A");
                }
                else{
                    checkCM(output, initialTime, history, po, rf, bco, args[2]);
                }

            }
            else if(args[1].equals("CCv")){
                checkCC(output, initialTime, history, po, rf, bco);
                checkCCv(output, initialTime, history, po, rf, bco);
            }
            else if(args[1].equals("ALL")){
                checkCC(output, initialTime, history, po, rf, bco);
                if(reader.getTotalNum() < 1000) { //暂时只为操作数小于1000的运行记录检查CM
                    checkCM(output, initialTime, history, po, rf, bco, "A");
                }
                checkCCv(output, initialTime, history, po, rf, bco);
            }

            output.close();
        }
        System.out.println("Finish running");

    }


}