#!/usr/bin/env python

import pcap_util
import sys

if len(sys.argv) < 3:
	print 'usage:', sys.argv[0], '<filename> <index>'
	exit()

filename = sys.argv[1]
index = int(sys.argv[2])

with open(filename, 'r+b') as f:
	header = pcap_util.TraceHeader(f.read(24))
	f.seek(index)
	record = pcap_util.PacketHeader(f.read(16), 0, header.swap)
	print 'timestamp, incl_len, orig_len'
	print record
	print pcap_util.btostr(f.read(record.incl_len))
