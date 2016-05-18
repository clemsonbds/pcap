package deflate;

import java.util.BitSet;

public class DynamicHuffmanBlock extends DeflateBlock {

	public DynamicHuffmanBlock(boolean isFinal) {
		super(isFinal);
	}

	public int parseHuffmanTrees(BitSet bits, int offset) {
		return 0;
	}
}
