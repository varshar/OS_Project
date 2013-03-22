package edu.iiitb.test;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import edu.iiitb.ga.Individual;
import edu.iiitb.utils.*;




public class GAMapper extends MapReduceBase
implements Mapper<LongArrayWritable,LongWritable, LongArrayWritable, LongWritable> {
	long max = -1;
	LongArrayWritable maxInd;
	private String mapTaskId = "";
	long fit = 0;
	JobConf conf;
	int pop = 1;
	public static String rootDir = "/home/varsha/";	
	@Override
	public void configure(JobConf job) {
		System.out.println("conf in MapR");

		conf = job;
		mapTaskId = job.get("mapred.task.id");
		pop = Integer.parseInt(job.get("ga.populationPerMapper"));
	}
	int processedInd = 0;
	@Override
	public void map(LongArrayWritable key, LongWritable value, OutputCollector<LongArrayWritable, LongWritable> oc, Reporter rep) throws IOException {
		// Compute the fitness for every individual
		System.out.println("compute fitness in MapR");

		LongWritable[] individual = key.getArray();
		fit = Individual.fitness(individual);
		// System.err.println(value + " : " + individual + " : " + fit);

		//Keep track of the maximum fitness
		if(fit > max) {
			max = fit;
			maxInd = new LongArrayWritable(individual);
		}
		oc.collect(key, new LongWritable(fit));
		processedInd++;
		if(processedInd == pop -1) {
			closeAndWrite();
		}
	}

	public void closeAndWrite() throws IOException {
		// At the end of Map(), write the best found individual to a file
		System.out.println("closeandWrite in MapR");

		Path tmpDir = new Path( rootDir + "GA");
		Path outDir = new Path(tmpDir, "global-map");

		// HDFS does not allow multiple mappers to write to the same file, hence create one for each mapper
		Path outFile = new Path(outDir, mapTaskId);
		FileSystem fileSys = FileSystem.get(conf);
		SequenceFile.Writer writer = SequenceFile.createWriter(fileSys, conf, 
				outFile, LongArrayWritable.class, LongWritable.class, 
				CompressionType.NONE);

		// System.err.println("Max ind = " + maxInd.toString() + " : " + max);
		writer.append(maxInd, new LongWritable(max));
		writer.close();
	}

}

