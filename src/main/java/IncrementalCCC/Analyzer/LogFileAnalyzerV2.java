package IncrementalCCC.Analyzer;

import lombok.Data;

import java.io.*;
import java.util.*;


public class LogFileAnalyzerV2 {

    public static final String FILE_ENCODING = "UTF-8";

    String logPath;
    String outPath;

    @Data
    class fileInfo{
        String type;
        String wc;
        String rc;
        int ops;
        long icoTime;
        long bcoTime;
        long cfTime;
        long ihboTime;
        long bhboTime;

        fileInfo(){
            type = null;
            wc = null;
            rc = null;
            ops = 0;
            icoTime = 0;
            bcoTime = 0;
            cfTime = 0;
            ihboTime = 0;
            bhboTime = 0;
        }


    }

    LogFileAnalyzerV2(String logPath, String outPath){
        this.logPath = logPath;
        this.outPath = outPath;
    }

    public void analyzeFileName(fileInfo info, String line){

        info.setType(line.substring(line.indexOf("selected-data/")+14, line.indexOf("/mongo")));
        info.setWc(line.substring(line.indexOf("wc-:")+4, line.indexOf("-rc")));
        info.setRc(line.substring(line.indexOf("rc-:")+4, line.indexOf("-ti")));
        String subline = line.substring(line.indexOf("ops-")+4);
        String subsubline = subline.substring(0, subline.indexOf("-"));
//        System.out.println(subsubline);
       info.setOps(Integer.valueOf(subsubline));
    }

    public void analyze() throws Exception{

        HashMap<String, LinkedList<String>> classifier = new HashMap<>();
        File outfile = new File(this.outPath);
        PrintWriter output = new PrintWriter(outfile);
        boolean toPrint = false;
        File logFile = new File(this.logPath);
        if(logFile.exists() && logFile.isFile()){
            InputStreamReader read = new InputStreamReader(new FileInputStream(logFile), FILE_ENCODING);
            BufferedReader bufferedReader = new BufferedReader(read);
            System.out.println("Reading logFile:" + logFile.getName());
            String tempLine;
            fileInfo info = new fileInfo();
            LinkedList<String> tempList = new LinkedList<>();

            while((tempLine = bufferedReader.readLine()) != null){
                if(tempLine.startsWith("Checking")){
                    System.out.println("last info:" + info);
                    String key = info.type + "-" + info.wc + "-" + info.rc;
                    if(!classifier.containsKey(key)){
                        tempList = new LinkedList<>();
                        tempList.add(info.ops + "   " + info.icoTime + "    " + info.bcoTime + "    " + info.cfTime + " "+ info.ihboTime + "    " + info.bhboTime);
                        classifier.put(key, tempList);
                    }
                    else{
                        tempList = classifier.get(key);
                        tempList.add(info.ops + "   " + info.icoTime + "    " + info.bcoTime + "    " + info.cfTime + " "+ info.ihboTime + "    " + info.bhboTime);
                    }
                    info = new fileInfo();
                    analyzeFileName(info, tempLine);
                    if(info.type.equals("local_stable")){
                        toPrint = true;
                    }
                    else{
                        toPrint = false;
                    }
                }
                else if(tempLine.contains("ico Time")){
                    info.setIcoTime(Long.valueOf(tempLine.substring(tempLine.indexOf(":")+1, tempLine.indexOf("ns"))));
//                    System.out.println("Setting ico Time:" + info.getIcoTime());
//                    System.out.println(info);
                }
                else if(tempLine.contains("bco Time")){
                    info.setBcoTime(Long.valueOf(tempLine.substring(tempLine.indexOf(":")+1, tempLine.indexOf("ns"))));
                }
                else if(tempLine.contains("cf Time")){
                    info.setCfTime(Long.valueOf(tempLine.substring(tempLine.indexOf(":")+1, tempLine.indexOf("ns"))));
                }
                else if(tempLine.contains("bhbo Time")){
                    info.setBhboTime(Long.valueOf(tempLine.substring(tempLine.indexOf(":")+1, tempLine.indexOf("ns"))));
                }
                else if(tempLine.contains("ihbo Time")){
                    info.setIhboTime(Long.valueOf(tempLine.substring(tempLine.indexOf(":")+1, tempLine.indexOf("ns"))));
                }

                if(toPrint){
                    output.println(tempLine);
                }
            }
//            System.out.println("final info:" + info);
            read.close();
            bufferedReader.close();

            for(String key: classifier.keySet()){
                System.out.println("Printing info for key:" + key);
                tempList = classifier.get(key);
                for(int i = 0; i < tempList.size(); i++){
                    System.out.println(tempList.get(i));
                }
                System.out.println("-----------------------------------------------------");
            }

            output.close();
        }

    }


    public static void main(String args[]) throws Exception{
        LogFileAnalyzerV2 analyzer = new LogFileAnalyzerV2("/Users/yi-huang/Project/MongoTrace/Debug/selected-data_ALL_out_0120.txt","/Users/yi-huang/Project/MongoTrace/selected-data_ALL_out_0120.txt");
        analyzer.analyze();
    }



}
