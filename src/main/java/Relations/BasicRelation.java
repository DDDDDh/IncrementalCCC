package Relations;

import History.Operation;

import java.util.BitSet;
import java.util.LinkedList;

public class BasicRelation implements BasicRelationInterface{

//    private boolean[][] relationMatrix;
    private BitSet[] relationMatrix; //用每个操作的后继节点链表伪装的邻接矩阵
    private int matrixSize;

    public BasicRelation(int size){
//        this.relationMatrix = new boolean[size][size];
        this.relationMatrix = new BitSet[size];
        for(int i = 0; i < size; i++){
            this.relationMatrix[i] = new BitSet(size);
        }
        this.matrixSize = size;
    }

    public void setTrue(int fromIndex, int toIndex){
        BitSet tempSet = this.relationMatrix[fromIndex];
        tempSet.set(toIndex, true);
    }

    public boolean existEdge(int fromIndex, int toIndex){
        return this.relationMatrix[fromIndex].get(toIndex);
    }

    public void printMatrix(){
        String curLine = "";
        for(int i = 0; i < this.matrixSize; i++){
            curLine = "";
            for(int j = 0; j < this.matrixSize; j++){
//                if(this.relationMatrix[i][j]){
                if(this.relationMatrix[i].get(j)){
                    curLine += "1 ";
                }
                else{
                    curLine += "0 ";
                }
            }
            System.out.println(curLine);
        }
    }

    public BitSet[] getRelationMatrix(){
        return this.relationMatrix;
    }

    public int getMatrixSize(){
        return this.matrixSize;
    }

    //将关系r1与关系r2取并集，再存入当前关系中
    public void union(BasicRelation r1, BasicRelation r2){
        assert(r1.getMatrixSize() == r2.getMatrixSize());
        assert(this.getMatrixSize() == r1.getMatrixSize());

        int size = this.getMatrixSize();

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
//                if(r1Matrix[i][j] || r2Matrix[i][j]){
                if(r1.existEdge(i, j)|| r2.existEdge(i, j)){
                    this.setTrue(i, j);
                }
            }
        }
    }

    public void unionAndSetVis(BasicRelation r1, BasicRelation r2, LinkedList<Operation> opList){
        assert(r1.getMatrixSize() == r2.getMatrixSize());
        assert(this.getMatrixSize() == r1.getMatrixSize());
        assert (r1.getMatrixSize() == opList.size());

        int size = this.getMatrixSize();

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
//                if(r1Matrix[i][j] || r2Matrix[i][j]){
                if(r1.existEdge(i, j) || r2.existEdge(i, j)){
                    this.setTrue(i, j);
                    opList.get(j).getCoList().set(i, true);
                }
            }
        }
    }

    public boolean checkEqual(BasicRelation otherMatrix){

        if(this.getMatrixSize() != otherMatrix.getMatrixSize()){
            return false;
        }

        int size = this.getMatrixSize();

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(this.existEdge(i, j) ^ otherMatrix.existEdge(i,j)){ //如果两个矩阵中有一个点不同
                    return false;
                }
            }
        }

        return true;
    }

    //利用拓扑排序判环
    public boolean cycleDetection(){

        LinkedList<Integer> stack = new LinkedList<>();
        int size = this.getMatrixSize();
        int[] inDegree = new int[size];
        int tempDegree;

        for(int j = 0; j < size; j++){
            tempDegree = 0;
            for(int i = 0; i < size; i++){
                if(this.existEdge(i, j)){
                    tempDegree++;
                }
            }
            inDegree[j] = tempDegree;
        }

        int count = 0; //判环辅助变量
        for(int i = 0; i < size; i++){
            if(inDegree[i] == 0){ //找到入度为0的点，入栈
                stack.addFirst(i);
                inDegree[i] = -1;
            }
        }

        int curID;
        while(!stack.isEmpty()){
            curID = stack.removeFirst();
            for(int i = 0; i < size; i++){
                if(this.existEdge(curID, i)){
                    inDegree[i]--;
                    if(inDegree[i] == 0){
                        stack.addFirst(i);
                        inDegree[i] = -1;
                    }
                }
            }
        }
        if(count < size) {
            return true;
        }
        return false;
    }

    public void computeTransitiveClosure(){
        int size = this.getMatrixSize();
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                for( int k = 0; k < size; k++){
                    if(!existEdge(i,j)) { //如果i->j本身就有边，不需要更新
                        if (existEdge(i, k) && existEdge(k, j)) {
                            this.setTrue(i,j);
                        }
                    }
                }
            }
        }
    }

    //利用当前关系矩阵更新list中操作的可达关系
    public void updateListByMatrix(LinkedList<Operation> opList){

        assert (opList.size() == this.getMatrixSize());

        int size = this.getMatrixSize();

        BitSet curList;

        for(int i = 0; i < size; i++){
            curList = this.getRelationMatrix()[i]; //得到点i的后继列表
            //对于每条i->j的边，在j的前驱列表里设置可见
            for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
                opList.get(j).getCoList().set(i,true);
            }
        }
    }

    public void updateCoList(LinkedList<Operation> opList){
        int size = this.getMatrixSize();
        Operation curOp;
        BitSet curList;
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(existEdge(i, j)){
                    curOp = opList.get(j);
                    curList = curOp.getCoList();
                    curList.set(i, true);
                }
            }
        }
    }

}
