import binascii
import struct
import sys

HEADER_LEN = 16

def btostr(b):
	return binascii.hexlify(b)
def btoshort(b, littleE=False):
	if littleE:
		fmt = '<H'
	else:
		fmt = '>H'
	i, = struct.unpack(fmt, b)
	return i
def btoint(b, littleE=False):
#	if swap:
#		b = b[::-1]

#	return int(btostr(b), 16)
	if littleE:
		fmt = '<I'
	else:
		fmt = '>I'
	i, = struct.unpack(fmt, b)
	return i

def shorttob(n, littleE=False):
	fmt = '<H' if littleE else 'HI'
	return struct.pack(fmt, n)

def inttob(n, littleE=False):
	fmt = '<I' if littleE else '>I'
	return struct.pack(fmt, n)

# this is just bisect.bisect_left, but handles reverse order arrays.
# calling this will return an index in the list according to the value of x,
# all values >= x will have lower indices, values < x higher indices.
# ex: reverse_bisect_left([5,4,3,2,1], 3) = 2
# ex: reverse_bisect_left([5,4,3,2,1], 2) = 3
# ex: reverse_bisect_left([5,4,3,2,1], 6) = 0
# ex: reverse_bisect_left([5,4,3,2,1], 0) = 5
def reverse_bisect_left(a, x, lo=0, hi=None):
	if lo < 0:
		raise ValueError('lo must be non-negative')
	if hi is None:
		hi = len(a)
	while lo < hi:
		mid = (lo+hi)//2
		if a[mid] > x: lo = mid+1
		else: hi = mid
	return lo

class TraceHeader:
	def __init__(self, byte_array):
		self.magic =    btostr(byte_array[:4])
		self.swap =     True if self.magic == 'd4c3b2a1' else False
		self.version_major = btoshort(byte_array[4:6], self.swap)
		self.version_minor = btoshort(byte_array[6:8], self.swap)
		self.version =  str(self.version_major) + '.' + str(self.version_minor)
		self.timezone = btoint(byte_array[8:12], self.swap)
		self.sigfigs =  btoint(byte_array[12:16], self.swap)
		self.snaplen =  btoint(byte_array[16:20], self.swap)
		self.network =  btoint(byte_array[20:], self.swap)
	def to_bytes(self):
		b = bytearray().fromhex(self.magic)
		b += shorttob(self.version_major, self.swap)
		b += shorttob(self.version_minor, self.swap)
		b += inttob(self.timezone, self.swap)
		b += inttob(self.sigfigs, self.swap)
		b += inttob(self.snaplen, self.swap)
		b += inttob(self.network, self.swap)
		return b

class PacketHeader:
	def __init__(self, byte_array, index, swap):
		(self.ts_sec, self.ts_usec, self.incl_len, self.orig_len) = \
			struct.unpack('<IIII' if swap else '>IIII', byte_array[index:index+16])
	def valid(self, snap_len):
		if self.incl_len > 0 \
		 and self.incl_len == min(self.orig_len, snap_len):
			return True
		return False
	def __repr__(self):
		return "(%d.%d,%d,%d)" % (self.ts_sec, self.ts_usec, self.incl_len, self.orig_len)

def extract_headers(file, swap, end=sys.maxint, start_offset=24):
	headers = {}
	index = 0
	offset = 0
	chunk_size = 65535

	b = file.read(min(end, chunk_size))

	while len(b) >= HEADER_LEN:
		max_index = len(b) - HEADER_LEN

		while index - offset <= max_index:
			header = PacketHeader(b, index - offset, swap)
			headers[index+start_offset] = header
			index += header.incl_len + HEADER_LEN

		offset += max_index+1
		b = b[max_index+1:] + file.read(min(end-offset, chunk_size))

	return headers

def recapture(infile_name, outfile_name, new_snaplen):
	with open(infile_name, 'rb') as infile:
		with open(outfile_name, 'wb') as outfile:
			header = TraceHeader(infile.read(24))



