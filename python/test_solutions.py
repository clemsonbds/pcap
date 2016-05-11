#!/usr/bin/python

import sys
import pcap_util
import split_pcap
import sequential
import random_access_lee

filename = sys.argv[1]
start_byte = int(sys.argv[2])

if len(sys.argv) > 3:
	end_byte = int(sys.argv[3])
else:
	end_byte = start_byte

if len(sys.argv) > 4:
	step = int(sys.argv[4])
else:
	step = 1

rapcap=False
lee=True

with open(filename, "rb") as f:
	header = pcap_util.TraceHeader(f.read(24))

	swap = header.swap
	snaplen = header.snaplen
#	snaplen = 256000

	indices = sequential.build_indices(f, swap, end=end_byte + 3*snaplen)
#	print indices
#	indices = []

	while start_byte <= end_byte:
		if rapcap:
			f.seek(start_byte)
			stats = split_pcap.Stats()

			try:
				solution = split_pcap.find_start(f, snaplen, swap, stats) + start_byte
			except ValueError as e:
				solution = e.args[0]

			if solution in indices:
				v = 'v'
			else:
				v = ' '

			print 'RAPCAP: %s solution: %d  reads: %d  bytes_tested: %d  solutions_tested: %d  nonmerges: %d' % (v, solution, stats.file_reads, stats.bytes_tested, stats.solutions_tested, stats.nonmerges)

		if lee:
			f.seek(start_byte)
			stats = split_pcap.Stats()
			solution = random_access_lee.find_start(f, snaplen, swap, stats) + start_byte

			if solution in indices:
				v = 'v'
			else:
				v = ' '

			print 'Lee:    %s solution: %d' % (v, solution)

		start_byte += step
