package deflate;

import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;


final class CircularDictionarySymbols implements CircularDictionary {
	private class Symbol {
		private BitSet poss;
		public int value;
		int count = 0;

		public Symbol(SymbolFactory f) {
			this(f, -1);
		}

		public Symbol(SymbolFactory f, int v) {
			factory = f;
			value = v;
		}

		public void isNot(int v) {
			poss.clear(v);
			System.out.println("eliminating possibility, cardinality is now " + poss.cardinality());
			if (poss.cardinality() == 1) {
				value = poss.nextSetBit(0);
				poss = null;
				System.out.println("solved symbol for value " + value);
				factory.claimValue(this, value);
				factory.adjustResolvedSymbolCount(count);
			}
		}
		
		public void decrementCount() {
			count--;
			if (value != -1)
				factory.adjustResolvedSymbolCount(-1);
			if (count == 0)
				factory.removeSymbol(this);
		}
		public void incrementCount() {
			if (value != -1)
				factory.adjustResolvedSymbolCount(1);
			count++;
		}
		
		public String toString() {
			if (value == -1)
				return "?";
			return Integer.toString(value);
		}
	}
	
	private class SymbolContainer {
		private Symbol symbol = null;
		private int count; // copies of this container in the buffer

		public SymbolContainer() {
			this(null);
		}

		public SymbolContainer(Symbol s) {
			this.symbol = s;
			count = 0;
		}
		
		public Symbol getSymbol() {
			if (symbol == null)
				symbol = factory.newSymbol();
			return symbol;
		}

		public void decrementCount() {
			count--;
			if (symbol != null)
				symbol.decrementCount();
		}
		public void incrementCount() {
			count++;
			if (symbol != null)
				symbol.incrementCount();
		}
	}
	
	class SymbolFactory {
		ArrayList<Symbol> symbols;
		BitSet unknowns;
		int resolvedSymbolCount = 0;

		public SymbolFactory(int maxSize) {
			symbols = new ArrayList<Symbol>(maxSize);
			unknowns = new BitSet(256);
			unknowns.set(0, 256);
		}

		public void claimValue(Symbol symbol, int value) {
			for (Symbol s: symbols) {
				if (s.equals(symbol) || s.value != -1)
					continue;
				s.isNot(value);
			}
			unknowns.clear(value);
		}

		public void removeSymbol(Symbol symbol) {
			symbols.remove(symbol);
		}

		public void adjustResolvedSymbolCount(int amount) {
			resolvedSymbolCount += amount;
//			System.out.println("count is now " + resolvedSymbolCount);
		}

		public int numResolved() {
			return resolvedSymbolCount;
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
			s.poss = (BitSet) unknowns.clone();
			symbols.add(s);
			return s;
		}
		private Symbol newSymbol(int v) {
			Symbol s = new Symbol(this, v);
			claimValue(s, v);
			symbols.add(s);
			return s;
		}
	}
	
	
	SymbolContainer[] data;
	SymbolFactory factory;
	int mask;
	int output_position = 0;
//	TreeSet<Integer> unknowns = new TreeSet<Integer>();
	
	public CircularDictionarySymbols(int size) {
		// simplified for 2^x size, other sizes would require reworking modulo logic slightly
		data = new SymbolContainer[size];
		factory = new SymbolFactory(size);
		mask = data.length-1;

		for (int i = 0; i < size; i++) {
			data[i] = new SymbolContainer();
			data[i].incrementCount();
//			unknowns.add(i-size);
		}
	}

	private int getIndex(int i) {
		return Math.floorMod(i, data.length);
	}

	public void append(int value) {
		int index = getIndex(output_position);
		data[index].decrementCount(); // rolling off
		Symbol symbol = factory.getSymbol(value);
		data[index] = new SymbolContainer(symbol);
		data[index].incrementCount();
		
		output_position++;
		
	}
	
	public void copy(int dist, int len, OutputStream out) throws IOException {
		if (len < 0 || dist < 1 || dist > data.length)
			throw new IllegalArgumentException();


		// string of length [len] at [dist] must be the most recent
		if (dist > len) {
			int string_pos = output_position - dist;
//			SymbolStringView string = new SymbolStringView(output_position - dist, len);
//			SymbolStringView comp = new SymbolStringView();
			int num_unknowns = 0;
			int unknown_offset = -1;

			for (int i = 0; i < len; i++) {
				if (data[getIndex(string_pos + i)].getSymbol().value == -1) {
					unknown_offset = i;
					num_unknowns++;
				}
			}
			
			// two methods: if there's an unknown in the string to be copied, then we eliminate possibilities
			// within the string by comparing it to solved strings in more recent output.  do this first so we can
			// potentially solve the string
			if (num_unknowns == 1) {
				// test against all more recent strings without unknowns.
				Symbol unknown = data[getIndex(string_pos + unknown_offset)].getSymbol();

				int comp_pos = string_pos + unknown_offset + 1; // start looking at 1 past the unknown
				int comp_end = comp_pos + unknown_offset; // we know the rest of the original string is known, don't test again

				// iterate through all strings that have no unknowns
				while (comp_end < output_position && comp_pos <= output_position - len) { // stop when no long enough strings possible
					if (data[getIndex(comp_end)].getSymbol().value == -1)
						comp_pos = comp_end+1; // next possible string is past the unknown
					else if (comp_end - comp_pos + 1 == len) {
						// test for similarity to the original string, apart from the unknown symbol
						boolean similar = true;
						for (int i = 0; i < len; i++) {
							if (i == unknown_offset)
								continue;
							if (data[getIndex(string_pos + i)].getSymbol().value != data[getIndex(comp_pos + i)].getSymbol().value) {
								similar = false;
								break;
							}
						}

						// if they're similar, we know that the unknown symbol is not the same as the known symbol in the comparable
						// string, or the compressor would have chosen it as the most recent
						if (similar) {
							unknown.isNot(data[getIndex(comp_pos + unknown_offset)].getSymbol().value);
						
							if (unknown.value != -1) {
								System.out.println("(1) solved a symbol! " + unknown.value);
								// recursive constraint satisfaction here
								num_unknowns = 0;
								break;
							}
						}

						comp_pos++;
					}

					comp_end++;
				}

			}

			// if there are no unknowns, we can use it to eliminate possibilities in unknowns in more recent output
			if (num_unknowns == 0) {
				int comp_end = string_pos + len + 1;
				int comp_pos = string_pos + 1;
				unknown_offset = -1;

				while (comp_end < output_position && comp_pos <= output_position - len) {
					if (data[getIndex(comp_end)].getSymbol().value == -1) {
						if (unknown_offset >= 0) // two or more unknowns in this string
							comp_pos += unknown_offset + 1; // jump past the first unknown

						unknown_offset = len-1;
					}

					if (comp_end - comp_pos + 1 == len) { // not true if we currently have 2+ unknowns behind comp_end
						if (unknown_offset >= 0) { // if there's an unknown in the string
							// test for similarity to the original string, apart from the unknown symbol
							boolean similar = true;
							for (int i = 0; i < len; i++) {
								if (i == unknown_offset)
									continue;
								if (data[getIndex(string_pos + i)].getSymbol().value != data[getIndex(comp_pos + i)].getSymbol().value) {
									similar = false;
									break;
								}
							}

							// if they're similar, we know that the unknown symbol is not the same as the known symbol in the comparable
							// string, or the compressor would have chosen it as the most recent

							if (similar) {
								Symbol unknown = data[getIndex(comp_pos + unknown_offset)].getSymbol();
								unknown.isNot(data[getIndex(string_pos + unknown_offset)].getSymbol().value);
							
								if (unknown.value != -1) {
									System.out.println("(2) solved a symbol! " + unknown.value);
									// recursive constraint satisfaction here
								}
							}
						}

						comp_pos++;
					}

					if (unknown_offset >= 0)
						unknown_offset--;

					comp_end++;
				}
			}
		}

		for (int i = 0; i < len; i++) {
			int readIndex = getIndex(output_position - dist);
			int writeIndex = getIndex(output_position);
			
			data[writeIndex].decrementCount();
			data[writeIndex] = data[readIndex];
			data[readIndex].incrementCount();
			
			output_position++;
		}

	}
	
}
