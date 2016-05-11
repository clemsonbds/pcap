#!/usr/bin/python

import sys
import pcap_util

filename = sys.argv[1]

with open(filename, "rb") as f:
	header = pcap_util.TraceHeader(f.read(24))

print 'magic:   ', header.magic
print 'version: ', header.version
print 'timezone:', header.timezone
print 'sigfigs: ', header.sigfigs
print 'snaplen: ', header.snaplen
print 'network: ', header.network
