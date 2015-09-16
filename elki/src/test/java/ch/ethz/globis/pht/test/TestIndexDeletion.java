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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.PhTree;
import ch.ethz.globis.pht.test.util.TestSuper;
import ch.ethz.globis.pht.test.util.TestUtil;
import ch.ethz.globis.pht.util.Bits;

public class TestIndexDeletion extends TestSuper {

	private PhTree<long[]> create(int dim, int depth) {
		return TestUtil.newTree(dim, depth);
	}
	
	@Test
	public void testDeleteSingle() {
		PhTree<long[]> ind = create(3, 32);
		Random R = new Random(0);
		for (int i = 0; i < 100000; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt(), R.nextInt()};
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			ind.put(v, v);
			assertTrue(ind.contains(v));
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testDeleteMulti2D() {
		PhTree<long[]> ind = create(2, 16);
		Random R = new Random(1);
		int N = 200000;
		long[][] vals = new long[N][];
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt()};
			//System.out.println("i=" + i + "  vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			vals[i] = v;
			if (ind.put(v, v) != null) {
				//catch duplicates, maybe in future we should just skip them
				i--;
				continue;
			}
		}
		
		for (int i = 0; i < N; i++) {
			long[] v = vals[i];
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testDeleteMulti3D() {
		PhTree<long[]> ind = create(3, 32);
		Random R = new Random(0);
		int N = 200000;
		long[][] vals = new long[N][];
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt(), R.nextInt()};
			//System.out.println("vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			vals[i] = v;
			if (ind.put(v, v) != null) {
				//catch duplicates, maybe in future we should just skip them
				i--;
				continue;
			}
		}
		
		for (int i = 0; i < N; i++) {
			long[] v = vals[i];
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testDeleteMultiMulti() {
		for (int s = 0; s < 5000; s++) {
            //this is needed to clean up the trees that were created in a call to checkSeed
            TestUtil.beforeTest();
			checkSeed(s);
		}
	}
	
	private void checkSeed(int s) {
		PhTree<long[]> ind = create(3, 32);
		Random R = new Random(s);
		int N = 100;
		long[][] vals = new long[N][];
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt(), R.nextInt()};
			//System.out.println("vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			vals[i] = v;
			if (ind.put(v, v) != null) {
				//catch duplicates, maybe in future we should just skip them
				fail();
			}
		}
		
		for (int i = 0; i < N; i++) {
			long[] v = vals[i];
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			assertNotNull("s=" + s + " i="+ i, ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testBug1() {
		PhTree<long[]> ind = create(2, 32);
		ArrayList<long[]> vA = new ArrayList<long[]>();
		vA.add(new long[]{1157023572, 396984392});//, 349120689});
		vA.add(new long[]{1291704192, 862408176});//, 837789372});
		
		for (long[] v: vA) {
			//System.out.println("v: " + Bits.toBinary(v, 32));
			assertNull(ind.put(v, v));
		}

		for (long[] v: vA) {
			//ind.printTree();
			assertNotNull(ind.remove(v));
		}
	}

	@Test
	public void testBug2() {
		PhTree<long[]> ind = create(2, 32);
		Random R = new Random(1);
		int N = 20;
		long[][] vals = new long[N][];
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt()};
			//System.out.println("v=" + Arrays.toString(v));
			vals[i] = v;
			ind.put(v, v);
		}
		
		for (int i = 0; i < N; i++) {
			long[] v = vals[i];
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			//ind.printTree();
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testBug3() {
		PhTree<long[]> ind = create(2, 32);
		Random R = new Random(3);
		int N = 25;
		long[][] vals = new long[N][];
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt()};
			vals[i] = v;
			ind.put(v, v);
		}
		
		for (int i = 0; i < N; i++) {
			long[] v = vals[i];
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testBug3b() {
		PhTree<long[]> ind = create(2, 32);
		ArrayList<long[]> vA = new ArrayList<long[]>();
		
		vA.add(new long[]{1904347123, 1743248268});
		vA.add(new long[]{1773228306, 318397575});
		vA.add(new long[]{2093614540, 470886284});
		
		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			//System.out.println("adding: " + Bits.toBinary(v, 32));
			ind.put(v, v);
		}

		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("checking: " + Bits.toBinary(v, 32));
			assertTrue(ind.contains(v));
		}
		
		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//ind.printTree();
			//System.out.println("deleting i=" + i + "  " + Bits.toBinary(v, 32));
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testBug4() {
		PhTree<long[]> ind = create(2, 32);
		Random R = new Random(0);
		int N = 10;
		long[][] vals = new long[N][];
		for (int i = 0; i < N; i++) {
			long[] v = new long[]{R.nextInt(), R.nextInt()};
			//System.out.println("vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			//System.out.println("v=" + Arrays.toString(v));
			vals[i] = v;
			ind.put(v, v);
		}
		
		for (int i = 0; i < N; i++) {
			long[] v = vals[i];
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testBug4b() {
		PhTree<long[]> ind = create(2, 32);
		ArrayList<long[]> vA = new ArrayList<long[]>();

		vA.add(new long[]{-1557280266, 1327362106});
		vA.add(new long[]{-518907128, 99135751});
		vA.add(new long[]{-252332814, 755814641});
		
		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			ind.put(v, v);
		}

		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			//ind.printTree();
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testBug5() {
		PhTree<long[]> ind = create(2, 32);
		ArrayList<long[]> vA = new ArrayList<long[]>();

		vA.add(new long[]{-845879838, -187653156});
		vA.add(new long[]{-82500903, -2124478282});
		vA.add(new long[]{-423784092, -1668441430});
		vA.add(new long[]{-763332258, -1982438190});
		
		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			ind.put(v, v);
		}

		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			//ind.printTree();
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testBug6() {
		//seed=2264
		PhTree<long[]> ind = create(2, 32);
		ArrayList<long[]> vA = new ArrayList<long[]>();

		vA.add(new long[]{-571503246, -911425707});
		vA.add(new long[]{-291777302, -243251700});
		vA.add(new long[]{-2102706601, -693417435});
		vA.add(new long[]{-828373431, -133249064});
		vA.add(new long[]{-502327513, -1036737024});
		
		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("vA.add(new long[]{" + v[0] + ", " + v[1] + "});");
			assertNull(ind.put(v, v));
		}

		for (int i = 0; i < vA.size(); i++) {
			long[] v = vA.get(i);
			//System.out.println("i=" + i + "  " + Bits.toBinary(v, 32));
			//ind.printTree();
			//System.out.println();
			assertNotNull(ind.remove(v));
			assertFalse(ind.contains(v));
			//try again.
			assertNull(ind.remove(v));
			assertFalse(ind.contains(v));
		}
	}
	
	@Test
	public void testHighD64Neg() {
		final int MAX_DIM = 31;
		final int N = 1000;
		final int DEPTH = 64;
		Random R = new Random(0);
		
		for (int DIM = 3; DIM <= MAX_DIM; DIM++) {
			//System.out.println("d="+ DIM);
			long[][] vals = new long[N][];
			PhTree<long[]> ind = create(DIM, DEPTH);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = R.nextLong();
				}
				vals[i] = v;
				assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
			}

			//delete all
			for (long[] v: vals) {
				assertTrue("DIM=" + DIM + " v=" + Bits.toBinary(v, DEPTH), ind.contains(v));
				assertNotNull(ind.remove(v));
			}
			
			//check empty result
			long[] min = new long[DIM];
			long[] max = new long[DIM];
			for (int i = 0; i < DIM; i++) {
				min[i] = Long.MIN_VALUE;
				max[i] = Long.MAX_VALUE;
			}
			Iterator<long[]> it = ind.query(min, max);
			assertFalse(it.hasNext());

			assertEquals(0, ind.size());
		}
	}

	/**
	 * Fails only with NI-threshold = 0 for subs and posts.
	 */
	@Test
	public void testHighD64NegBug2() {
		final int DIM = 32;
		final int DEPTH = 64;

		long[][] vals = {
				{ -1891420301368910726L, -7510416602987178625L, 7854356540669723662L, 
					-4313796973086734422L, -3735585381220621847L, -2358801111684039663L, 
					8500592403625077914L, -3165069678806047833L, 408239638479573154L, 
					1394612278238908584L, 2346699220259279979L, 3580868071482899881L, 
					7961017168244967288L, -2014050115153595926L, 8051105003483558108L, 
					-3700506842608314642L, -4048666454762884880L, 9008299648439358285L, 
					6204108650229647936L, 778480900451172040L, -8434661093710232123L, 
					-8212527426587524194L, -917510832255457703L, 3958127369241215261L, 
					2259550045798965095L, 7032686370062058363L, 4591905256552858337L, 
					902882491532829926L, 303331575839660663L, 7449544573896043481L, 
					3092090196943101957L, 4515887688766405296L,  },
				{ -4062685296717429368L, -1458447326603816251L, 72520369377032730L, 
					2504023206924321121L, 1315032411642037417L, -671118087238692233L, 
					-5937697448152876824L, 2914069232554644162L, -122761879731138883L, 
					-2704799728189953919L, -3595630054475699660L, -6904106471410655605L, 
					5398810977192619702L, -6244681884589765467L, 3001783947703718265L, 
					9096083008774451479L, 7106685045394120506L, 1612660455506562941L, 
					-1602009131231155926L, 4674088701860260058L, 3523904087147023653L, 
					-7471386555745361678L, 6434863598619692329L, 3519486867992011992L, 
					2580325349084506629L, 1716045732687783621L, -7492979958698560176L, 
					-4514641440177765589L, 3721608777574387356L, -1662765351114890487L, 
					3457037762958540780L, -1786853829876224128L,  }};
		final int N = vals.length;
		PhTree<long[]> ind = create(DIM, DEPTH);
		for (int i = 0; i < N; i++) {
			long[] v = vals[i];
			assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
		}

		Iterator<long[]> it3 = ind.queryExtent();
		assertArrayEquals(vals[1], it3.next());
		assertArrayEquals(vals[0], it3.next());

		//delete all
		for (long[] v: vals) {
			assertTrue("DIM=" + DIM + " v=" + Bits.toBinary(v, DEPTH), ind.contains(v));
			assertNotNull(ind.remove(v));
		}

		//check empty result
		long[] min = new long[DIM];
		long[] max = new long[DIM];
		for (int i = 0; i < DIM; i++) {
			min[i] = Long.MIN_VALUE;
			max[i] = Long.MAX_VALUE;
		}
		Iterator<long[]> it = ind.query(min, max);
		assertFalse(it.hasNext());

		assertEquals(0, ind.size());
	}

	@Test
	public void testBug7() {
		final int DIM = 2;
		final int DEPTH = 64;

		long[][] vals = {
				{4629873810166860768L, 4633240206133798917L},
				{4629871921680873272L, 4633238188380428366L},
				{4629872369648298706L, 4633237661037059499L},
				{4629872475131046230L, 4633239827761061474L},
				//{4629873810166860768L, 4633240206133798917L},
				{4629874203105928256L, 4633238020480604758L},
				{4629875721663427610L, 4633238021184292200L},
				{4629874845854037575L, 4633240064692623119L},
				{4629875863526815872L, 4633239255452065076L}
		};
		PhTree<long[]> ind = create(DIM, DEPTH);
		for (long[] v: vals) {
			assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
		}

		//delete all
		for (long[] v: vals) {
			assertTrue("DIM=" + DIM + " v=" + Bits.toBinary(v, DEPTH), ind.contains(v));
			assertNotNull(ind.remove(v));
		}

		//check empty result
		long[] min = new long[DIM];
		long[] max = new long[DIM];
		for (int i = 0; i < DIM; i++) {
			min[i] = Long.MIN_VALUE;
			max[i] = Long.MAX_VALUE;
		}
		Iterator<long[]> it = ind.query(min, max);
		assertFalse(it.hasNext());

		assertEquals(0, ind.size());
	}
	
	@Test
	public void testBug7b() {
		final int DIM = 2;
		final int DEPTH = 64;

		long[][] vals = { 
				{ 4629871990712611310L, 4633238174588154507L },
				{ 4629872427280300188L, 4633237649496585454L },
				{ 4629872509330255900L, 4633239820935293288L },
				{ 4629873810166860768L, 4633240206133798917L },
				{ 4629874203105928256L, 4633238020480604758L },
				{ 4629874845854037575L, 4633240064692623119L },
				{ 4629875721663427610L, 4633238021184292200L },
				{ 4629875863526815872L, 4633239255452065076L }, 
		};
		PhTree<long[]> ind = create(DIM, DEPTH);
		for (long[] v : vals) {
			assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
		}


		// update 1
		ind.remove(vals[0]);
		long[] value1 = { 4629871921680873272L, 4633238188380428366L };
		ind.put(value1, value1);

		//update2
		ind.remove(vals[1]);	
		long[] value2 = { 4629872369648298706L, 4633237661037059499L };
		ind.put(value2, value2);

		//update 3
		ind.remove(vals[2]);	
		long[] value3 = { 4629872475131046230L, 4633239827761061474L };
		ind.put(value3, value3);


		//delete error
		assertTrue(ind.contains(value2));
		assertNotNull(ind.remove(vals[3]));
		assertTrue(ind.contains(value2));
	}

	@Test
	public void testBug7c() {
		final int DIM = 2;
		final int DEPTH = 64;

		long[][] vals = { 
				{ 4629871990712611310L, 4633238174588154507L },
				//{ 4629871921680873272L, 4633238188380428366L },
				//{ 4629872427280300188L, 4633237649496585454L },  //2 old
				{ 4629872369648298706L, 4633237661037059499L },  //2 new
				//{ 4629872509330255900L, 4633239820935293288L }, // 2 old
				{ 4629872475131046230L, 4633239827761061474L }, //3 new
				{ 4629873810166860768L, 4633240206133798917L },
				{ 4629874203105928256L, 4633238020480604758L },
				{ 4629874845854037575L, 4633240064692623119L },
				{ 4629875721663427610L, 4633238021184292200L },
				{ 4629875863526815872L, 4633239255452065076L }, 
		};
		PhTree<long[]> ind = create(DIM, DEPTH);
		for (long[] v : vals) {
			assertNull(Bits.toBinary(v, DEPTH), ind.put(v, v));
		}


		// update 1
		assertNotNull(ind.remove(vals[0]));
		long[] value1 = { 4629871921680873272L, 4633238188380428366L };
		assertNull(ind.put(value1, value1));

		//update2
//		System.out.println("removing " + Arrays.toString(vals[1]));
//		assertTrue(ind.delete(vals[1]));	
//		long[] value2 = { 4629872369648298706L, 4633237661037059499L };
//		System.out.println("inserting " + Arrays.toString(value2));
//		assertFalse(ind.insert(value2));
//		long[] value2 = vals[1];

		//update 3
		assertNotNull(ind.remove(vals[2]));
		assertNull(ind.put(vals[2], vals[2]));
		assertTrue(ind.contains(value1));
		assertTrue(ind.contains(vals[1]));
		assertTrue(ind.contains(vals[2]));
		assertTrue(ind.contains(vals[3]));
		assertTrue(ind.contains(vals[4]));
		assertTrue(ind.contains(vals[5]));
		assertTrue(ind.contains(vals[6]));
		assertTrue(ind.contains(vals[7]));


		//delete error
		assertTrue(ind.contains(vals[1]));
		assertNotNull(ind.remove(vals[3]));
		assertTrue(ind.contains(vals[1]));
	}
}
