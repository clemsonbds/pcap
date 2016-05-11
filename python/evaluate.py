#!/usr/bin/python

import sys
import pcap_util
import split_pcap
import sequential
import random_access_lee

filename = sys.argv[1]

if len(sys.argv) > 2:
	start_byte = int(sys.argv[2])
else:
	start_byte = 24

if len(sys.argv) > 3:
	end_byte = int(sys.argv[3])
else:
	end_byte = sys.maxint

rapcap=False
lee=True

class ConfusionMatrix:
	def __init__(self):
		self.TP = 0
		self.FP = 0
		self.TN = 0
		self.FN = 0
	def __repr__(self):
		return 'TP: %d, FP: %d, TN: %d, FN: %d' % (self.TP, self.FP, self.TN, self.FN)

with open(filename, "rb") as f:
	header = pcap_util.TraceHeader(f.read(24))

	swap = header.swap
	snaplen = header.snaplen

	correct_indices = sequential.build_indices(f, swap, end=end_byte + 3*snaplen)

	if lee:
		f.seek(start_byte)
		stats = split_pcap.Stats()
		conf_matrix = ConfusionMatrix()
		random_access_lee.evaluate(correct_indices, conf_matrix, f, snaplen, swap, stats, end=end_byte, ts_delta=1)
		print conf_matrix
