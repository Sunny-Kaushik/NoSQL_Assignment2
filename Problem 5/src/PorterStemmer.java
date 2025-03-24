public class PorterStemmer 
{

    private StringBuilder wordBuffer;
    private int end, j, k;

    public PorterStemmer() {
        wordBuffer = new StringBuilder();
    }

    public static String stem(String word) {
        PorterStemmer stemmer = new PorterStemmer();
        stemmer.add(word.toCharArray(), word.length());
        stemmer.stem();
        return stemmer.toString();
    }

    public void add(char[] word, int length) {
        wordBuffer.setLength(0);
        wordBuffer.append(word, 0, length);
        end = wordBuffer.length() - 1;
    }

    @Override
    public String toString() {
        return wordBuffer.substring(0, end + 1);
    }

    public void stem() {
        if (wordBuffer.length() > 2) {
            step1();
            step2();
            step3();
            step4();
            step5();
        }
    }

    private boolean isConsonant(int i) {
        char ch = wordBuffer.charAt(i);
        return (ch != 'a' && ch != 'e' && ch != 'i' && ch != 'o' && ch != 'u' &&
                !(ch == 'y' && i > 0 && isConsonant(i - 1)));
    }

    private void step1() {
        if (wordBuffer.charAt(end) == 's') {
            if (wordBuffer.substring(end - 1, end + 1).equals("ss")) return;
            if (wordBuffer.substring(end - 2, end + 1).equals("ies")) {
                wordBuffer.replace(end - 2, end + 1, "i");
                end -= 2;
            } else if (wordBuffer.charAt(end - 1) != 's') {
                end--;
            }
        }
    }

    private void step2() {
        if (wordBuffer.charAt(end) == 'e') {
            if (end > 0 && wordBuffer.charAt(end - 1) == 'l' && isConsonant(end - 2)) {
                end--;
            }
        }
    }

    private void step3() {
        if (end > 0 && wordBuffer.charAt(end) == 'y' && isConsonant(end - 1)) {
            wordBuffer.setCharAt(end, 'i');
        }
    }

    private void step4() {
        if (end > 0 && wordBuffer.charAt(end) == 'e') {
            if (isConsonant(end - 1)) {
                end--;
            }
        }
    }

    private void step5() {
        if (end > 1 && wordBuffer.charAt(end) == 'e' && isConsonant(end - 1)) {
            end--;
        }
    }
}
