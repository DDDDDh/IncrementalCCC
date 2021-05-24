package IncrementalCCC;

import IncrementalCCC.History.History;
import IncrementalCCC.History.HistoryReader;
import IncrementalCCC.Relations.*;

import java.util.HashMap;

public class HappenBeforeGenerator {

    public int maxIndex = 100000;

    public void setMaxIndex(int maxIndex) {
        this.maxIndex = maxIndex;
    }


    public HashMap<Integer, BasicRelation> getProcessMatrix(String url) throws Exception {
        HistoryReader reader = new HistoryReader(url);
        History history = new History(reader.readHistory(this.maxIndex));
        history.setOpNum(reader.getTotalNum()); //读取完一个历史记录之后，一定一定要记得设置总操作数...

        if (!history.isDifferentiated()) {
            System.out.println("Detected not differentiated.");
            return null;
        }

        ProgramOrder po = new ProgramOrder(history.getOpNum());
        System.out.println("Calculating Program Order");
        po.calculateProgramOrder(history);

        ReadFrom rf = new ReadFrom(history.getOpNum());
        System.out.println("Calculating Read From");
        rf.calculateReadFrom(history);

        BasicCausalOrder bco = new BasicCausalOrder(history.getOpNum());
        System.out.println("Calculating Causal Order");
        bco.computeCO(history, po, rf);

        BasicHappenBeforeOrder hbo = new BasicHappenBeforeOrder(history.getOpNum());
        System.out.println("Calculating Happened Before");
        hbo.calculateHBo(history, po, rf, bco);

//        for (Integer curProcess : hbo.processMatrix.keySet()) {
//            BasicRelation curMatrix = hbo.processMatrix.get(curProcess);
//            System.out.println("Process " + curProcess);
//            curMatrix.printMatrix();
//        }
        return hbo.processMatrix;
    }

    public static void main(String[] args) throws Exception {
        HappenBeforeGenerator hboGenerator = new HappenBeforeGenerator();
        hboGenerator.setMaxIndex(200);
//        String url = "D:\\Education\\Programs\\Java\\Causal-Memory-Checking-Java\\src\\main\\resources\\latest\\history_1w.edn";
//        String url = "D:\\Education\\Programs\\Java\\Causal-Memory-Checking-Java\\src\\main\\resources\\latest\\history_1w.edn";
        String url = "D:\\Education\\Programs\\Java\\Causal-Memory-Checking-Java\\src\\main\\resources\\adhoc\\paper_history_d.edn";
        HashMap<Integer, BasicRelation> processMatrix = hboGenerator.getProcessMatrix(url);

        for (Integer curProcess : processMatrix.keySet()) {
            BasicRelation curMatrix = processMatrix.get(curProcess);
            System.out.println("Process " + curProcess);
//            curMatrix.printMatrix();
//            curMatrix.printRelationMatrix();
        }
    }
}
