import binascii
import struct

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
		swap = self.swap

		self.version =  str(btoshort(byte_array[4:6], swap)) + '.' + str(btoshort(byte_array[6:8], swap))
		self.timezone = btoint(byte_array[8:12], swap)
		self.sigfigs =  btoint(byte_array[12:16], swap)
		self.snaplen =  btoint(byte_array[16:20], swap)
		self.network =  btoint(byte_array[20:], swap)

class PacketHeader:
	def __init__(self, byte_array, index, swap):
		self.ts_sec = btoint(byte_array[index : index+4], swap)
		self.ts_usec = btoint(byte_array[index+4 : index+8], swap)
		self.incl_len = btoint(byte_array[index+8 : index+12], swap)
		self.orig_len = btoint(byte_array[index+12 : index+16], swap)
	def valid(self, snap_len):
#		print 'validating incl=%d orig=%d snap=%d' % (self.incl_len, self.orig_len, snap_len)
		if self.incl_len > 0 \
		 and self.incl_len == min(self.orig_len, snap_len):
			return True
		return False
	def __repr__(self):
		return "(%d.%d,%d,%d)" % (self.ts_sec, self.ts_usec, self.incl_len, self.orig_len)
