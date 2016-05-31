f = open("2.txt", "wb")
size = 1048576 #1MB
array_letter = ['a','b','c','d','e','f','g','h']
for i in range(0,8):
  f.write(array_letter[i] * (size-1))
  f.write("\n")
f.close()
