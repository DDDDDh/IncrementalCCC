package History;

import java.util.HashMap;
import java.util.LinkedList;
import lombok.*;

@Data
public class CMOperation extends Operation{

    int lastRead;
    int processReadID; //当前线程读操作全序的编号;
    int earlistRead;
    int masterPid; //当前考虑的线程id
    LinkedList<Integer> rrList; //Reachable Read List, 后向链表，标示当前写操作可达的读操作集合
    HashMap<String, Integer> precedingWrite; //标示对当前操作的可见写操作集合而言，每个变量上最近一个写操作
    LinkedList<Integer> preList; //前驱列表
    LinkedList<Integer> sucList; //后继列表

}
