/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.PhTree;
import ch.ethz.globis.pht.test.util.TestUtil;
import ch.ethz.globis.pht.util.Bits;

public class TestIndexPrint {

	private PhTree<long[]> create(int dim, int depth) {
		return TestUtil.newTree(dim, depth);
	}
	
	@Test
	public void testPrint() {
		final int N = 1000;
		final Random R = new Random(0);
		
		PhTree<long[]> ind = create(3, 32);
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt(), R.nextInt()};
			ind.put(v, v);
		}

		ArrayList<String> keys = new ArrayList<>();
		Iterator<long[]> it = ind.queryExtent();
		while (it.hasNext()) {
			long[] v = it.next();
			keys.add(Bits.toBinary(v, ind.getDEPTH()));
		}
	
		String out = ind.toStringPlain();
		for (String key: keys) {
			assertTrue("key=" + key, out.contains(key));
		}
	}

	@Test
	public void testPrintBug() {
		final int N = 5;
		final Random R = new Random(4);
		
		PhTree<long[]> ind = create(3, 32);
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt(), R.nextInt()};
			ind.put(v, v);
		}

		ArrayList<String> keys = new ArrayList<>();
		Iterator<long[]> it = ind.queryExtent();
		while (it.hasNext()) {
			long[] v = it.next();
			keys.add(Bits.toBinary(v, ind.getDEPTH()));
		}
	
		String out = ind.toStringPlain();
		for (String key: keys) {
			assertTrue("key=" + key, out.contains(key));
		}
	}

}
