import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Extract100 {
    public static void main(String[] args) {
        String inputFile = args[0]; // Path to input TSV file
        String outputFile = args[1]; // Path to output text file

        // Read the TSV file and store word-count pairs in a map
        Map<String, Integer> wordCountMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    String word = parts[0];

		try {
                    int count = Integer.parseInt(parts[1]);
wordCountMap.put(word, count);
		}
catch (Exception e) {
	break;
}
                    
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create a priority queue to store word-count pairs
        PriorityQueue<Map.Entry<String, Integer>> maxHeap = new PriorityQueue<>((a, b) -> b.getValue() - a.getValue());
        maxHeap.addAll(wordCountMap.entrySet());

        // Write the top 50 words to the output file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            int count = 0;
            while (!maxHeap.isEmpty() && count < 100) {
                Map.Entry<String, Integer> entry = maxHeap.poll();
                writer.write(entry.getKey() + "\t" + entry.getValue() + "\n");
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Top 100 words written to " + outputFile);
    }
}