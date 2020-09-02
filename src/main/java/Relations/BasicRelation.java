package Relations;

import History.Operation;

import java.util.BitSet;
import java.util.LinkedList;

public class BasicRelation implements BasicRelationInterface{

    private boolean[][] relationMatrix;
    private int matrixSize;

    public BasicRelation(int size){
        this.relationMatrix = new boolean[size][size];
        this.matrixSize = size;
    }


    public void setTrue(int fromIndex, int toIndex){
        this.relationMatrix[fromIndex][toIndex] = true;
    }

    public boolean existEdge(int fromIndex, int toIndex){
        return this.relationMatrix[fromIndex][toIndex];
    }

    public void printMatrix(){
        String curLine = "";
        for(int i = 0; i < this.matrixSize; i++){
            curLine = "";
            for(int j = 0; j < this.matrixSize; j++){
                if(this.relationMatrix[i][j]){
                    curLine += "1 ";
                }
                else{
                    curLine += "0 ";
                }
            }
            System.out.println(curLine);
        }
    }

    public boolean[][] getRelationMatrix() {
        return this.relationMatrix;
    }

    public int getMatrixSize(){
        return this.matrixSize;
    }

    //将关系r1与关系r2取并集，再存入当前关系中
    public void union(BasicRelation r1, BasicRelation r2){
        assert(r1.getMatrixSize() == r2.getMatrixSize());
        assert(this.getMatrixSize() == r1.getMatrixSize());

        boolean[][] r1Matrix = r1.getRelationMatrix();
        boolean[][] r2Matrix = r2.getRelationMatrix();

        int size = this.getMatrixSize();

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(r1Matrix[i][j] || r2Matrix[i][j]){
                    this.setTrue(i, j);
                }
            }
        }
    }

    public void unionAndSetVis(BasicRelation r1, BasicRelation r2, LinkedList<Operation> opList){
        assert(r1.getMatrixSize() == r2.getMatrixSize());
        assert(this.getMatrixSize() == r1.getMatrixSize());
        assert (r1.getMatrixSize() == opList.size());

        boolean[][] r1Matrix = r1.getRelationMatrix();
        boolean[][] r2Matrix = r2.getRelationMatrix();

        int size = this.getMatrixSize();

        for(int i = 0; i < size; i++){
            for(int j = 0; j < size; j++){
                if(r1Matrix[i][j] || r2Matrix[i][j]){
                    this.setTrue(i, j);
                    opList.get(j).getCoList().set(i, true);
                }
            }
        }


    }

}
