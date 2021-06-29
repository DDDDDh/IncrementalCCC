package DBConnector;
import com.mongodb.*;
import com.mongodb.client.*;
import org.bson.Document;
import History.*;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

public class MongoConnector {

    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    MongoCollection<Document> mongoCollection;


    public static String printOp(String type, Operation op, int index) {
        if (op.getValue() == -1) {
            return "{:type :" + type + ", :f :" + op.getType() + ", :value [" + op.getKey() + " " + "nil" + "], :process " + op.getProcess() + ", :time " + System.nanoTime() + ", :position " + op.getPosition() + ", :index " + index + "}";
        }
        else{
            return "{:type :" + type + ", :f :" + op.getType() + ", :value [" + op.getKey() + " " + op.getValue() + "], :process " + op.getProcess() + ", :time " + System.nanoTime() + ", :position " + op.getPosition() + ", :index " + index + "}";
        }
    }

    public void startConnection(String mongoSRV, String dbName, String collectionName){

        mongoClient = MongoClients.create(mongoSRV);
//        List<ServerAddress> addresses = new ArrayList<>();
//        ServerAddress serverAddress = new ServerAddress("114.212.82.99", 26000);
//        addresses.add(serverAddress);
//
////        MongoCredential mongoCredential = MongoCredential.createScramSha1Credential("dh","test_mongo","dh".toCharArray());
//        MongoCredential mongoCredential = MongoCredential.createScramSha256Credential("dh","test_mongo","dh".toCharArray());
//
////        ConnectionString connectionString = new ConnectionString("mongodb://host1:27107,host2:27017/?ssl=true");
////        CommandListener myCommandListener = ...;
////        MongoClientSettings settings = MongoClientSettings.builder()
////                .addCommandListener(myCommandListener)
////                .applyConnectionString(connectionString)
////                .build();
////
////        MongoClient mongoClient = MongoClients.create(settings);
////        MongoClient mongoClient = MongoClients.create("mongodb://user1:pwd1@host1/?authSource=db1&authMechanism=SCRAM-SHA-1");
////
//        mongoClient = MongoClients.create(
//          MongoClientSettings.builder()
//                  .applyToClusterSettings(builder ->
//                    builder.hosts(addresses))
//                  .credential(mongoCredential)
//                  .build());

        mongoDatabase = mongoClient.getDatabase(dbName);

        mongoCollection = mongoDatabase.getCollection(collectionName);
    }

    public void clearDB(){

        Document tempDocument = new Document();
        long docCount = mongoCollection.countDocuments();
//        System.out.println("number of documents before clear:" + docCount);
        mongoCollection.deleteMany(tempDocument);

//        MongoCursor<Document> cursor = mongoCollection.find().iterator();
//        while(cursor.hasNext()) {
//            mongoCollection.deleteOne(cursor.next());
//        }


        docCount = mongoCollection.countDocuments();
//        System.out.println("number of documents after clear:" + docCount);

    }

    public void closeConnection(){
        mongoClient.close();
    }

    public void mongoRun(String mongoSRV, String dbName, String collectionName, String historyPath, String logPath) throws Exception{

        startConnection(mongoSRV, dbName, collectionName);
        //每次运行之前将数据库清零
        clearDB();

        File outfile = new File(logPath);
        PrintWriter output = new PrintWriter(outfile);

        Document actual;

        //读取historyPath中的文件,按照配置连接到mongodb,模拟运行后输出结果到logPath
        HistoryReader reader = new HistoryReader(historyPath);
        LinkedList<Operation> opList = reader.readHistory();
        Operation curOp;
        Document tempDoc;
        Document queryDoc;
        int index = 0;

        for(int i = 0; i < opList.size(); i++){
            curOp = opList.get(i);
//            System.out.println(curOp.easyPrint());
            tempDoc = new Document("Field", curOp.getKey());
            queryDoc = new Document("Field", curOp.getKey());
            tempDoc.put("Value",curOp.getValue());
            String type = "";
            if(curOp.isWrite()){
                UpdateOptions options = new UpdateOptions();
                options.upsert(true);
                UpdateResult result = mongoCollection.withWriteConcern(WriteConcern.W1).updateOne(queryDoc, new Document("$set", tempDoc), options);
//                System.out.println("Successful write(" +curOp.getKey()+")"+curOp.getValue());
                type = "ok";

            }
            else if(curOp.isRead()){
                try {
                    actual = mongoCollection.withReadConcern(ReadConcern.LOCAL).find(queryDoc).limit(1).iterator().next();
                }catch (NoSuchElementException e){
                    actual = null;
                    type = "invoke";
                }
                if(actual!=null){
                    type = "ok";
                    curOp.setValue((int)actual.get("Value"));
//                    System.out.println("Read("+curOp.getKey()+")"+actual.get("Value"));
                }
                else{
//                    System.out.println("Read("+curOp.getKey()+")null;");
                    type = "ok"; //读到不存在的键，也设置为-1
                    curOp.setValue(-1);
                }
            }
            else{
                System.out.println("Error!");
            }
            output.println(printOp(type, curOp, index++));
        }

        output.close();
       closeConnection();
    }


    public static void main(String args[]) throws Exception{

        MongoConnector connector =  new MongoConnector();
//        connector.mongoRun("mongodb+srv://m220student:m220password@mflix.9pm5g.mongodb.net/test", "test_mongo", "original_data", "/Users/yi-huang/Project/IncrementalCCC/target/RandomHistories/CC/100/Running_202112111_opNum100_processNum10_rRate3_wRate1_50.edn","target/RandomHistories/mongoLogfile_0228.txt" );
        connector.startConnection("mongodb+srv://m220student:m220password@mflix.9pm5g.mongodb.net/test", "test_mongo", "original_data");
        connector.clearDB();
        connector.closeConnection();


    }


}
