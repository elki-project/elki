package ch.ethz.globis.pht.pre;

/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011-2015
Eidgenössische Technische Hochschule Zürich (ETH Zurich)
Institute for Information Systems
GlobIS Group

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

import java.util.Arrays;

import ch.ethz.globis.pht.pre.ColumnType.DoubleColumn;
import ch.ethz.globis.pht.pre.ColumnType.FloatColumn;
import ch.ethz.globis.pht.pre.ColumnType.IntColumn;
import ch.ethz.globis.pht.pre.ColumnType.LongColumn;
import ch.ethz.globis.pht.util.BitTools;

public class EmptyPP implements PreProcessorPoint {

	private ColumnType[] types;
	
	public EmptyPP() {
		types = null;
	}
	
	public EmptyPP(ColumnType[] types) {
		this.types = Arrays.copyOf(types, types.length);
	}
	
	@Override
	public void pre(double[] raw, long[] pre) {
		if (types != null) {
			for (int d=0; d<raw.length; d++)
				if (types[d] instanceof LongColumn || types[d] instanceof IntColumn)
					pre[d] = (long)raw[d];
				else if (types[d] instanceof DoubleColumn)
					pre[d] = BitTools.toSortableLong(raw[d]);
				else if (types[d] instanceof FloatColumn)
					pre[d] = BitTools.toSortableLong((float)raw[d]);
				else
					throw new RuntimeException("Unsupported ColumnType");
		} else {
			for (int d=0; d<raw.length; d++)
				pre[d] = BitTools.toSortableLong(raw[d]);
		}
	}

	@Override
	public void post(long[] pre, double[] post) {
		if (types != null) {
			for (int d=0; d<pre.length; d++)
				if (types[d] instanceof LongColumn || types[d] instanceof IntColumn)
					post[d] = (double) pre[d];
				else if (types[d] instanceof DoubleColumn)
					post[d] = BitTools.toDouble(pre[d]);
				else
					throw new RuntimeException("Unsupported ColumnType");	
		} else {
			for (int d=0; d<pre.length; d++)
				post[d] = BitTools.toDouble(pre[d]);
		}
	}

}
