package HistoryProducer;

import site.ycsb.Client;
import site.ycsb.WorkloadException;
import site.ycsb.generator.*;
import lombok.*;

import java.io.FileNotFoundException;

@Data
public class YCSBProducer extends RandomProducer{ //利用YCSB生成操作

    int opNum;
    double readProportion; //读操作比例(0 to 1)
    double writeProportion; //写操作比例(0 to 1)
    int varRange;   //变量范围,也即均匀分布的最大值
    int valueRange; //写入值范围
    int processRange;   //线程数
    int globalTime; //操作的全局时间
    int globalPosition;    //全局position编号
    String varDistribution; //生成变量的分布规则
    String processDistribution; //生成线程的分布规则
    protected int[] varCounter;
    int readCount;
    int writeCount;
    int globalIndex;

    protected NumberGenerator varProducer; //访问变量生成器
    protected DiscreteGenerator typeProducer; //操作类型生成器
    protected NumberGenerator processProducer; //对应线程生成器
    protected AcknowledgedCounterGenerator transactionInsertVarSequence;

    public YCSBProducer() throws WorkloadException{ //默认构造函数, 记录变量的default value
        this.setOpNum(1000);
        this.setReadProportion(0.75);
        this.setWriteProportion(0.25);
        this.setVarRange(50);
        this.setValueRange(1000);
        this.setProcessRange(10);
        this.setGlobalTime(0);
        this.setGlobalPosition(0);
        this.setVarDistribution("uniform");
        this.setProcessDistribution("uniform");
        this.varCounter = new int[this.getVarRange()];
        this.transactionInsertVarSequence = new AcknowledgedCounterGenerator(this.getOpNum());
        this.init();
        this.setReadCount(0);
        this.setWriteCount(0);
        this.setGlobalIndex(0);
    }

    public YCSBProducer(int opNum, double rp, double wp, int varR, int valR, int pr, String vd, String pd) throws WorkloadException{
        this.setOpNum(opNum);
        this.setReadProportion(rp);
        this.setWriteProportion(wp);
        this.setVarRange(varR);
        this.setValueRange(valR);
        this.setProcessRange(pr);
        this.setProcessNum(pr);
        this.setGlobalTime(0);
        this.setGlobalPosition(0);
        this.setVarDistribution(vd);
        this.setProcessDistribution(pd);
        this.varCounter = new int[this.getVarRange()];
        this.transactionInsertVarSequence = new AcknowledgedCounterGenerator(this.getOpNum());
        this.init();
        this.setReadCount(0);
        this.setWriteCount(0);
        this.setGlobalIndex(0);
    }

    protected void initTypeProducer(){ //初始化操作类型生成器
        this.typeProducer = new DiscreteGenerator();
        this.typeProducer.addValue(this.getReadProportion(), "Read");
        this.typeProducer.addValue(this.getWriteProportion(), "Write");
    }

    protected void initVarProducer() throws WorkloadException{ //初始化访问变量生成器
        if(this.getVarDistribution().equals("uniform")){
            this.varProducer = new UniformLongGenerator(0, this.getVarRange()-1);
        }
        else if(this.getVarDistribution().equals("exponential")){
            double percentile = Double.parseDouble(ExponentialGenerator.EXPONENTIAL_PERCENTILE_DEFAULT);
            double frac = Double.parseDouble(ExponentialGenerator.EXPONENTIAL_FRAC_DEFAULT);
            this.varProducer = new ExponentialGenerator(percentile, this.getVarRange() * frac);
        }
        else if(this.getVarDistribution().equals("sequential")){
            this.varProducer = new SequentialGenerator(0, this.getVarRange()-1);
        }
        else if(this.getVarDistribution().equals("zipfan")){
            final double insertproportion = 0.0;
            int opcount = this.getVarRange();
            int expectednewkeys = (int) ((opcount) * insertproportion * 2.0); // 2 is fudge factor
            this.varProducer = new ScrambledZipfianGenerator(0, opcount + expectednewkeys);
        }
        else if(this.getVarDistribution().equals("latest")){
            this.varProducer = new SkewedLatestGenerator(transactionInsertVarSequence);
        }
        else if(this.getVarDistribution().equals("hotspot")){
            double hotsetfraction = 0.2; //热点集的比例
            double hotopnfraction = 0.8; //访问热点集操作的比例
            //0.2, 0.8的配置意味着，在生成的操作种，有80%的操作都在访问某20%的热点数据
            this.varProducer = new HotspotIntegerGenerator(0, this.getVarRange() - 1, hotsetfraction, hotopnfraction);
        }
        else{
            throw new WorkloadException("Unknown request distribution of var:" + this.getVarDistribution() + "!");
        }

    }

    protected void initProcessProducer() throws WorkloadException{
        if(this.getProcessDistribution().equals("uniform")){
            this.processProducer = new UniformLongGenerator(0, this.getProcessRange()-1);
        }
        else if(this.getProcessDistribution().equals("hotspot")){
            double hotsetfraction = 0.2;
            double hotopnfraction = 0.8;
            this.processProducer = new HotspotIntegerGenerator(0, this.getProcessRange(), hotsetfraction, hotopnfraction);
        }
        else{
            throw new WorkloadException("Only support uniform and hotspot distribution for the moment!");
        }
    }

    protected void init() throws WorkloadException{

        this.initTypeProducer();
        this.initVarProducer();
        this.initProcessProducer();
//        System.out.println("Init OK");
    }

    public static long nextVarNum(NumberGenerator varProducer, AcknowledgedCounterGenerator transactionInsertVarSequence){
        long varNum = 0;
        if(varProducer instanceof ExponentialGenerator){
            do{
                varNum = transactionInsertVarSequence.lastValue() - varProducer.nextValue().intValue();
            } while (varNum < 0);
        } else{
            do{
                varNum = varProducer.nextValue().intValue();
            } while (varNum > transactionInsertVarSequence.lastValue());
        }
        return varNum;
    }

    public String varTransformer(int varNum){
        String var = "";
        int tempNum = varNum;
        StringBuilder builder = new StringBuilder();
        while(tempNum>=26){
            builder.append("A");
            tempNum = tempNum-26;
        }
        String tempVar = String.valueOf((char)(65+tempNum));
        builder.append(tempVar);
        var = builder.toString();
        return var;
    }

    public GeneratedOperation nextOperation(){
        GeneratedOperation nextOp = new GeneratedOperation();
        nextOp.setIndex(this.globalIndex++);
        nextOp.setType(1); //type OK
        String opType = this.typeProducer.nextString();
        int varInt = (int)nextVarNum(this.varProducer, this.transactionInsertVarSequence);
//        System.out.println("varInt:" + varInt);
        if(opType.equals("Read")){
//            System.out.println("type read");
            nextOp.setMethod(Methods.methods.Read);
            nextOp.setValue(0);
            this.setReadCount(this.getReadCount()+1);
        }
        else if(opType.equals("Write")){
//            System.out.println("type write");
            nextOp.setMethod(Methods.methods.Write);
            //因为unique value的假设，保持每个变量上写操作写入的值递增即可
            varCounter[varInt]++;
            nextOp.setValue(varCounter[varInt]);
//            System.out.println("value:" + varCounter[varInt]);
            this.setWriteCount(this.getWriteCount()+1);
        }
        else{
            System.out.println("Error, invalid operation type:" + opType + "!");
        }

        //将varInt变为以'A'开始的char值
        //65 is the ASCII code for 'A'
//        long a = 65;
//        a = a+varInt;
//        String varName = String.valueOf((char) a);
        String varName = varTransformer(varInt);
//        System.out.println("var name:" + varName);
        nextOp.setVariable(varName);
//        System.out.println("Return" + nextOp.printBare());
        int processNum = this.processProducer.nextValue().intValue();
//        System.out.println("process:" + processNum);
        nextOp.setProcess(processNum);

        return nextOp;
    }

    public void generateHistory(){

        GeneratedOperation curOp;
        for(int i = 0; i < this.getOpNum(); i++){
            curOp = this.nextOperation();
            this.opList.add(curOp);
        }
    }

    public static void main(String args[]) throws WorkloadException, FileNotFoundException{

        YCSBProducer producer = new YCSBProducer(100,0.75,0.25,20,100,5,"uniform","uniform");
        System.out.println("Begin to produce history...");
        producer.generatePath();
        producer.generateHistory();
        producer.printToFile();
        System.out.println("History is printed to file, log path:" + producer.getOutputPath());
        System.out.println("Total Read:" + producer.getReadCount() + " Total Write:" + producer.getWriteCount());
    }

}
