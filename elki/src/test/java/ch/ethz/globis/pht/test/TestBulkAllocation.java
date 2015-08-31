/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.ethz.globis.pht.PhTreeHelper;
import ch.ethz.globis.pht.util.BitsLong;
import ch.ethz.globis.pht.util.Refs;

public class TestBulkAllocation {

	@Test
	public void testSizeCalcOld() {
		PhTreeHelper.setAllocBatchSize(0);
		int[] results = {0,1,2,3};
		for (int i = 0; i < results.length; i++) {
			int size = BitsLong.calcArraySize(i*64);
			assertEquals(results[i], size);
		}
	}
	
	@Test
	public void testSizeCalc1() {
		PhTreeHelper.setAllocBatchSize(1);
		int[] results = {0,1,2,3};
		for (int i = 0; i < results.length; i++) {
			int size = BitsLong.calcArraySize(i*64);
			assertEquals("i=" + i, results[i], size);
		}
	}
	
	@Test
	public void testSizeCalc2() {
		PhTreeHelper.setAllocBatchSize(2);
		int[] results = {0,2,2,4,4,6,6};
		for (int i = 0; i < results.length; i++) {
			int size = BitsLong.calcArraySize(i*64);
			assertEquals(results[i], size);
		}
	}
	
	@Test
	public void testSizeCalc3() {
		PhTreeHelper.setAllocBatchSize(3);
		int[] results = {0,3,3,3,6,6,6,9,9,9,12,12,12};
		for (int i = 0; i < results.length; i++) {
			int size = BitsLong.calcArraySize(i*64);
			assertEquals(results[i], size);
		}
	}
	
	@Test
	public void testSizeCalcRefOld() {
		PhTreeHelper.setAllocBatchSize(0);
		int[] results = {0,2,2,4,4,6,6};
		for (int i = 0; i < results.length; i++) {
			int size = Refs.calcArraySize(i);
			assertEquals(results[i], size);
		}
	}
	
	@Test
	public void testSizeCalcRef1() {
		PhTreeHelper.setAllocBatchSize(1);
		int[] results = {0,2,2,4,4,6,6};
		for (int i = 0; i < results.length; i++) {
			int size = Refs.calcArraySize(i);
			assertEquals("i=" + i, results[i], size);
		}
	}
	
	@Test
	public void testSizeCalcRef2() {
		PhTreeHelper.setAllocBatchSize(2);
		int[] results = {0,4,4,4,4,8,8,8,8,12,12,12,12};
		for (int i = 0; i < results.length; i++) {
			int size = Refs.calcArraySize(i);
			assertEquals(results[i], size);
		}
	}
	
	@Test
	public void testSizeCalcRef3() {
		PhTreeHelper.setAllocBatchSize(3);
		int[] results = {0,6,6,6,6,6,6,12,12,12,12,12,12,18,18};
		for (int i = 0; i < results.length; i++) {
			int size = Refs.calcArraySize(i);
			assertEquals(results[i], size);
		}
	}
	
}
