#!/usr/bin/python

import sys
import pcap_util
import sequential

filename = sys.argv[1]

if len(sys.argv) > 3:
	start = int(sys.argv[2])
	end = int(sys.argv[3])
elif len(sys.argv) > 2:
	start = 0
	end = int(sys.argv[2])
else:
	start = 0
	end = sys.maxint

with open(filename, 'rb') as f:
	header = pcap_util.TraceHeader(f.read(24))
	f.seek(start+24)
	headers = pcap_util.extract_headers(f, header.swap, end=end)

	for index in sorted(headers.keys()):
		print "%d %d %d %d.%d" % (index, headers[index].incl_len, headers[index].orig_len, headers[index].ts_sec, headers[index].ts_usec)
