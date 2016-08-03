package examples;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import boundary.RecordFormat;
import deflate.DeflateBoundaryDetector;

public class DetectDeflateBoundaryExample {

	public static void main(String[] args) throws IOException {
		File file = new File("/Users/jason/git/pcap/test_data/netflix.pcap.gz");
		FileInputStream fis = null;

		fis = new FileInputStream(file);
		DataInputStream dis = new DataInputStream(fis);

		RecordFormat detector = new DeflateBoundaryDetector(dis);
		int start_index = detector.detect();

		System.out.println("start index is at " + start_index);

	}

}
