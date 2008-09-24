#!/bin/python
import os, os.path, subprocess

r=range(1,21)

specfile="axis-parallel-template.xml"
generated="axis-parallel-%d0k.csv"
cmd=[os.getenv("JAVA","java"), "-cp",
	os.path.expanduser(os.getenv("ELKI","~/ELKI/bin")),
	"experimentalcode.erich.generator.bymodel.GeneratorXMLSpec",
	"-bymodel.spec", specfile]
scaleparam="-bymodel.sizescale"
outparam="-wrapper.out"

for num in r:
	subp = subprocess.call(cmd + [scaleparam, str(num), outparam,
		generated % num])
