package HistoryProducer;

//主要负责把生成的随机操作排列成合理的顺序，并输出到文件中

public class randomProducer {



    int opNum;
    int processNum;



    public static void main(String args[]){

        operationProducer producer = new operationProducer(3, 1);
        generatedOperation tempOp;
        int wCount = 0;
        int rCount = 0;
        for (int i = 0; i < 100; i++){
            tempOp = producer.nextOperation();
            if(tempOp.isWrite()){
                wCount++;
            }
            else{
                rCount++;
            }
            System.out.println(tempOp.easyPrint());
        }

        System.out.println("Total write num:" + wCount);
        System.out.println("TOtal read num:" + rCount);

    }




}
