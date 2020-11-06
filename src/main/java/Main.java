import Checker.CCChecker;
import Checker.CCvChecker;
import Checker.CMChecker;
import History.History;
import History.HistoryReader;
import HistoryProducer.CCProducer;
import Relations.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Main {



    public static void main(String args[]) throws Exception{

        String stressTestOut = "src/main/resources/RandomHistories/StressTestLogfile.txt";
        File outfile = new File(stressTestOut);
        PrintWriter output = new PrintWriter(outfile);
        output.println("-----Begin Stress Test-----");
        boolean ccResult;
        boolean ccvResult;
        boolean cmResult;
        int countCC = 0;
        int countCCv = 0;
        int countCM = 0;

        for(int i = 0; i < 1000; i++) {
            output.println("No. " + i);
            CCProducer ccProducer = new CCProducer(100, 5, 3, 1);
            ccProducer.generatePath();
            ccProducer.generateCCHistory();
            ccProducer.printToFile();
            ccProducer.printToFileDebug();

            String url = ccProducer.getOutputPath();
            HistoryReader reader = new HistoryReader(url);

            History history = new History(reader.readHistory());
            history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...

            if(!history.isDifferentiated()){
                System.out.println("Detected not differentiated.");
                output.println("Detected not differentiated.");
                continue;
            }

            ProgramOrder po = new ProgramOrder(history.getOpNum());
            po.calculateProgramOrder(history);

            ReadFrom rf = new ReadFrom(history.getOpNum());
            rf.calculateReadFrom(history);

            BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
            bco.computeCO(history, po, rf);
            CCChecker ccChecker = new CCChecker(history, po, rf, bco);
            ccResult = ccChecker.checkCC();
            output.println("Chekcing CC, result:" + ccResult);
            if(ccResult){
                countCC++;
            }
            else{
                if(ccChecker.isCyclicCO()){
                    output.println("CyclicCO detected!");
                }
                else if(ccChecker.isThinAirRead()){
                    output.println("ThinAirRead detected!");
                }
                else if (ccChecker.isWriteCOInitRead()){
                    output.println("WriteCOInitRead detected!");
                }
                else if (ccChecker.isWriteCORead()){
                    output.println("WriteCORead detected!");
                }
                output.close();
                System.exit(-1);
            }

            ConflictRelation cf = new ConflictRelation(history.getOpNum());
            cf.caculateConflictRelation(history, bco);
            CCvChecker ccvChecker = new CCvChecker(history, po, rf, bco, cf);
            ccvResult = ccvChecker.checkCCv();
            output.println("Chekcing CCv, result:" + ccvResult);
            if(ccvResult){
                countCCv++;
            }

            BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
            hbo.calculateHBo(history, po, rf, bco);
            CMChecker cmChecker = new CMChecker(history, po, rf, bco, hbo);
            cmResult = cmChecker.checkCM();
            output.println("Checking CM, result:" + cmResult);
            if(cmResult){
                countCM++;
            }

        }

        output.println("-----End Stress Test-----");

        output.println("CC: " + countCC + "CCv: " + countCCv + "CM: " + countCM);

        output.close();

    }

}
