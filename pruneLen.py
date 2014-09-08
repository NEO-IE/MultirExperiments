#sg
import sys
FIELD_NUMBER = 
for line in open(sys.argv[1], 'r'):
    if(len(line.split("\t")[FIELD_NUMBER]) < sys.argv[2]):
        print line
