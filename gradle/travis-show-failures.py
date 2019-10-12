#!/usr/bin/python3
import glob, sys

for d in glob.glob("*/build/test-results/*/TEST*.xml") + glob.glob("*/build/test-results/*/TEST*.xml"):
    failed = False
    for line in open(d):
        if "<failure" in line:
            failed = True
            break
    if not failed: continue
    sys.stdout.write(open(d).read())
