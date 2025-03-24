if [ ! -d "opennlp_classes" ]; then
  mkdir opennlp_classes
  cd opennlp_classes
  jar -xf ../lib/opennlp-tools-1.9.3.jar
  cd ..
fi

javac -cp "$(hadoop classpath):./lib/opennlp-tools-1.9.3.jar" -d doc_frequency_classes src/DocFreq.java

jar -cvf DocFreq.jar -C doc_frequency_classes . -C opennlp_classes .