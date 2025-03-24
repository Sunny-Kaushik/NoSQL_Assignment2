import org.apache.hadoop.mapreduce.Job;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.io.Writable;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Math;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import opennlp.tools.stemmer.PorterStemmer;



class Strip implements Writable {

    private Integer[] data = new Integer[100];
    // private Map<String, Integer> data= new HashMap<String, Integer>();
    private Integer N = 100;

    public Strip() {
        Arrays.fill(data, 0);
    }

    public Strip(Integer[] data) {
        this.data = data;
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        for (int i = 0; i < N; i++) {
            data[i] = in.readInt();
        }
    }

    @Override
    public void write(DataOutput out) throws IOException {
        for (int i = 0; i < N; i++) {
            out.writeInt(data[i]);
        }

    }

    public void setOne(Integer idx) {
        data[idx] = 1;
    }

    public void set(Integer idx, Integer val) {
        data[idx] = val;
    }

    public Integer get(Integer idx) {
        return data[idx];
    }

    public void add(Strip other) {
        for (int i = 0; i < N; i++) {
            data[i] = data[i] + other.get(i);
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(data);
    }

    public void addOne(Integer idx) {
        data[idx] += 1;
    }

}

class FileOpener {
    private Map<String, Integer> wordToIndex = new HashMap<String, Integer>();
    private Map<Integer, String> indexToWord = new HashMap<Integer, String>();
    private Map<Integer, Integer> frequentWordsWithCount = new HashMap<Integer, Integer>();
    private Integer id = 0;

    public FileOpener(String fileName) {
        readFrequentsFile(fileName);
    }

    public static BufferedReader open(String filename) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
        } catch (IOException ioe) {
            System.err.println("Caught exception while opening the file '" + StringUtils.stringifyException(ioe));
        }
        return reader;
    }

    public void readFrequentsFile(String fileName) {
        try {
            BufferedReader reader = open(fileName);
            String line = null;

            while ((line = reader.readLine()) != null) {
                String word = line.split("\t")[0];
                // System.out.println("DEBUG:    " + word);
                String count = line.split("\t")[1];
                // System.out.println("DEBUG:    " + count);
                System.out.println("");
                frequentWordsWithCount.put(id, Integer.parseInt(count));
                wordToIndex.put(word, id);
                indexToWord.put(id, word);
                id++;
            }
            close(reader);
        } catch (IOException ioe) {
            System.err
                    .println("Caught exception while parsing the cached file '" + StringUtils.stringifyException(ioe));
        }
    }

    public void close(BufferedReader reader) {
        try {
            reader.close();
        } catch (IOException ioe) {
            System.err.println("Caught exception while closing the file '" + StringUtils.stringifyException(ioe));
        }

    }

    public Integer getWordToIndex(String word) {
        return wordToIndex.get(word);
    }

    public String getIndexToWord(Integer index) {
        return indexToWord.get(index);
    }

    public Integer getDFCountFromString(String word) {
        return frequentWordsWithCount.get(wordToIndex.get(word));
    }

    public Integer getDFCountFromIndex(Integer index) {
        return frequentWordsWithCount.get(index);
    }

    public Map<Integer, Integer> getFrequentWordsWithCount() {
        return frequentWordsWithCount;
    }

}

public class TermFreq2 {

    public static class StripMapper extends Mapper<Object, Text, Text, Strip> {
        private Text word = new Text();

        private boolean caseSensitive = false;
        private Set<String> patternsToSkip = new HashSet<String>();

        private Map<Integer, Integer> frequentWordsWithCount = new HashMap<Integer, Integer>();
        private Map<Integer, Integer> TFFreq = new HashMap<Integer, Integer>();
        private String frequentsFileName;
        private String docFileName;

        private FileOpener topFile = null;

        private Map<String, Strip> localAggregate = new HashMap<String, Strip>();

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            caseSensitive = conf.getBoolean("tfidf.case.sensitive", false);
            topFile = new FileOpener("./input/partbin.txt");
            frequentWordsWithCount = topFile.getFrequentWordsWithCount();
            docFileName = ((FileSplit) context.getInputSplit()).getPath().getName(); // get Document name
            if (conf.getBoolean("tfidf.skip.patterns", false)) {
                parseSkipFile("./stopwords.txt");
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

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            PorterStemmer stemmer = new PorterStemmer();


            String line = (caseSensitive) ? value.toString() : value.toString().toLowerCase();

            for (String pattern : patternsToSkip) {
                line = line.replaceAll(pattern, "");
            }
            String[] tokens = line.split("[^\\w']+");

            //

            for (String token : tokens) {
                String stemmed_token = stemmer.stem(token);
                Integer tokenInd = topFile.getWordToIndex(stemmed_token);
                if (frequentWordsWithCount.containsKey(tokenInd)) {
                    if (!TFFreq.containsKey(tokenInd)) {
                        TFFreq.put(tokenInd, 1);
                    } else {
                        TFFreq.put(tokenInd, TFFreq.get(tokenInd) + 1);
                    }
                }
            }

            Strip stripTF = new Strip();

            for (Integer tokenInd : TFFreq.keySet()) {
                stripTF.set(tokenInd, TFFreq.get(tokenInd));
            }

            word.set(docFileName);
            context.write(word, stripTF);

        }
    }

    public static class StripReducer extends Reducer<Text, Strip, Text, Text> {

        private FileOpener topFile = null;
        private String frequentsFileName;

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
            // Configuration conf = context.getConfiguration();
            // URI[] cacheURIs = Job.getInstance(conf).getCacheFiles();
            // Path frequentsPath = new Path(cacheURIs[0].getPath());
            // frequentsFileName = frequentsPath.getName().toString();
            topFile = new FileOpener("./input/partbin.txt");
        }

        public void reduce(Text key, Iterable<Strip> values, Context context)
                throws IOException, InterruptedException {
            Strip result = new Strip();
            for (Strip strip : values) {
                result.add(strip);
            }

            for (int i = 0; i < 100; i++) {
                Integer dfFreq = topFile.getDFCountFromIndex(i);
                Double idf = Math.log(10000.0 / (1 + dfFreq));
                Integer tf = result.get(i);
                Double tfidf = tf.doubleValue() * idf;
                Text outVal = new Text(topFile.getIndexToWord(i) + "\t" + tfidf.toString());
                context.write(key, outVal);
            }

        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "tfidf");
        job.setJarByClass(TermFreq2.class);

        job.setMapperClass(StripMapper.class);
        job.setReducerClass(StripReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Strip.class);

        job.addCacheFile(new Path(args[2]).toUri());

        for (int i = 0; i < args.length; ++i) {
            if ("-skippatterns".equals(args[i])) {
                job.getConfiguration().setBoolean("tfidf.skip.patterns", true);
                job.addCacheFile(new Path(args[++i]).toUri());
            } else if ("-casesensitive".equals(args[i])) {
                job.getConfiguration().setBoolean("tfidf.case.sensitive", true);
            } else if ("-d".equals(args[i])) {
                job.getConfiguration().setInt("tfidf.d", Integer.parseInt(args[++i]));
            }
        }

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }

}
