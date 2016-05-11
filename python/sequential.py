import pcap_util
import sys

HEADER_LEN = 16

def build_indices(file, swap, start_offset=24, end=sys.maxint):


	indices = []
	next_index = 0
	offset = 0
	chunk_size = 65535

	b = file.read(min(end, chunk_size))
#	print 'read %d bytes' % (len(b))

	while len(b) >= HEADER_LEN:
		max_index = len(b) - HEADER_LEN
#		print 'len b: %d, offset: %d, max_index: %d' % (len(b), offset, max_index)
		while next_index - offset <= max_index:
			indices.append(next_index+start_offset)
			index = next_index - offset
			next_index += pcap_util.btoint(b[index+8:index+12], swap) + HEADER_LEN

		offset += max_index+1
		b = b[max_index+1:] + file.read(min(end-offset, chunk_size))


	return indices
