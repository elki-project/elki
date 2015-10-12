package ch.ethz.globis.pht.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.PhTree;
import ch.ethz.globis.pht.PhTree.PhIterator;
import ch.ethz.globis.pht.test.util.TestUtil;
import ch.ethz.globis.pht.util.Bits;

public class TestNearestNeighbour {

	@Test
	public void testDirectHit() {
		PhTree<long[]> idx = TestUtil.newTree(2, 8);
		idx.put(new long[]{2,2}, new long[]{2,2});
		idx.put(new long[]{1,1}, new long[]{1,1});
		idx.put(new long[]{1,3}, new long[]{1,3});
		idx.put(new long[]{3,1}, new long[]{3,1});
		
		List<long[]> result = idx.nearestNeighbour(0, 3, 3);
		assertTrue(result.isEmpty());
		
		result = idx.nearestNeighbour(1, 2, 2);
		assertEquals(1, result.size());
		check(8, result.get(0), 2, 2);
		
		result = idx.nearestNeighbour(1, 1, 1);
		assertEquals(1, result.size());
		check(8, result.get(0), 1, 1);
		
		result = idx.nearestNeighbour(1, 1, 3);
		assertEquals(1, result.size());
		check(8, result.get(0), 1, 3);
		
		result = idx.nearestNeighbour(1, 3, 1);
		assertEquals(1, result.size());
		check(8, result.get(0), 3, 1);
	}
	
	@Test
	public void testNeighbour1of4() {
		PhTree<long[]> idx = TestUtil.newTree(2, 8);
		idx.put(new long[]{2,2}, new long[]{2,2});
		idx.put(new long[]{1,1}, new long[]{1,1});
		idx.put(new long[]{1,3}, new long[]{1,3});
		idx.put(new long[]{3,1}, new long[]{3,1});
		
		List<long[]> result = idx.nearestNeighbour(1, 3, 3);
		check(8, result.get(0), 2, 2);
		assertEquals(1, result.size());
	}
	
	@Test
	public void testNeighbour1of5DirectHit() {
		PhTree<long[]> idx = TestUtil.newTree(2, 8);
		idx.put(new long[]{3,3}, new long[]{3,3});
		idx.put(new long[]{2,2}, new long[]{2,2});
		idx.put(new long[]{1,1}, new long[]{1,1});
		idx.put(new long[]{1,3}, new long[]{1,3});
		idx.put(new long[]{3,1}, new long[]{3,1});
		
		List<long[]> result = idx.nearestNeighbour(1, 3, 3);
		check(8, result.get(0), 3, 3);
		assertEquals(1, result.size());
	}
	
	@Test
	public void testNeighbour4_5of4() {
		PhTree<long[]> idx = TestUtil.newTree(2, 8);
		idx.put(new long[]{3,3}, new long[]{3,3});
		idx.put(new long[]{2,2}, new long[]{2,2});
		idx.put(new long[]{4,4}, new long[]{4,4});
		idx.put(new long[]{2,4}, new long[]{2,4});
		idx.put(new long[]{4,2}, new long[]{4,2});
		
		List<long[]> result = idx.nearestNeighbour(4, 3, 3);
		
//		for (long[] l: result) {
//			System.out.println(Arrays.toString(l));
//		}
		
		checkContains(result, 3, 3);
		checkContains(result, 4, 4);
		checkContains(result, 4, 2);
		checkContains(result, 2, 2);
		checkContains(result, 2, 4);
		
		assertEquals(5, result.size());
	}
	
	@Test
	public void testQueryND64Random1() {
		final int DIM = 5;
		final int LOOP = 10;
		final int N = 1000;
		final int NQ = 1000;
		final int MAXV = 1000;
		final Random R = new Random(0);
		for (int d = 0; d < LOOP; d++) {
			PhTree<Object> ind = TestUtil.newTree(DIM, 32);
			for (int i = 0; i < N; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = Math.abs(R.nextInt(MAXV)); //INTEGER 32 bit !!!
				}
				ind.put(v, null);
			}
			for (int i = 0; i < NQ; i++) {
				long[] v = new long[DIM];
				for (int j = 0; j < DIM; j++) {
					v[j] = Math.abs(R.nextInt(MAXV)); //TODO try long?
				}
				long[] exp = nearestNeighbor1(ind, v);
//				System.out.println("d="+ d + "   i=" + i + "   minD=" + dist(v, exp));
//				System.out.println("v="+ Arrays.toString(v));
//				System.out.println("exp="+ Arrays.toString(exp));
				List<long[]> nnList = ind.nearestNeighbour(1, v);
				
//				System.out.println(ind.toStringPlain());
//				System.out.println("v  =" + Arrays.toString(v));
//				System.out.println("exp=" + Arrays.toString(exp));
				assertTrue("i=" + i + " d=" + d, !nnList.isEmpty());
				long[] nn = nnList.get(0);
				check(v, exp, nn);
			}
		}
	}
	
	/**
	 * This used to return an empty result set.
	 */
	@Test
	public void testNN1EmptyResultError() {
		long[][] data = {
				{47, 15, 53, },
				{54, 77, 77, },
				{73, 62, 95, },
		};

		final int DIM = data[0].length;
		final int N = data.length;
		PhTree<Object> ind = TestUtil.newTree(DIM, 64);
		for (int i = 0; i < N; i++) {
			ind.put(data[i], data[i]);
		}
		
		long[] v={44, 84, 75};
		long[] exp = nearestNeighbor1(ind, v);
		List<long[]> nnList = ind.nearestNeighbour(1, v);
		assertTrue(!nnList.isEmpty());
		long[] nn = nnList.get(0);
		check(v, exp, nn);
	}
	
	/**
	 * This used to return an empty result set.
	 */
	@Test
	public void testWrongResult() {
		long[][] data = {
				{6, 23, 48, 22, 52, },
				{46, 73, 83, 30, 48, },
				{90, 74, 60, 32, 47, },
				{53, 35, 42, 47, 28, },
				{81, 79, 54, 74, 2, },
				{86, 52, 28, 90, 98, },
				{15, 4, 34, 13, 9, },
				{82, 86, 44, 51, 36, },
				{88, 14, 42, 76, 86, },
				{3, 84, 61, 66, 83, },
				{23, 35, 47, 85, 65, },
				{88, 28, 63, 72, 55, },
				{13, 99, 94, 31, 90, },
				{2, 29, 93, 40, 9, },
				{16, 75, 71, 79, 66, },
				{39, 62, 7, 93, 96, },
				{95, 90, 68, 16, 26, },
				{47, 94, 89, 49, 68, },
				{74, 1, 6, 18, 2, },
				{86, 73, 73, 56, 92, },
				{26, 38, 93, 16, 7, },
				{57, 14, 93, 3, 42, },
				{42, 85, 17, 61, 34, },
				{25, 44, 35, 60, 6, },
				{68, 44, 1, 9, 7, },
				{79, 93, 58, 12, 73, },
				{69, 74, 20, 58, 73, },
				{78, 47, 17, 54, 4, },
				{32, 27, 80, 93, 5, },
				{3, 8, 64, 15, 85, },
		};
		
		final int DIM = data[0].length;
		final int N = data.length;
		PhTree<Object> ind = TestUtil.newTree(DIM, 64);
		for (int i = 0; i < N; i++) {
			ind.put(data[i], null);
		}
		
		long[] exp = {15, 4, 34, 13, 9};
		
		assertTrue(ind.contains(exp));
		
		long[] center = {32, 0, 22, 7, 5};
		
		List<long[]> lst = ind.nearestNeighbour(1, center);
		assertTrue(!lst.isEmpty());
		long[] nn = lst.get(0);
		assertArrayEquals(exp, nn);
	}
	
	@Test
	public void testQueryBugNeg() {
		long[][] data = {
				{-1204229761, 1708741064, -68868220, -1707824853, 1030257863, },
				{1234196896, -336109864, 315988961, -949408358, -659516549, },
				{-1653052902, 237016407, 1996740714, 1571095592, -77864042, },
				{-805213861, -1889046784, 209226615, 1069778368, -1653259991, },
				{1463740813, -1648553937, -138002245, 1129744474, -1496837342, },
				{-1983851114, 418360594, 1043383554, 1843457426, -745581042, },
				{-596752058, -1204026143, -1289062157, 1903690050, 148894743, },
				{1833617972, 269941049, -1601119646, -1009009792, 2138805185, },
				{1030803498, -11806928, -1656884171, -924072989, 1158089640, },
				{2082138894, 1408747128, -675281235, 1670863412, -2019758977, },
				{1897679064, -762908299, 2114529136, -80347365, 437812083, },
				{-971184186, 399461718, -672096027, -1414885155, -889253355, },
				{857578626, 1537099160, 1737460704, 1447988357, -1034846627, },
				{-1101992826, -447906913, -337652800, -2093295083, 320068164, },
				{-1083253817, -1458243351, -1428210552, 1521501881, -563614502, },
				{1704074100, 308372608, -26247210, 1385490637, -233170527, },
				{1777665782, 1200723512, 252388312, 1654570087, -1411301091, },
				{1313549057, 1532173787, -646122639, -608769759, 1740548742, },
				{428074767, -54393106, -419742356, -1294990268, 547203805, },
				{1470766840, -582965602, 112395555, 273901336, -1556687076, },
				{-1488491765, 1957299205, 108834455, -940677743, 1398346750, },
				{2096213985, -1974558019, 346291113, -347173436, 58192461, },
				{-2071940291, -1358311478, -760143927, -395406161, -1352210406, },
				{-843124110, -1011894451, 331990750, 2146995581, 771157882, },
				{621914456, -1932661751, 762661064, 1814282142, -1757470741, },
				{888640500, 1280710491, -112555630, -1062355718, -1025180692, },
				{-15818258, 188218856, 1154007495, 1909126369, 966646362, },
				{1750418031, -1142383570, -248350145, 670011702, 1565733342, },
				{-828028905, 1814815076, -1991859631, -2040400033, -1127051781, },
				{-698613902, 105023480, 747625090, -418770485, -145401753, },
				{-460038536, 548689525, -1215501295, -1486050454, 419568609, },
				{-1416040334, -549244587, -463900570, 1560215565, 1882489208, },
				{-148763711, 535304924, 1275887840, -924522691, -1079555321, },
				//Expected value!!
				{263687591, 1522964456, 1803185563, 203545143, 1010187655, },
				
				{-1041865758, 1720861294, -1583473098, 2016464942, -1279198356, },
				{-266110292, 818229608, 1662695035, 1669269467, 959550146, },
				{2018809849, -1436097053, -1968666526, 843773646, -1507873363, },
				{-1575855534, -509985175, 1364758887, 1409875087, -48062780, },
				{-956407428, -1423962916, 508144127, 1390448190, 1555398449, },
				{1423627621, -224500804, 15034675, -1955178570, -693214864, },
				{-946512315, 1546692851, 1447359913, -155320462, 475044201, },
				{266832649, -2123571286, -1498617367, -493617668, -302551325, },
				{-708817877, -1716392768, -2094811994, -2020052896, -1983042372, },
				{-1645059274, 809467165, -2108311590, 523253163, -229799878, },
				{170444011, 251371343, -1738098855, -441257476, -1305244596, },
				{896195619, -918178832, -524832686, 2000780836, 1312504570, },
				{-1184268540, -867828825, -87701481, 2071101800, -1705926671, },
				{1259220273, -1510124202, -1254817838, -45569204, -1833916732, },
				{-1630641165, -1572822966, -196588564, -406415676, 6684287, },
				{873778210, 1672018674, 1622600151, 184196524, 356161218, }
		};
		
		final int DIM = data[0].length;
		final int N = data.length;
		PhTree<Object> ind = TestUtil.newTree(DIM, 64);
		for (int i = 0; i < N; i++) {
			ind.put(data[i], null);
		}
		
		long[] exp = {263687591, 1522964456, 1803185563, 203545143, 1010187655};
		long[] min = {-855017900, 407637298, 209062726, -774848964, 567211952};
		long[] max = {963606146, 2226261345L, 2027686773, 1043775082, 2385835999L};
		
		assertTrue(ind.contains(exp));
		
		boolean fail = true;
		PhIterator<?> pvi = ind.query(min, max);
		while (pvi.hasNext()) {
			long[] x = pvi.nextKey();
			if (Arrays.equals(exp, x)) {
				fail = false;
			}
		}
		if (fail) {
			fail();
		}
	}
	
	@Test
	public void testQueryBugPos() {
		long[][] data = {
				{6, 23, 48, 22, 52, },
				{46, 73, 83, 30, 48, },
				{90, 74, 60, 32, 47, },
				{53, 35, 42, 47, 28, },
				{81, 79, 54, 74, 2, },
				{86, 52, 28, 90, 98, },
				{15, 4, 34, 13, 9, },
				{82, 86, 44, 51, 36, },
				{88, 14, 42, 76, 86, },
				{3, 84, 61, 66, 83, },
				{23, 35, 47, 85, 65, },
				{88, 28, 63, 72, 55, },
				{13, 99, 94, 31, 90, },
				{2, 29, 93, 40, 9, },
				{16, 75, 71, 79, 66, },
				{39, 62, 7, 93, 96, },
				{95, 90, 68, 16, 26, },
				{47, 94, 89, 49, 68, },
				{74, 1, 6, 18, 2, },
				{86, 73, 73, 56, 92, },
				{26, 38, 93, 16, 7, },
				{57, 14, 93, 3, 42, },
				{42, 85, 17, 61, 34, },
				{25, 44, 35, 60, 6, },
				{68, 44, 1, 9, 7, },
				{79, 93, 58, 12, 73, },
				{69, 74, 20, 58, 73, },
				{78, 47, 17, 54, 4, },
				{32, 27, 80, 93, 5, },
				{3, 8, 64, 15, 85, },
		};
		
		final int DIM = data[0].length;
		final int N = data.length;
		PhTree<Object> ind = TestUtil.newTree(DIM, 64);
		for (int i = 0; i < N; i++) {
			ind.put(data[i], null);
		}
		
		long[] exp = {15, 4, 34, 13, 9};
		long[] min = {8, -23, -1, -16, -18};
		long[] max = {55, 23, 45, 30, 28};
		
		assertTrue(ind.contains(exp));
		
		boolean fail = true;
		PhIterator<?> pvi = ind.query(min, max);
		while (pvi.hasNext()) {
			long[] x = pvi.nextKey();
			if (Arrays.equals(exp, x)) {
				fail = false;
			}
		}
		if (fail) {
			fail();
		}
		
		long[] center = {32, 0, 22, 7, 5};
		exp = nearestNeighbor1(ind, center);
		List<long[]> lst = ind.nearestNeighbour(1, center);
		long[] nn = lst.get(0);
		assertArrayEquals(exp, nn);
	}
	
	@Test
	public void testNPE() {
		final int DIM = 2;
		final int N = 100;
		final int MAXV = 100;
		final Random R = new Random(0);

		PhTree<Object> ind = TestUtil.newTree(DIM, 32);
		for (int i = 0; i < N; i++) {
			long[] v = new long[DIM];
			for (int j = 0; j < DIM; j++) {
				v[j] = Math.abs(R.nextInt(MAXV)); //INTEGER 32 bit !!!
			}
			ind.put(v, null);
		}

		long[] v = new long[DIM];
		for (int j = 0; j < DIM; j++) {
			v[j] = Math.abs(R.nextInt(MAXV)); //TODO try long?
		}
		long[] exp = nearestNeighbor1(ind, v);
		List<long[]> nnList = ind.nearestNeighbour(1, v);
		assertTrue(!nnList.isEmpty());
		long[] nn = nnList.get(0);
		check(v, exp, nn);
	}
	

	
	private long[] nearestNeighbor1(PhTree<?> tree, long[] q) {
		double d = Double.MAX_VALUE;
		long[] best = null;
		PhIterator<?> i = tree.queryExtent();
		while (i.hasNext()) {
			long[] cand = i.nextKey();
			double dNew = dist(q, cand);
			if (dNew < d) {
				d = dNew;
				best = cand;
			}
		}
		return best;
	}
	
	private long[] nearestNeighborK(PhTree<?> tree, long[] q) {
		double d = Double.MAX_VALUE;
		long[] best = null;
		PhIterator<?> i = tree.queryExtent();
		while (i.hasNext()) {
			long[] cand = i.nextKey();
			double dNew = dist(q, cand);
			if (dNew < d) {
				d = dNew;
				best = cand;
			}
		}
		return best;
	}
	
	private void check(long[] v, long[] c1, long[] c2) {
		for (int i = 0; i < c1.length; i++) {
			if (c1[i] != c2[i]) {
				double d1 = dist(v, c1);
				double d2 = dist(v, c2);
				double maxEps = Math.abs(d2-d1)/d1;
				if (maxEps >= 1) {
					System.out.println("WARNING: different values found: " + d1 + "/" + d2);
					System.out.println("c1=" + Arrays.toString(c1));
					System.out.println("c2=" + Arrays.toString(c2));
					fail();
				}
				break;
			}
		}
	}

	private double dist(long[] v1, long[] v2) {
		double d = 0;
		for (int i = 0; i < v1.length; i++) {
			double dl = v1[i] - v2[i];
			d += dl*dl;
		}
		return Math.sqrt(d);
	}
	
	private void check(int DEPTH, long[] t, long ... ints) {
		for (int i = 0; i < ints.length; i++) {
			assertEquals("i=" + i + " | " + Bits.toBinary(ints, DEPTH) + " / " + 
					Bits.toBinary(t, DEPTH), ints[i], t[i]);
		}
	}

	private void checkContains(List<long[]> l, long ... v) {
		for (long[] vl: l) {
			if (Arrays.equals(vl, v)) {
				return;
			}
		}
		fail("Not found: " + Arrays.toString(v));
	}
	
//	private void check(long[] t, long[] s) {
//		for (int i = 0; i < s.length; i++) {
//			assertEquals("i=" + i + " | " + Bits.toBinary(s) + " / " + 
//					Bits.toBinary(t), (short)s[i], (short)t[i]);
//		}
//	}
}
