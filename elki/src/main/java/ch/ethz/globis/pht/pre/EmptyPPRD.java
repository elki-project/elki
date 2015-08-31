package ch.ethz.globis.pht.pre;

/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
import java.util.Arrays;

import ch.ethz.globis.pht.pre.ColumnType.DoubleColumn;
import ch.ethz.globis.pht.pre.ColumnType.FloatColumn;
import ch.ethz.globis.pht.pre.ColumnType.IntColumn;
import ch.ethz.globis.pht.pre.ColumnType.LongColumn;
import ch.ethz.globis.pht.util.BitTools;

public class EmptyPPRD implements PreProcessorRangeD {

	private ColumnType[] types;
	
	public EmptyPPRD() {
		types = null;
	}
	
	public EmptyPPRD(ColumnType[] types) {
		this.types = Arrays.copyOf(types, types.length);
	}
	
	@Override
	public void pre(double[] raw1, double[] raw2, long[] pre) {
		final int pDIM = raw1.length;
		if (types != null) {
			for (int d=0; d<pDIM; d++) {
				if (types[d] instanceof LongColumn || types[d] instanceof IntColumn) {
					pre[d] = (long)raw1[d];
					pre[d+pDIM] = (long)raw2[d];
				} else if (types[d] instanceof DoubleColumn) {
					pre[d] = BitTools.toSortableLong(raw1[d]);
					pre[d+pDIM] = BitTools.toSortableLong(raw2[d]);
				} else if (types[d] instanceof FloatColumn) {
					pre[d] = BitTools.toSortableLong((float)raw1[d]);
					pre[d+pDIM] = BitTools.toSortableLong((float)raw2[d]);
				} else {
					throw new RuntimeException("Unsupported ColumnType");
				}
			}
		} else {
			for (int d=0; d<pDIM; d++) {
				pre[d] = BitTools.toSortableLong(raw1[d]);
				pre[d+pDIM] = BitTools.toSortableLong(raw2[d]);
			}
		}
	}

	@Override
	public void post(long[] pre, double[] post1, double[] post2) {
		final int pDIM = post1.length;
		if (types != null) {
			for (int d=0; d<pDIM; d++)
				if (types[d] instanceof LongColumn || types[d] instanceof IntColumn) {
					post1[d] = (double) pre[d];
					post2[d] = (double) pre[d+pDIM];
				} else if (types[d] instanceof DoubleColumn) {
					post1[d] = BitTools.toDouble(pre[d]);
					post2[d] = BitTools.toDouble(pre[d+pDIM]);
				} else {
					throw new RuntimeException("Unsupported ColumnType");
				}
		} else {
			for (int d=0; d<pDIM; d++) {
				post1[d] = BitTools.toDouble(pre[d]);
				post2[d] = BitTools.toDouble(pre[d+pDIM]);
			}
		}
	}

}
