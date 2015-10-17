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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import ch.ethz.globis.pht.PhDistanceF;
import ch.ethz.globis.pht.PhTreeF;
import ch.ethz.globis.pht.PhTreeF.PhIteratorF;
import ch.ethz.globis.pht.PhTreeF.PhQueryKNNF;
import ch.ethz.globis.pht.util.BitTools;
import ch.ethz.globis.pht.util.Bits;

public class TestNearestNeighbourF {

  private <T> PhTreeF<T> newTree(int DIM) {
    return PhTreeF.create(DIM);
  }

  @Test
  public void testDirectHit() {
    PhTreeF<double[]> idx = newTree(2);
    idx.put(new double[]{2,2}, new double[]{2,2});
    idx.put(new double[]{1,1}, new double[]{1,1});
    idx.put(new double[]{1,3}, new double[]{1,3});
    idx.put(new double[]{3,1}, new double[]{3,1});

    List<double[]> result = toList(idx.nearestNeighbour(0, 3, 3));
    assertTrue(result.isEmpty());

    result = toList(idx.nearestNeighbour(1, 2, 2));
    assertEquals(1, result.size());
    check(8, result.get(0), 2, 2);

    result = toList(idx.nearestNeighbour(1, 1, 1));
    assertEquals(1, result.size());
    check(8, result.get(0), 1, 1);

    result = toList(idx.nearestNeighbour(1, 1, 3));
    assertEquals(1, result.size());
    check(8, result.get(0), 1, 3);

    result = toList(idx.nearestNeighbour(1, 3, 1));
    assertEquals(1, result.size());
    check(8, result.get(0), 3, 1);
  }

  @Test
  public void testNeighbour1of4() {
    PhTreeF<double[]> idx = newTree(2);
    idx.put(new double[]{2,2}, new double[]{2,2});
    idx.put(new double[]{1,1}, new double[]{1,1});
    idx.put(new double[]{1,3}, new double[]{1,3});
    idx.put(new double[]{3,1}, new double[]{3,1});

    List<double[]> result = toList(idx.nearestNeighbour(1, 3, 3));
    check(8, result.get(0), 2, 2);
    assertEquals(1, result.size());
  }

  @Test
  public void testNeighbour1of5DirectHit() {
    PhTreeF<double[]> idx = newTree(2);
    idx.put(new double[]{3,3}, new double[]{3,3});
    idx.put(new double[]{2,2}, new double[]{2,2});
    idx.put(new double[]{1,1}, new double[]{1,1});
    idx.put(new double[]{1,3}, new double[]{1,3});
    idx.put(new double[]{3,1}, new double[]{3,1});

    List<double[]> result = toList(idx.nearestNeighbour(1, 3, 3));
    check(8, result.get(0), 3, 3);
    assertEquals(1, result.size());
  }

  @Test
  public void testNeighbour4_5of4() {
    PhTreeF<double[]> idx = newTree(2);
    idx.put(new double[]{3,3}, new double[]{3,3});
    idx.put(new double[]{2,2}, new double[]{2,2});
    idx.put(new double[]{4,4}, new double[]{4,4});
    idx.put(new double[]{2,4}, new double[]{2,4});
    idx.put(new double[]{4,2}, new double[]{4,2});

    List<double[]> result = toList(idx.nearestNeighbour(4, 3, 3));

    checkContains(result, 3, 3);
    int n = 1;
    n += contains(result, 4, 4) ? 1 : 0;
    n += contains(result, 4, 2) ? 1 : 0;
    n += contains(result, 2, 2) ? 1 : 0;
    n += contains(result, 2, 4) ? 1 : 0;

    assertTrue(n >= 4);
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
      PhTreeF<Object> ind = newTree(DIM);
      PhQueryKNNF<Object> q = ind.nearestNeighbour(1, new double[DIM]);
      for (int i = 0; i < N; i++) {
        double[] v = new double[DIM];
        for (int j = 0; j < DIM; j++) {
          v[j] = R.nextDouble()*MAXV;
        }
        ind.put(v, null);
      }
      for (int i = 0; i < NQ; i++) {
        double[] v = new double[DIM];
        for (int j = 0; j < DIM; j++) {
          v[j] = R.nextDouble()*MAXV;
        }
        double[] exp = nearestNeighbor1(ind, v);
        //        System.out.println("d="+ d + "   i=" + i + "   minD=" + dist(v, exp));
        //        System.out.println("v="+ Arrays.toString(v));
        //        System.out.println("exp="+ Arrays.toString(exp));
        List<double[]> nnList = toList(q.reset(1, null, null, v));

        //        System.out.println(ind.toStringPlain());
        //        System.out.println("v  =" + Arrays.toString(v));
        //        System.out.println("exp=" + Arrays.toString(exp));
        assertTrue("i=" + i + " d=" + d, !nnList.isEmpty());
        double[] nn = nnList.get(0);
        check(v, exp, nn);
      }
    }
  }

  @Test
  public void testQueryND64RandomDF() {
    final int DIM = 5;
    final int LOOP = 10;
    final int N = 1000;
    final int NQ = 1000;
    final int MAXV = 1000;
    final Random R = new Random(0);
    for (int d = 0; d < LOOP; d++) {
      PhTreeF<Object> ind = newTree(DIM);
      PhQueryKNNF<Object> q = ind.nearestNeighbour(1, new double[DIM]);
      for (int i = 0; i < N; i++) {
        double[] v = new double[DIM];
        for (int j = 0; j < DIM; j++) {
          v[j] = R.nextDouble()*MAXV;
        }
        ind.put(v, null);
      }
      for (int i = 0; i < NQ; i++) {
        double[] v = new double[DIM];
        for (int j = 0; j < DIM; j++) {
          v[j] = R.nextDouble()*MAXV;
        }
        double[] exp = nearestNeighbor1(ind, v);
        //        System.out.println("d="+ d + "   i=" + i + "   minD=" + dist(v, exp));
        //        System.out.println("v="+ Arrays.toString(v));
        //        System.out.println("exp="+ Arrays.toString(exp));
        List<double[]> nnList = toList(q.reset(1, PhDistanceF.THIS, null, v));

        //        System.out.println(ind.toStringPlain());
        //        System.out.println("v  =" + Arrays.toString(v));
        //        System.out.println("exp=" + Arrays.toString(exp));
        assertTrue("i=" + i + " d=" + d, !nnList.isEmpty());
        double[] nn = nnList.get(0);
        check(v, exp, nn);
      }
    }
  }


  @Test
  public void testQueryND64RandomCenterAway() {
    final int DIM = 5;
    final int LOOP = 10;
    final int N = 1000;
    final int NQ = 1000;
    final int MAXV = 1000;
    final Random R = new Random(0);
    for (int d = 0; d < LOOP; d++) {
      PhTreeF<Object> ind = newTree(DIM);
      PhQueryKNNF<Object> q = ind.nearestNeighbour(1, new double[DIM]);
      for (int i = 0; i < N; i++) {
        double[] v = new double[DIM];
        for (int j = 0; j < DIM; j++) {
          v[j] = R.nextDouble();//*MAXV;
        }
        ind.put(v, null);
      }
      for (int i = 0; i < NQ; i++) {
        double[] v = new double[DIM];
        for (int j = 0; j < DIM; j++) {
          v[j] = R.nextDouble()*MAXV;
        }
        double[] exp = nearestNeighbor1(ind, v);
        //        System.out.println("d="+ d + "   i=" + i + "   minD=" + dist(v, exp));
        //        System.out.println("v="+ Arrays.toString(v));
        //        System.out.println("exp="+ Arrays.toString(exp));
        List<double[]> nnList = toList(q.reset(1, PhDistanceF.THIS, null, v));

        //        System.out.println(ind.toStringPlain());
        //        System.out.println("v  =" + Arrays.toString(v));
        //        System.out.println("exp=" + Arrays.toString(exp));
        assertTrue("i=" + i + " d=" + d, !nnList.isEmpty());
        double[] nn = nnList.get(0);
        check(v, exp, nn);
      }
    }
  }


  /**
   * This used to return an empty result set.
   */
  @Test
  public void testNN1EmptyResultError() {
    double[][] data = {
        {47, 15, 53, },
        {54, 77, 77, },
        {73, 62, 95, },
    };

    final int DIM = data[0].length;
    final int N = data.length;
    PhTreeF<Object> ind = newTree(DIM);
    for (int i = 0; i < N; i++) {
      ind.put(data[i], data[i]);
    }

    double[] v={44, 84, 75};
    double[] exp = nearestNeighbor1(ind, v);
    List<double[]> nnList = toList(ind.nearestNeighbour(1, v));
    assertTrue(!nnList.isEmpty());
    double[] nn = nnList.get(0);
    check(v, exp, nn);
  }

  /**
   * This used to return an empty result set.
   */
  @Test
  public void testWrongResult() {
    double[][] data = {
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
    PhTreeF<Object> ind = newTree(DIM);
    for (int i = 0; i < N; i++) {
      ind.put(data[i], null);
    }

    double[] exp = {15, 4, 34, 13, 9};

    assertTrue(ind.contains(exp));

    double[] center = {32, 0, 22, 7, 5};

    List<double[]> lst = toList(ind.nearestNeighbour(1, center));
    assertTrue(!lst.isEmpty());
    double[] nn = lst.get(0);
    assertArrayEquals(exp, nn);
  }

  @Test
  public void testQueryBugNeg() {
    double[][] data = {
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
    PhTreeF<Object> ind = newTree(DIM);
    for (int i = 0; i < N; i++) {
      ind.put(data[i], null);
    }

    double[] exp = {263687591, 1522964456, 1803185563, 203545143, 1010187655};
    double[] min = {-855017900, 407637298, 209062726, -774848964, 567211952};
    double[] max = {963606146, 2226261345L, 2027686773, 1043775082, 2385835999L};

    assertTrue(ind.contains(exp));

    boolean fail = true;
    PhIteratorF<?> pvi = ind.query(min, max);
    while (pvi.hasNext()) {
      double[] x = pvi.nextKey();
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
    double[][] data = {
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
    PhTreeF<Object> ind = newTree(DIM);
    for (int i = 0; i < N; i++) {
      ind.put(data[i], null);
    }

    double[] exp = {15, 4, 34, 13, 9};
    double[] min = {8, -23, -1, -16, -18};
    double[] max = {55, 23, 45, 30, 28};

    assertTrue(ind.contains(exp));

    boolean fail = true;
    PhIteratorF<?> pvi = ind.query(min, max);
    while (pvi.hasNext()) {
      double[] x = pvi.nextKey();
      if (Arrays.equals(exp, x)) {
        fail = false;
      }
    }
    if (fail) {
      fail();
    }

    double[] center = {32, 0, 22, 7, 5};
    exp = nearestNeighbor1(ind, center);
    List<double[]> lst = toList(ind.nearestNeighbour(1, center));
    double[] nn = lst.get(0);
    assertArrayEquals(exp, nn);
  }

  @Test
  public void testNPE() {
    final int DIM = 2;
    final int N = 100;
    final int MAXV = 100;
    final Random R = new Random(0);

    PhTreeF<Object> ind = newTree(DIM);
    for (int i = 0; i < N; i++) {
      double[] v = new double[DIM];
      for (int j = 0; j < DIM; j++) {
        v[j] = R.nextDouble()*MAXV;
      }
      ind.put(v, null);
    }

    double[] v = new double[DIM];
    for (int j = 0; j < DIM; j++) {
      v[j] = R.nextDouble()*MAXV;
    }
    double[] exp = nearestNeighbor1(ind, v);
    List<double[]> nnList = toList(ind.nearestNeighbour(1, v));
    assertTrue(!nnList.isEmpty());
    double[] nn = nnList.get(0);
    check(v, exp, nn);
  }


  private double[] nearestNeighbor1(PhTreeF<?> tree, double[] q) {
    double d = Double.MAX_VALUE;
    double[] best = null;
    PhIteratorF<?> i = tree.queryExtent();
    while (i.hasNext()) {
      double[] cand = i.nextKey();
      double dNew = dist(q, cand);
      if (dNew < d) {
        d = dNew;
        best = cand;
      }
    }
    return best;
  }

  private double[] nearestNeighborK(PhTreeF<?> tree, double[] q) {
    double d = Double.MAX_VALUE;
    double[] best = null;
    PhIteratorF<?> i = tree.queryExtent();
    while (i.hasNext()) {
      double[] cand = i.nextKey();
      double dNew = dist(q, cand);
      if (dNew < d) {
        d = dNew;
        best = cand;
      }
    }
    return best;
  }

  private void check(double[] v, double[] c1, double[] c2) {
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

  private double dist(double[] v1, double[] v2) {
    double d = 0;
    for (int i = 0; i < v1.length; i++) {
      double dl = v1[i] - v2[i];
      d += dl*dl;
    }
    return Math.sqrt(d);
  }

  private void check(int DEPTH, double[] t, double ... ints) {
    for (int i = 0; i < ints.length; i++) {
      assertEquals("i=" + i + " | " + toBinary(ints, DEPTH) + " / " + 
          toBinary(t, DEPTH), ints[i], t[i], 0.0);
    }
  }

  private void checkContains(List<double[]> l, double ... v) {
    for (double[] vl: l) {
      if (Arrays.equals(vl, v)) {
        return;
      }
    }
    fail("Not found: " + Arrays.toString(v));
  }

  private boolean contains(List<double[]> l, double ... v) {
    for (double[] vl: l) {
      if (Arrays.equals(vl, v)) {
        return true;
      }
    }
    return false;
  }
  
  //  private void check(long[] t, long[] s) {
  //    for (int i = 0; i < s.length; i++) {
  //      assertEquals("i=" + i + " | " + Bits.toBinary(s) + " / " + 
  //          Bits.toBinary(t), (short)s[i], (short)t[i]);
  //    }
  //  }

  private List<double[]> toList(PhQueryKNNF<?> q) {
    ArrayList<double[]> ret = new ArrayList<>();
    while (q.hasNext()) {
      ret.add(q.nextKey());
    }
    return ret;
  }

  private String toBinary(double[] d, int DEPTH) {
    long[] l = new long[d.length];
    for (int i = 0; i < l.length; i++) {
      l[i] = BitTools.toSortableLong(d[i]);
    }
    return Bits.toBinary(l, DEPTH);
  }

  private void assertArrayEquals(double[] exp, double[] nn) {
    if (exp.length != nn.length) {
      fail("Expected length" + exp.length + " got " + nn.length);
    }
    for (int i = 0; i < exp.length; i++) {
      if (exp[i] != nn[i]) {
        fail("Expected " + exp[i] + " got " + nn[i]);
      }
    }

  }
}
