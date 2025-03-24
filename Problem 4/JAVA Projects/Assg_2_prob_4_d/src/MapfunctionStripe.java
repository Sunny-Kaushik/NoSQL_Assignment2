import org.apache.hadoop.mapreduce.Job;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.io.Text;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;



public class MapfunctionStripe {
	
	public static class StripeMapper extends Mapper<Object, Text, Text, Stripe> {
		private Text word = new Text();
		
		private boolean caseSensitive = false;
		private Set<String> patternsToSkip = new HashSet<String>();
		private int d;
		private Set<String> frequentWords = new TreeSet<String>();
		private Map<String, Integer> wordIndex = new HashMap<String, Integer>();
		
		@Override
		public void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
			caseSensitive = conf.getBoolean("cooc.case.sensitive", false);
			d = conf.getInt("cooc.d", 1);
			
			URI[] cacheURIs = Job.getInstance(conf).getCacheFiles();
			
			Path frequentsPath = new Path(cacheURIs[0].getPath());
			String frequentsFileName = frequentsPath.getName().toString();
			parseFrequentsFile(frequentsFileName);
			
			if (conf.getBoolean("cooc.skip.patterns", false)) {
					Path patternsPath = new Path(cacheURIs[1].getPath());
					String patternsFileName = patternsPath.getName().toString();
					parseSkipFile(patternsFileName);
			}
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
		
		private void parseFrequentsFile(String fileName) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String line = null;
				while ((line = reader.readLine()) != null) {
					String word = line.split("\t")[0];
					frequentWords.add(word);
				}
				reader.close();
			} catch (IOException ioe) {
				System.err.println(
						"Caught exception while parsing the cached file '" + StringUtils.stringifyException(ioe));
			}
			int k = 0;
			for (String word : frequentWords) {
				wordIndex.put(word, k);
				k++;
			}
		}
		
		@Override
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String line = (caseSensitive) ? value.toString() : value.toString().toLowerCase();
			Map<String, Stripe> localAggregate = new HashMap<String, Stripe>();
			for (String pattern : patternsToSkip) {
				line = line.replaceAll(pattern, "");
			}
			String[] tokens = line.split("[^\\w']+");
			Set<Integer> frequentIndices = new LinkedHashSet<Integer>();
			for (int i=0; i < tokens.length; i++) {
				if (frequentWords.contains(tokens[i])) {
					frequentIndices.add(i);
				}
			}
			for (Integer idx: frequentIndices) {
				int j = 1;
				while(j <= d) {
					if (frequentIndices.contains(idx+j)) {
						String word1 = tokens[idx];
						String word2 = tokens[idx+j];
						if (localAggregate.get(word1) != null) {
							Stripe strip = localAggregate.get(word1);
							strip.addOne(wordIndex.get(word2));
							localAggregate.put(word1, strip);
						}
						else {
							Stripe strip = new Stripe();
							strip.setOne(wordIndex.get(word2));
							localAggregate.put(word1, strip);
						}
					}
					j++;
				}
			}
			for(Map.Entry<String, Stripe> entry: localAggregate.entrySet()) {
				word.set(entry.getKey());
				context.write(word, entry.getValue());
			}			
		}
		
	}
	
	public static class StripeReducer extends Reducer<Text, Stripe, Text, Stripe> {
		

		public void reduce(Text key, Iterable<Stripe> values, Context context)
				throws IOException, InterruptedException {
			Stripe result = new Stripe();
			for (Stripe strip : values) {
				result.add(strip);
			}
			context.write(key, result);
		}
	}
	
	public static void main(String[] args) throws Exception {

		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "pairs");

		job.setMapperClass(StripeMapper.class);
		job.setReducerClass(StripeReducer.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Stripe.class);
		
		job.addCacheFile(new Path(args[2]).toUri());

		for (int i = 0; i < args.length; ++i) {
			if ("-skippatterns".equals(args[i])) {
				job.getConfiguration().setBoolean("cooc.skip.patterns", true);
				job.addCacheFile(new Path(args[++i]).toUri());
			} else if ("-casesensitive".equals(args[i])) {
				job.getConfiguration().setBoolean("cooc.case.sensitive", true);
			} else if ("-d".equals(args[i])) {
				job.getConfiguration().setInt("cooc.d", Integer.parseInt(args[++i]));
			}
		}

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	
	
	
}