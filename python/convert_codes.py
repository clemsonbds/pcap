#!/usr/bin/env python

import sys

filename = sys.argv[1]

with open(filename) as f:
	for line in f.readlines():
		parts = line.strip().split('-')

		if len(parts) == 1:
			print int(parts[0], 16)
		else:
			for num in range(int(parts[0], 16), int(parts[1], 16) + 1):
				print num
