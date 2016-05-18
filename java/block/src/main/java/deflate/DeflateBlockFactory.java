package deflate;

import java.util.BitSet;

import block.InvalidHeaderException;

public class DeflateBlockFactory {
	private int readAsInt(BitSet bits, int offset, int nbits) {
		if (nbits < 0 || nbits >= 32)
			throw new IllegalArgumentException();
			
		int result = 0;
		for (int i = 0; i < nbits; i++)
			result |= bits.get(offset + i) ? 1 : 0 << i;

		return result;
	}

	public DeflateBlock parseHeader(BitSet bits, int offset, int offset_alignment) throws InvalidHeaderException {
		boolean isFinal = bits.get(offset);
		int blockType = readAsInt(bits, offset+1, 2);

		offset += 3;
		offset_alignment = (offset_alignment + 3) % 8;
		
		if (blockType == 0) {
			UncompressedBlock block = new UncompressedBlock(isFinal);
			offset += 8-offset_alignment; // align to byte boundary
//			block.parseLength(bits, offset);
			return block;
		}
		else if (blockType == 1) {
			FixedHuffmanBlock block = new FixedHuffmanBlock(isFinal);
			return block;
		}
		else if (blockType == 2) {
			DynamicHuffmanBlock block = new DynamicHuffmanBlock(isFinal);
			block.parseHuffmanTrees(bits, offset);
			return block;
		}
		
		throw new InvalidHeaderException();
	}
}
