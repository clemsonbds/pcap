package format;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
//import org.apache.hadoop.mapred.FixedLengthInputFormat;
import org.apache.hadoop.util.*;

//import org.apache.hadoop.mapreduce.lib.input.FixedLengthInputFormat;

import format.OverlapLengthInputFormat;

public class ReadWriteSplits extends Configured implements Tool{

  public static class TokenizerMapper
       extends Mapper<LongWritable, BytesWritable, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    public void map(LongWritable key, BytesWritable value, Context context
                    ) throws IOException, InterruptedException {
//      StringTokenizer itr = new StringTokenizer(value.toString());
        int byteCount = value.getLength();
        word.set(Integer.toString(byteCount));
        context.write(word, one);
/*      while (itr.hasMoreTokens()) {
        word.set(itr.nextToken());
        context.write(word, one);
      } */
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
    }
  }

  public static void main(String args[]) throws Exception {
    int res = ToolRunner.run(new ReadWriteSplits(), args);
    System.exit(res);
  }

  public int run(String[] args) throws Exception {
    Path inputPath = new Path(args[0]);
    Path outputPath = new Path(args[1]);

    Configuration conf = getConf();

    conf.setInt(OverlapLengthInputFormat.FIXED_RECORD_LENGTH, Integer.parseInt(args[2]));
    conf.setInt(OverlapLengthInputFormat.OVERLAP_LENGTH, Integer.parseInt(args[3]));

    Job job = new Job(conf, this.getClass().toString());

    FileInputFormat.setInputPaths(job, inputPath);
    FileOutputFormat.setOutputPath(job, outputPath);

    job.setJobName("Split Test");
    job.setJarByClass(ReadWriteSplits.class);

    job.setInputFormatClass(OverlapLengthInputFormat.class);
    
    job.setOutputFormatClass(TextOutputFormat.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(IntWritable.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);

    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);

    return job.waitForCompletion(true) ? 0 : 1;
  }



}
