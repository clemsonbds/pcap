import pcap_util
import time
import sys
import struct

ethcodes = set()

with open('./codes_hex') as f:
	for line in f.readlines():
		parts = line.strip().split('-')

		if len(parts) == 1:
			ethcodes.add(int(parts[0], 16))
		else:
			for num in range(int(parts[0], 16), int(parts[1], 16) + 1):
				ethcodes.add(num)
#	print ethcodes

debug = False

def is_valid_header(mm, index, snap_len, swap, stats, params):
	(h1_incl_len, h1_orig_len) = struct.unpack('<II' if swap else '>II', mm[index+8:index+16])

	if h1_incl_len > h1_orig_len: # modified to allow for truncated pcap files
		return False

	if h1_incl_len < 42 or h1_incl_len > snap_len:
		return False

	h2_index = index + 16 + h1_incl_len

	if h2_index + 16 >= len(mm):
		raise ValueError

	(h2_incl_len, h2_orig_len) = struct.unpack('<II' if swap else '>II', mm[h2_index+8:h2_index+16])

	if h2_incl_len != h2_orig_len:
		return False

	(ethcode,) = struct.unpack('>H', mm[index+28:index+30])

	if ethcode <= 1500:
		if ethcode != h1_orig_len - 14:
			return False
	elif ethcode not in ethcodes:
		return False

	return True

def find_start(file, end_byte, snap_len, swap, stats, params):
	# grab chunks in size of complete max length packet and header

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
			h1_orig_len = pcap_util.btoint(chunk[h1_index+12 : h1_index+16], swap)

			if h1_incl_len != h1_orig_len:
				continue

#			if h1_incl_len < 42 or h1_incl_len > 65535:
			if h1_incl_len < 42 or h1_incl_len > snap_len:
				continue

			h2_index = h1_index + h1_incl_len + HEADER_LEN
			h2_incl_len = pcap_util.btoint(chunk[h2_index+8 : h2_index+12], swap)
			h2_orig_len = pcap_util.btoint(chunk[h2_index+12 : h2_index+16], swap)

			if h2_incl_len != h2_orig_len:
				continue

			e = pcap_util.btoshort(chunk[h1_index+28 : h1_index+30], False)
#			print pcap_util.btostr(chunk[h1_index+16:h1_index+30])
#			print ':'.join(format(x, '02x') for x in chunk[h1_index+16:h1_index+30])

#			print e
			if e <= 1500:
				if e != h1_orig_len - 14:
					continue
			elif e not in ethcodes:
				continue

			return index

		chunk_offset += window_size
		chunk = chunk[window_size:] + file.read(window_size)

	raise ValueError(index)

