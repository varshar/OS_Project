package edu.iiitb.ga;

import org.apache.hadoop.io.LongWritable;

public class Individual {
	
	private static final int LONG_BITS = 64;	
	
	public static long fitness(LongWritable[] individual) {
		System.out.println("fitness long in MapR");

		long f=0;
		for(int i=0; i<individual.length; i++) {
			long mask = 1;
			for(int j=0; j<LONG_BITS; j++) {					
				f += ((individual[i].get() & mask) > 0)? 1 : 0;
				mask = mask << 1;
			}
		}
		//			System.err.println("Fitness of " + individual + " is " + f);
		return f;
	}

}
