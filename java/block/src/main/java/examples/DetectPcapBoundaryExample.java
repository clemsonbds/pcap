package examples;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import pcap.PcapRecordFormat;

public class DetectPcapBoundaryExample {

	public static void main(String[] args) throws IOException {
		File file = new File("/Users/jason/git/pcap/test_data/smallFlows.pcap");
		FileInputStream fis = null;

		fis = new FileInputStream(file);
		DataInputStream dis = new DataInputStream(fis);

		byte[] hbytes = new byte[24];
		
		dis.readFully(hbytes);

		String magic = hbytes.toString().substring(0, 8);
		java.nio.ByteOrder byteOrder;
		if (magic == "a1b2c3d4")
			byteOrder = java.nio.ByteOrder.BIG_ENDIAN;
		else
			byteOrder = java.nio.ByteOrder.LITTLE_ENDIAN;
			
		int snap_len = java.nio.ByteBuffer.wrap(hbytes, 16, 4).order(byteOrder).getInt();

		LzoRecordFormat detector = new LzoRecordFormat(dis, snap_len, byteOrder);
		int start_index = detector.detect();

		System.out.println("snaplen: " + snap_len + "  endian: " + byteOrder);
		System.out.println("start index is at " + start_index);

	}

}
