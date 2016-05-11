#!/usr/bin/python

import sys
import pcap_util
import split_pcap

filename = sys.argv[1]
start_byte = int(sys.argv[2])

stats = split_pcap.Stats()

with open(filename, "rb") as f:
	header = pcap_util.parse_header(f)

	swap = pcap_util.should_swap(header[0])
	snaplen = header[4]
	print swap
	# this is just so we can specify the start for testing, real algorithm starts at 0
	f.seek(start_byte)

	try:
		solution = split_pcap.find_start(f, snaplen, swap, stats)
		print 'solution:', solution + start_byte
	except ValueError as e:
		print e.args[0]

print "file reads:      ", stats.file_reads
print "bytes_tested:    ", stats.bytes_tested
print "solutions tested:", stats.solutions_tested
