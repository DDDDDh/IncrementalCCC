package Relations;

import History.Operation;

import java.util.LinkedList;

public interface BasicRelationInterface {

    void setTrue(int fromIndex, int toIndex);
    boolean existEdge(int fromIndex, int toIndex);
    void printMatrix();
    boolean[][] getRelationMatrix();
    int getMatrixSize();
    void union(BasicRelation r1, BasicRelation r2);
    void unionAndSetVis(BasicRelation r1, BasicRelation r2, LinkedList<Operation> opList);
}
