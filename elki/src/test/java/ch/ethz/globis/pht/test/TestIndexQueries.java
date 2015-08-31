/*
 * Copyright 2011-2015 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.pht.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.PhTree;
import ch.ethz.globis.pht.PhTree.PhIterator;
import ch.ethz.globis.pht.test.util.TestSuper;
import ch.ethz.globis.pht.test.util.TestUtil;
import ch.ethz.globis.pht.util.BitTools;
import ch.ethz.globis.pht.util.Bits;

public class TestIndexQueries extends TestSuper {

	private static final Object O = new Object();
	
	private PhTree<long[]> createNV(int dim, int depth) {
		return TestUtil.newTree(dim, depth);
	}

	private PhTree<Object> create(int dim, int depth) {
		return TestUtil.newTree(dim, depth);
	}

	@Test
	public void testQuerySingle() {
		final int N = 30000;

		PhTree<long[]> ind = createNV(1, 16);
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{i};
			ind.put(v, v);
		}

		Iterator<long[]> it;
		it = ind.query(new long[]{N+1}, new long[]{250});
		assertFalse(it.hasNext());
		
		it = ind.query(new long[]{0}, new long[]{N-1});
		for (int i = 0; i < N; i++) {
			long[] a = it.next();
			assertEquals(i, a[0]);
		}
		assertFalse(it.hasNext());
		
		for (int i = 0; i < N; i++) {
			it = ind.query(new long[]{i}, new long[]{i});
			long[] a = it.next();
			assertEquals(i, a[0]);
//			if (it.hasNext()) {
//				System.out.println("i=" + i + "  v=" + Bits.toBinary(a, 8));
//				while (it.hasNext()) {
//					a = it.next();
//					System.out.println("i=" + i + "  v=" + Bits.toBinary(a, 8));
//				}
//			}
			assertFalse("i=" + i, it.hasNext());
		}		
	}
	
	@Test
	public void testQuerySingle_Bug1() {
		final int N = 20;

		PhTree<long[]> ind = createNV(1, 16);
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{i};
			ind.put(v, v);
		}
		
		Iterator<long[]> it = ind.query(new long[]{N+1}, new long[]{250});
		//This used to fail:
		assertFalse(it.hasNext());
	}
	
	/**
	 * Queries just hang ...
	 */
	@Test
	public void testQuerySingleRandomND_Bug1() {
		final int DIM = 5;
		final int N = 4;
		final int[][] R = {
				{1119861723, 920690087, 884700943, 210555024, 328969292}, 
				{1030398242, 594888813, 297670466, 504451821, 610952588}, 
				{2119407664, 1802131665, 808099030, 1524428497, 1920740629}, 
				{1891815476, 1332619371, 1095443318, 286253201, 336482252}
				}; 
		
		for (int d = 0; d < DIM; d++) {
			PhTree<long[]> ind = createNV(DIM, 32);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int dd = 0; dd < DIM; dd++) {
					v[dd] = Math.abs(R[i][dd]);
				}
				v[d] = i;
				ind.put(v, v);
			}
			
			//check empty result
			int MAX = Integer.MAX_VALUE;
			Iterator<long[]> it = ind.query(new long[]{N+1, 0, 0, 0, 0}, 
					new long[]{250, MAX, MAX, MAX, MAX});
			assertFalse(it.hasNext());
		}
	}
	
	
	
	@Test
	public void testQueryFullExtent64() {
		final int DIM = 5;
		final int N = 10000;
		
		for (int d = 0; d < DIM; d++) {
			PhTree<long[]> ind = createNV(DIM, 64);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				v[d] = i;
//				System.out.println("d="+ d + "  i=" + i + "  v=" + PhTree.toBinary(v, 16));
				ind.put(v, v);
			}
			
			//check full result
			Iterator<long[]> it = ind.queryExtent();
			for (int i = 0; i < N; i++) {
				long[] v = it.next();
				assertEquals(i, v[d]);
			}
			assertFalse(it.hasNext());
		}
	}

	
	
	@Test
	public void testInsertionFailure() {
		final int DEPTH = 32;
		final int DIM = 1;
		final int N = 6;
		final Random R = new Random(0);
		
		long[][] valsArr = new long[N][];

		PhTree<long[]> ind = createNV(DIM, DEPTH);
		for (int i = 0; i < N; i++) {
			long[] v = new long[DIM];
			v[0] = Math.abs(R.nextInt());
			//System.out.println("i=" + i + "  v=" + Bits.toBinary(v, DEPTH));
			ind.put(v, v);
			valsArr[i] = v;
		}

		//check full result with exists()
		for (int i = 0; i < N; i++) {
			long[] v = valsArr[i];
			assertTrue("i= " + i + "  v[d]=" + v[0] + " / " + Bits.toBinary(v[0]), ind.contains(v));
		}
	}

	
	@Test
	public void testEmptyStackFailure() {
		final int DEPTH = 32;
		final int DIM = 2;
		final int N = 60000;
		final Random R = new Random(0);
		
		HashSet<Long> valsSet = new HashSet<Long>();

		PhTree<long[]> ind = createNV(DIM, DEPTH);
		for (int i = 0; i < N; i++) {
			long[] v = new long[DIM];
			v[0] = Math.abs(R.nextInt());
			//System.out.println("d="+ d + "  i=" + i + "  v=" + PhTree.toBinary(v, 16));
			if (valsSet.contains((Long)v[0])) {
				i--;
				continue;
			}
			ind.put(v, v);
			valsSet.add((Long)v[0]);
		}

		//check full result with extent
		Iterator<long[]> it = ind.queryExtent();
		int n = 0;
		for (int i = 0; i < N; i++) {
			long[] v = it.next();
			//System.out.println("i=" + i + "  v=" + Bits.toBinary(v, DEPTH));
			//assertTrue("i= " + i + "  v[d]=" + v[d], valsSet.contains((Long)v[d]));
			valsSet.remove((Long)v[0]);
		}
		assertEquals(valsSet.size(), n);
		assertFalse(it.hasNext());
	}

	
	@Test
	public void testQueryFullExtent64Random() {
		final int DIM = 1;
		final int N = 10000;
		final Random R = new Random(0);
		
		HashSet<Long> valsSet = new HashSet<Long>();
		long[][] valsArr = new long[N][];
		for (int d = 0; d < DIM; d++) {
			PhTree<long[]> ind = createNV(DIM, 64);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				v[d] = R.nextLong(); 
//				System.out.println("d="+ d + "  i=" + i + "  v=" + PhTree.toBinary(v, 16));
				ind.put(v, v);
				valsSet.add((Long)v[d]);
				valsArr[i] = v;
			}
			
			//check full result with exists()
			for (int i = 0; i < N; i++) {
				long[] v = valsArr[i];
				assertTrue("i= " + i + "  d=" + d + "   v[d]=" + v[d] + " / " + Bits.toBinary(v[d]), ind.contains(v));
			}
			
			//check full result with extent
			Iterator<long[]> it = ind.queryExtent();
			for (int i = 0; i < N; i++) {
				long[] v = it.next();
				//assertTrue("i= " + i + "  v[d]=" + v[d], valsSet.contains((Long)v[d]));
				valsSet.remove((Long)v[d]);
			}
			assertFalse(it.hasNext());
//			System.out.println("Remaining: "+ valsSet.toString());
		}
	}

	
	
	@Test
	public void testQueryFullExtent() {
		final int DIM = 5;
		final int N = 10000;
		
		for (int d = 0; d < DIM; d++) {
			PhTree<long[]> ind = createNV(DIM, 16);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				v[d] = i;
//				System.out.println("d="+ d + "  i=" + i + "  v=" + PhTree.toBinary(v, 16));
				ind.put(v, v);
			}
			
			//check full result
			Iterator<long[]> it = ind.queryExtent();
			for (int i = 0; i < N; i++) {
				long[] v = it.next();
				assertEquals(i, v[d]);
			}
			assertFalse(it.hasNext());
		}
	}

	
	
	@Test
	public void testQueryND() {
		final int DIM = 5;
		final int N = 100;
		final int DEPTH = 16;
		
		for (int d = 0; d < DIM; d++) {
			PhTree<long[]> ind = createNV(DIM, DEPTH);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = (i >> (j*2)) & 0x03;  //mask off sign bits
				}
				assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
			}
			
			//check empty result
			Iterator<long[]> it;
			int n = 0;
			it = ind.query(new long[]{0, 0, 0, 0, 0}, new long[]{0, 0, 0, 0, 0});
			while(it.hasNext()) {
				long[] v = it.next();
				assertEquals(v[0], 0);
				n++;
			}
			assertFalse(it.hasNext());

			//lies outside value range...
//			it = ind.query(50, 500, 50, 500, 50, 500, 50, 500, 50, 500);
//			assertFalse(it.hasNext());

			//check full result
			//it = ind.query(0, 50, 0, 50, 0, 50, 0, 50, 0, 50);
			it = ind.query(new long[]{0, 0, 0, 0, 0}, new long[]{50, 50, 50, 50, 50});
			for (int i = 0; i < N; i++) {
				long[] v = it.next();
				assertNotNull(v);
			}
			assertFalse(it.hasNext());

			//check partial result
			n = 0;
			//it = ind.query(0, 0, 0, 50, 0, 50, 0, 50, 0, 50);
			it = ind.query(new long[]{0, 0, 0, 0, 0}, new long[]{0, 50, 50, 50, 50});
			while (it.hasNext()) {
				n++;
				long[] v = it.next();
				assertEquals(0, v[0]);
			}
			assertEquals(25, n);

			n = 0;
			//it = ind.query(1, 1, 0, 50, 0, 50, 0, 50, 0, 50);
			it = ind.query(new long[]{1, 0, 0, 0, 0}, new long[]{1, 50, 50, 50, 50});
			while (it.hasNext()) {
				n++;
				long[] v = it.next();
				assertEquals(1, v[0]);
			}
			assertEquals(25, n);
			
			
			n = 0;
			//it = ind.query(3, 3, 0, 50, 0, 50, 0, 50, 0, 50);
			it = ind.query(new long[]{3, 0, 0, 0, 0}, new long[]{3, 50, 50, 50, 50});
			while (it.hasNext()) {
				n++;
				long[] v = it.next();
				assertEquals(3, v[0]);
			}
			assertEquals(25, n);
			
			
		}
	}
	
	
	/**
	 * Testing only positive 64 bit values (effectively 63 bit).
	 */
	@Test
	public void testQueryND64RandomPos() {
		final int DIM = 5;
		final int N = 100;
		final Random R = new Random();
		
		for (int d = 0; d < DIM; d++) {
			PhTree<long[]> ind = createNV(DIM, 64);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = Math.abs(R.nextLong());
				}
				ind.put(v, v);
			}
			
			//check empty result
			Iterator<long[]> it;
			//it = ind.query(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			it = ind.query(new long[]{0, 0, 0, 0, 0}, new long[]{0, 0, 0, 0, 0});
			assertFalse(it.hasNext());

			//check full result
//			it = ind.query(0, Long.MAX_VALUE, 
//					0, Long.MAX_VALUE, 
//					0, Long.MAX_VALUE, 
//					0, Long.MAX_VALUE, 
//					0, Long.MAX_VALUE);
			Long M = Long.MAX_VALUE;
			it = ind.query(new long[]{0, 0, 0, 0, 0}, new long[]{M, M, M, M, M});
			for (int i = 0; i < N; i++) {
				it.next();
				//System.out.println("v=" + Bits.toBinary(v, 64));
			}
			assertFalse(it.hasNext());

			//check partial result
			int n = 0;
//			it = ind.query(0, Long.MAX_VALUE, 
//					0, Long.MAX_VALUE,
//					0, Long.MAX_VALUE,
//					0, Long.MAX_VALUE,
//					0, Long.MAX_VALUE);
			it = ind.query(new long[]{0, 0, 0, 0, 0}, new long[]{M, M, M, M, M});
			while (it.hasNext()) {
				n++;
				it.next();
			}
			assertTrue("n=" + n, n > N/10.);
		}
	}
	
	
	@Test
	public void testQueryND64Random() {
		final int DIM = 5;
		final int N = 1000;
		final Random R = new Random();
		
		for (int d = 0; d < DIM; d++) {
			PhTree<long[]> ind = createNV(DIM, 64);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = R.nextLong();
				}
				ind.put(v, v);
			}
			
			long[] NULL = new long[DIM];
			long[] MIN = new long[DIM];
			long[] MAX = new long[DIM];
			for (int i = 0; i < DIM; i++) {
				MIN[i] = Long.MIN_VALUE;
				MAX[i] = Long.MAX_VALUE;
			}
			
			//check empty result
			Iterator<long[]> it;
			it = ind.query(NULL, NULL);
			assertFalse(it.hasNext());

			//check full result
			it = ind.query(MIN, MAX);
			for (int i = 0; i < N; i++) {
				long[] v = it.next();
//				System.out.println("v=" + Bits.toBinary(v, 64));
				assertNotNull(v);
			}
			assertFalse(it.hasNext());

			//check partial result
			int nTotal = 0;
			int n = 0;
			it = ind.query(NULL, MAX);
			while (it.hasNext()) {
				n++;
				nTotal++;
				it.next();
			}
			assertTrue("n=" + n, n > 1);

			n = 0;
			it = ind.query(MIN, NULL);
			while (it.hasNext()) {
				n++;
				nTotal++;
				it.next();
			}
			assertTrue("n=" + n, n > 1);
			//In average this is N/(2^DIM)
			assertTrue("nTotal=" + nTotal, nTotal > 10);
		}
	}
	
	
	@Test
	public void testPrecision() {
		PhTree<long[]> ind = createNV(2, 8);
		ind.put(new long[]{2,2}, new long[]{2,2});
		ind.put(new long[]{1,1}, new long[]{1,1});
		ind.put(new long[]{1,3}, new long[]{1,3});
		ind.put(new long[]{3,1}, new long[]{3,1});
		
		Iterator<long[]> it;
		
		//check point with hit
		it = ind.query(new long[]{2, 2}, new long[]{2, 2});
		long[] v = it.next();
		check(8, v, 2, 2);
		assertFalse(it.hasNext());

		it = ind.query(new long[]{2, 2}, new long[]{20, 20});
		v = it.next();
		check(8, v, 2, 2);
		assertFalse(it.hasNext());

		//check point without hit
		it = ind.query(new long[]{3, 3}, new long[]{3, 3});
		assertFalse(it.hasNext());

		//check point without hit
		it = ind.query(new long[]{3, 3}, new long[]{30, 30});
		assertFalse(it.hasNext());

		//check point without hit
		it = ind.query(new long[]{3, 3}, new long[]{3, 3});
		assertFalse(it.hasNext());

		//check full result
		it = ind.query(new long[]{1, 1}, new long[]{3, 3});
		for (int i = 0; i < 4; i++) {
			v = it.next();
			//System.out.println("v=" + Bits.toBinary(v, 64));
			assertNotNull(v);
		}
		assertFalse(it.hasNext());
	}
	
	@Test
	public void testPrecisionDouble() {
		PhTree<long[]> ind = createNV(2, 64);
		ind.put( d2l(2,2), d2l(2,2) );
		assertTrue(ind.contains(d2l(2,2)));
		ind.put( d2l(1,1), d2l(1,1) );
		assertTrue(ind.contains(d2l(2,2)));
		assertTrue(ind.contains(d2l(1,1)));
		ind.put( d2l(1,3), d2l(1,3) );
		assertTrue(ind.contains(d2l(2,2)));
		assertTrue(ind.contains(d2l(1,1)));
		assertTrue(ind.contains(d2l(1,3)));
		ind.put( d2l(3,1), d2l(3,1) );
		assertTrue(ind.contains(d2l(2,2)));
		assertTrue(ind.contains(d2l(1,1)));
		assertTrue(ind.contains(d2l(1,3)));
		assertTrue(ind.contains(d2l(3,1)));
		
		Iterator<long[]> it;
		
		//check point with hit
		it = ind.query( d2l(2, 2), d2l(2, 2) );
		long[] v = it.next();
		check(64, v,  d2l(2, 2) );
		assertFalse(it.hasNext());

		it = ind.query( d2l(2, 2), d2l(20, 20) );
		v = it.next();
		check(64, v,  d2l(2, 2) );
		assertFalse(it.hasNext());

		//check point without hit
		it = ind.query( d2l(3, 3), d2l(3, 3) );
		assertFalse(it.hasNext());

		//check point without hit
		it = ind.query( d2l(3, 3), d2l(30, 30) );
		assertFalse(it.hasNext());

		//check full result
		it = ind.query( d2l(1, 1), d2l(3, 3) );
		for (int i = 0; i < 4; i++) {
			v = it.next();
			//System.out.println("v=" + Bits.toBinary(v, 64));
			assertNotNull(v);
		}
		assertFalse(it.hasNext());
	}
		
	@Test
	public void testPrecisionDoubleNeg() {
		PhTree<long[]> ind = createNV(2, 64);
		ind.put( d2l(2,2), d2l(2,2) );
		ind.put( d2l(-1,-1), d2l(-1,-1) );
		ind.put( d2l(-1,3), d2l(-1,3) );
		ind.put( d2l(3,-1), d2l(3,-1) );
		
		Iterator<long[]> it;
		
		//check point with hit
		it = ind.query( d2l(-1, -1), d2l(-1, -1) );
		long[] v = it.next();
		check(64, v,  d2l(-1, -1) );
		assertFalse(it.hasNext());

		it = ind.query( d2l(-2, -2), d2l(-0.5, -0.5) );
		v = it.next();
		check(64, v,  d2l(-1, -1) );
		assertFalse(it.hasNext());

		//check point without hit
		it = ind.query( d2l(-3, -3), d2l(-3, -3) );
		assertFalse(it.hasNext());

		//check point without hit
		it = ind.query( d2l(-0.5, -0.5), d2l(0, 0) );
		assertFalse(it.hasNext());

		//check partial result
		it = ind.query( d2l(-1, -1), d2l(3, 1) );
		for (int i = 0; i < 2; i++) {
			v = it.next();
			//System.out.println("v=" + Bits.toBinary(v, 64));
			assertNotNull(v);
		}
		assertFalse(it.hasNext());

		//check full result
		it = ind.query( d2l(-1, -1), d2l(3, 3) );
		for (int i = 0; i < 4; i++) {
			v = it.next();
			//System.out.println("v=" + Bits.toBinary(v, 64));
			assertNotNull(v);
		}
		assertFalse(it.hasNext());
	}
		
	private long[] d2l(double ...d) {
		long[] v = new long[d.length];
		for (int i = 0; i < d.length; i++) {
			v[i] = BitTools.toSortableLong(d[i]);
		}
		return v;
	}

	private void check(int DEPTH, long[] t, long ... ints) {
		for (int i = 0; i < ints.length; i++) {
			assertEquals("i=" + i + " | " + Bits.toBinary(ints, DEPTH) + " / " + 
					Bits.toBinary(t, DEPTH), ints[i], t[i]);
		}
	}

	@Test
	public void testQueryHighD() {
		final int MAX_DIM = 31;
		final int N = 2000;
		final int DEPTH = 64;
		final long mask = ~(1L<<(DEPTH-1));  //only positive numbers!
		Random R = new Random(0);
		
		for (int DIM = 3; DIM <= MAX_DIM; DIM++) {
			//System.out.println("d="+ DIM);
			PhTree<long[]> ind = createNV(DIM, DEPTH);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = R.nextLong() & mask;
				}
				assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
			}
			
			//check empty result
			Iterator<long[]> it;
			int n = 0;
			long[] min = new long[DIM];
			long[] max = new long[DIM];
			it = ind.query(min, max);
			while(it.hasNext()) {
				long[] v = it.next();
				assertEquals(v[0], 0);
				n++;
			}
			assertFalse(it.hasNext());
			assertEquals(0, n);

			//check full result
			for (int i = 0; i < DIM; i++) {
				min[i] = 0;
				max[i] = Long.MAX_VALUE & mask;
			}
			it = ind.query(min, max);
			for (int i = 0; i < N; i++) {
				n++;
				long[] v = it.next();
				assertNotNull(v);
			}
			assertFalse(it.hasNext());

			//check partial result
			int n2 = 0;
			//0, 0, 0, 50, 0, 50, 0, 50, 0, 50
			max[0] = 0;
			it = ind.query(min, max);
			while (it.hasNext()) {
				n2++;
				long[] v = it.next();
				assertEquals(0, v[0]);
			}
			assertTrue(n2 < n);
		}
	}
	
	
	@Test
	public void testQueryHighD64() {
		final int MAX_DIM = 31;
		final int N = 1000;
		final int DEPTH = 64;
		final long mask = Long.MAX_VALUE;
		Random R = new Random(0);
		
		for (int DIM = 3; DIM <= MAX_DIM; DIM++) {
			//System.out.println("d="+ DIM);
			PhTree<long[]> ind = createNV(DIM, DEPTH);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = R.nextLong() & mask;
				}
				assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
			}
			
			//check empty result
			Iterator<long[]> it;
			int n = 0;
			long[] min = new long[DIM];
			long[] max = new long[DIM];
			it = ind.query(min, max);
			while(it.hasNext()) {
				long[] v = it.next();
				assertEquals(v[0], 0);
				n++;
			}
			assertFalse(it.hasNext());
			assertEquals(0, n);

			//check full result
			for (int i = 0; i < DIM; i++) {
				min[i] = 0;
				max[i] = Long.MAX_VALUE & mask;
			}
			it = ind.query(min, max);
			for (int i = 0; i < N; i++) {
				n++;
				long[] v = it.next();
				assertNotNull(v);
			}
			assertFalse(it.hasNext());

			//check partial result
			int n2 = 0;
			//0, 0, 0, 50, 0, 50, 0, 50, 0, 50
			max[0] = 0;
			it = ind.query(min, max);
			while (it.hasNext()) {
				n2++;
				long[] v = it.next();
				assertEquals(0, v[0]);
			}
			assertTrue(n2 < n);
		}
	}
	
	
	@Test
	public void testQueryHighD64Neg() {
		final int MAX_DIM = 31;
		final int N = 1000;
		final int DEPTH = 64;
		Random R = new Random(0);
		
		for (int DIM = 3; DIM <= MAX_DIM; DIM++) {
			//System.out.println("d="+ DIM);
			PhTree<long[]> ind = createNV(DIM, DEPTH);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = R.nextLong();
				}
				assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
				if (v[0] == 0) System.out.println("TIQ-0: " + Arrays.toString(v));
			}
			
			//check empty result
			Iterator<long[]> it;
			int n = 0;
			long[] min = new long[DIM];
			long[] max = new long[DIM];
			it = ind.query(min, max);
			while(it.hasNext()) {
				long[] v = it.next();
				assertEquals(v[0], 0);
				n++;
				System.out.println("TIQ-1: " + Arrays.toString(v));
			}
			assertFalse(it.hasNext());
			assertEquals(0, n);

			//check full result
			for (int i = 0; i < DIM; i++) {
				min[i] = Long.MIN_VALUE;
				max[i] = Long.MAX_VALUE;
			}
			it = ind.query(min, max);
			for (int i = 0; i < N; i++) {
				n++;
				assertTrue("DIM=" + DIM + " i=" + i, it.hasNext());
				long[] v = it.next();
				assertNotNull(v);
			}
			assertFalse(it.hasNext());
			assertEquals(N, n);

			//check partial result
			int n2 = 0;
			//0, 0, ...
			min[0] = 0;
			max[0] = 0;
			it = ind.query(min, max);
			while (it.hasNext()) {
				n2++;
				long[] v = it.next();
				assertEquals(0, v[0]);
			}
			assertTrue(n2 < n);
		}
	}
	
	
	@Test
	public void testQueryHighD64Neg_6_1() {
		final int N = 1;
		final int DEPTH = 64;
		final int DIM = 6;
		Random R = new Random(0);
		
		PhTree<long[]> ind = createNV(DIM, DEPTH);
		long[] v = new long[DIM];
		for (int j = 0; j < DIM; j++) {
			v[j] = R.nextLong();
		}
		assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));

		
		//check empty result
		Iterator<long[]> it;
		int n = 0;
		long[] min = new long[DIM];
		long[] max = new long[DIM];

		//check full result
		for (int i = 0; i < DIM; i++) {
			min[i] = Long.MIN_VALUE;
			max[i] = Long.MAX_VALUE;
		}
		it = ind.query(min, max);
		for (int i = 0; i < N; i++) {
			n++;
			assertTrue("DIM=" + DIM + " i=" + i, it.hasNext());
			long[] v2 = it.next();
			assertNotNull(v2);
		}
		assertFalse(it.hasNext());
		assertEquals(N, n);
	}
	
	
	@Test
	public void testBug64Neg_1() {
		final int N = 1;
		final int DEPTH = 64;
		Random R = new Random(0);

		final int DIM = 3;
		
		//System.out.println("d="+ DIM);
		PhTree<long[]> ind = createNV(DIM, DEPTH);
		for (int i = 0; i < N; i++) {
			long[] v = new long[DIM];
			for (int j = 0; j < DIM; j++) {
				v[j] = R.nextLong();
			}
			assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
		}

		//check empty result
		Iterator<long[]> it;
		long[] min = new long[DIM];
		long[] max = new long[DIM];

		//check full result
		for (int i = 0; i < DIM; i++) {
			min[i] = Long.MIN_VALUE;
			max[i] = Long.MAX_VALUE;
		}
		it = ind.query(min, max);
		for (int i = 0; i < N; i++) {
			assertTrue("DIM=" + DIM + " i=" + i, it.hasNext());
			long[] v = it.next();
			assertNotNull(v);
		}
		assertFalse(it.hasNext());
	}
	

	@Test
	public void testBug64Neg_2() {
		final int DIM = 10;
		final int N = 1000;
		final int DEPTH = 64;
		Random R = new Random(0);
		
		//System.out.println("d="+ DIM);
		PhTree<Object> ind = create(DIM, DEPTH);
		for (int i = 0; i < N; i++) {
			long[] v = new long[DIM];
			for (int j = 0; j < DIM; j++) {
				v[j] = R.nextLong();
			}
			assertEquals(Bits.toBinary(v, DEPTH), null, ind.put(v, O));
		}

		//check empty result
		PhIterator<Object> it;
		int n = 0;
		long[] min = new long[DIM];
		long[] max = new long[DIM];
		it = ind.query(min, max);
		while(it.hasNext()) {
			long[] v = it.nextKey();
			assertEquals(v[0], 0);
			n++;
		}
		assertFalse(it.hasNext());
		assertEquals(0, n);

//		List<?> list = ind.queryAll(min, max);
//		assertEquals(0, list.size());
	}
	
	
	/**
	 * This test the BLHC query optimisation, which only kicks in for k>=6.
	 */
	@Test
	public void testQuery7D() {
		final int MAX_DIM = 7;
		final int N = 500;
		final int DEPTH = 64;
		final long mask = ~(1L<<(DEPTH - 1));
		Random R = new Random(0);
		
		for (int DIM = 7; DIM <= MAX_DIM; DIM++) {
			//System.out.println("d="+ DIM);
			PhTree<long[]> ind = createNV(DIM, DEPTH);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = R.nextLong() & mask;
				}
				assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
			}
			
			long[] min = new long[DIM];
			long[] max = new long[DIM];
			for (int i = 0; i < DIM; i++) {
				min[i] = 0;
				max[i] = Long.MAX_VALUE & mask;
			}

			//check partial result
			int n2 = 0;
			//0, 0, 0, 50, 0, 50, 0, 50, 0, 50
			max[0] = 0;
			Iterator<long[]> it = ind.query(min, max);
			while (it.hasNext()) {
				n2++;
				long[] v = it.next();
				assertEquals(0, v[0]);
			}
			assertTrue(n2 < N);
		}
//		int nRep=1;
//		System.out.println(
//				"x1=" + TestPerf.STAT_X1/nRep + "/" + TestPerf.STAT_X1a + "/" + TestPerf.STAT_X1b +
//				"  x2 = " + TestPerf.STAT_X2/nRep + "/" + TestPerf.STAT_X2f2/nRep + 
//				" x3 = " + TestPerf.STAT_X3/nRep + "/" + TestPerf.STAT_X3f/nRep);
	}
	
	
	/**
	 * This used to return an empty result set.
	 */
	@Test
	public void testBugKeyNotFound() {
		long[][] data = {
				{23, 35, 47, 85, 65, },
				{39, 62, 7, 93, 96, },

				{13, 99, 94, 31, 90, },
				{47, 94, 89, 49, 68, },
				
				{26, 38, 93, 16, 7, },
				{57, 14, 93, 3, 42, },

				
				{88, 14, 42, 76, 86, },
				{86, 52, 28, 90, 98, },
				
				{69, 74, 20, 58, 73, },
				{79, 93, 58, 12, 73, },
				
//				{15, 4, 34, 13, 9, },
		};
		
		final int DIM = data[0].length;
		final int N = data.length;
		PhTree<Object> ind = TestUtil.newTree(DIM, 16);
		for (int i = 0; i < N; i++) {
			ind.put(data[i], null);
		}

		ind.put(new long[]{15, 4, 34, 13, 9, }, null);

		//		long[] min = {8, -23, -1, -16, -18};
//		long[] max = {55, 23, 45, 30, 28};
		long[] min = {15, 4, 34, 13, 9};
		long[] max = {15, 4, 34, 13, 9};
		PhIterator<?> it = ind.query(min, max);
		assertTrue(it.hasNext());
	}

}
