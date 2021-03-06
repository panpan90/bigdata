package com.netease.weblogOffline.statistics.changechannel;


import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.slf4j.LoggerFactory;

import com.netease.jurassic.hadoopjob.MRJob;
import com.netease.jurassic.hadoopjob.data.JobConstant;
import com.netease.weblogCommon.data.enums.NeteaseChannel_CS;
import com.netease.weblogCommon.data.enums.NeteaseContentType;
import com.netease.weblogOffline.common.DirConstant;
import com.netease.weblogOffline.common.weblogfilter.WeblogFilterUtils;
import com.netease.weblogOffline.data.ThreeStringWritable;
import com.netease.weblogOffline.utils.HadoopUtils;



/**
 * ChannelBroadurl  pv
 *        
 */

public class ChannelBroadurlPvMR extends MRJob {
	
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ChannelBroadurlPvMR.class);

	@Override
	public boolean init(String date) {

		inputList.add(DirConstant.WEBLOG_FilterLOG
				+ date);
        //输出列表
        outputList.add(DirConstant.WEBLOG_STATISTICS_TEMP_DIR +"ChannelBroadUrlPv/" + date);
 

		return true;
	}

    
    @Override
    public int run(String[] args) throws Exception {
    	int jobState = JobConstant.SUCCESSFUL;
        
    	Job job = HadoopUtils.getJob(this.getClass(), this.getClass().getName());


    	MultipleInputs.addInputPath(job, new Path(inputList.get(0)), SequenceFileInputFormat.class, LogMapper.class);
        
        //mapper
        job.setMapOutputKeyClass(ThreeStringWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        
        job.setCombinerClass(PvReducer.class);
        
        //reducer
        job.setReducerClass(PvReducer.class);
        job.setNumReduceTasks(16);
        FileOutputFormat.setOutputPath(job, new Path(outputList.get(0)));
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        
        job.setOutputKeyClass(ThreeStringWritable.class);
        job.setOutputValueClass(IntWritable.class);
        
        if(!job.waitForCompletion(true)){
            jobState = JobConstant.FAILED;
        }
        
        
        return jobState;
    }
    
    public static class LogMapper extends Mapper<NullWritable, Text, ThreeStringWritable, IntWritable> {
    	

    	
    	private IntWritable outputValue = new IntWritable(1);

        @Override
        public void map(NullWritable key, Text value, Context context) throws IOException, InterruptedException {



        	try {
        		HashMap<String,String> lineMap = WeblogFilterUtils.buildKVMap(value.toString());
 
        		String url =lineMap.get("url");
        		String event = lineMap.get("event");
        		
				NeteaseChannel_CS nce = NeteaseChannel_CS.getChannel(url);
				url = url.replace("_", "");
				if(event.equals("launch")&&nce != null&&NeteaseContentType.artical.match(url)){
					String str[] = lineMap.get("project").split("@version@");
			
					if (str.length==2){
						String	broad = str[1];
						if(broad.indexOf("_")==-1){
						String channelName = nce.getName();
		
						ThreeStringWritable cbu = new ThreeStringWritable(channelName,broad,url);
						context.write(cbu, outputValue);
						}
					}
				}
	
			} catch (Exception e) {
				context.getCounter("LogMapper", "mapException").increment(1);
			}
        }
       
    }
    
    public static class PvReducer extends Reducer<ThreeStringWritable, IntWritable, ThreeStringWritable, IntWritable> {
    	
    	private IntWritable outputValue = new IntWritable();
    	
        @Override
        protected void reduce(ThreeStringWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        	int sum = 0;
        	
        	for(IntWritable val : values){
        		sum += val.get();
        	}
        	
        	outputValue.set(sum);
			context.write(key, outputValue);
        }
    }
      
}


