###############################COMMANDS FOR PART A:#######################

hdfs dfs -put ./input/Wikipedia-EN-20120601_ARTICLES/user/protrigger99/input/

export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/hadoop/share/hadoop/common/hadoop-common-3.4.0.jar
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/hadoop/share/hadoop/mapreduce/hadoop-mapreduce-client-core-3.4.0.jar
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/hadoop/share/hadoop/hdfs/hadoop-hdfs-3.4.0.jar
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/hadoop/share/hadoop/common/hadoop-common-3.4.0.jar
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/hadoop/share/hadoop/mapreduce/hadoop-mapreduce-client-core-3.4.0.jar
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/hadoop/share/hadoop/hdfs/hadoop-hdfs-3.4.0.jar

./compile1.sh

hdfs dfs -rm -r /user/protrigger99/output

hadoop jar ./out/DocFreq.jar DocFreq input/Wikipedia-50-ARTICLES output
hadoop jar ./out/DocFreq.jar DocFreq input/Wikipedia-EN-20120601_ARTICLES output



hdfs dfs -rm -r /user/protrigger99/output

#################COMMANDS FOR PART B:##################



hdfs dfs -rm -r /user/protrigger99/output

./compile2.sh
hadoop jar ./TermFreq2.jar TermFreq2 input/Wikipedia-50-ARTICLES output input/stopwords.txt

hdfs dfs -rm -r /user/protrigger99/output
