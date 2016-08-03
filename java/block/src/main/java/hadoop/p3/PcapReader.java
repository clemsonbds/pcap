package hadoop.p3;

import java.io.IOException;
import java.io.InputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;

public class PcapReader {
	private static final int DEFAULT_BUFFER_SIZE = 2048;
	private int bufferSize = 2048;

	private static final int PCAP_FILE_HEADER_LENGTH = 24;

	private static final int PCAP_PACKET_HEADER_LENGTH = 16;

	private static final int PCAP_PACKET_HEADER_CAPLEN_POS = 8;
	private static final int PCAP_PACKET_HEADER_WIREDLEN_POS = 12;
	private static final int PCAP_PACKET_HEADER_CAPLEN_LEN = 4;
	private static final int PCAP_PACKET_HEADER_TIMESTAMP_LEN = 4;
	private static final int PCAP_PACKET_MIN_LEN = 53;
	private static final int PCAP_PACKET_MAX_LEN = 1519;
	private static final int MAGIC_NUMBER = -725372255;
	private static final int MIN_PKT_SIZE = 42;
	private long min_captime;
	private long max_captime;
	private InputStream in;
	private byte[] buffer;
	byte[] pcap_header;
	private int bufferLength = 0;
	int consumed = 0;

	public PcapReader(InputStream in, int bufferSize, long min_captime, long max_captime) {
		this.in = in;
		this.bufferSize = bufferSize;
		buffer = new byte[this.bufferSize];
		this.min_captime = min_captime;
		this.max_captime = max_captime;
	}

	public PcapReader(InputStream in, Configuration conf) throws IOException {
		this(in, 2048, conf.getLong("pcap.file.captime.min", 1309412600L),
				conf.getLong("pcap.file.captime.max", 1309585400L));
	}

	public void close() throws IOException {
		in.close();
	}

	int skipPartialRecord(int fraction) throws IOException {
		int pos = 0;
		byte[] captured = new byte[fraction];
		byte[] tmpTimestamp1 = new byte[4];
		byte[] tmpTimestamp2 = new byte[4];
		byte[] tmpCapturedLen1 = new byte[4];
		byte[] tmpWiredLen1 = new byte[4];
		byte[] tmpCapturedLen2 = new byte[4];
		byte[] tmpWiredLen2 = new byte[4];
		int caplen1 = 0;
		int wiredlen1 = 0;
		int caplen2 = 0;
		int wiredlen2 = 0;
		long timestamp2 = 0L;
		int size = 0;
		long endureTime = 100L;

		if ((size = in.read(captured)) < 42) {
			return 0;
		}

		while (pos < size) {
			if ((size - pos < 32) || (size - pos < 53)) {
				pos = size;
				break;
			}

			System.arraycopy(captured, pos, tmpTimestamp1, 0, 4);
			long timestamp1 = Bytes.toLong(BinaryUtils.flipBO(tmpTimestamp1, 4));

			System.arraycopy(captured, pos + 8, tmpCapturedLen1, 0, 4);
			caplen1 = Bytes.toInt(BinaryUtils.flipBO(tmpCapturedLen1, 4));

			System.arraycopy(captured, pos + 12, tmpWiredLen1, 0, 4);
			wiredlen1 = Bytes.toInt(BinaryUtils.flipBO(tmpWiredLen1, 4));

			if ((caplen1 > 53) && (caplen1 < 1519) && (size - pos - 32 - caplen1 > 0)) {

				System.arraycopy(captured, pos + 16 + caplen1 + 8, tmpCapturedLen2, 0, 4);
				caplen2 = Bytes.toInt(BinaryUtils.flipBO(tmpCapturedLen2, 4));

				System.arraycopy(captured, pos + 16 + caplen1 + 12, tmpWiredLen2, 0, 4);
				wiredlen2 = Bytes.toInt(BinaryUtils.flipBO(tmpWiredLen2, 4));

				System.arraycopy(captured, pos + 16 + caplen1, tmpTimestamp2, 0, 4);
				timestamp2 = Bytes.toLong(BinaryUtils.flipBO(tmpTimestamp2, 4));

				if ((timestamp1 >= min_captime) && (timestamp1 < max_captime) && (min_captime <= timestamp2)
						&& (timestamp2 < max_captime) && (wiredlen1 > 53) && (wiredlen1 < 1519) && (wiredlen2 > 53)
						&& (wiredlen2 < 1519) && (caplen1 > 0) && (caplen1 <= wiredlen1) && (caplen2 > 0)
						&& (caplen2 <= wiredlen2) && (timestamp2 >= timestamp1)
						&& (timestamp2 - timestamp1 < endureTime)) {
					return pos;
				}
			}

			pos++;
		}
		return pos;
	}

	int readPacket(int packetLen) throws IOException {
		int bufferPosn = 16;
		byte[] tmp_buffer = new byte[packetLen];

		if ((this.bufferLength = in.read(tmp_buffer)) < packetLen) {
			System.arraycopy(tmp_buffer, 0, buffer, bufferPosn, bufferLength);
			bufferPosn += bufferLength;

			byte[] newpacket = new byte[packetLen - bufferLength];

			if ((this.bufferLength = in.read(newpacket)) < 0)
				return bufferPosn;
			System.arraycopy(newpacket, 0, buffer, bufferPosn, bufferLength);
		} else {
			System.arraycopy(tmp_buffer, 0, buffer, bufferPosn, bufferLength);
		}
		bufferPosn += bufferLength;

		return bufferPosn;
	}

	int readPacketHeader() {
		int headerLength = 0;
		int headerPosn = 0;
		pcap_header = new byte[16];

		byte[] tmp_header = new byte[16];
		BytesWritable capturedLen = new BytesWritable();
		try {
			if ((headerLength = in.read(pcap_header)) < 16) {
				if (headerLength == -1)
					return 0;
				headerPosn += headerLength;

				byte[] newheader = new byte[16 - headerLength];

				if ((headerLength = in.read(newheader)) < 0) {
					consumed = headerPosn;
					return -1;
				}
				System.arraycopy(newheader, 0, pcap_header, headerPosn, headerLength);
			}
			capturedLen.set(pcap_header, 8, 4);
			System.arraycopy(pcap_header, 0, buffer, 0, 16);
			headerPosn = 0;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Bytes.toInt(BinaryUtils.flipBO(capturedLen.getBytes(), 4));
	}

	public int readFileHeader() {
		try {
			byte[] magic = new byte[4];
			bufferLength = in.read(buffer, 0, 24);
			System.arraycopy(buffer, 0, magic, 0, magic.length);

			if (Bytes.toInt(magic) != -725372255) {
				return 0;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bufferLength;
	}

	public int readLine(BytesWritable bytes, int maxLineLength, int maxBytesToConsume) throws IOException {
		bytes.set(new BytesWritable());
		boolean hitEndOfFile = false;
		long bytesConsumed = 0L;

		int caplen = readPacketHeader();

		if (caplen == 0) {
			bytesConsumed = 0L;
		} else if (caplen == -1) {
			bytesConsumed += consumed;

		} else if ((caplen > 0) && (caplen < 1519)) {
			if ((this.bufferLength = readPacket(caplen)) < caplen + 16) {
				hitEndOfFile = true;
			}
			bytesConsumed += bufferLength;

			if (!hitEndOfFile) {
				bytes.set(buffer, 0, caplen + 16);
			}
		}

		return (int) Math.min(bytesConsumed, 2147483647L);
	}

	public int readLine(BytesWritable str, int maxLineLength) throws IOException {
		return readLine(str, maxLineLength, Integer.MAX_VALUE);
	}

	public int readLine(BytesWritable str) throws IOException {
		return readLine(str, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}
}
