import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.StringUtils;

// import opennlp.tools.cmdline.postag.POSModelLoader;
// import opennlp.tools.postag.POSModel;
// import opennlp.tools.postag.POSSample;
// import opennlp.tools.postag.POSTaggerME;
// import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.stemmer.PorterStemmer;

public class DocFreq {

	public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();

		private boolean caseSensitive = false;
		private Set<String> patternsToSkip = new HashSet<String>();
		private Set<String> docFreqWords = new HashSet<String>();

		@Override
		public void setup(Context context) throws IOException, InterruptedException {
			// Configuration conf = context.getConfiguration();
			// caseSensitive = conf.getBoolean("docfreq.case.sensitive", false);
			// if (conf.getBoolean("docfreq.skip.patterns", false)) {
			// 	URI[] patternsURIs = Job.getInstance(conf).getCacheFiles();
			// 	for (URI patternsURI : patternsURIs) {
			// 		Path patternsPath = new Path(patternsURI.getPath());
			// 		String patternsFileName = patternsPath.getName().toString();
			// 		parseSkipFile(patternsFileName);
			// 	}
			// }
			parseSkipFile("./stopwords.txt");
		}

		private void parseSkipFile(String fileName) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String pattern = null;
				while ((pattern = reader.readLine()) != null) {
					patternsToSkip.add(pattern);
				}
				reader.close();
			} catch (IOException ioe) {
				System.err.println(
						"Caught exception while parsing the cached file '" + StringUtils.stringifyException(ioe));
			}
		}

		@Override
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

			PorterStemmer stemmer = new PorterStemmer();

			String line = (caseSensitive) ? value.toString() : value.toString().toLowerCase();

			for (String pattern : patternsToSkip) {
				line = line.replaceAll(pattern, "");
			}
			String[] tokens = line.split("[^\\w']+");
			// System.out.println("DEBUG: " + tokens); 

			for (String token : tokens) {
				String stemmed_token = stemmer.stem(token);
				docFreqWords.add(stemmed_token);
			}
		}

		@Override
		public void cleanup(Context context) throws IOException, InterruptedException {
			for (String token : docFreqWords) {
				word.set(token);
				context.write(word, one);
			}
		}
	}

	public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		private IntWritable result = new IntWritable();

		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			result.set(sum);
			context.write(key, result);
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "docfreq");
		job.setJarByClass(DocFreq.class);

		job.setMapperClass(TokenizerMapper.class);
		// job.setCombinerClass(IntSumReducer.class); // enable to use 'local
		// aggregation'
		job.setReducerClass(IntSumReducer.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		for (int i = 0; i < args.length; ++i) {
			if ("-skippatterns".equals(args[i])) {
				System.out.println("DEBUG: STOPWORDS SKIPPED");
				job.getConfiguration().setBoolean("docfreq.skip.patterns", true);
				job.addCacheFile(new Path(args[++i]).toUri());
			} else if ("-casesensitive".equals(args[i])) {
				job.getConfiguration().setBoolean("docfreq.case.sensitive", true);
			}
		}

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}