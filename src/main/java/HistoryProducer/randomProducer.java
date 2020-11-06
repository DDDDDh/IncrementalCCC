package HistoryProducer;

//主要负责把生成的随机操作排列成合理的顺序，并输出到文件中

import java.util.Calendar;
import java.util.LinkedList;
import lombok.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

@Data
public class randomProducer {

    int opNum;
    int processNum;
    LinkedList<generatedOperation> opList;
    String outputPath;
    int wRate;
    int rRate;
//    String url = "src/main/resources/BadPatternExamples/CyclicHB2_history.edn";

    randomProducer(){
        this.setOpNum(20); //默认生成20个操作
        this.setProcessNum(5); //默认分配在5个线程
        this.setRRate(3);  //默认读写比例为3：1
        this.setWRate(1);
        this.opList = new LinkedList<>();
    }

    randomProducer(int opNum, int processNum, int rRate, int wRate){
        this.setOpNum(opNum);
        this.setProcessNum(processNum);
        this.setRRate(rRate);
        this.setWRate(wRate);
        this.opList = new LinkedList<>();
    }

    public void generatePath(){
        String url = "src/main/resources/RandomHistories/";
        Calendar cTime = Calendar.getInstance();
        String timeStamp = "" + cTime.get(Calendar.YEAR)  + (cTime.get(Calendar.MONTH)+1)  + cTime.get(Calendar.DAY_OF_MONTH) + cTime.get(Calendar.HOUR_OF_DAY);
        url += "Running_"+timeStamp+"_opNum"+this.getOpNum()+"_processNum" +this.getProcessNum() +"_rRate"+this.getRRate()+"_wRate" + this.getWRate()+".edn";
        this.setOutputPath(url);
    }

    public String printOp(generatedOperation tempOp){
        String tempStr = "{";
        //填充type字段
        if(tempOp.getType() ==1){ //暂时只考虑生成type ok的操作
            tempStr +=":type :ok, ";
        }
        else{

        }

        //填充f字段
        if(tempOp.getMethod() == methods.Write){
            tempStr += ":f :write, ";
        }
        else if(tempOp.getMethod() == methods.Read){
            tempStr += ":f :read, ";
        }

        //填充value字段
        if(tempOp.getValue() != -1) {
            tempStr += ":value [" + tempOp.getVariable() + " " + tempOp.getValue() + "], ";
        }
        else{
            tempStr += ":value [" + tempOp.getVariable() + " nil], ";
        }

        //填充process字段
        tempStr += ":process " + tempOp.getProcess() +", ";

        //填充time字段
        tempStr += ":time " + tempOp.getTime() +", ";

        //填充position字段
        tempStr += ":position " + tempOp.getPosition() + ", ";

        //填充link字段
        tempStr += ":link " + tempOp.getLink() +", ";

        //填充index字段
        tempStr += ":index " + tempOp.getIndex() +"}";
        return tempStr;
    }

    public void printToFile() throws FileNotFoundException{
        File outfile = new File(this.getOutputPath());
        PrintWriter output = new PrintWriter(outfile);
        generatedOperation tempOp;
        String tempStr = "";
        for(int i = 0; i < this.opList.size(); i++){
            tempOp = this.opList.get(i);
            tempStr = printOp(tempOp);
            output.println(tempStr);
        }
        output.close();
    }

//    {:type :ok, :f :write, :value [b 1], :process 3, :time 19, :position 19, :link nil, :index 18}

    public static void main(String args[]) throws Exception{


//        randomProducer rProducer = new randomProducer();
//
//        operationProducer oProducer = new operationProducer(rProducer.getRRate(), rProducer.getWRate());
//        generatedOperation tempOp;
//
//        int wCount = 0;
//        int rCount = 0;
//
//        for(int i = 0; i < rProducer.getOpNum(); i++){
//            tempOp = oProducer.nextOperation();
//            rProducer.getOpList().add(tempOp);
//            if(tempOp.isWrite()){
//                wCount++;
//            }
//            else{
//                rCount++;
//            }
//            System.out.println(tempOp);
//        }
//
//        System.out.println("Total write num:" + wCount);
//        System.out.println("TOtal read num:" + rCount);
//
//        rProducer.generatePath();
//        rProducer.printToFile();

        CCProducer ccProducer = new CCProducer(100, 5, 3, 1);
        ccProducer.generatePath();
        ccProducer.generateCCHistory();
        ccProducer.printToFile();
        ccProducer.printToFileDebug();

    }




}
