import pcap_util
import time
import sys
import struct

HEADER_LEN = 16

def is_valid_header(mm, index, snap_len, swap, stats, params):
	(h1_ts_sec, h1_ts_usec, h1_incl_len, h1_orig_len) = \
		struct.unpack('<IIII' if swap else '>IIII', mm[index:index+16])

	# this is assumed based on the description of the window_size variable
	if h1_incl_len > snap_len*2 - 2*HEADER_LEN:
		return False

	# condition 2 (a) doesn't make sense
	if h1_orig_len - h1_incl_len >= snap_len:
		return False

	h2_index = index + 16 + h1_incl_len

	if h2_index + 16 >= len(mm):
		raise ValueError

	(h2_ts_sec, h2_ts_usec, h2_incl_len, h2_orig_len) = \
		struct.unpack('<IIII' if swap else '>IIII', mm[h2_index:h2_index+16])

	# condition 2 (b)
	if h2_orig_len - h2_incl_len >= snap_len:
		return False

	# don't interpret timestamps if not used
	if params.ts_min == 0 and params.ts_max == 0 and params.ts_delta == 0:
#		print 'passing header', index, 'with incl_len', h1_incl_len, ' and orig_len', h1_orig_len
		return True

	# 1 - modified to allow testing for partial window boundaries
	h1_ts = h1_ts_sec + h1_ts_usec / 1000000.0
	h2_ts = h2_ts_sec + h2_ts_usec / 1000000.0

	if params.ts_min != 0:
		if h1_ts < params.ts_min or h2_ts < params.ts_min:
			return False

	if params.ts_max != 0:
		if h1_ts > params.ts_max or h2_ts > params.ts_max:
			return False

	if params.ts_delta != 0:
#		if index == 3594:
#			print h1_ts, h2_ts
		if h2_ts - h1_ts >= params.ts_delta:
			return False

	return True

def find_start(file, end_byte, snap_len, swap, stats, params):
	# grab chunks in size of complete max length packet and header
	ts_min = params.ts_min
	ts_max = params.ts_max
	ts_delta = params.ts_delta

	window_size = snap_len * 2

	chunk = file.read(window_size * 2)
	chunk_offset = 0
	stats.file_reads += 1
	index = -1

	while len(chunk) >= window_size:
#		start = time.clock()

		max_index = min(end_byte, chunk_offset + window_size)
#		print 'max index:', max_index

		while index < max_index:
			index += 1
#			print 'testing index', index

			h1_index = index - chunk_offset
			h1_incl_len = pcap_util.btoint(chunk[h1_index+8 : h1_index+12], swap)

			# this is assumed based on the description of the window_size variable
			if h1_incl_len > window_size - 2*HEADER_LEN:
				continue

#			print 'pass 1'

			h1_ts = float('%d.%d' % (pcap_util.btoint(chunk[h1_index : h1_index+4], swap), pcap_util.btoint(chunk[h1_index+4 : h1_index+8], swap)))

			h2_index = h1_index + h1_incl_len + HEADER_LEN
			h2_ts = float('%d.%d' % (pcap_util.btoint(chunk[h2_index : h2_index+4], swap), pcap_util.btoint(chunk[h2_index+4 : h2_index+8], swap)))

			# 1 - modified to allow testing for partial window boundaries
			if ts_min != 0:
				if h1_ts < ts_min or h2_ts < ts_min:
					continue

#			print 'pass 2'

#			print '%0.6f %0.6f %0.6f' % (ts_max, h1_ts, h2_ts)

			if ts_max != 0:
				if h1_ts > ts_max or h2_ts > ts_max:
					continue
#			else:
#				if h1_ts > ts_max or h2_ts > ts_max:
#					print h1_ts, h2_ts

			if ts_delta != 0:
				if h2_ts - h1_ts >= ts_delta:
					continue

#			print 'pass 3'

			h1_orig_len = pcap_util.btoint(chunk[h1_index+12 : h1_index+16], swap)
			h2_incl_len = pcap_util.btoint(chunk[h2_index+8 : h2_index+12], swap)
			h2_orig_len = pcap_util.btoint(chunk[h2_index+12 : h2_index+16], swap)

			# 2 (this doesn't make sense)
			if h1_orig_len - h1_incl_len >= snap_len \
			or h2_orig_len - h2_incl_len >= snap_len:
				continue

#			print 'pass 4'

			# 3
#			print h2.ts - h1.ts

#			print 'pass 5'


			return index

		chunk_offset += window_size
		chunk = chunk[window_size:] + file.read(window_size)

	raise ValueError(index)

