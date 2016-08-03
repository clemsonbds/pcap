package deflate;

import java.util.ArrayList;
import java.util.List;

import block.Solution;

public class DoubleSortedSet {
	List<Solution> list = new ArrayList<Solution>();
	
	public int size() {
		return list.size();
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public boolean contains(Solution s) {
		return list.contains(s);
	}

	public boolean push(Solution s) {
		return false;
	}

	public Solution pop() {
		return list.remove(0);
	}
	
	public boolean remove(Solution s) {
		return false;
	}

	public void clear() {
		list.clear();
	}

	public Solution first() {
		return list.get(0);
	}

	public Solution last() {
		return list.get(list.size()-1);
	}

}
