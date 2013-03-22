package edu.iiitb.test;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import edu.iiitb.utils.*;




	public class InitialGAMapper extends MapReduceBase
	implements Mapper<LongArrayWritable,LongWritable, LongArrayWritable, LongWritable> {
		Random rng;		
		int LONGS_PER_ARRAY;
		LongWritable[] individual ;
		public static final int LONG_BITS = 64;
		
		Log log = LogFactory.getLog(InitialGAMapper.class);
		
		@Override
		public void configure(JobConf jc) {
			log.info("conf longs per array in MapR");

			LONGS_PER_ARRAY = Integer.parseInt(jc.get("ga.longsPerArray"));
			rng = new Random(System.nanoTime());
			individual = new LongWritable[LONGS_PER_ARRAY];
		}

		public InitialGAMapper() {
			System.out.println("initialCGAMapper in CGA");
			individual = new LongWritable[(12-1)*LONG_BITS + 1];
		 
		}
		
		public void map(LongArrayWritable key, LongWritable value, OutputCollector<LongArrayWritable, LongWritable> oc, Reporter rep) throws IOException {
			System.out.println("map long in MapR");

			for(int i=0; i<value.get(); i++) {
				// Generate initial individual
				for(int l=0; l<LONGS_PER_ARRAY; l++) {
					long ind = 0;
					for(int m=0; m < LONG_BITS; m++) {
						ind = ind | (rng.nextBoolean()? 0: 1);
						// Don't shift for the last bit
						if(m != LONG_BITS - 1)
							ind = ind << 1;						
					}
					individual[l] = new LongWritable(ind);
//					System.out.print(individual[l].get());
				}
				oc.collect(new LongArrayWritable(individual), new LongWritable(0));
			}
		}
	}
