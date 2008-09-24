#!/bin/python
import os, os.path, subprocess

templatefile="axis-parallel-template.xml"
output="axis-parallel-%dd.xml"
r=range(10,101,10)
skip=10
mark="""<!-- INSERTION MARK -->"""
insert="""<uniform min="0.0" max="1.0" />"""

generated="axis-parallel-%dd.csv"
cmd=[os.getenv("JAVA","java"), "-cp",
	os.path.expanduser(os.getenv("ELKI","~/ELKI/bin")),
	"experimentalcode.erich.generator.bymodel.GeneratorXMLSpec"]
inparam="-bymodel.spec"
outparam="-wrapper.out"

fi=open(templatefile,"r")
contents=fi.read()
fi.close()

for num in r:
	delta = num - skip
	print "Making for dim=%d" % num
	repeated = insert * delta
	of = open(output % num, "w")
	of.write(contents.replace(mark, repeated))
	of.close()

	subp = subprocess.call(cmd + [inparam, output % num, outparam,
		generated % num])
