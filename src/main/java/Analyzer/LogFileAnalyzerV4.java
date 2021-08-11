package Analyzer;
import lombok.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;


//Batch file analyzer for logs in different parameters
public class LogFileAnalyzerV4 {
    public static final String FILE_ENCODING = "UTF-8";
    @Data
    class Para{
        int opNum;
        int processNum;
        int varNum;

        Para(){
            opNum = 0;
            processNum = 0;
            varNum = 0;
        }
        Para(int opNum, int processNum){
            this.opNum = opNum;
            this.processNum = processNum;
            this.varNum = 0;
        }
        Para(int opNum, int processNum, int varNum){
            this.opNum = opNum;
            this.processNum = processNum;
            this.varNum = varNum;
        }
        @Override
        public String toString(){
            return this.opNum + "    "+ this.processNum + "    "+ this.varNum;
        }
    }

    @Data
    class Info{
        @ExcelProperty("opNum")
        int count;
        @ExcelProperty("processNum")
        long processNum;
        @ExcelProperty("varNum")
        long varNum;
        @ExcelProperty("ico_avg")
        long ico_avg;
        @ExcelProperty("bco_avg")
        long bco_avg;
        @ExcelProperty("cf_avg")
        long cf_avg;
        @ExcelProperty("ihbo_avg")
        long ihbo_avg;
        @ExcelProperty("bhbo_avg")
        long bhbo_avg;

        Info(){
            count = 0;
            bco_avg = 0;
            ico_avg = 0;
            cf_avg = 0;
            bhbo_avg = 0;
            ihbo_avg = 0;
            processNum = 0;
            varNum = 0;
        }
        Info(int count, long processNum, long varNum, long ico_avg, long bco_avg, long cf_avg, long ihbo_avg, long bhbo_avg){
            this.count = count;
            this.processNum = processNum;
            this.varNum = varNum;
            this.ico_avg = ico_avg;
            this.bco_avg = bco_avg;
            this.cf_avg = cf_avg;
            this.ihbo_avg = ihbo_avg;
            this.bhbo_avg = bhbo_avg;
        }
    }

    public long iterAvg(long lastAvg, long cur, int count){
        return lastAvg + ((cur - lastAvg) / (count+1));
    }

    public List<Info> logFileAnalyzer(String logPath, int readBuffer) throws Exception{

        Map<Integer, String> lineData = new HashMap<>();
        Map<LogFileAnalyzerV4.Para, LogFileAnalyzerV4.Info> globalInfo = new HashMap<>();
        int line = 0;
        File file = new File(logPath);
        if(file.isFile() && file.exists()){
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), FILE_ENCODING);
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineContent = null;
            int index = 1;
            String tempLine;
            int opNum;
            int processNum;
            int varNum;
            while ((lineContent = bufferedReader.readLine())!= null){
                if(index > line){
                    lineData.put(index-1, lineContent);
                    if((index - line) ==  readBuffer){ //读够了一次指定的行
//                        for(int i = 0; i < readBuffer; i++){
//                            System.out.println("Line " + i + " Conetent:" + lineData.get(line+i));
//                        }
                        tempLine = lineData.get(line+2);
                        opNum = Integer.valueOf(tempLine.substring(tempLine.indexOf("opNum")+5, tempLine.indexOf("processNum")-1));
                        processNum = Integer.valueOf(tempLine.substring(tempLine.indexOf("processNum")+10, tempLine.indexOf("varNum")-1));
                        if (tempLine.indexOf("_", tempLine.indexOf("varNum") + 6) == -1) {
                            varNum = Integer.valueOf(tempLine.substring(tempLine.indexOf("varNum")+6, tempLine.indexOf(".",tempLine.indexOf("varNum")+6)));
                        }else {
                            varNum = Integer.valueOf(tempLine.substring(tempLine.indexOf("varNum") + 6, tempLine.indexOf("_", tempLine.indexOf("varNum") + 6)));
                        }
                        LogFileAnalyzerV4.Para key = new Para(opNum, processNum, varNum);
//                        int key = Integer.valueOf(tempLine.substring(tempLine.indexOf("num:")+4)) / 10 *10;
//                        int key = Integer.valueOf(tempLine.substring(tempLine.indexOf("ops-")+4, tempLine.indexOf("-no-")));
                        LogFileAnalyzerV4.Info tempInfo;
                        tempLine = lineData.get(line+6);
                        long cur_bco = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+7);
                        long cur_ico = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+8);
                        long cur_cf = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+9);
                        long cur_bhbo = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+10);
                        long cur_ihbo = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));

                        if(!globalInfo.containsKey(key)){
                            tempInfo = new LogFileAnalyzerV4.Info();
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

        System.out.println("opNum   processNum  varNum  ico_avg bco_avg cf_avg  ihbo_avg    bhbo_avg");

        List<Info> infoList = new LinkedList<>();

        for(Para key: globalInfo.keySet()){
//            System.out.println("-----------------------------------------------------");
//            System.out.println("Average time usage for " + key + " operations:");
//            System.out.println("Total number of histories: " + globalInfo.get(key).count);
//            System.out.println("BCO : " + globalInfo.get(key).bco_avg);
//            System.out.println("ICO : " + globalInfo.get(key).ico_avg);
//            System.out.println("CF  : " + globalInfo.get(key).cf_avg);
//            System.out.println("BHBO: " + globalInfo.get(key).bhbo_avg);
//            System.out.println("IHBO: " + globalInfo.get(key).ihbo_avg);
            System.out.println(key + "  " + globalInfo.get(key).ico_avg + "  " + globalInfo.get(key).bco_avg + " " + globalInfo.get(key).cf_avg + " " + globalInfo.get(key).ihbo_avg+ " " + globalInfo.get(key).bhbo_avg  +"");
            Info tempInfo = new Info(key.getOpNum(), key.getProcessNum(), key.getVarNum(), globalInfo.get(key).ico_avg, globalInfo.get(key).bco_avg, globalInfo.get(key).cf_avg, globalInfo.get(key).ihbo_avg, globalInfo.get(key).bhbo_avg);
            infoList.add(tempInfo);
        }
        return infoList;
    }

    public void printToExcel(String fileName, List<Info> infoList){
        EasyExcel.write(fileName, Info.class).sheet("sheet1").doWrite(infoList);
    }

    public static void main(String args[]) throws Exception{
        LogFileAnalyzerV4 analyzer = new LogFileAnalyzerV4();
        List<Info> tList = analyzer.logFileAnalyzer("target/Data/Running_8_10/GlobalLog_8_10.txt", 11);
        analyzer.printToExcel("target/Data/Running_8_10/mergeResults.xlsx", tList);
    }

}
