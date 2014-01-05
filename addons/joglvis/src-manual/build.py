#!/usr/bin/python
from lxml import etree
import gzip, re, copy, tempfile, subprocess, os

SVG_NAMESPACE="http://www.w3.org/2000/svg"
INKSCAPE_NAMESPACE="http://www.inkscape.org/namespaces/inkscape"
_safe = re.compile("^[A-Za-z]+$")

sizes=[64,32,16,8,4]

tree = etree.parse(gzip.open("Markers.svgz"))

labels = etree.ETXPath("//{%s}g/@{%s}label" % (SVG_NAMESPACE, INKSCAPE_NAMESPACE))(tree)
for l in labels:
	if not _safe.match(l): raise Exception("Label not safe: "+l)

	ctree = copy.deepcopy(tree)
	layers = etree.ETXPath("//{%s}g[./@{%s}label]" % (SVG_NAMESPACE, INKSCAPE_NAMESPACE))(ctree)
	for layer in layers:
		l2 = layer.get("{%s}label" % INKSCAPE_NAMESPACE)
		if l2 == l:
			layer.attrib["style"]=""
		else:
			layer.attrib["style"]="display:none"

	f = tempfile.NamedTemporaryFile(delete=False)
	f.write(etree.tostring(ctree))
	f.close()
	cmd=["rsvg-convert",
		"-w", "62", "-h", "62",
		"-o", "/tmp/%s.png" % l,
		f.name]
	print "Running", " ".join(cmd)
	subprocess.call(cmd)
	os.unlink(f.name)

for size in sizes:
	cmd = ["montage"]
	for l in labels: cmd.append("/tmp/%s.png" % l)
	cmd.extend(["-geometry", "%sx%s+1+1" % (size-2, size-2), "-background", "none", "PNG32:markers-%s.png" % size ])
	print "Running", " ".join(cmd)
	subprocess.call(cmd)
for l in labels: os.unlink("/tmp/%s.png" % l)
