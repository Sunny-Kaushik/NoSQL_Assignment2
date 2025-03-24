import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.Writable;

class Stripe implements Writable {
	
	private Integer[] data = new Integer[50];
	private Integer N = 50;
	
	public Stripe() {
		Arrays.fill(data, 0);
	}
	
	public Stripe(Integer[] data) {
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
	
	public int get(Integer idx) {
		return data[idx];
	}
	
	public void add(Stripe other) {
		for(int i = 0; i < N; i++) {
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