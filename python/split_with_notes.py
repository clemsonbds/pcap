'''
Proving:

preconditions:
    1. input size > snaplen + 2*header_len
      if it's less than this, it's possible that we don't get a complete header
      in the first chunk, and the only solution can't be found

we need to prove that iff a solution exists, it exists in the output solution
set.  no solutions must mean malformed input, and not a packet larger than the
input (see precondition 1).  if solution set > 1, we don't have enough information
and this file must be read as part of a larger piece (possibly appended to the
preceeding chunk, implementation detail, outside scope of this work).  Therefore,
if output is 1 solution, it is the only correct solution.
'''

header_len = 16
incl_offset = 8
orig_offset = 12

# test all candidates in a chunk of bytes, return a new candidate list of those that
# are ahead of this chunk
def test_candidates(chunk, snaplen, swap, candidates, chunk_offset = 0, bytes_tested = 0):
	new_candidates = []
	next_chunk_candidates = []

	# inner loop, test and requeue candidates until we need to get more data
	# we break on two conditions: run out of candidates (no solution) or getting
	# more data, which means that all candidates remaining have passed at least
	# one iteration (since we started with next_header_index within the first chunk)
	# this should guarantee that if there is a solution, it is contained within the
	# candidate list.
#		print btostr(bytes_read)
	for (next_header_index, start_index) in candidates:

		# for experiments, not part of algorithm
		if next_header_index + header_len > bytes_tested:
			bytes_tested = next_header_index + header_len

		incl_index = next_header_index + incl_offset - chunk_offset
		orig_index = next_header_index + orig_offset - chunk_offset
		incl_len = pcap_util.btoint(chunk[incl_index : incl_index + 4], swap)
		orig_len = pcap_util.btoint(chunk[orig_index : orig_index + 4], swap)

		# conditions for candidacy:
		# incl and orig must both be greater than 0
		# orig >= incl
		# incl <= snaplen
		if incl_len > 0 and incl_len <= orig_len and incl_len <= snaplen:
			next_header_index += incl_len + header_len

			# if the complete next header is in this chunk, append to the new candidates
			# to test again on this chunk.  otherwise, we'll test with a new chunk of data
			if next_header_index > chunk_offset + len(chunk) - header_len:
				c = next_chunk_candidates
			else:
				c = new_candidates

			# find the insertion index in our ordered list with binary search:
			insert_index = pcap_util.reverse_bisect_left(c, (next_header_index, start_index))

			# prune other solutions that are just the next hop for this solution
			# coalesce solution if another with the same header index is due to be checked
			if c[insert_index][0] == next_header_index:
				c[insert_index] = (next_header_index, min(c[insert_index][1], start_index))
			else:
				c.insert(insert_index, (next_header_index, start_index))

			# is appending the best?  inserting in order of next_header_index
			# might save a file seek.  on the other hand, maybe we want the others
			# to fail fast.  all of them but one have to fail, and if they require
			# a file seek to fail, then so be it.  inserting in order might save
			# nothing and just cost searching the list.
			#
			# solving a problem:  if headers of 3 packets in the dataset fit within snaplen,
			# they can form a pair of solutions, one of them two packets ahead of the other.
			# without keeping a history, this might be difficult to coalesce.  this might be
			# solved with a better solution tracking data structure than a list, maybe a tree?
			# but we can get around it by always ensuring that we're testing the next candidate
			# solution in order of next_header_index.
			#
			# actually, can we manipulate insertion into this list to guarantee
			# that all next_header_index fall within the current chunk or later?
			# meaning, we don't read until this chunk is exhausted.  If so,
			# we can avoid appending chunks to bytes_read, and just keep an offset
			# index for the chunk, reduce memory usage and byte copying
			# it may be faster to do this with a search for next_header_index within
			# this chunk rather than insertion, depends on data structure.  plus,
			# simple appending might facilitate solution coalescing with the tail.

# test all candidates in a chunk of bytes, return a new candidate list of those that
# are ahead of this chunk
def narrow_candidates2(chunk, snaplen, swap, candidates, chunk_offset = 0, bytes_tested = 0):
	new_candidates = []
	next_chunk_candidates = []

	# inner loop, test and requeue candidates until we need to get more data
	# we break on two conditions: run out of candidates (no solution) or getting
	# more data, which means that all candidates remaining have passed at least
	# one iteration (since we started with next_header_index within the first chunk)
	# this should guarantee that if there is a solution, it is contained within the
	# candidate list.
#		print btostr(bytes_read)
	while len(candidates) > 0:

		# the candidate list is guaranteed to be in descending order of next_header_index
		# break to get more bytes if needed
		# this may not be possible if we checked incl vs snaplen to generate this candidate
		# but this is the loop exit condition, we check the remaining solutions below
		if candidates[-1][0] + header_len > chunk_offset + len(chunk):
			break

		# remove this candidate, we'll decide whether to append after checking its candidacy again
		(next_header_index, start_index) = candidates.pop(-1)

		if next_header_index + header_len > bytes_tested:
			bytes_tested = next_header_index + header_len

		incl_index = next_header_index + incl_offset - chunk_offset
		orig_index = next_header_index + orig_offset - chunk_offset
		incl_len = pcap_util.btoint(chunk[incl_index : incl_index + 4], swap)
		orig_len = pcap_util.btoint(chunk[orig_index : orig_index + 4], swap)

		# conditions for candidacy:
		# incl and orig must both be greater than 0
		# orig >= incl
		# incl <= snaplen
		if incl_len > 0 and incl_len <= orig_len and incl_len <= snaplen:
			next_header_index += incl_len + header_len

			# prune other solutions that are just the next hop for this solution
			# can we simply coalesce with the tail rather than search the list?
			# in early stage, more likely to find a viable solution at the tail,
			# and it will coalesce eventually
			#
			# since we're inserting in order, just coalesce with next hop
			# binary search on ordered list:
			insert_index = pcap_util.reverse_bisect_left(candidates, (next_header_index, start_index))

			if insert_index < len(candidates) and candidates[insert_index][0] == next_header_index:
				# coalesce and replace
				candidates[insert_index] = (next_header_index, min(candidates[insert_index][1], start_index))
			else:
				# insert
				candidates.insert(insert_index, (next_header_index, start_index))

			# is appending the best?  inserting in order of next_header_index
			# might save a file seek.  on the other hand, maybe we want the others
			# to fail fast.  all of them but one have to fail, and if they require
			# a file seek to fail, then so be it.  inserting in order might save
			# nothing and just cost searching the list.
			#
			# solving a problem:  if headers of 3 packets in the dataset fit within snaplen,
			# they can form a pair of solutions, one of them two packets ahead of the other.
			# without keeping a history, this might be difficult to coalesce.  this might be
			# solved with a better solution tracking data structure than a list, maybe a tree?
			# but we can get around it by always ensuring that we're testing the next candidate
			# solution in order of next_header_index.
			#
			# actually, can we manipulate insertion into this list to guarantee
			# that all next_header_index fall within the current chunk or later?
			# meaning, we don't read until this chunk is exhausted.  If so,
			# we can avoid appending chunks to bytes_read, and just keep an offset
			# index for the chunk, reduce memory usage and byte copying
			# it may be faster to do this with a search for next_header_index within
			# this chunk rather than insertion, depends on data structure.  plus,
			# simple appending might facilitate solution coalescing with the tail.

def find_start(file, snaplen, swap, file_reads=0, bytes_tested=0):
	# grab chunks in size of complete max length packet and header
	chunksize = snaplen + header_len

	# candidate list of tuples (next_header_index, start_index) to make it simpler to use
	# bisect for insertion, because python compares tuples by first element first.
	# candidates are all possible header start bytes, avoiding partial header at the end
	# we keep this list in reverse order so all removals are from the back, and we insert
	# (in order) rarely
	#
	# in theory, the ideal data structure here would be one optimized for insert and
	# peek/removal of minimum.  a BST can do both in O(lg n), maybe a hybrid BST over
	# linked list for O(lg n) insertion and O(1) popmin?
	candidates = [(x, x) for x in range(chunksize-1, -1, -1)]

	# worst case: missing first byte of header at start, with a max length
	# packet.  need to get the complete next header.
	chunk = file.read(chunksize + header_len - 1)
	chunk_offset = 0
	file_reads += 1
#	bytes_read = bytearray(0)

	# outer loop, terminate when we've read the whole file, or if there is only one candidate left
	while len(chunk) > 0:

		# narrow_candidates will exhaust the candidate list and return two sets of new ones
		while (len(candidates) > 1)
		(candidates, next_chunk_candidates) = narrow_candidates(chunk, snaplen, swap, candidates, chunk_offset, bytes_tested)

		# if there 0 or 1 left, no need to keep testing, break out early
		if len(candidates) <= 1:
			break

		chunk_offset += len(chunk)
		chunk = file.read(chunksize)

		file_reads += 1

	return [x[0] for x in candidates]
