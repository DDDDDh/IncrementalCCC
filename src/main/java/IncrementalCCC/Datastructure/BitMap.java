package IncrementalCCC.Datastructure;

import java.util.BitSet;

public class BitMap {

    private int AdjArray[];

   // public



    public static void main(String args[]){

        BitSet bit1 = new BitSet(20);
        BitSet bit2 = new BitSet(20);


        for(int i = 0; i < 10; i++){
            if(i%2 == 0){
                bit1.set(i,true);
            }
            else{
                bit2.set(i, true);
            }
        }
//        System.out.println("Bit1:");
//        for(int i = 0; i < bit1.size(); i++){
//            if(bit1.get(i)) {
//                System.out.println(i);
//            }
//        }

        System.out.println("Bit2:");
        for(int i = 0; i < bit2.size(); i++){
            if(bit2.get(i)) {
                System.out.println(i);
            }
        }

        System.out.println("Bit2 original:" + bit2.toString());

        int nextTrue = bit2.nextSetBit(0);
        while(nextTrue != -1){
            System.out.println("Get: " + nextTrue);
            nextTrue = bit2.nextSetBit(nextTrue+1);
        }
        for(int i = bit2.nextSetBit(0); i >= 0; i = bit2.nextSetBit(i+1)){
            System.out.println("Sample get:" + i);
        }

//        bit1.set(1);
//
//        BitSet bit3 = (BitSet) bit2.clone();
//        bit3.or(bit1);
//
//        System.out.println("Bit3:");
//        for(int i = 0; i < bit3.size(); i++){
//            if(bit3.get(i)) {
//                System.out.println(i);
//            }
//        }

    }

}
