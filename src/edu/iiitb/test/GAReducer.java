package edu.iiitb.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.iiitb.utils.*;
import edu.iiitb.ga.*;


public class GAReducer extends MapReduceBase
implements Reducer<LongArrayWritable, LongWritable, LongArrayWritable, LongWritable> {
	public static final int LONG_BITS = 64;
	int tournamentSize = 5;
	int LONGS_PER_ARRAY;

	LongWritable[][] tournamentInd;
	long[] tournamentFitness = new long[2*tournamentSize];

	int processedIndividuals=0;
	int r=0;
	LongArrayWritable[] ind = new LongArrayWritable[2];
	Random rng;
	int pop = 1;
	GAReducer() {
		rng = new Random(System.nanoTime());
	}
	@Override
	public void configure(JobConf jc) {		
		System.out.println("configure in MapR");

		LONGS_PER_ARRAY = Integer.parseInt(jc.get("ga.longsPerArray"));
		tournamentInd = new LongWritable[2*tournamentSize][LONGS_PER_ARRAY];
		pop = Integer.parseInt(jc.get("ga.populationPerMapper"));
	}

	OutputCollector<LongArrayWritable, LongWritable> _output;


	public void reduce(LongArrayWritable key, Iterator<LongWritable> values,
			OutputCollector<LongArrayWritable, LongWritable> output, Reporter rep)
	throws IOException {
		// Save the output collector for later use
		_output = output;
		System.out.println("reduce in MapR");

		while(values.hasNext()) {
			long fitness = values.next().get();
			tournamentInd[processedIndividuals%tournamentSize] = key.getArray();
			tournamentFitness[processedIndividuals%tournamentSize] = fitness;

			if ( processedIndividuals < tournamentSize ) {
				// Wait for individuals to join in the tournament and put them for the last round
				tournamentInd[processedIndividuals%tournamentSize + tournamentSize] = key.getArray();
				tournamentFitness[processedIndividuals%tournamentSize + tournamentSize] = fitness;
			} else {
				// Conduct a tournament over the past window
				ind[processedIndividuals%2] = new LongArrayWritable(Selection.tournament(processedIndividuals,tournamentSize,tournamentFitness,tournamentInd)); 

				if ((processedIndividuals - tournamentSize) %2 == 1) {					
					// Do crossover every odd iteration between successive individuals
					Crossover.uniformCrossover(rng,  ind,LONGS_PER_ARRAY, LONG_BITS);
					output.collect(ind[0], new LongWritable(0));
					output.collect(ind[1], new LongWritable(0));
				}
			}
			processedIndividuals++;
//			System.err.println(" " + processedIndividuals);
		}
		if(processedIndividuals == pop - 1) {
			closeAndWrite();
		}
	}

	public void closeAndWrite() {
		System.out.println("Closing reducer in MapR");
		// Cleanup for the last window of tournament
		for(int k=0; k<tournamentSize; k++) {
			// Conduct a tournament over the past window				
			ind[processedIndividuals%2] = new LongArrayWritable(Selection.tournament(processedIndividuals,tournamentSize,tournamentFitness,tournamentInd)); 

			if ((processedIndividuals - tournamentSize) %2 == 1) {					
				// Do crossover every odd iteration between successive individuals
				Crossover.uniformCrossover(rng,  ind,LONGS_PER_ARRAY, LONG_BITS);
				try {
					_output.collect(ind[0], new LongWritable(0));
					_output.collect(ind[1], new LongWritable(0));
				} catch (IOException e) {
					System.err.println("Exception in collector of reducer");
					e.printStackTrace();
				}
			}
			processedIndividuals++;
		}
	}


}
