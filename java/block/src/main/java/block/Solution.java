package block;

public class Solution implements Comparable<Solution> {
	public int last_index;
	public int next_index;

	public Solution(int last, int next) {
		last_index = last;
		next_index = next;
	}

	public int compareTo(Solution o) {
//		System.out.println("comparing this " + this + " to " + o);
		return this.next_index - o.next_index;
	}
	
	public String toString() {
		return "(" + last_index + ", " + next_index + ")";
	}
}
