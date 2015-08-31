package ch.ethz.globis.pht.pre;

/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
import java.util.Arrays;

import ch.ethz.globis.pht.pre.ColumnType.IntColumn;
import ch.ethz.globis.pht.pre.ColumnType.LongColumn;

public class EmptyPPR implements PreProcessorRange {

	private ColumnType[] types;
	
	public EmptyPPR() {
		types = null;
	}
	
	public EmptyPPR(ColumnType[] types) {
		this.types = Arrays.copyOf(types, types.length);
	}
	
	@Override
	public void pre(long[] raw1, long[] raw2, long[] pre) {
		final int pDIM = raw1.length;
		if (types != null) {
			for (int d=0; d<pDIM; d++) {
				if (types[d] instanceof LongColumn || types[d] instanceof IntColumn) {
					pre[d] = raw1[d];
					pre[d+pDIM] = raw2[d];
				} else {
					throw new RuntimeException("Unsupported ColumnType");
				}
			}
		} else {
			for (int d=0; d<pDIM; d++) {
				pre[d] = raw1[d];
				pre[d+pDIM] = raw2[d];
			}
		}
	}

	@Override
	public void post(long[] pre, long[] post1, long[] post2) {
		final int pDIM = post1.length;
		if (types != null) {
			for (int d=0; d<pDIM; d++)
				if (types[d] instanceof LongColumn || types[d] instanceof IntColumn) {
					post1[d] = pre[d];
					post2[d] = pre[d+pDIM];
				} else {
					throw new RuntimeException("Unsupported ColumnType");
				}
		} else {
			for (int d=0; d<pDIM; d++) {
				post1[d] = pre[d];
				post2[d] = pre[d+pDIM];
			}
		}
	}

}
