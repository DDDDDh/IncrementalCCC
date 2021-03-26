package Analyzer;

import Relations.IncrementalHappenBeforeOrder;

import java.io.*;
import java.util.*;


public class LogFileAnalyzer {

    public static final String FILE_ENCODING = "UTF-8";

    class Info{
        int count;
        long bco_avg;
        long ico_avg;
        long cf_avg;
        long bhbo_avg;
        long ihbo_avg;

        Info(){
            count = 0;
            bco_avg = 0;
            ico_avg = 0;
            cf_avg = 0;
            bhbo_avg = 0;
            ihbo_avg = 0;
        }
    }

    public void mergeLogs(String folderPath) throws Exception{

        List<String> files = new ArrayList<String>();
        File file = new File(folderPath);
        File[] tempList = file.listFiles();

        String logPath = folderPath + "/total.txt";
        File outfile = new File(logPath);
        PrintWriter output = new PrintWriter(outfile);

        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                files.add(tempList[i].toString());
                //文件名，不包含路径
                //String fileName = tempList[i].getName();
            }
        }

        for (String filename: files) {
            String curFileName = filename;
            File curFile = new File(curFileName);
            System.out.println("Dealing with " + curFileName);
            if(curFile.isFile() && curFile.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(curFile), FILE_ENCODING);
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineContent = null;
                while ((lineContent = bufferedReader.readLine()) != null) {
                    output.println(lineContent);
                }
                read.close();
                bufferedReader.close();
            }
        }

        System.out.println("Totally dealing with " + files.size() + " files");
        output.close();

    }

    public long iterAvg(long lastAvg, long cur, int count){
        return lastAvg + ((cur - lastAvg) / (count+1));
    }

    public void logFileAnalyzer(String logPath, int readBuffer) throws Exception{

        Map<Integer, String> lineData = new HashMap<>();
        Map<Integer, Info> globalInfo = new HashMap<>();
        int line = 0;
        File file = new File(logPath);
        if(file.isFile() && file.exists()){
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), FILE_ENCODING);
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineContent = null;
            int index = 1;
            String tempLine;
            while ((lineContent = bufferedReader.readLine())!= null){
                if(index > line){
                    lineData.put(index-1, lineContent);
                    if((index - line) ==  readBuffer){ //读够了一次指定的行
                        for(int i = 0; i < readBuffer; i++){
                            System.out.println("Line " + i + " Conetent:" + lineData.get(line+i));
                        }
                        tempLine = lineData.get(line+1);
                        int key = Integer.valueOf(tempLine.substring(tempLine.indexOf("_opNum")+6, tempLine.indexOf("_processNum")));
//                        int key = Integer.valueOf(tempLine.substring(tempLine.indexOf("ops-")+4, tempLine.indexOf("-no-")));
                        Info tempInfo;
                        tempLine = lineData.get(line+5);
                        long cur_bco = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+6);
                        long cur_ico = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+7);
                        long cur_cf = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+8);
                        long cur_bhbo = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+9);
                        long cur_ihbo = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));

                        if(!globalInfo.containsKey(key)){
                            tempInfo = new Info();
                            tempInfo.count = 1;
                            tempInfo.bco_avg = cur_bco;
                            tempInfo.ico_avg = cur_ico;
                            tempInfo.cf_avg = cur_cf;
                            tempInfo.bhbo_avg = cur_bhbo;
                            tempInfo.ihbo_avg = cur_ihbo;
                            globalInfo.put(key, tempInfo);
                        }
                        else{
                            tempInfo = globalInfo.get(key);
                            tempInfo.bco_avg = iterAvg(tempInfo.bco_avg, cur_bco, tempInfo.count);
                            tempInfo.ico_avg = iterAvg(tempInfo.ico_avg, cur_ico, tempInfo.count);
                            tempInfo.cf_avg = iterAvg(tempInfo.cf_avg, cur_cf, tempInfo.count);
                            tempInfo.bhbo_avg = iterAvg(tempInfo.bhbo_avg, cur_bhbo, tempInfo.count);
                            tempInfo.ihbo_avg = iterAvg(tempInfo.ihbo_avg, cur_ihbo, tempInfo.count);
                            tempInfo.count = tempInfo.count+1;
                            globalInfo.put(key, tempInfo);
                        }
                        line = index;
                    }
                }
                index++;
            }
            read.close();
            bufferedReader.close();
        }

        for(int key: globalInfo.keySet()){
//            System.out.println("-----------------------------------------------------");
//            System.out.println("Average time usage for " + key + " operations:");
//            System.out.println("Total number of histories: " + globalInfo.get(key).count);
//            System.out.println("BCO : " + globalInfo.get(key).bco_avg);
//            System.out.println("ICO : " + globalInfo.get(key).ico_avg);
//            System.out.println("CF  : " + globalInfo.get(key).cf_avg);
//            System.out.println("BHBO: " + globalInfo.get(key).bhbo_avg);
//            System.out.println("IHBO: " + globalInfo.get(key).ihbo_avg);
            System.out.println(key + "  " + globalInfo.get(key).ico_avg + "  " + globalInfo.get(key).bco_avg + " " + globalInfo.get(key).cf_avg + " " + globalInfo.get(key).ihbo_avg+ " " + globalInfo.get(key).bhbo_avg);
        }
    }

    public static void main(String args[]) throws Exception{
        LogFileAnalyzer analyzer = new LogFileAnalyzer();
        analyzer.logFileAnalyzer("target/RandomHistories/StressTestMongoOut_0322/total.txt", 10);
//        analyzer.logFileAnalyzer("target/CheckingSeletedLog.txt",10);
//        analyzer.mergeLogs("target/RandomHistories/StressTestOriginalOut_0322");
    }



}
