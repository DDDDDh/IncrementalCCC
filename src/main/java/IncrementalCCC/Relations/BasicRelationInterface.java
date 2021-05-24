package IncrementalCCC.Relations;

import IncrementalCCC.History.Operation;

import java.util.LinkedList;

import java.util.BitSet;

public interface BasicRelationInterface {

    void setTrue(int fromIndex, int toIndex);
    boolean existEdge(int fromIndex, int toIndex);
    void printMatrix();
//    boolean[][] getRelationMatrix();
    BitSet[] getRelationMatrix();
    int getMatrixSize();
    void union(BasicRelation r1, BasicRelation r2);
    void unionAndSetVis(BasicRelation r1, BasicRelation r2, LinkedList<Operation> opList);
}
