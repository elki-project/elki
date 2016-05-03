#!/usr/bin/python
import os, os.path, sys, glob, re, zipfile

tdir, jdir = sys.argv[1], sys.argv[2]
legal = os.path.join(tdir, "license")
if not os.path.isdir(legal): os.makedirs(legal)

extract=[
[r"(^|/)avalon", r"META-INF/(LICENSE|NOTICE|README)(\.txt)?$", "\\1-avalon"],
[r"(^|/)batik", r"META-INF/(LICENSE|NOTICE|README)(\.txt)?$", "\\1-batik"],
[r"(^|/)commons", r"META-INF/(LICENSE|NOTICE|README)(\.txt)?$", "\\1-commons"],
[r"(^|/)elki[^/]+.jar$", r"license/(LICENSE|NOTICE|README)(\.txt)?$", "\\1"],
[r"(^|/)fop", r"META-INF/(LICENSE|NOTICE|README)(\.txt)?$", "\\1-fop"],
# junit won't be included.
[r"(^|/)(junit|hamcrest)", None, None],
# libsvm doesn't include a license.txt
[r"(^|/)libsvm", None, None],
# Trove doesn't include a license.txt
[r"(^|/)trove4j", None, None],
[r"(^|/)xalan", r"^/?([^/]+)[-\.](LICENSE|NOTICE|README)(\.txt)?$", "\\2-\\1"],
[r"(^|/)xml-apis", r"license/(LICENSE|NOTICE|README)(\.txt)?$", "\\1-xml-apis"],
[r"(^|/)xml-apis", r"license/(LICENSE|NOTICE|README)\.([^.]+?)(\.txt)?$", "\\1-\\2"],
[r"(^|/)xmlgraphics-commons", r"META-INF/(LICENSE|NOTICE|README)(\.txt)?$", "\\1-xmlgraphics-commons"],
]
for row in extract:
	row.append(re.compile(row[0]))
	row.append(re.compile(row[1]) if row[1] else None)

for f in glob.glob(os.path.join(jdir, "*.jar")):
	matchers = []
	for row in extract:
		if row[3].search(f):
			matchers.append(row)
	if len(matchers) == 0:
		sys.stderr.write("No extraction rules for: %s\n" % f)
		continue
	matchers = filter(lambda x: x[1] is not None, matchers)
	if len(matchers) == 0: continue # Ignore
	# print >>sys.stderr, "Processing:", f
	count=0
	z = zipfile.ZipFile(f, "r")
	for n in z.namelist():
		out = None
		for row in matchers:
			m = row[4].search(n)
			if not m: continue
			if out and out != m.expand(row[2]):
				sys.stderr.write("Renaming conflict: %s %s %s\n" % (n, out, m.expand(row[2])))
				continue
			out = m.expand(row[2])
			# print >>sys.stderr, n, m.expand(row[2]), row[0]
		if not out: continue
		out = os.path.join(legal, out)
		by = z.read(n).replace("\r\n", "\n")
		count += 1
		if os.path.isfile(out):
			exist = open(out,"r").read()
			if exist == by: continue
			sys.stderr.write("Warning: overwriting %s\n" % out)
		if not os.path.isdir(os.path.dirname(out)):
			os.makedirs(os.path.dirname(out))
		open(out,"w").write(by)
	sys.stderr.write("Processed: %s extracted %d files\n" % (f, count))
	z.close()
