package Relations;

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

    public void printMatrx(){
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

}
