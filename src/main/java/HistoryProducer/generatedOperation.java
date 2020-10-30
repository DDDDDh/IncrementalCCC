package HistoryProducer;

//use loombok

import lombok.*;

enum methods{
    Write, Read
}

@Data
public class generatedOperation {

    int type; //0表示初始状态，1表示ok，2表示invoke
    methods method;
    String variable;
    int value;
    int process;
    int time;
    int position;
    String link;
    int index;


    //默认操作：R(a)0;
    public generatedOperation(){
        this.setType(0);
        this.setMethod(methods.Read);
        this.setVariable("a");
        this.setValue(0);
        this.setProcess(0);
        this.setTime(0);
        this.setPosition(0);
        this.setLink("nil");
        this.setIndex(0);
    }

    public String easyPrint(){
        String temp = "";
        if(this.getMethod() == methods.Read){
            temp += "R";
        }
        else if(this.getMethod() == methods.Write){
            temp += "W";
        }
        if(this.getValue()!= -1) {
            temp = temp + "(" + this.getVariable() + ")" + this.getValue();
        }
        else{
            temp = temp + "(" + this.getVariable() + ")" + "nil";
        }

        return temp;
    }

    public boolean isWrite(){
        if(this.getMethod() == methods.Write){
            return true;
        }
        else{
            return false;
        }
    }

    public boolean isRead(){
        if(this.getMethod() == methods.Read){
            return true;
        }
        else{
            return false;
        }
    }



}
