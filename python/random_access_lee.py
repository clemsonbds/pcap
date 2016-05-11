import pcap_util
import time
import sys

HEADER_LEN = 16

def find_start(file, snap_len, swap, stats, ts_min=0, ts_max=0, ts_delta=60):
	# grab chunks in size of complete max length packet and header
	window_size = snap_len * 2

	chunk = file.read(window_size * 2)
	file_offset = 0
	stats.file_reads += 1
	index = -1

	while len(chunk) >= window_size:
#		start = time.clock()

		max_index = file_offset + window_size
#		print 'max index:', max_index

		while index <= max_index:
			index += 1

			h1_index = index - file_offset
			h1_incl_len = pcap_util.btoint(chunk[h1_index+8 : h1_index+12], swap)

			# this is assumed based on the description of the window_size variable
			if h1_incl_len > window_size - 2*HEADER_LEN:
				continue


			h1_orig_len = pcap_util.btoint(chunk[h1_index+12 : h1_index+16], swap)
			h1_ts = float('%d.%d' % (pcap_util.btoint(chunk[h1_index : h1_index+4], swap), pcap_util.btoint(chunk[h1_index+4 : h1_index+8], swap)))

			h2_index = h1_index + h1_incl_len + HEADER_LEN
			h2_incl_len = pcap_util.btoint(chunk[h2_index+8 : h2_index+12], swap)
			h2_orig_len = pcap_util.btoint(chunk[h2_index+12 : h2_index+16], swap)
			h2_ts = float('%d.%d' % (pcap_util.btoint(chunk[h2_index : h2_index+4], swap), pcap_util.btoint(chunk[h2_index+4 : h2_index+8], swap)))

			# 1 - modified to allow testing for partial window boundaries
			if ts_min != 0:
				if h1_ts < ts_min or h2_ts < ts_min:
					continue
			if ts_max != 0:
				if h1_ts > ts_max or h2_ts > ts_max:
					continue

			# 2 (this doesn't make sense)
			if h1_orig_len - h1_incl_len >= snap_len \
			or h2_orig_len - h2_incl_len >= snap_len:
				continue

			# 3
#			print h2.ts - h1.ts
			if h2_ts - h1_ts >= ts_delta:
				continue


			return index

		file_offset += window_size
		chunk = chunk[window_size:] + file.read(window_size)

	raise ValueError(len(chunk))

def evaluate(correct_indices, conf_matrix, file, snap_len, swap, stats, end=sys.maxint, ts_min=0, ts_max=0, ts_delta=60):
	offset = 24
	solutions = []

	try:
		while offset < end:
			file.seek(offset)
			solution = find_start(file, snap_len, swap, stats, ts_min, ts_max, ts_delta) + offset
			solutions.append(solution)
			offset = solution+1
		bytes_tested = end
	except ValueError as e:
		bytes_tested = int(e.args[0]) + offset - 2*snap_len-1

	# undefined behavior for last window_size bytes, remove from analysis
	while solutions[-1] > bytes_tested:
		solutions.pop()

	correct = sorted([x for x in correct_indices if x < bytes_tested], reverse=True)
	solutions.sort(reverse=True)

	while len(correct) > 0 or len(solutions) > 0:
#		print '*' if len(solutions) and len(correct) and solutions[-1] != correct[-1] else '', solutions[-1] if len(solutions) else '    ', correct[-1] if len(correct) else ' '
		if len(correct) == 0:
			conf_matrix.FP += len(solutions)
			break
		elif len(solutions) == 0:
			conf_matrix.FN += len(correct)
			break
		elif solutions[-1] < correct[-1]:
			conf_matrix.FP += 1
			solutions.pop()
		elif solutions[-1] > correct[-1]:
			conf_matrix.FN += 1
			correct.pop()
		else: # equal
			conf_matrix.TP += 1
			solutions.pop()
			correct.pop()

	conf_matrix.TN = bytes_tested - conf_matrix.TP - conf_matrix.FP - conf_matrix.FN
