package hadoop.p3;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class PcapRecordReader extends RecordReader<LongWritable, BytesWritable> {
	private static final Log LOG = LogFactory.getLog(PcapRecordReader.class.getName());

	private CompressionCodecFactory compressionCodecs = null;
	private long start;
	private long pos;
	private long end;
	private PcapReader in;
	int maxLineLength;
	private boolean fileheader_skip = true;
	private LongWritable key = null;
	private BytesWritable value = null;

	public PcapRecordReader() {
	}

	public void initialize(InputSplit genericSplit, TaskAttemptContext context) throws IOException {
		FileSplit split = (FileSplit) genericSplit;
		Configuration job = context.getConfiguration();
		maxLineLength = job.getInt("mapred.linerecordreader.maxlength", Integer.MAX_VALUE);
		fileheader_skip = job.getBoolean("pcap.file.header.skip", true);

		start = split.getStart();
		end = (start + split.getLength());
		Path file = split.getPath();

		compressionCodecs = new CompressionCodecFactory(job);
		CompressionCodec codec = compressionCodecs.getCodec(file);

		FileSystem fs = file.getFileSystem(job);
		FSDataInputStream fileIn = fs.open(split.getPath());

		boolean skipFileHeader = false;
		boolean skipPartialRecord = false;
		int fraction = 4000;

		if (codec != null) {
			in = new PcapReader(codec.createInputStream(fileIn), job);
			end = Long.MAX_VALUE;
			skipFileHeader = true;
		} else {
			if (start == 0L) {
				skipFileHeader = true;
			} else {
				skipPartialRecord = true;
				fileIn.seek(start);
			}

			in = new PcapReader(fileIn, job);
		}
		if ((skipFileHeader) && (fileheader_skip)) {
			start += in.readFileHeader();
		}
		if (skipPartialRecord) {
			int skip = in.skipPartialRecord(fraction);
			while (skip == fraction) {
				start += skip;
				skip = in.skipPartialRecord(fraction);
			}
			start += skip;
			fileIn.seek(start);
			in = new PcapReader(fileIn, job);
		}

		pos = start;
	}

	public boolean nextKeyValue() throws IOException {
		if (key == null) {
			key = new LongWritable();
		}

		key.set(pos);

		if (value == null) {
			value = new BytesWritable();
		}

		int newSize = 0;

		while (pos < end) {

			newSize = in.readLine(value, maxLineLength,
					Math.max((int) Math.min(2147483647L, end - pos), maxLineLength));
			if (newSize == 0) {
				pos = end;
				break;
			}

			pos += newSize;
			if (newSize < maxLineLength) {
				break;
			}
			LOG.info("Skipped line of size " + newSize + " at pos " + (pos - newSize));
		}
		if (newSize == 0) {
			key = null;
			value = null;
			return false;
		}
		return true;
	}

	public float getProgress() {
		if (start == end) {
			return 0.0F;
		}
		return Math.min(1.0F, (float) (pos - start) / (float) (end - start));
	}

	public synchronized void close() throws IOException {
		if (in != null) {
			in.close();
		}
	}

	public LongWritable getCurrentKey() throws IOException, InterruptedException {
		return key;
	}

	public BytesWritable getCurrentValue() throws IOException, InterruptedException {
		return value;
	}
}
