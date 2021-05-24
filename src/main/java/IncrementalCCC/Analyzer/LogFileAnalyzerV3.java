package IncrementalCCC.Analyzer;

//Log file analyzer to help get data from CheckingSelectedLogXXXX.txt

import lombok.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LogFileAnalyzerV3 {


    public static final String FILE_ENCODING = "UTF-8";

    @Data
    class Info{
        int count;
        long bco_avg;
        long ico_avg;
        long cf_avg;
        long bhbo_avg;
        long ihbo_avg;
        long bhbo_loop;
        long ihbo_loop;

        Info(){
            count = 0;
            bco_avg = 0;
            ico_avg = 0;
            cf_avg = 0;
            bhbo_avg = 0;
            ihbo_avg = 0;
            bhbo_loop = 0;
            ihbo_loop = 0;
        }
    }

    public long iterAvg(long lastAvg, long cur, int count){
        return lastAvg + ((cur - lastAvg) / (count+1));
    }

    public void logFileAnalyzer(String logPath, int readBuffer) throws Exception{

        Map<Integer, String> lineData = new HashMap<>();
        Map<Integer, LogFileAnalyzerV3.Info> globalInfo = new HashMap<>();
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
                        tempLine = lineData.get(line+2);
                        int key = Integer.valueOf(tempLine.substring(tempLine.indexOf("num:")+4)) / 10 *10;
//                        int key = Integer.valueOf(tempLine.substring(tempLine.indexOf("ops-")+4, tempLine.indexOf("-no-")));
                        LogFileAnalyzerV3.Info tempInfo;
                        tempLine = lineData.get(line+3);
                        long cur_bco = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+4);
                        long cur_ico = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+5);
                        long cur_cf = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+6);
                        long cur_bhbo = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+7);
                        long cur_ihbo = Long.valueOf(tempLine.substring(tempLine.indexOf("time:")+6));
                        tempLine = lineData.get(line+8);
                        long cur_bhbo_loop = Long.valueOf(tempLine.substring(tempLine.indexOf("loop:")+6));
                        tempLine = lineData.get(line+9);
                        long cur_ihbo_loop = Long.valueOf(tempLine.substring(tempLine.indexOf("loop:")+6));

                        if(!globalInfo.containsKey(key)){
                            tempInfo = new LogFileAnalyzerV3.Info();
                            tempInfo.count = 1;
                            tempInfo.bco_avg = cur_bco;
                            tempInfo.ico_avg = cur_ico;
                            tempInfo.cf_avg = cur_cf;
                            tempInfo.bhbo_avg = cur_bhbo;
                            tempInfo.ihbo_avg = cur_ihbo;
                            tempInfo.bhbo_loop = cur_bhbo_loop;
                            tempInfo.ihbo_loop = cur_ihbo_loop;
                            globalInfo.put(key, tempInfo);
                        }
                        else{
                            tempInfo = globalInfo.get(key);
                            tempInfo.bco_avg = iterAvg(tempInfo.bco_avg, cur_bco, tempInfo.count);
                            tempInfo.ico_avg = iterAvg(tempInfo.ico_avg, cur_ico, tempInfo.count);
                            tempInfo.cf_avg = iterAvg(tempInfo.cf_avg, cur_cf, tempInfo.count);
                            tempInfo.bhbo_avg = iterAvg(tempInfo.bhbo_avg, cur_bhbo, tempInfo.count);
                            tempInfo.ihbo_avg = iterAvg(tempInfo.ihbo_avg, cur_ihbo, tempInfo.count);
                            tempInfo.bhbo_loop = iterAvg(tempInfo.bhbo_loop, cur_bhbo_loop, tempInfo.count);
                            tempInfo.ihbo_loop = iterAvg(tempInfo.ihbo_loop, cur_ihbo_loop, tempInfo.count);
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

        System.out.println("ico_avg bco_avg cf_avg  ihbo_avg    bhbo_avg    ihbo_loop   bhbo_loop");

        for(int key: globalInfo.keySet()){
//            System.out.println("-----------------------------------------------------");
//            System.out.println("Average time usage for " + key + " operations:");
//            System.out.println("Total number of histories: " + globalInfo.get(key).count);
//            System.out.println("BCO : " + globalInfo.get(key).bco_avg);
//            System.out.println("ICO : " + globalInfo.get(key).ico_avg);
//            System.out.println("CF  : " + globalInfo.get(key).cf_avg);
//            System.out.println("BHBO: " + globalInfo.get(key).bhbo_avg);
//            System.out.println("IHBO: " + globalInfo.get(key).ihbo_avg);
            System.out.println(key + "  " + globalInfo.get(key).ico_avg + "  " + globalInfo.get(key).bco_avg + " " + globalInfo.get(key).cf_avg + " " + globalInfo.get(key).ihbo_avg+ " " + globalInfo.get(key).bhbo_avg + "    " + globalInfo.get(key).ihbo_loop + "   " + globalInfo.get(key).bhbo_loop +"");
        }
    }

    public static void main(String args[]) throws Exception{
        LogFileAnalyzerV3 analyzer = new LogFileAnalyzerV3();
        analyzer.logFileAnalyzer("CheckingSelectedLog0329_local_failure.txt", 14);
    }


}
