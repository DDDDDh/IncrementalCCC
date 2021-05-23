package HistoryProducer;

//use loombok

import lombok.*;

import java.util.HashSet;

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
    HashSet<Integer> causalPast;


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
        this.causalPast = new HashSet<>();
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

    public String printBare(){
        String temp = "";
        if(this.getMethod() == methods.Read){
            temp += "r";
        }
        else if(this.getMethod() == methods.Write){
            temp += "w";
        }
        if(this.getValue()!= -1) {
            temp = temp + this.getVariable() + this.getValue();
        }
        else{
            temp = "";
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

    public void updateCausalPast(generatedOperation otherOp){

        for(Integer i: otherOp.getCausalPast()){ //把另一操作的causalPast里的操作都加入本身
            this.causalPast.add(i);
        }

        //再加上自身的下标
        this.causalPast.add(this.getIndex());
    }

    public boolean causallyLaterThan(generatedOperation otherOp){
        if(this.getCausalPast().contains(otherOp.getIndex())){
            return true;
        }
        else{
            return false;
        }
    }


}
