package edu.iiitb.ga;

import java.util.Random;

import org.apache.hadoop.io.LongWritable;

import edu.iiitb.utils.*;

public class Crossover {

	
public static void uniformCrossover(Random rng, LongArrayWritable ind[],int LONGS_PER_ARRAY,int LONG_BITS) {
		//Perform uniform crossover
		System.out.println("crossover in MapR");

		LongWritable[] ind1 = ind[0].getArray();
		LongWritable[] ind2 = ind[1].getArray();
		LongWritable[] newInd1 = new LongWritable[LONGS_PER_ARRAY];
		LongWritable[] newInd2 = new LongWritable[LONGS_PER_ARRAY];
		//			System.err.print("[GA] Crossing over " + ind[0] + " + " + ind[1]);

		for(int i=0; i<LONGS_PER_ARRAY; i++) {
			long i1 = 0, i2 = 0, mask = 1;
			for(int j=0; j<LONG_BITS; j++) {
				if(rng.nextDouble() > 0.5) {
					i2 |= ind2[i].get() & mask;
					i1 |= ind1[i].get() & mask;
				} else {
					i1 |= ind2[i].get() & mask;
					i2 |= ind1[i].get() & mask;
				}
				mask = mask << 1;
			}
			newInd1[i] = new LongWritable(i1);
			newInd2[i] = new LongWritable(i2);
		}

		ind[0] = new LongArrayWritable(newInd1);
		ind[1] = new LongArrayWritable(newInd2);
		//			System.err.println("[GA] Got " + ind[0] + " + " + ind[1]);
	}

}
