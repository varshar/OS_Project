package edu.iiitb.ga;

import org.apache.hadoop.io.LongWritable;

public class Selection {


	// Tournament selection without replacement
	public static LongWritable[] tournament(int startIndex, int tournamentSize, long[] tournamentFitness, LongWritable[][] tournamentInd) {
	
		LongWritable[] tournamentWinner = null;
		long tournamentMaxFitness = -1;
		for(int j=0; j<tournamentSize; j++) {
			if(tournamentFitness[j] > tournamentMaxFitness) {
				tournamentMaxFitness = tournamentFitness[j];
				tournamentWinner = tournamentInd[j];
			}
		}
		return tournamentWinner;
	}

}
