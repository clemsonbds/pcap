package deflate;

import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;


final class CircularDictionarySymbols2 implements CircularDictionary {
	private class Symbol {
		private long[] poss = new long[4];
		public int value;
		int count = 0;

		public Symbol(SymbolFactory f) {
			this(f, -1);

			for (int i = 0; i < poss.length; i++)
				poss[i] = Long.MAX_VALUE;
		}

		public Symbol(SymbolFactory f, int v) {
			factory = f;
			value = v;
		}

		public void decrementCount() {
			count--;
			if (count == 0)
				factory.removeSymbol(this);
		}
		public void incrementCount() {
			count++;
		}
		
		public String toString() {
			if (value == -1)
				return "?";
			return Integer.toString(value);
		}
	}
	
	private class SymbolContainer {
		private Symbol symbol;
		private int count; // copies of this container in the buffer

		public SymbolContainer(Symbol s) {
			this.symbol = s;
			count = 0; // handle manually
		}
		public void decrementCount() {
			count--;
			symbol.decrementCount();
		}
		public void incrementCount() {
			count++;
			symbol.incrementCount();
		}
	}
	
	class SymbolFactory {
		ArrayList<Symbol> symbols;

		public SymbolFactory(int maxSize) {
			symbols = new ArrayList<Symbol>(maxSize);
		}

		public void removeSymbol(Symbol symbol) {
			symbols.remove(symbol);
		}

		public int numUnresolved() {
			int result = 0;
			for (Symbol s: symbols)
				if (s.value == -1)
					result++;
			return result;
		}
		
		public Symbol getSymbol(int v) {
			if (v == -1)
				return newSymbol();

			for (Symbol s: symbols)
				if (s.value == v)
					return s;

			return newSymbol(v);
		}
		public Symbol newSymbol() {
			Symbol s = new Symbol(this);
			symbols.add(s);
			return s;
		}
		private Symbol newSymbol(int v) {
			Symbol s = new Symbol(this, v);
			symbols.add(s);
			return s;
		}
	}
	
	private class StringTracker {
		private HashMap<List<Symbol>, LinkedList<Integer>> stringMap = new HashMap<List<Symbol>, LinkedList<Integer>>();
		private LinkedList<Map.Entry<Integer, LinkedList<Symbol>>> positionList = new LinkedList<Map.Entry<Integer, LinkedList<Symbol>>>();
		int num_indices = 0;
		int num_symbols = 0;

		public String toString() {
			return "stringtracker stats at position " + output_position + ": " + stringMap.size() + " strings, " + num_indices + " indices, " + num_symbols + " symbols";
		}

		public void clean() {
			// clean up
			while (!positionList.isEmpty() && positionList.peek().getKey() < output_position - 32768) {
				Map.Entry<Integer, LinkedList<Symbol>> e = positionList.pop(); // roll off position map
//				System.out.println("position is " + output_position + ", removing symbol " + e.getValue() + " with index " + e.getKey());
				removeString(e.getValue(), e.getKey()); // remove from indices
			}
		}
		
		// only modifies stringMap
		public void removeString(LinkedList<Symbol> string, Integer position) {
			LinkedList<Integer> indices = stringMap.get(string);
			if (indices != null && indices.remove(position)) {
				num_indices--;
				if (indices.size() == 0) {
					stringMap.remove(string);
					num_symbols -= string.size();
				}
			}
		}
		
		public void addString(LinkedList<Symbol> s) {
			clean();
			LinkedList<Integer> indices = stringMap.get(s);
			
			if (indices == null) {
				indices = new LinkedList<Integer>();
				stringMap.put(new LinkedList<Symbol>(s), indices);
				num_symbols += s.size();
			}
			else {
//				System.out.println("found repeated string " + s + " at position " + output_position + " with indices " + indices);
			}

			int start_position = output_position - (s.size()-1);
			indices.add(start_position); // add to tail
			positionList.addLast(new AbstractMap.SimpleEntry<Integer, LinkedList<Symbol>>(start_position, new LinkedList<Symbol>(s)));
			num_indices++;
//			System.out.println(this);
		}

		public int contains(LinkedList<Symbol> s) {
			LinkedList<Integer> indices = stringMap.get(s);
			if (indices != null)
				return output_position - indices.get(0);
			return 0;
		}
	}
	
	SymbolContainer[] data;
	SymbolFactory factory;
	int mask;
	int index = 0;
	int output_position = 0;
	LinkedList<Symbol> literals = new LinkedList<Symbol>();
	StringTracker tracker = new StringTracker();
	
	public CircularDictionarySymbols2(int size) {
		// simplified for 2^x size, other sizes would require reworking modulo logic slightly
		data = new SymbolContainer[size];
		factory = new SymbolFactory(size);
		mask = data.length-1;

		for (int i = 0; i < 32768; i++) {
			data[i] = new SymbolContainer(factory.newSymbol());
			data[i].incrementCount();
		}
	}

	private int getIndex(int i) {
		while (i < 0)
			i += data.length;
		return i & mask;
	}

	public void append(int value) {
		data[index].decrementCount(); // rolling off
		Symbol symbol = factory.getSymbol(value);
		data[index] = new SymbolContainer(symbol);
		data[index].incrementCount();

		literals.addLast(symbol);
		if (literals.size() > 3) {
			literals.removeFirst();
			int dist = tracker.contains(literals);
			if (dist > 0) {
				System.out.println("found repeat of string " + literals + " at distance " + dist);
/*
				LinkedList<Symbol> a = new LinkedList<Symbol>();
				LinkedList<Symbol> b = new LinkedList<Symbol>();
				for (int i = 0; i < literals.size(); i++) {
					a.addLast(data[getIndex(index-(literals.size()-1)+i)].symbol);
					b.addLast(data[getIndex(index-dist+i)].symbol);
				}
				System.out.println(a + " " + b);
*/
			}
			tracker.addString(literals);
		}

		index = getIndex(index+1);
		output_position++;
	}
	
	public void copy(int dist, int len, OutputStream out) throws IOException {
		if (len < 0 || dist < 1 || dist > data.length)
			throw new IllegalArgumentException();
		
		if (literals.size() >= 3)
			System.out.println("found " + literals.size() + " literals in a row");
		literals.clear();

		int readIndex = getIndex(index - dist);
//		System.out.println("starting copy from distance " + dist + ", index " + readIndex + ", of len " + len);

		for (int i = 0; i < len; i++) {
//			System.out.println("copying index " + readIndex + " [" + data[readIndex] + "] to index " + index + " [" + data[index] + "]");

			data[index].decrementCount();
			data[index] = data[readIndex];
			data[index].incrementCount();
			
			readIndex = getIndex(readIndex + 1);
			index = getIndex(index + 1);
		}

		output_position += len;
	}
	
}
