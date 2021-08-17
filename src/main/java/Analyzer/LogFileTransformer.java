package Analyzer;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import DataStructure.*;

import java.io.*;

public class LogFileTransformer {
    final String KEY_TYPE = ":type ";
    final String KEY_F = " :f ";
    final String KEY_VALUE = " :value ";
    final String KEY_PROCESS = " :process ";
    final String KEY_TIME = " :time ";
    final String KEY_POSITION = " :position ";
    final String KEY_LINK = " :link ";
    int idx;

    String transferToJson(String ednPath) throws IOException {
        String jsonPath = ednPath.replace(".edn", ".json");
        BufferedReader in = new BufferedReader(new FileReader(ednPath));
        File outFile = new File(jsonPath);
        PrintWriter output = new PrintWriter(outFile);
        String tempLine;
        while ((tempLine = in.readLine())!= null){
            tempLine = StringUtils.strip(tempLine, "{}");
            JsonLine op = this.getJsonLine(tempLine);
            String jsonString = JSON.toJSONString(op);
            System.out.println("JSON line:" + jsonString);
            output.println(jsonString);
        }
        output.close();
        return jsonPath;
    }

    public JsonLine getJsonLine(String line) {
        String[] subs = line.split(",");
        String type = StringUtils.remove(subs[0], KEY_TYPE);
        if (!type.equals(":ok")) { //如果不是正常返回的操作，则忽略
            return null;
        }
        String f = StringUtils.remove(subs[1], KEY_F);
        String val = StringUtils.remove(subs[2], KEY_VALUE);
        String[] kv = StringUtils.strip(val, "[]").split(" ");
        String key = kv[0];
        int value;
        if(kv[1].equals("nil") || kv[1].equals("0")){ //读到0或者-1都是初值
            value = -1;
        }
        else{
            value = Integer.parseInt(kv[1]);
        }
        int process = Integer.parseInt(StringUtils.remove(subs[3], KEY_PROCESS));
        long time = Long.parseLong(StringUtils.remove(subs[4], KEY_TIME));
        long position = Long.parseLong(StringUtils.remove(subs[5], KEY_POSITION));
        int index = idx++; //注意，这个index是读入正常操作之后进行的另外编号，与历史记录里本身的index无关
        return new JsonLine(type, f, key, value, process, time, position, index);
    }

    public static void main(String[] args) throws Exception{
        LogFileTransformer transformer = new LogFileTransformer();
        transformer.transferToJson("/Users/yi-huang/Desktop/Yi_Huang/TestZone/mongoLogfile_8_15_opNum500_processNum5_varNum50_1.edn");
        System.out.println("End.");
    }

}
