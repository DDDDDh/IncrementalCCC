package Relations;

import History.*;
import lombok.Data;

import java.util.BitSet;
import java.util.LinkedList;

@Data
public class BasicRelation implements BasicRelationInterface{

//    private boolean[][] relationMatrix;
    private BitSet[] relationMatrix; //用每个操作的后继节点数组伪装的邻接矩阵
    private int matrixSize;

    //标识变量，用于HBo计算时返回标志
    private int processID;
    private boolean isCyclicCO;
    private boolean isThinAirRead;
    private boolean isCyclicHB;
    private int loopTime;

    public BasicRelation(int size){
//        this.relationMatrix = new boolean[size][size];
        this.relationMatrix = new BitSet[size];
        for(int i = 0; i < size; i++){
            this.relationMatrix[i] = new BitSet(size);
        }
        this.matrixSize = size;
        this.processID = -1;
        this.isCyclicCO = false;
        this.isThinAirRead = false;
        this.isCyclicHB = false;
    }

    public void setTrue(int fromIndex, int toIndex){
        BitSet tempSet = this.relationMatrix[fromIndex];
        tempSet.set(toIndex, true);
    }

    public void setFalse(int fromIndex, int toIndex){
        BitSet tempSet = this.relationMatrix[fromIndex];
        tempSet.set(toIndex, false);
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
                    System.out.println("First Matrix:" + i + " to " + j +":" + this.existEdge(i,j));
                    System.out.println("Second Matrix:" + i + " to " + j +":" + otherMatrix.existEdge(i,j));
                    return false;
                }
            }
        }

        return true;
    }


    public boolean checkEqualDebug(BasicRelation otherMatrix, LinkedList<Operation> opList){

        if(this.getMatrixSize() != otherMatrix.getMatrixSize()){
            return false;
        }

        int size = this.getMatrixSize();

        boolean isTrue = true;

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(this.existEdge(i, j) ^ otherMatrix.existEdge(i,j)){ //如果两个矩阵中有一个点不同
                    System.out.print("This["+i+"]["+j+"]:" + this.existEdge(i,j));
                    System.out.println(" OtherMatrix["+i+"]["+j+"]:" +otherMatrix.existEdge(i,j));
                    System.out.println("Operation i:" + opList.get(i).easyPrint() +" on process " + opList.get(i).getProcess() + " Operation j:" + opList.get(j).easyPrint()+" on process " + opList.get(j).getProcess());
//                    return false;
                    isTrue = false;
                }
            }
        }

        return isTrue;
    }


    //注意，这里是会需要用this.existEdge判断是否存在边的，因此需要先初始化关系矩阵
    public LinkedList<Integer> topoSort(History history){
        LinkedList<Operation> opList = history.getOperationList();
        LinkedList<Integer> topoList = new LinkedList<>();
        LinkedList<Integer> stack = new LinkedList<>();
        int size = this.getMatrixSize();
        int[] inDegree = new int[size];
        int tempDegree = 0;

        //根据邻接矩阵为每个点初始化入度
        for(int j = 0; j < size; j++){
            tempDegree = 0;
            for(int i = 0; i < size; i++){
                if(this.existEdge(i,j)){
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
        Operation curOp;
        while(!stack.isEmpty()){
            curID = stack.removeFirst();
            curOp = opList.get(curID);
            topoList.add(curID);
            curOp.setTopoID(count++);
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
//        if(count < size){
//            this.isCyclicCO = true;
//            System.out.println("Detected CyclicCO!");
//        }
//        System.out.println("Count: " + count + " Size:" + size);
        return topoList;
    }

    //利用拓扑排序判环
    public boolean cycleDetection(){

//        System.out.println("Hello cycle detection");

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

        BitSet outNodes = new BitSet(size);
        for(int i = 0; i < size; i++){
            outNodes.set(i, true);
        }

        int curID;
        while(!stack.isEmpty()){
            curID = stack.removeFirst();
            count++;
//            System.out.println("Out:" + curID);
            outNodes.set(curID, false);

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
//            System.out.println("Detected a cycle in matrix between nodes:");
//            for (int i = outNodes.nextSetBit(0); i >= 0; i = outNodes.nextSetBit(i + 1)) {
//                System.out.print(i + " ");
//            }
//            System.out.println();
            return true;
        }
//        System.out.println("No cycle in matrix.");
        return false;
    }

    //利用邻接矩阵本身判环
    public boolean cycleDetectionByMatrix(){
        int size = this.getMatrixSize();
        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(this.existEdge(i,j) && this.existEdge(j, i)){
                    System.out.println("There is a cycle between " + i + " and " + j);
                    return true;
                }
            }
        }
        return false;
    }

    //使用Washall算法计算传递闭包
    public void computeTransitiveClosure(){
        int size = this.getMatrixSize();
        for(int k = 0; k < size; k++){
            for(int i = 0; i < size; i++){
                for( int j = 0; j < size; j++){
                    if(!existEdge(i,j)) { //如果i->j本身就有边，不需要更新
                        if (existEdge(i, k) && existEdge(k, j)) {
                            this.setTrue(i,j);
                        }
                    }
                }
            }
        }
    }

    //使用BFS计算传递闭包
    public void computeTransitiveClosureByBFS(){
        int size = this.getMatrixSize();
        for(int i = 0; i < size; i++){
            BFSUtil(i);
        }
    }

    public void BFSUtil(int src){
        LinkedList<Integer> stack = new LinkedList<>();
        stack.push(src);
        int curID;
        BitSet succeedList;
        while(!stack.isEmpty()){
            curID = stack.removeFirst();
            succeedList = this.getRelationMatrix()[curID];
            if(curID != src) {
                this.setTrue(src, curID);
            }
            for(int j = succeedList.nextSetBit(0); j >= 0; j = succeedList.nextSetBit(j+1)){
                stack.push(j);
            }
        }
    }

    //利用当前关系矩阵更新list中操作的可达关系
    //注意，该函数会改变opList中操作的可见范围
    public void updateListByMatrix(LinkedList<Operation> opList){

        assert (opList.size() == this.getMatrixSize());

        int size = this.getMatrixSize();

        BitSet curList;

        for(int i = 0; i < size; i++){
            curList = this.getRelationMatrix()[i]; //得到点i的后继列表
            //对于每条i->j的边，在j的前驱列表里设置可见
//            for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
//                opList.get(j).getCoList().set(i,true);
//            }
            for(int j = 0; j < size; j++){
                if(curList.get(j)){
                    opList.get(j).getCoList().set(i,true);
                }
                else{
                    opList.get(j).getCoList().set(i,false);
                }
            }
        }
    }

    public void updateCMListByMatrix(LinkedList<CMOperation> opList){
        assert (opList.size() == this.getMatrixSize());

        int size = this.getMatrixSize();

        BitSet curList;

        for(int i = 0; i < size; i++){
            curList = this.getRelationMatrix()[i]; //得到点i的后继列表
            //对于每条i->j的边，在j的前驱列表里设置可见
//            for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
//                opList.get(j).getCoList().set(i,true);
//            }
            for(int j = 0; j < size; j++){
                if(curList.get(j)){
                    opList.get(j).getCoList().set(i,true);
                }
                else{
                    opList.get(j).getCoList().set(i,false);
                }
            }
        }
    }

    //根据opList中的coList更新当前关系矩阵
    public void updateMatrixByCMList(LinkedList<CMOperation> opList){
        assert (opList.size() == this.getMatrixSize());
        int size = this.getMatrixSize();
        Operation curOp;
        BitSet curList;

        for(int i = 0; i< size; i++){
            curOp = opList.get(i);
            curList = curOp.getCoList();
            for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
                this.getRelationMatrix()[j].set(i);
            }
        }
    }

    public void updateMatrixByList(LinkedList<Operation> opList){
        assert (opList.size() == this.getMatrixSize());
        int size = this.getMatrixSize();
        Operation curOp;
        BitSet curList;

        for(int i = 0; i< size; i++){
            curOp = opList.get(i);
            curList = curOp.getCoList();
            for(int j = curList.nextSetBit(0); j >= 0; j = curList.nextSetBit(j+1)){
                this.getRelationMatrix()[j].set(i);
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
