package com.netease.weblogOffline.statistics.tokaola;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.slf4j.LoggerFactory;

import com.netease.jurassic.hadoopjob.MRJob;
import com.netease.jurassic.hadoopjob.data.JobConstant;
import com.netease.weblogCommon.data.enums.NeteaseChannel_CS;
import com.netease.weblogCommon.logparsers.LogParser;
import com.netease.weblogCommon.utils.TextUtils;
import com.netease.weblogOffline.common.DirConstant;
import com.netease.weblogOffline.common.weblogfilter.WeblogFilterUtils;
//import com.netease.weblogOffline.common.logparsers.WeblogParser;
import com.netease.weblogOffline.data.StringTuple;
import com.netease.weblogOffline.utils.HadoopUtils;

/**
 *考拉回流URL统计
 * 
 */

public class InfoOfUrlMR extends MRJob {

	private static final org.slf4j.Logger log = LoggerFactory
			.getLogger(InfoOfUrlMR.class);

	@Override
	public boolean init(String date) {
		// weblog输入目录
		//inputList.add(DirConstant.WEBLOG_LOG + date);
		inputList.add(DirConstant.WEBLOG_FilterLOG
				+ date);
	//	inputList.add(DirConstant.WEBLOG_LOG + "test");
		// navlog输入目录
		inputList.add("/ntes_weblog/nav/navLog/nav/" + date);
		// 输出列表
		outputList.add(DirConstant.WEBLOG_STATISTICS_DIR+"result_other/kaola/" + date);
		return true;
	}

	@Override
	public int run(String[] args) throws Exception {
		int jobState = JobConstant.SUCCESSFUL;

		Job job = HadoopUtils
				.getJob(this.getClass(), this.getClass().getName());

		Configuration conf = job.getConfiguration();

		// weblog
		MultipleInputs.addInputPath(job, new Path(inputList.get(0)),
				SequenceFileInputFormat.class, WeblogMapper.class);

		// navlog
		MultipleInputs.addInputPath(job, new Path(inputList.get(1)),TextInputFormat.class, NavlogMapper.class);

		// mapperoutput
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);

		// reducer
		job.setReducerClass(InfoOfUrlReducer.class);
		job.setNumReduceTasks(8);
		FileOutputFormat.setOutputPath(job, new Path(outputList.get(0)));
		job.setOutputFormatClass(SequenceFileOutputFormat.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(StringTuple.class);

		if (!job.waitForCompletion(true)) {
			jobState = JobConstant.FAILED;
		}

		return jobState;
	}

	public static class WeblogMapper extends
			Mapper<NullWritable, Text, Text, Text> {

//		private static final LogParser logParser = new WeblogParser();

		@Override
		public void map(NullWritable key, Text value, Context context)
				throws IOException, InterruptedException {

//			Map<String, String> logMap = new HashMap<String, String>();

			try {
				
				HashMap<String,String> lineMap = WeblogFilterUtils.buildKVMap(value.toString());
  
                String cdata_href = lineMap.get("cdata_href");
                String event = lineMap.get("event");
                String url = TextUtils.quickUnescape(lineMap.get("url"));
                String channel = NeteaseChannel_CS.getChannelName(url);
 
                
				if (url.indexOf("own=")!=-1&&cdata_href.indexOf("www.kaola.com")!=-1&&event.equals("click")) {
	
						String[] str = url.split("\\?");
						

						String[] urlParams = str[1].split("#")[0].split("&");
						Map<String, String> result = new HashMap<String, String>();
						
						for (String s : urlParams){
						
								int first = s.indexOf("=");
								if (first != -1){
									result.put(s.substring(0, first).trim().toLowerCase(), TextUtils.notNullStr(s.substring(first + 1, s.length())));
								}
						
						}
						
						StringBuilder sb  = new StringBuilder();
						sb.append(str[0]).append(",")
						.append(result.get("own")).append(",")
						.append(result.get("channel")).append(",")
						.append(result.get("page")).append(",")
						.append(result.get("screen")).append(",")
						.append(result.get("place")).append(",").append(channel);
						
						context.write(new Text(sb.toString()),new Text(url));
             		}

			} catch (Exception e) {
				context.getCounter("WeblogMapper", "parseError")
						.increment(1);
			}
		}

//		private String stringProcess(String s) {
//			return (StringUtils.isBlank(s) ? "(null)" : s
//					.trim());
//		}

	}

	public static class NavlogMapper extends
			Mapper<LongWritable, Text, Text, Text> {

		private static final LogParser logParser = new NavlogParser();

		@Override
		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {

			Map<String, String> logMap = new HashMap<String, String>();

			try {
				logMap = logParser.parse(value.toString());
                String url = stringProcess(logMap.get("url"));
				
				String channel = stringProcess(NeteaseChannel_CS
						.getChannelName(url));
				
				String cdata_href = stringProcess(logMap.get("cdata_href"));
				String event = stringProcess(logMap.get("event"));
                
				if (url.indexOf("own=")!=-1&&cdata_href.indexOf("www.kaola.com")!=-1&&event.equals("click")) {

						String[] str = url.split("\\?");
	
						String[] urlParams = str[1].split("#")[0].split("&");
						Map<String, String> result = new HashMap<String, String>();
						
						for (String s : urlParams){
							
								int first = s.indexOf("=");
								if (first != -1){
									result.put(s.substring(0, first).trim(), TextUtils.notNullStr(s.substring(first + 1, s.length())));
								}
							
						}
						
						StringBuilder sb  = new StringBuilder();
						sb.append(str[0]).append(",")
						.append(result.get("own")).append(",")
						.append(result.get("channel")).append(",")
						.append(result.get("page")).append(",")
						.append(result.get("screen")).append(",")
						.append(result.get("place")).append(",").append(channel);
						
						 context.write(new Text(sb.toString()), new Text(url));
					
				}

			} catch (Exception e) {
				context.getCounter("NavlogMapper", "parseError")
						.increment(1);
			}
		}

		private String stringProcess(String s) {
			return (StringUtils.isBlank(s) ? "(null)" : s
					.trim());
		}

	}

	public static class InfoOfUrlReducer extends
			Reducer<Text, Text, Text, StringTuple> {

		@Override
		protected void reduce(Text key, Iterable<Text> values,
				Context context) throws IOException, InterruptedException {
			StringTuple st = new StringTuple();
			int sum = 0 ;
			for (Text val :values){
			st.add(val.toString());
			 sum ++;	
			}
            StringBuilder sb = new StringBuilder();
            sb .append(key.toString()).append(",").append(sum);
			context.write(new Text(sb.toString()),st);
		}
	}

}
