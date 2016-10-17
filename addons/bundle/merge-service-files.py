#!/usr/bin/python
import os, os.path, sys, glob, re, zipfile
from collections import defaultdict

tdir, jdir = sys.argv[1], sys.argv[2]

def skey(a):
  """ Complex sorting hack: elki > elki-core > elki-* > others """
  a = os.path.basename(a)
  if re.search(r"^elki-[0-9]", a): return (-10, a)
  if re.search(r"^elki-core-", a): return (-5, a)
  if re.search(r"^elki-", a): return (-1, a)
  return (0, a)

prod = defaultdict(list)
for f in sorted(glob.glob(os.path.join(jdir, "*.jar")), key=skey):
  # print >>sys.stderr, "Processing:", f
  count=0
  z = zipfile.ZipFile(f, "r")
  for n in z.namelist():
    if not n.startswith("META-INF/elki/"): continue
    if n == "META-INF/elki/": continue # Folder name
    with z.open(n) as s:
      prod[os.path.basename(n)].extend(
        map(lambda x: x.decode("utf-8").strip("\n"), s.readlines()))
    count += 1
  if count > 0: sys.stderr.write("Processed: %s %d service files\n" % (f, count))
  z.close()

od = os.path.join(tdir, "META-INF", "elki")
if not os.path.exists(od): os.makedirs(od)
for k, v in prod.items():
  out = "\n".join(v) + "\n"
  with open(os.path.join(od, k), "w") as of:
    of.write(out)
