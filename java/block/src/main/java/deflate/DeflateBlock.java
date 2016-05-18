package deflate;

import block.Block;

public abstract class DeflateBlock implements Block {
	boolean isFinal;
	int blockType;
	int headerLength;

	public DeflateBlock(boolean isFinal) {
		this.isFinal = isFinal;
		headerLength = 3;
	}
	
	public boolean isValid() {
		if (blockType == 3)
			return false;
		
		return true;
	}
}
