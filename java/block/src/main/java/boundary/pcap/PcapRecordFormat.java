package boundary.pcap;

import java.nio.ByteOrder;

import boundary.Record;
import boundary.RecordFormat;
import boundary.Util;

public class PcapRecordFormat implements RecordFormat {
	final int HEADER_LEN = 16;
	int snap_len;
	private ByteOrder byteOrder;

	public PcapRecordFormat(int snap_len, java.nio.ByteOrder byteOrder) {
		this.snap_len = snap_len;
		this.byteOrder = byteOrder;
	}
		
	@Override
	public Record interpretRecord(byte[] bytes, int offset) {
		int incl_len = Util.readInt(bytes, offset + 8, byteOrder);
		int orig_len = Util.readInt(bytes, offset + 12, byteOrder);

//		if (orig_len > 0 && incl_len == Math.min(orig_len, snap_len))
		if (orig_len > 0 && incl_len > 0 && incl_len <= Math.min(orig_len, snap_len))
			return new Record(HEADER_LEN, incl_len);

		return null;
	}

	@Override
	public int maxRecordBodyLen() {
		return snap_len;
	}

	@Override
	public int maxRecordHeaderLen() {
		return HEADER_LEN;
	}

	@Override
	public ByteOrder byteOrder() {
		return byteOrder;
	}
}
