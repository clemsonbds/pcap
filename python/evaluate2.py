#!/usr/bin/python

import sys
import pcap_util
import split_pcap
import sequential
import random_access_lee
import random_access_lukashin
import mmap
import struct
import os


class Parameters:
	def __init__(self):
		self.a=0


def get_solutions(is_valid_header_func, mm, snap_len, swap, stats, params, start_offset=24, last_index=sys.maxint):
	i = start_offset
	solutions = []

	while i <= last_index:
		try:
			if is_valid_header_func(mm, i, snap_len, swap, stats, params):
				solutions.append(i)
			i += 1
		except ValueError as e:
#			print 'stopped at index', i
			break

	return solutions

if __name__ == "__main__":
	dump_file = sys.argv[1]

	file_prefix = dump_file.rsplit('.', 1)[0]

	start_byte = 24
#	start_byte = 9990715
	if len(sys.argv) > 2:
		last_index = int(sys.argv[2])
		last_byte = last_index + 2*65535 # probably
	else:
		last_index = sys.maxint
		last_byte = 0

	rapcap=False
	lee=False
	lukashin=True

	file_size = os.path.getsize(dump_file)
	if last_byte > file_size:
		last_byte = file_size

	with open(dump_file, "r+b") as f:
		mm = mmap.mmap(f.fileno(), last_byte) # last_byte == 0 means whole file
		header = pcap_util.TraceHeader(mm[:24])
		swap = header.swap
		snaplen = header.snaplen
#		print 'alg,ts_min,ts_max,ts_delta,reads,TP,FP,TN,FN'

		if lee:
			# find min and max timestamps
			index = 24
			real_min_ts = sys.maxint
			real_max_ts = 0

			while index + 16 < len(mm):
				(ts_sec, ts_usec, incl_len) = \
					struct.unpack('<III' if swap else '>III', mm[index:index+12])
				ts = ts_sec + ts_usec/1000000.0

				if ts < real_min_ts:
					real_min_ts = ts
				if ts > real_max_ts:
					real_max_ts = ts

				if index > last_index: # go one index higher than last_index
					break

				index += 16 + incl_len

#			print 'timestamp range:', real_min_ts, real_max_ts
			ts_deltas = [0, 1, 60]
			ts_mins = [0, real_min_ts]
			ts_maxs = [0, real_max_ts]
#			ts_deltas = [1]
#			ts_mins = [real_min_ts]
#			ts_maxs = [real_max_ts]
#			ts_deltas = [0]
#			ts_mins = [0]
#			ts_maxs = [0]

			for ts_delta in ts_deltas:
				for ts_min in ts_mins:
					for ts_max in ts_maxs:
						test_name = file_prefix + '_lee_delta_' + str(ts_delta)
						if ts_min == 0:
							test_name += '_min_no'
						else:
							test_name += '_min_yes'
						if ts_max == 0:
							test_name += '_max_no'
						else:
							test_name += '_max_yes'

						print 'running test', test_name
						stats = split_pcap.Stats()
						params = Parameters()
						params.ts_min = ts_min
						params.ts_max = ts_max
						params.ts_delta = ts_delta

						solution_indices = get_solutions(
							random_access_lee.is_valid_header,
							mm, snaplen, swap, stats, params, start_offset=start_byte, last_index=last_index)

						with open(test_name + '.indices', 'w') as w:
							for s in solution_indices:
								w.write(str(s) + '\n')

		if lukashin:
			stats = split_pcap.Stats()
			params = Parameters()
			test_name = file_prefix + '_lukashin'
			print 'running test', test_name

			solution_indices = get_solutions(
				random_access_lukashin.is_valid_header,
				mm, snaplen, swap, stats, params, start_offset=start_byte, last_index=last_index)

			with open(test_name + '.indices', 'w') as w:
				for s in solution_indices:
					w.write(str(s) + '\n')
#					print s
#			c = evaluate(correct_indices, solution_indices, start_byte, bytes_tested)
#			print ','.join(['lukashin', '0','0','0', str(stats.file_reads), str(c.TP), str(c.FP), str(c.TN), str(c.FN)])

