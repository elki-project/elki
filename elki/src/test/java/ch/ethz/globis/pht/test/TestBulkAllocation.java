package ch.ethz.globis.pht.test;

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
