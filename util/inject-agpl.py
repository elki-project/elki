#!/usr/bin/python
# -*- coding: utf-8 -*-
import sys, re

template="""
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
""".strip()

ismarked=re.compile("(General Public License|Copyright \()", re.I)
iselki=re.compile("This file is part of ELKI:", re.I)

for fn in sys.argv[1:]:
	fi = open(fn)
	c = fi.read()
	fi.close()

	if ismarked.search(c):
		if not ismarked.search(c):
			print fn, "already contained a copyright notice!"
		continue

	if fn.endswith("package-info.java"):
		d, p = c.strip().rsplit("\n", 1)
		if not p.startswith("package"):
			print fn, "does not end with package statement!"
			continue
		of = open(fn, "w")
		# Inject copyright notice before the package name, but after the
		# documentation, this is nicer for eclipse
		of.write(d)
		of.write("\n\n")
		of.write(template)
		of.write("\n")
		of.write(p)
		of.close()
	else:
		p, b = c.split("\n", 1)
		if not p.startswith("package"):
			print fn, "does not start with package statement!"
			continue
		# Inject copyright notice
		of = open(fn, "w")
		of.write(p)
		of.write("\n")
		of.write(template)
		of.write("\n")
		of.write(b)
		of.close()
