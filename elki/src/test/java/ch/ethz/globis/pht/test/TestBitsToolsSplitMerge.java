/*
 * Copyright 2011-2013 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.util.BitTools;


public class TestBitsToolsSplitMerge {

	@Test
	public void testSplitMerge32() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			long l = Math.abs(rnd.nextInt());
			BitTools.split(l, a, 0, 1, 32);
			long l2 = BitTools.merge(a, 0, 1, 32);
			assertEquals(l, l2);
		}
	}

	@Test
	public void testSplitMerge63() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			long l = rnd.nextLong()>>>1;
			BitTools.split(l, a, 0, 1, 63);
			long l2 = BitTools.merge(a, 0, 1, 63);
			assertEquals(l, l2);
		}
	}

	@Test
	public void testSplitMerge64() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			long l = rnd.nextLong();
			BitTools.split(l, a, 0, 1, 64);
			long l2 = BitTools.merge(a, 0, 1, 64);
			assertEquals(l, l2);
		}
	}
	
	@Test
	public void testSplitMergeFloat() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			float f = rnd.nextFloat();
			long l = BitTools.toSortableLong(f);
			BitTools.split(l, a, 0, 1, 32);
			long l2 = BitTools.merge(a, 0, 1, 32);
			assertEquals(l, l2);
			float f2 = BitTools.toFloat(l2);
			assertEquals(f, f2, 0.0);
		}
	}

	
	@Test
	public void testSplitMergeDouble() {
		Random rnd = new Random();
		long[] a = new long[2];
		for (int i = 0; i < 1000; i++) {
			double d = rnd.nextDouble();
			long l = BitTools.toSortableLong(d);
			//System.err.println("ARGGHHH 63!!!!");
			BitTools.split(l, a, 0, 1, 63);
			long l2 = BitTools.merge(a, 0, 1, 63);
			assertEquals(l, l2);
			double d2 = BitTools.toDouble(l2);
			assertEquals(d, d2, 0.0);
		}
	}
}
