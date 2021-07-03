package DBConnector;

import History.*;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import site.ycsb.Status;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MyMongoClient {

    MongoClient mongoClient; //建立一个连接到数据库的Client，不用于读写，只用于监视数据库的状态
    MongoDatabase mongoDatabase;
    MongoCollection<Document> mongoCollection;
    String mongoSRV;
    String dbName;
    String collectionName;
    int target; //target operation per second
    String logPath;
    ReadConcern readConcern;
    WriteConcern writeConcern;
    PrintWriter output;

    public MyMongoClient(String mongoSRV, String dbName, String collectionName, int target, String logPath, ReadConcern readConcern, WriteConcern writeConcern) throws FileNotFoundException{
        this.mongoSRV = mongoSRV;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.target = target;
        this.logPath = logPath;
        File outfile = new File(this.logPath);
        this.output = new PrintWriter(outfile);
        this.readConcern = readConcern;
        this.writeConcern = writeConcern;
        this.startConnection();
        this.clearDB();//开始运行前先清理数据库
    }

    public void startConnection(){
        mongoClient = MongoClients.create(this.mongoSRV);
        mongoDatabase = mongoClient.getDatabase(this.dbName);
        mongoCollection = mongoDatabase.getCollection(this.collectionName);
    }

    public static String printOp(String type, Operation op, int index) {
        if (op.getValue() == -1) {
            return "{:type :" + type + ", :f :" + op.getType() + ", :value [" + op.getKey() + " " + "nil" + "], :process " + op.getProcess() + ", :time " + System.nanoTime() + ", :position " + op.getPosition() + ", :index " + index + "}";
        }
        else{
            return "{:type :" + type + ", :f :" + op.getType() + ", :value [" + op.getKey() + " " + op.getValue() + "], :process " + op.getProcess() + ", :time " + System.nanoTime() + ", :position " + op.getPosition() + ", :index " + index + "}";
        }
    }

    public void closeConnection(){
        this.output.close();
        mongoClient.close();
    }

    public void clearDB(){

        Document tempDocument = new Document();
        long docCount = mongoCollection.countDocuments();
//        System.out.println("number of documents before clear:" + docCount);
        mongoCollection.deleteMany(tempDocument);
        docCount = mongoCollection.countDocuments();
//        System.out.println("number of documents after clear:" + docCount);

    }

    public String loadAndRun(String srcLog) throws IOException{
        String runningLog = "";
        HistoryReader reader = new HistoryReader(srcLog);
        History inputHis = new History(reader.readHistory());
        inputHis.setOpNum(reader.getTotalNum());
        Set<Integer> processes = inputHis.getProcessOpList().keySet();
        int processNum = processes.size();
        System.out.println("processNum:" + processNum);

        //compute the target throughput
        double targetperthreadperms = -1;
        if (target > 0) {
            double targetperthread = ((double) this.target) / ((double) processNum);
            targetperthreadperms = targetperthread / 1000.0;
        }

        HashMap<Integer, LinkedList<Operation>> processOpList = new HashMap<Integer, LinkedList<Operation>>();
        for(Integer curProcess: processes){
            processOpList.put(curProcess, new LinkedList<Operation>());
        }
        inputHis.generateProcessOpList(processOpList);
        HashMap<Integer, String> processLogPath = new HashMap<Integer, String>();
//        int curProcess;
        final CountDownLatch completeLatch = new CountDownLatch(processNum);
        final List<MyMongoClientThread> clients = new ArrayList<>(processNum);
        LinkedList<Operation> curOpList;
        String curLog;
        AtomicInteger globalIndex = new AtomicInteger(0);
        for(Integer curProcess: processes){
            curOpList = processOpList.get(curProcess);
            if(curOpList == null){
                System.out.println("A null???");
            }
            curLog = this.logPath.substring(0, this.logPath.indexOf(".edn")) + "_" + curProcess + ".edn";
            processLogPath.put(curProcess, curLog);
            MyMongoClientThread t =  new MyMongoClientThread(this.mongoSRV, this.dbName, this.collectionName,
                    curLog, this.readConcern, this.writeConcern, targetperthreadperms, curOpList, completeLatch, globalIndex);
            t.setThreadId(curProcess);
            clients.add(t);
        }
        final Map<Thread, MyMongoClientThread> threads = new HashMap<>(processNum);
        for(MyMongoClientThread client: clients){
            threads.put(new Thread(client), client);
        }
        for (Thread t : threads.keySet()) {
            t.start();
        }

        int opsDone = 0;

        for (Map.Entry<Thread, MyMongoClientThread> entry : threads.entrySet()) {
            try {
                entry.getKey().join();
                opsDone += entry.getValue().getOpsDone();
            } catch (InterruptedException ignored) {
                // ignored
            }
        }

        //检查opsDone是否等于history.num
        assertTrue("opsDone should equals history op num", opsDone == inputHis.getOpNum());
//        System.out.println("OpsDone:" + opsDone);

        //TODO:将多线程的log整合成一个log

        LinkedList<Operation> globalOpList = new LinkedList<Operation>();
        for(Integer curProess: processes){
            curOpList = processOpList.get(curProess);
            for(Operation curOp: curOpList){
                boolean arranged = false;
                for(int i = 0; i < globalOpList.size(); i++){
                    if(curOp.getPosition() < globalOpList.get(i).getPosition()){
                        globalOpList.add(i, curOp);
                        arranged = true;
                        break;
                    }
                }
                if(!arranged){ //如果没有插入在队列中间，那么就在尾部添加。
                    globalOpList.add(curOp);
                }
            }
        }

        for(int i = 0; i < globalOpList.size(); i++){
            Operation curOp = globalOpList.get(i);
            output.println(printOp("ok", curOp, i));
        }

        this.closeConnection();
        runningLog = this.logPath;
        return runningLog;
    }


    //TODO: split multi-thread and single thread.

    public static void main(String args[]) throws Exception {
        System.out.println("hello world");
    }


}
