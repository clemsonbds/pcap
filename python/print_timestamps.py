#!/usr/bin/env python

import pcap_util
import sys

HEADER_LEN = 16
start_offset=24

if (len(sys.argv) > 2):
	end=int(sys.argv[2])
else:
	end=sys.maxint

with open(sys.argv[1]) as f:
	header = pcap_util.TraceHeader(f.read(start_offset))

	swap = header.swap

	next_index = 0
	offset = 0
	chunk_size = 65535

	b = f.read(min(end, chunk_size))
#	print 'read %d bytes' % (len(b))

	while len(b) >= HEADER_LEN:
		max_index = len(b) - HEADER_LEN
#		print 'len b: %d, offset: %d, max_index: %d' % (len(b), offset, max_index)
		while next_index - offset <= max_index:
			index = next_index - offset

			h = pcap_util.PacketHeader(b, index, swap)
			ts = '%d.%06d' % (h.ts_sec, h.ts_usec)
			print next_index+start_offset, h.incl_len, h.orig_len, ts

			next_index += h.incl_len + HEADER_LEN

		offset += max_index+1
		b = b[max_index+1:] + f.read(min(end-offset, chunk_size))


