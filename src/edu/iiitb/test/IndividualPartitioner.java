package edu.iiitb.test;

import java.util.Random;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Partitioner;

public class IndividualPartitioner<LongArrayWritable, LongWritable> implements Partitioner<LongArrayWritable, LongWritable> {
	// Partitions randomly independent of the passed <K, V>
	Random rng;

	public void configure(JobConf arg0) {
		System.out.println("conf argo0 in MapR");

		rng = new Random(System.nanoTime());
	}

	public int getPartition(LongArrayWritable arg0, LongWritable arg1, int numReducers) {
		System.out.println("gePartition in MapR");

		return (Math.abs(rng.nextInt()) % numReducers);
	}		
}	
