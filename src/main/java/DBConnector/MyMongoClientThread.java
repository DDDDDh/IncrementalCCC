package DBConnector;

import DataStructure.JsonLine;
import History.Operation;
import com.alibaba.fastjson.JSON;
import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import site.ycsb.Status;

import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;

public class MyMongoClientThread implements Runnable{

    private final CountDownLatch completeLatch;
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    MongoCollection<Document> mongoCollection;
    String logPath;
    String mongoSRV;
    String dbName;
    String collectionName;
    PrintWriter output;
    LinkedList<Operation> opList; //需要执行的操作列表
    ReadConcern readConcern;
    WriteConcern writeConcern;
    int threadid;
    AtomicInteger globalIndex;
    ClientSession session;

    //Used to control ops throughput
    private double targetOpsPerMs;
    private int opsDone;
    private long targetOpsTickNs;

    public MyMongoClientThread(String mongoSRV, String dbName, String collectionName, String logPath,
                               ReadConcern readConcern, WriteConcern writeConcern, double targetOpsPerMs,
                               LinkedList<Operation> opList, CountDownLatch completeLatch,
                               AtomicInteger globalIndex) throws FileNotFoundException{
        this.mongoSRV = mongoSRV;
        this.dbName = dbName;
        this.collectionName = collectionName;
        this.logPath = logPath;
        File outfile = new File(this.logPath);
        this.output = new PrintWriter(outfile);
        this.readConcern = readConcern;
        this.writeConcern = writeConcern;
        this.completeLatch = completeLatch;
        this.opList = opList;
        this.globalIndex = globalIndex;
        this.opsDone = 0;
        if(targetOpsPerMs > 0){
            this.targetOpsPerMs = targetOpsPerMs;
            targetOpsTickNs = (long) (1000000 / targetOpsPerMs);
        }

    }

    public void setThreadId(final int threadId) {
        this.threadid = threadId;
    }

    public void startConnection(){
        mongoClient = MongoClients.create(this.mongoSRV);
        this.session = mongoClient.startSession(ClientSessionOptions.builder().causallyConsistent(true).build());
        mongoDatabase = mongoClient.getDatabase(this.dbName);
        mongoCollection = mongoDatabase.getCollection(this.collectionName);
    }

    public void closeConnection(){
        mongoClient.close();
    }

    public Status read(Operation op){
        try{
            Document tempDoc = new Document("Field", op.getKey());
//            FindIterable<Document> findIterable = this.mongoCollection.withReadPreference(ReadPreference.secondaryPreferred()).withReadConcern(this.readConcern).find(tempDoc);
            FindIterable<Document> findIterable = this.mongoCollection.withReadConcern(this.readConcern).find(session,tempDoc);
//            .withReadPreference(ReadPreference.secondaryPreferred())
            Document queryResult = findIterable.first();
            if(queryResult != null){
                op.setValue((Long) queryResult.get("Value"));
                op.setTime(System.nanoTime());
                op.setPosition(this.globalIndex.getAndIncrement());
            }
            else{
//                System.out.println("Not found, set to -1");
                op.setValue(-1);
                op.setTime(System.nanoTime());
                op.setPosition(this.globalIndex.getAndIncrement());
            }

            return queryResult != null ? Status.OK : Status.NOT_FOUND;
        } catch (Exception e){
            System.err.println("hi" + e.toString());
            System.out.println("ewwwwwwwwwww");
            return Status.ERROR;
        }
    }

    public Status write(Operation op){
        try{
            UpdateOptions options = new UpdateOptions();
            options.upsert(true);
            Document tempDoc = new Document("Field", op.getKey());
            Document queryDoc = new Document("Field", op.getKey());
            tempDoc.put("Value", op.getValue());
            this.mongoCollection.withWriteConcern(this.writeConcern).updateOne(session, queryDoc, new Document("$set", tempDoc), options);
            op.setTime(System.nanoTime());
            op.setPosition(this.globalIndex.getAndIncrement());
            return Status.OK;
        } catch (Exception e){
            System.out.println("hmmmmmmmmmmmmm");
            System.err.println(e.toString());
            return Status.ERROR;
        }
    }

    /**
     * Ref:ClientThread.java from ycsb 0.17.0
     * @param deadline
     */
    private static void sleepUntil(long deadline) {
        while (System.nanoTime() < deadline) {
            LockSupport.parkNanos(deadline - System.nanoTime());
        }
    }

    private void throttleNanos(long startTimeNanos) {
        //throttle the operations
        if (targetOpsPerMs > 0) {
            // delay until next tick
            long deadline = startTimeNanos + opsDone * targetOpsTickNs;
            sleepUntil(deadline);
        }
    }

    public int getOpsDone() {
        return this.opsDone;
    }

    public static String printOp(String type, Operation op, int index) {
        if (op.getValue() == -1) {
            return "{:type :" + type + ", :f :" + op.getType() + ", :value [" + op.getKey() + " " + "nil" +
                    "], :process " + op.getProcess() + ", :time " + System.nanoTime() + ", :position " +
                    op.getPosition() + ", :index " + index + "}";
        }
        else{
            return "{:type :" + type + ", :f :" + op.getType() + ", :value [" + op.getKey() + " " +
                    op.getValue() + "], :process " + op.getProcess() + ", :time " + System.nanoTime() +
                    ", :position " + op.getPosition() + ", :index " + index + "}";
        }
    }

    public String printOpJson(Operation op, int index){
        String tempStr;
        JsonLine tempLine = new JsonLine(op, index);
        tempStr = JSON.toJSONString(tempLine);
        return tempStr;
    }

    @Override
    public void run(){
        this.startConnection();
        System.out.println("Thread " + this.threadid + " start!");
        //NOTE: Switching to using nanoTime and parkNanos for time management here such that the measurements
        // and the client thread have the same view on time.

        //spread the thread operations out so they don't all hit the DB at the same time
        // GH issue 4 - throws exception if _target>1 because random.nextInt argument must be >0
        // and the sleep() doesn't make sense for granularities < 1 ms anyway
        if ((targetOpsPerMs > 0) && (targetOpsPerMs <= 1.0)) {
//            System.out.println("targetOpsTickNS:" + (int)targetOpsTickNs);
            long randomMinorDelay = ThreadLocalRandom.current().nextLong(targetOpsTickNs);
            sleepUntil(System.nanoTime() + randomMinorDelay);
        }

        int index = 0;
        Operation curOp;
        long startTimeNanos = System.nanoTime();
        for(int i = 0; i < this.opList.size(); i++){
            curOp = this.opList.get(i);
//            System.out.println("curOp:" + curOp.toString() + " thread:" + this.threadid);
            if(curOp.isRead()){
                Status readStat = this.read(curOp);
                if(readStat == Status.OK){
//                    output.println(printOp("ok", curOp, index++));
                    output.println(printOpJson(curOp, index++));
                }
                else if(readStat == Status.NOT_FOUND){
//                    output.println(printOp("ok", curOp, index++));
                    output.println(printOpJson(curOp, index++));
                }
                else{
                    System.out.println("Error when doing op:" + curOp.easyPrint());
                    System.out.println("status:" + readStat);
                }
            }
            else if(curOp.isWrite()){
                if(this.write(curOp) == Status.OK){
//                    output.println(printOp("ok", curOp, index++));
                    output.println(printOpJson(curOp, index++));
                }
                else{
                    System.out.println("Error when doing op:" + curOp.easyPrint());
                }
            }
            else{
                System.out.println("Error! Only support read and write for the moment!");
            }

            this.opsDone++; //记得在每个操作结束后为计数器+1
            throttleNanos(startTimeNanos);
        }


        output.close(); //结束运行之前记得把文件输出关闭
        this.closeConnection(); //关闭与数据库的连接
        System.out.println("Thread " + this.threadid + " ends!");
        completeLatch.countDown();
    }
}
