/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.pre;

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
