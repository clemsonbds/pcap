package deflate;

import java.io.IOException;
import java.io.InputStream;

import bits.BitInput;
import bits.BitInputStream;
import boundary.AbstractDecompressor;
import boundary.InvalidBlockException;

public class DeflateDecompressor extends AbstractDecompressor {

	public DeflateDecompressor(InputStream stream) {
		super(stream);
	}

	public int findIndependentStart(int start_index) throws IOException, InvalidBlockException {
		BitInput in = new BitInputStream(stream);
		DeflateBlockFactory factory = new DeflateBlockFactory();

		in.skip(start_index);
		CircularDictionarySymbols dictionary = new CircularDictionarySymbols(32768);

		int nblocks = 0;
		int lookbacks = 32768;
		while (lookbacks > 0) {
			nblocks++;
			DeflateBlock block = factory.parseHeader(in);
			block.decode(in, dictionary, null);
			start_index += block.length();
//			lookbacks = dictionary.factory.numUnresolved();
			lookbacks = 32768 - dictionary.factory.numResolved();
			System.out.println(nblocks + ": read block of length " + block.length() + ", " + in.available() + " bytes remaining, "+ lookbacks + " lookbacks remaining");
		}
		System.out.println("eliminated lookbacks in " + nblocks + " blocks");
		
		return start_index;
	}

}
