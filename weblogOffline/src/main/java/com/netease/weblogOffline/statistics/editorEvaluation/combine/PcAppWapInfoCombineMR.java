package com.netease.weblogOffline.statistics.editorEvaluation.combine;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import com.netease.jurassic.hadoopjob.MRJob;
import com.netease.jurassic.hadoopjob.annotation.NotCheckInputFile;
import com.netease.jurassic.hadoopjob.data.JobConstant;
import com.netease.weblogOffline.common.DirConstant;
import com.netease.weblogOffline.data.MultiStatisticResultWrapWritable;
import com.netease.weblogOffline.data.StatisticResultWritable;
import com.netease.weblogOffline.utils.HadoopUtils;
@NotCheckInputFile
public class PcAppWapInfoCombineMR extends MRJob {

	@Override
	public boolean init(String date) {
 		inputList.add(DirConstant.WEBLOG_STATISTICS_EDITOR_EVALUATION+"pcInfoCombine/"+date);
 		inputList.add(DirConstant.WEBLOG_STATISTICS_EDITOR_EVALUATION+"appInfoCombine/"+date);
		inputList.add(DirConstant.WEBLOG_STATISTICS_EDITOR_EVALUATION+"wapInfoAndShareCombine/"+date);
		outputList.add(DirConstant.WEBLOG_STATISTICS_EDITOR_EVALUATION+"pcAppWapInfoCombine/"+date);
		return true;
	}

	@Override
	public int run(String[] args) throws Exception {
		int jobState = JobConstant.SUCCESSFUL;
		
		Job job = HadoopUtils.getJob(this.getClass(), this.getClass().getName() + "_step1");
		boolean b =false;
		
		b|=addInputPath(job, new Path(inputList.get(0)), SequenceFileInputFormat.class, InfoCombineMapper.class);
		b|=addInputPath(job, new Path(inputList.get(1)), SequenceFileInputFormat.class, InfoCombineMapper.class); 
		b|=addInputPath(job, new Path(inputList.get(2)), SequenceFileInputFormat.class, InfoCombineMapper.class); 
		
		if (!b){
			return jobState;
		}

		// mapper
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(MultiStatisticResultWrapWritable.class);

		// reducer
		job.setReducerClass(InfoCombineReducer.class);
		job.setNumReduceTasks(16);
		FileOutputFormat.setOutputPath(job, new Path(outputList.get(0)));
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(MultiStatisticResultWrapWritable.class);
		
		if (!job.waitForCompletion(true)) {
			jobState = JobConstant.FAILED;
		}
		
		return jobState;
	}


	
	public static class InfoCombineMapper extends Mapper<Text, MultiStatisticResultWrapWritable, Text, MultiStatisticResultWrapWritable> {
		@Override
		public void map(Text key, MultiStatisticResultWrapWritable value, Context context) throws IOException, InterruptedException {
			context.write(key, value);//key: id,url
		}
	}
	
	
	public static class InfoCombineReducer extends Reducer<Text, MultiStatisticResultWrapWritable, Text, MultiStatisticResultWrapWritable> {
		private MultiStatisticResultWrapWritable  outValue = new MultiStatisticResultWrapWritable();
		@Override
		protected void reduce(Text key, Iterable<MultiStatisticResultWrapWritable> values, Context context) throws IOException, InterruptedException {
			outValue.getMsr().getDataMap().clear();
			outValue.getConf().clear();
			
			for (MultiStatisticResultWrapWritable val : values) {	
				for(Entry<Text, StatisticResultWritable> e:val.getMsr().getDataMap().entrySet()){
					outValue.getMsr().getDataMap().put(new Text(e.getKey()),new StatisticResultWritable(e.getValue()));
				}
				
				for(Entry<String, String> e:val.getConf().entrySet()){
					outValue.getConf().put(e.getKey(),e.getValue());
				}
			}
			
			if (outValue.getMsr().getDataMap().size() > 0){
				context.write(key, outValue);
			}		
		}
	}     
}
