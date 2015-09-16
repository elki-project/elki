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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;
import org.zoodb.index.critbit.BitTools;

import ch.ethz.globis.pht.PhEntry;
import ch.ethz.globis.pht.PhTree;
import ch.ethz.globis.pht.PhTree.PhIterator;
import ch.ethz.globis.pht.test.util.TestSuper;
import ch.ethz.globis.pht.test.util.TestUtil;

public class TestValues extends TestSuper {

    public static <T> PhTree<T> createTree(int dim, int depth) {
    	return TestUtil.newTree(dim, depth);
    }
    
	@Test
	public void test3D() {
		smokeTest(10000, 3, 32, 0);
	}
	
	@Test
	public void test2D() {
		smokeTest(100000, 2, 32, 0);
	}
	
	@Test
	public void test2D_8() {
		smokeTest(100, 2, 32, 2);
	}
	
	@Test
	public void test2D_8_BugNP() {
//		for (int i = 0; i < 1000; i++) {
//			System.out.println("iii=" + i);
			smokeTest(20, 2, 32, 205);
//		}
	}
	
	private void smokeTest(int N, int DIM, int DEPTH, long SEED) { 
		Random R = new Random(SEED);
		PhTree<Integer> ind = createTree(DIM, DEPTH);
		long[][] keys = new long[N][DIM];
		for (int i = 0; i < N; i++) {
			for (int d = 0; d < DIM; d++) {
				keys[i][d] = R.nextInt(); //INT!
			}
			if (ind.contains(keys[i])) {
				i--;
				continue;
			}
			//build
			assertNull(ind.put(keys[i], Integer.valueOf(i)));
			//System.out.println("key=" + Bits.toBinary(keys[i], 64));
			//System.out.println(ind);
			assertTrue("i="+ i, ind.contains(keys[i]));
			assertEquals(i, (int)ind.get(keys[i]));
		}
		
		
		//first check
		for (int i = 0; i < N; i++) {
			assertTrue(ind.contains(keys[i]));
			assertEquals(i, (int)ind.get(keys[i]));
		}
		
		//update
		for (int i = 0; i < N; i++) {
			assertEquals(i, (int)ind.put(keys[i], Integer.valueOf(-i)));
			assertTrue(ind.contains(keys[i]));
			assertEquals(-i, (int)ind.get(keys[i]));
		}
		
		//check again
		for (int i = 0; i < N; i++) {
			assertTrue(ind.contains(keys[i]));
			assertEquals(-i, (int)ind.get(keys[i]));
		}
		
		//delete
		for (int i = 0; i < N; i++) {
			//System.out.println("Removing: " + Bits.toBinary(keys[i], 64));
			//System.out.println("Tree: \n" + ind);
			assertEquals(-i, (int)ind.remove(keys[i]));
			assertFalse(ind.contains(keys[i]));
			assertNull(ind.get(keys[i]));
		}
		
		assertEquals(0, ind.size());
	}
	
	@Test
	public void testBug1() {
		long[][] keys = {
				{0b00100000, 0b10110101},//   v=null
				{0b00100001, 0b10110100},//   v=null
		};
		
		PhTree<Integer> ind = createTree(2, 16);

		ind.put(keys[0], 0);
		assertNotNull(ind.get(keys[0]));
		assertEquals(0, (int)ind.get(keys[0]));

		ind.put(keys[1], 1);
		
		assertNotNull(ind.get(keys[0]));
		assertEquals(0, (int)ind.get(keys[0]));
		assertNotNull(ind.get(keys[1]));
		assertEquals(1, (int)ind.get(keys[1]));
	}
	
	/**
	 * Problem: the NI sub-structure doesn't copy key[] when inserting it internally.
	 */
	@Test
	public void testBug2() {
		int DIM = 6;
		double[][] keysD = {
				{0.730967787376657, 0.24053641567148587, 0.6374174253501083, 0.5504370051176339, 0.5975452777972018, 0.3332183994766498}, 
				{0.3851891847407185, 0.984841540199809, 0.8791825178724801, 0.9412491794821144, 0.27495396603548483, 0.12889715087377673}, 
				{0.14660165764651822, 0.023238122483889456, 0.5467397571984656, 0.9644868606768501, 0.10449068625097169, 0.6251463634655593}, 
				{0.4107961954910617, 0.7763122912749325, 0.990722785714783, 0.4872328470301428, 0.7462414053223305, 0.7331520701949938}, 
				{0.8172970714093244, 0.8388903500470183, 0.5266994346048661, 0.8993350116114935, 0.13393984058689223, 0.0830623982249149}, 
				{0.9785743401478403, 0.7223571191888487, 0.7150310138504744, 0.14322038530059678, 0.4629578184224229, 0.004485602182885184}, 
				{0.07149831487989411, 0.34842022979166454, 0.3387696535357536, 0.859356551354648, 0.9715469888517128, 0.8657458802140383}, 
				{0.6125811047098682, 0.17898798452881726, 0.21757041220968598, 0.8544871670422907, 0.009673497300974332, 0.6922930069529333}, 
				{0.7713129661706796, 0.7126874281456893, 0.2112353749298962, 0.7830924897671794, 0.945333238959629, 0.014236355103667941} 
		};
		
		PhTree<Object> ind = createTree(DIM, 64);

		Object V = new Object();
		long[] buf = new long[DIM];
		for (int i = 0; i < keysD.length; i++) {
			for (int j = 0; j < DIM; j++) {
				buf[j] = BitTools.toSortableLong(keysD[i][j]); 
			}
			assertNull(ind.put(buf, V));
		}
	}
	
	
	@Test
	public void testQuery() {
		int N = 1000;
		int DIM = 3;
		int DEPTH = 64; 
		Random R = new Random(0);
		PhTree<Integer> ind = createTree(DIM, DEPTH);
		long[][] keys = new long[N][DIM];
		for (int i = 0; i < N; i++) {
			for (int d = 0; d < DIM; d++) {
				keys[i][d] = R.nextInt(); //INT!
			}
			if (ind.contains(keys[i])) {
				i--;
				continue;
			}
			//build
			assertNull(ind.put(keys[i], Integer.valueOf(i)));
			assertTrue(ind.contains(keys[i]));
			assertEquals(i, (int)ind.get(keys[i]));
		}

		//extent query
		PhIterator<Integer> i1 = ind.queryExtent();
		int n = 0;
		while (i1.hasNext()) {
			PhEntry<Integer> e = i1.nextEntry();
			assertArrayEquals(keys[e.getValue()], e.getKey());
			n++;
		}
		assertEquals(N, n);
		
		//full range query
		long[] min = new long[DIM];
		long[] max = new long[DIM];
		Arrays.fill(min, Long.MIN_VALUE);
		Arrays.fill(max, Long.MAX_VALUE);
		i1 = ind.query(min, max);
		n = 0;
		while (i1.hasNext()) {
			PhEntry<Integer> e = i1.nextEntry();
			assertNotNull(e);
			assertArrayEquals(keys[e.getValue()], e.getKey());
			n++;
		}
		assertEquals(N, n);
		
		//spot queries
		for (int i = 0; i < N; i++) {
			i1 = ind.query(keys[i], keys[i]);
			assertTrue(i1.hasNext());
			PhEntry<Integer> e = i1.nextEntry();
			assertArrayEquals(keys[i], e.getKey());
			assertEquals(i, (int)e.getValue());
			assertFalse(i1.hasNext());
		}
		
		//delete
		for (int i = 0; i < N; i++) {
			//System.out.println("Removing: " + Bits.toBinary(keys[i], 64));
			//System.out.println("Tree: \n" + ind);
			assertEquals(i, (int)ind.remove(keys[i]));
			assertFalse(ind.contains(keys[i]));
			assertNull(ind.get(keys[i]));
		}
		
		assertEquals(0, ind.size());
	}
	

	@Test
	public void testQueryBug() {
		int DIM = 3;
		int DEPTH = 64; 
		PhTree<Integer> ind = createTree(DIM, DEPTH);
		long[][] keys = {
				{629649304, -1266264776, 99807007},
				{5955764, -1946737912, 39620447},
				{1086124775, -1609984092, 1227951724},
		};
		int N = keys.length;
		for (int i = 0; i < N; i++) {
			if (ind.contains(keys[i])) {
				i--;
				continue;
			}
			//build
			assertNull(ind.put(keys[i], Integer.valueOf(i)));
			assertTrue(ind.contains(keys[i]));
			assertEquals(i, (int)ind.get(keys[i]));
		}

		//extent query
		PhIterator<Integer> i1 = ind.queryExtent();
		int n = 0;
		while (i1.hasNext()) {
			PhEntry<Integer> e = i1.nextEntry();
			assertArrayEquals(keys[e.getValue()], e.getKey());
			n++;
		}
		assertEquals(N, n);
		
		//full range query
		long[] min = new long[DIM];
		long[] max = new long[DIM];
		Arrays.fill(min, Long.MIN_VALUE);
		Arrays.fill(max, Long.MAX_VALUE);
		i1 = ind.query(min, max);
		n = 0;
		while (i1.hasNext()) {
			PhEntry<Integer> e = i1.nextEntry();
			assertNotNull(e);
			assertArrayEquals(keys[e.getValue()], e.getKey());
			n++;
		}
		assertEquals(N, n);
		
		//spot queries
		for (int i = 0; i < N; i++) {
			i1 = ind.query(keys[i], keys[i]);
			assertTrue(i1.hasNext());
			PhEntry<Integer> e = i1.nextEntry();
			assertArrayEquals(keys[i], e.getKey());
			assertEquals(i, (int)e.getValue());
			assertFalse(i1.hasNext());
		}
		
		//delete
		for (int i = 0; i < N; i++) {
			//System.out.println("Removing: " + Bits.toBinary(keys[i], 64));
			//System.out.println("Tree: \n" + ind);
			assertEquals(i, (int)ind.remove(keys[i]));
			assertFalse(ind.contains(keys[i]));
			assertNull(ind.get(keys[i]));
		}
		
		assertEquals(0, ind.size());
	}
}
