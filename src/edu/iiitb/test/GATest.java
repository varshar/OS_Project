package edu.iiitb.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import edu.iiitb.utils.LongArrayWritable;



public class GATest extends Configured  implements Tool{

	public static String rootDir = "/home/varsha/";	
	private static final int LONG_BITS = 64;
	private static final int LONGS_PER_ARRAY = 12;
	public static void main(String[] argv) throws Exception {
		System.out.println("Main in CGA");
/* Move this to the library run method */
		int res = ToolRunner.run(new Configuration(), new GATest(), argv);
		System.exit(res);
	}



	@Override
	public int run(String[] arg0) throws Exception {
		System.out.println("run in MapR");

		if (arg0.length != 5) {
			System.err.println("Usage: GeneticMR <nMaps> <nReducers> <variables> <nIterations> <popTimesNlogN>");
			ToolRunner.printGenericCommandUsage(System.err);
			return -1;
		}

		int	nMaps = Integer.parseInt(arg0[0]);
		int	nReducers = Integer.parseInt(arg0[1]);
		int strLen = Integer.parseInt(arg0[2]);
		int iter = Integer.parseInt(arg0[3]);
		int pop = (int) Math.ceil(Integer.parseInt(arg0[4]) * strLen * Math.log(strLen) / Math.log(2));
		System.out.println("Number of Maps = " + nMaps);  
		launch(nMaps, nReducers, null, null, strLen, pop, iter);

		return 0;
	}
	//(4,4,null,null,1000,10,10,4,0)
//launch(nMaps, nReducers, null, null, strLen, pop, iter, t, it);
	//launch(nMaps, nReducers, null, null, strLen, pop, iter);
		void launch(int numMaps, int numReducers, String jt, String dfs, int strLen, int pop, int iter) {
			System.out.println("Launch in MapR");

			int LONGS_PER_ARRAY = (int) Math.ceil(strLen / LONG_BITS);
			int it=0;
			while(true) {
				JobConf jobConf = new JobConf(getConf(), GATest.class);

				//turn on speculative execution - yahoo tutorial  
				jobConf.setSpeculativeExecution(true);
				//how to split i/p files and read them
				//SeqFile i/p format - special type of binary files specific binary
				jobConf.setInputFormat(SequenceFileInputFormat.class);

				jobConf.setOutputKeyClass(LongArrayWritable.class);
				jobConf.setOutputValueClass(LongWritable.class);
				jobConf.setOutputFormat(SequenceFileOutputFormat.class);

				jobConf.set("ga.longsPerArray", LONGS_PER_ARRAY + "");

				jobConf.setNumMapTasks(numMaps);
				jobConf.setPartitionerClass(IndividualPartitioner.class);

				
				if (jt != null) { jobConf.set("mapred.job.tracker", jt); }
				if (dfs != null) { FileSystem.setDefaultUri(jobConf, dfs); }
				jobConf.setJobName("ga-mr-" + it);
				System.out.println("launching");

				Path tmpDir = new Path(rootDir + "GA");
				System.out.println("tmpdir created");
				Path inDir = new Path(tmpDir, "iter" + it);
				Path outDir = new Path(tmpDir, "iter" + (it + 1));
				FileInputFormat.setInputPaths(jobConf, inDir);
				FileOutputFormat.setOutputPath(jobConf, outDir);

				FileSystem fileSys = null;
				try {
					fileSys = FileSystem.get(jobConf);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				int populationPerMapper = pop/numMaps;
				jobConf.set("ga.populationPerMapper", populationPerMapper + "");

				if(it == 0) {
					// Initialization
/*					try {
						fileSys.delete(tmpDir, true);
					} catch(IOException ie) {
						System.out.println("Exception while deleting");
						ie.printStackTrace();
					}
					System.out.println("Deleting dir");
*/
					for(int i=0; i < numMaps; ++i) {
						Path file = new Path(inDir, "part-"+String.format("%05d", i));
						SequenceFile.Writer writer = null;
						try {
							writer = SequenceFile.createWriter(fileSys, jobConf, 
									file, LongArrayWritable.class, LongWritable.class, CompressionType.NONE);
						}catch(Exception e) {
							System.out.println("Exception while instantiating writer");
							e.printStackTrace();
						}

						// Generate dummy input					
						LongWritable[] individual = new LongWritable[1];
						individual[0] = new LongWritable(populationPerMapper);
						try{
							writer.append(new LongArrayWritable(individual), new LongWritable(populationPerMapper));
						}catch(Exception e) {
							System.out.println("Exception while appending to writer");
							e.printStackTrace();
						}

						try{
							writer.close();
						} catch(Exception e) {
							System.out.println("Exception while closing writer");
							e.printStackTrace();
						}
						System.out.println("Writing dummy input for Map #" + i);
					}
					jobConf.setMapperClass(InitialGAMapper.class);
					jobConf.setReducerClass(IdentityReducer.class);
					jobConf.setNumReduceTasks(0);
				} // End of if it == 0
				else {
					jobConf.setMapperClass(GAMapper.class);
					jobConf.setReducerClass(GAReducer.class);
					jobConf.setNumReduceTasks(numReducers);
					try {
						fileSys.delete(outDir, true);
						fileSys.delete(new Path(tmpDir, "global-map"), true);
					} catch(IOException ie) {
						System.out.println("Exception while deleting");
						ie.printStackTrace();
					}
				}
				
				System.out.println("Starting Job");
				long startTime = System.currentTimeMillis();

				try {
					JobClient.runJob(jobConf);
				} catch (IOException e) {
					System.out.println("Exception while running job");
					e.printStackTrace();
				}


				LongWritable max = new LongWritable();
				LongArrayWritable maxInd = new LongArrayWritable();
				LongWritable finalMax = new LongWritable(-1);
				LongArrayWritable finalInd = null;

				// At the end of job, find out the best individual
				if(it > 0) {
					Path global = new Path(tmpDir, "global-map");

					FileStatus[] fs = null;
					SequenceFile.Reader reader = null;
					try {
						fs = fileSys.listStatus(global);
					} catch (IOException e) {
						System.out.println("Exception while instantiating reader in find winner");
						e.printStackTrace();
					}

					for(int i=0; i<fs.length; i++) {
						Path inFile = fs[i].getPath();
						try {
							reader = new SequenceFile.Reader(fileSys, inFile,
									jobConf);
						} catch (IOException e) {
							System.out.println("Exception while instantiating reader");
							e.printStackTrace();
						}

						try {
							while(reader.next(maxInd, max)) {
								if(max.get() > finalMax.get()) {
									finalMax = max;
									finalInd = maxInd;
								}
							}
						} catch (IOException e) {
							System.out.println("Exception while reading from reader");
							e.printStackTrace();
						}
						try {
							reader.close();
						} catch (IOException e) {
							System.out.println("Exception while closing reader");
							e.printStackTrace();
						}
					}

					/*			System.out.println("The best individual is : (" + finalInd + " , " + finalMax.get() + ")");
					System.out.println("Job Finished in "+
							(System.currentTimeMillis() - startTime)/1000.0 + " seconds");
					 */
					System.out.println("GA:" + it + ":" + LONGS_PER_ARRAY * LONG_BITS + ":" + pop + ":" + finalMax.get() + ":" + (System.currentTimeMillis() - startTime));
					if(finalMax.get() >= LONGS_PER_ARRAY * LONG_BITS - 10)
						break;
				}
				it++;
			}
		}
				}
