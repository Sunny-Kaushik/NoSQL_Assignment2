if [ ! -d "opennlp_classes" ]; then
  mkdir opennlp_classes
  cd opennlp_classes
  jar -xf ../lib/opennlp-tools-1.9.3.jar
  cd ..
fi

javac -cp "$(hadoop classpath):./lib/opennlp-tools-1.9.3.jar" -d term_frequency_classes src/TermFreq2.java

jar -cvf TermFreq2.jar -C term_frequency_classes . -C opennlp_classes .