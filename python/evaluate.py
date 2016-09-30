#!/usr/bin/python

import sys
import pcap_util
import split_pcap
import sequential
import random_access_lee
import random_access_lukashin

class ConfusionMatrix:
	def __init__(self):
		self.TP = 0
		self.FP = 0
		self.TN = 0
		self.FN = 0
	def __repr__(self):
		return 'TP: %d, FP: %d, TN: %d, FN: %d' % (self.TP, self.FP, self.TN, self.FN)

class Parameters:
	def __init__(self):
		self.a=0

def evaluate(correct, solutions, start_offset=24, bytes_tested=sys.maxint):
#	print bytes_tested, len(correct), len(solutions)

	if len(correct) > 0:
		last_correct_index = len(correct) - 1
		while correct[last_correct_index] > bytes_tested + start_offset:
			last_correct_index -= 1
		correct = sorted(correct_indices[:last_correct_index+1], reverse=True)

	if len(solutions) > 0:
		last_solution_index = len(solutions) - 1
		while solutions[last_solution_index] > bytes_tested + start_offset:
			last_solution_index -= 1
		solutions = sorted(solutions[:last_solution_index+1], reverse=True)

	conf_matrix = ConfusionMatrix()
#	print correct, solutions

	while len(correct) > 0 or len(solutions) > 0:
		if len(correct) == 0:
			conf_matrix.FP += len(solutions)
			break
		elif len(solutions) == 0:
			conf_matrix.FN += len(correct)
			break
		elif solutions[-1] > correct[-1]:
			conf_matrix.FP += 1
			solutions.pop()
		elif solutions[-1] < correct[-1]:
			conf_matrix.FN += 1
			correct.pop()
		else: # equal
			conf_matrix.TP += 1
			solutions.pop()
			correct.pop()

	conf_matrix.TN = bytes_tested - conf_matrix.TP - conf_matrix.FP - conf_matrix.FN
	return conf_matrix

def get_solutions(find_start_func, file, snap_len, swap, stats, params, start_offset=24, end=sys.maxint):
        offset = start_offset
        solutions = []

        try:
                while True:
                        file.seek(offset)
                        solution = find_start_func(file, end_byte-offset, snap_len, swap, stats, params) + offset
                        solutions.append(solution)
                        offset = solution+1
        except ValueError as e:
                bytes_tested = int(e.args[0]) + offset - start_offset + 1

        return bytes_tested, solutions

if __name__ == "__main__":
	dump_file = sys.argv[1]
	index_file = sys.argv[2]

	start_byte = 24

	if len(sys.argv) > 3:
		end_byte = int(sys.argv[3]) + start_byte - 1
	else:
		end_byte = sys.maxint

	rapcap=False
	lee=False
	lukashin=True

	real_min_ts = sys.maxint
	real_max_ts = 0

	correct_indices = []

	with open(index_file) as f:
		for line in f.readlines():
			index_str, incl_len, _, ts_str = line.split()
			index = int(index_str)

			correct_indices.append(index)

			ts = float(ts_str)
			if ts < real_min_ts:
				real_min_ts = ts
			if ts > real_max_ts:
				real_max_ts = ts

			# breaking down here to add one more to max_ts, to make Lee work.  will prune in evaluation
			if index > end_byte:
				break

	with open(dump_file, "rb") as f:
		header = pcap_util.TraceHeader(f.read(24))
		swap = header.swap
		snaplen = header.snaplen
#		print 'alg,ts_min,ts_max,ts_delta,reads,TP,FP,TN,FN'

		if lee:
			ts_deltas = [0, 1, 60]
			ts_mins = [0, real_min_ts]
			ts_maxs = [0, real_max_ts]

			find_start_func = random_access_lee.find_start

			for ts_delta in ts_deltas:
				for ts_min in ts_mins:
					for ts_max in ts_maxs:
						stats = split_pcap.Stats()
						params = Parameters()
						params.ts_min = ts_min
						params.ts_max = ts_max
						params.ts_delta = ts_delta

						bytes_tested, solution_indices = get_solutions(find_start_func, f, snaplen, swap, stats, params, start_offset=start_byte, end=end_byte)
						c = evaluate(correct_indices, solution_indices, start_byte, bytes_tested)
						print ','.join('lee', ts_min, ts_max, ts_delta, stats.file_reads, c.TP, c.FP, c.TN, c.FN)
		if lukashin:
			stats = split_pcap.Stats()
			params = Parameters()

			find_start_func = random_access_lukashin.find_start
			bytes_tested, solution_indices = get_solutions(find_start_func, f, snaplen, swap, stats, params, start_offset=start_byte, end=end_byte)
			for x in solution_indices:
				print x
#			c = evaluate(correct_indices, solution_indices, start_byte, bytes_tested)
#			print ','.join(['lukashin', '0','0','0', str(stats.file_reads), str(c.TP), str(c.FP), str(c.TN), str(c.FN)])
