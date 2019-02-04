/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.math.linearalgebra;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit test for QR decomposition.
 * 
 * As reference if the QR Decomposition is working correctly the QR
 * decomposition functions of python3 numpy 1.12.1-3 package,
 * and octave 4.2.1 were used to calculate the correct result.
 *
 * @author Erich Schubert
 * @author Merlin Dietrich
 * @since 0.7.5
 */
public final class QRDecompositionTest {
  private static final double[][] M = { //
      { 99, 53, 1, 36, 92, 53, 23, -13, 14, -80 }, //
      { -40, 28, 62, 21, -27, 18, -26, 25, 59, 1 }, //
      { 13, -44, -80, 72, 25, 94, -30, -82, -25, -20 }, //
      { 87, 63, -76, 89, 14, -16, 36, -9, -89, 97 }, //
      { 34, -35, 14, 58, 9, -86, -94, -100, -57, 59 }, //
      { -24, -19, -98, 50, 98, -17, 2, 62, -19, -80 }, //
      { -7, -81, 96, -6, -14, 93, -87, -98, -89, 75 }, //
      { 45, -84, -85, 5, -24, 15, -14, -96, 20, 96 }, //
      { 58, -78, 98, 13, -30, -35, -42, -67, -81, -60 }, //
      { -39, 40, -62, -12, -71, 56, -59, 35, -34, -94 }, //
      { -47, -67, -36, -78, -62, -57, -87, 79, -43, 36 }, //
      { 4, -73, -13, 25, -28, -23, -11, 40, -29, -86 }, //
      { -33, -1, -47, 14, 30, -19, -2, 29, -28, -59 } };

  private static final double[][] Q_NUMPY = { //
      { -0.5609808, -0.22897877, 0.03453391, 0.15315285, -0.38040134, -0.2557967, -0.3630094, 0.33914302, -0.29791983, -0.11549717 }, //
      { 0.22665891, -0.14834545, 0.27000422, -0.42409299, 0.23284698, -0.04168175, -0.04680079, 0.19674463, -0.56056158, 0.30038819 }, //
      { -0.07366415, 0.2183273, -0.3534289, -0.39899094, 0.04224936, -0.45694717, -0.00875397, -0.10727794, -0.07608988, -0.09541652 }, //
      { -0.49298313, -0.28128137, -0.27827269, -0.18922638, 0.37286029, 0.13604861, 0.03076088, 0.17059541, 0.41100508, 0.43997165 }, //
      { -0.19266007, 0.18068643, 0.03988104, -0.34588228, 0.09338436, 0.48215135, -0.56044071, -0.37607659, -0.15083164, -0.05078833 }, //
      { 0.13599535, 0.08549468, -0.41521831, -0.3460679, -0.52317268, 0.13626419, -0.0856599, 0.24715769, 0.05051845, 0.02313689 }, //
      { 0.03966531, 0.39274674, 0.35298629, -0.16191133, -0.08175946, -0.49960903, -0.26329238, -0.00387538, 0.38880044, 0.34364993 }, //
      { -0.25499127, 0.42279234, -0.39469319, 0.25857725, 0.1445212, -0.13948065, 0.13848698, -0.3238092, -0.34839762, 0.12321134 }, //
      { -0.32865542, 0.39740183, 0.3642577, -0.03762339, 0.14334547, 0.15936863, 0.03100735, 0.20383843, 0.16214909, -0.42563005 }, //
      { 0.22099244, -0.20654114, -0.23548093, 0.04059729, 0.51090273, -0.29772057, -0.40767952, 0.13200353, 0.07455998, -0.44969292 }, //
      { 0.26632422, 0.31264228, -0.18535196, 0.44892297, 0.07540451, 0.2096527, -0.4328197, 0.34859649, -0.00129953, 0.31727536 }, //
      { -0.02266589, 0.35701435, -0.09248929, -0.18343847, 0.1851614, 0.1118405, 0.31530434, 0.56170102, -0.13701706, -0.11565145 }, //
      { 0.1869936, -0.00491301, -0.19527958, -0.17597947, -0.16948721, 0.1221491, -0.03412181, -0.04766221, 0.27170533, -0.24481283 } };

  private static final double[][] R_NUMPY = { //
      { -176.47662735, -10.73796586, 2.31758736, -96.14870963, -65.41376143, -4.90149893, -38.36202052, 127.0366526, 54.08081593, -45.5414415 }, { 0., -205.15529749, 26.48780652, -14.04574229, -47.96653746, -15.32189473, -110.60923218, -102.96646247, -79.63098275, 36.79662449 }, { 0., 0., 242.23340995, -48.86661202, -38.631622, -1.07663499, -19.27602277, -45.29954314, 10.94363098, 14.81993916 }, { 0., 0., 0., -127.10309591, -57.78009643, -23.97981176, 24.49194055, 52.96853963, 42.42144841, 37.66796299 }, { 0., 0., 0., 0., -143.28052672, -4.28033246, -51.80422555, -26.96481647, -43.89216604, 61.25782548 }, { 0., 0., 0., 0., 0., -190.86828728, 5.3332843, 64.59420421, -13.5860553, 18.58871992 }, { 0., 0., 0., 0., 0., 0., 124.72133429, 28.31474906, 71.18551349, -4.73561384 }, { 0., 0., 0., 0., 0., 0., 0., 131.75600232, -36.47948289, -139.16223542 }, { 0., 0., 0., 0., 0., 0., 0., 0., -125.1023439, 26.40411814 }, { 0., 0., 0., 0., 0., 0., 0., 0., 0., 190.5011458 } };

  private static final double[][] Q_OCTAVE = { //
      { -0.5609808023188088, -0.228978768130125, 0.03453391224038137, 0.1531528528852334, -0.3804013418188837, -0.2557966967830153, -0.3630094043983836, 0.3391430231677329, -0.2979198295999402, -0.1154971725525416 }, //
      { 0.2266589100278015, -0.1483454534703137, 0.2700042238589698, -0.4240929851738812, 0.2328469828381808, -0.0416817462853825, -0.04680079005041395, 0.1967446329543257, -0.5605615813560102, 0.3003881947316116 }, //
      { -0.0736641457590355, 0.2183273043882638, -0.3534289028176957, -0.3989909377057776, 0.04224936077845472, -0.456947166521257, -0.008753968720364623, -0.1072779404954957, -0.07608987996887417, -0.09541652228629885 }, //
      { -0.4929831293104683, -0.2812813741201204, -0.2782726872116074, -0.1892263830414622, 0.3728602869477068, 0.1360486093461999, 0.03076087537606546, 0.1705954092738633, 0.4110050760694947, 0.4399716482790399 }, //
      { -0.1926600735236313, 0.1806864250920764, 0.03988103657570209, -0.3458822802303157, 0.09338435996898066, 0.4821513548116002, -0.5604407067899729, -0.3760765904867311, -0.1508316446728739, -0.05078832523164518 }, //
      { 0.1359953460166809, 0.08549468052448078, -0.4152183122558316, -0.3460678971148873, -0.5231726828377113, 0.1362641926502566, -0.08565990302627896, 0.2471576868164062, 0.05051845424089513, 0.02313688990933054 }, //
      { 0.03966530925486526, 0.3927467447743394, 0.3529862887753657, -0.1619113345746532, -0.08175946269714233, -0.4996090314031008, -0.2632923791738811, -0.003875376100572883, 0.3888004350814354, 0.343649929401522 }, //
      { -0.2549912737812767, 0.4227923366024302, -0.3946931890121958, 0.2585772473864537, 0.144521200264545, -0.1394806471114956, 0.1384869772071582, -0.3238092034636126, -0.3483976170822832, 0.1232113436624614 }, //
      { -0.3286554195403122, 0.3974018300875252, 0.3642577003661374, -0.03762338653825096, 0.1433454707129216, 0.1593686331231659, 0.0310073469485692, 0.2038384345968912, 0.1621490861599973, -0.4256300457444612 }, //
      { 0.2209924372771065, -0.2065411411029607, -0.2354809252244215, 0.04059728782875782, 0.5109027261053656, -0.2977205654247393, -0.407679523865492, 0.1320035280271998, 0.07455998483046583, -0.4496929194531319 }, //
      { 0.2663242192826668, 0.312642278356913, -0.1853519621060631, 0.4489229716489354, 0.07540450955235717, 0.2096526997840654, -0.4328197047566908, 0.3485964863760004, -0.001299532058873697, 0.3172753618559941 }, //
      { -0.02266589100278015, 0.3570143518550805, -0.09248929327520765, -0.1834384664751122, 0.1851613977857876, 0.1118405031619269, 0.3153043390108959, 0.5617010155350197, -0.1370170578503515, -0.1156514543967723 }, //
      { 0.1869936007729363, -0.004913014257681335, -0.1952795819694221, -0.1759794658421194, -0.1694872119929945, 0.1221490955617446, -0.03412180501152451, -0.04766220943809182, 0.2717053342915861, -0.2448128284071716 } };

  private static final double[][] R_OCTAVE = { //
      { -176.4766273476462, -10.7379658625671, 2.317587355034265, -96.14870963379337, -65.41376143402351, -4.901498929351206, -38.3620205222054, 127.036652597832, 54.08081593263344, -45.541441497336 }, //
      { 0, -205.1552974922518, 26.48780651790559, -14.0457422910626, -47.96653746243623, -15.32189473167241, -110.6092321796881, -102.9664624657724, -79.6309827481856, 36.79662449084746 }, //
      { 0, 0, 242.2334099473518, -48.86661201985542, -38.63162200478573, -1.076634990099222, -19.27602276774847, -45.2995431408285, 10.94363097677307, 14.81993916404392 }, //
      { 0, 0, 0, -127.1030959062385, -57.78009642853891, -23.97981175659016, 24.49194054672926, 52.96853962742615, 42.42144840552683, 37.66796299200229 }, //
      { 0, 0, 0, 0, -143.2805267191095, -4.280332458001809, -51.8042255535471, -26.96481647148225, -43.89216604086222, 61.25782548021888 }, //
      { 0, 0, 0, 0, 0, -190.8682872803722, 5.333284295920585, 64.59420420662612, -13.58605530322502, 18.58871992198336 }, //
      { 0, 0, 0, 0, 0, 0, 124.721334285689, 28.31474905960179, 71.18551349227089, -4.73561384100995 }, //
      { 0, 0, 0, 0, 0, 0, 0, 131.7560023158572, -36.47948288811226, -139.1622354150675 }, //
      { 0, 0, 0, 0, 0, 0, 0, 0, -125.1023439006518, 26.40411813513857 }, //
      { 0, 0, 0, 0, 0, 0, 0, 0, 0, 190.5011457972481 }, };

  @Test
  public void testJamaExample() {
    double[][] M = transpose(new double[][] { { 1., 2., 3., 4. }, { 5., 6., 7., 8. }, { 9., 10., 11., 12. } });
    QRDecomposition qr = new QRDecomposition(M);
    final double[][] q = qr.getQ(), r = qr.getR();
    assertTrue(almostEquals(unitMatrix(q[0].length), transposeTimes(q, q)));
    checkTriangular(r);
    assertTrue("Not a proper decomposition.", almostEquals(M, times(q, r), 1e-14));
  }

  @Test
  public void testJamaSolve() {
    double[][] s = { { 5., 8. }, { 6., 9. } };
    double[][] in = { { 13 }, { 15 } };
    double[][] sol = { { 1 }, { 1 } };
    double[][] o = new QRDecomposition(s).solve(in);
    assertTrue("Not solved.", almostEquals(sol, o, 1e-14));

    double[] in2 = { 13, 15 };
    double[] sol2 = { 1, 1 };
    double[] o2 = new QRDecomposition(s).solve(in2);
    assertTrue("Not solved.", almostEquals(sol2, o2, 1e-14));

    double[][] p = { { 4., 1., 1. }, { 1., 2., 3. }, { 1., 3., 6. } };
    double[][] o3 = new QRDecomposition(p).solve(unitMatrix(3));
    assertTrue("Not solved.", almostEquals(unitMatrix(3), times(p, o3), 1e-14));
  }

  @Test
  public void testWikipedia() {
    double[][] m = { //
        { 12, -51, 4 }, //
        { 6, 167, -68 }, //
        { -4, 24, -41 }//
    };
    QRDecomposition qr = new QRDecomposition(m);
    double[][] q = qr.getQ(), r = qr.getR();

    // Check that Q^T Q = Unit, i.e. rotation factor.
    assertTrue(almostEquals(unitMatrix(q[0].length), transposeTimes(q, q)));
    checkTriangular(r);
    assertTrue("Not a proper decomposition.", almostEquals(m, times(q, r), 1e-13));
  }

  public void checkTriangular(double[][] r) {
    for(int row = 1; row < r.length; row++) {
      for(int col = 0; col < row; col++) {
        assertEquals(0., r[row][col], 0.);
      }
    }
  }

  @Test
  public void testRank4() {
    double delta = 1e-14;
    double[][] m = transpose(new double[][] { //
        { 1, 1, 1, 1 + delta, 1, 1 }, //
        { 1, 1, 1, delta, 0, 0 }, //
        { 0, 0, 0, 1, 1, 1 }, //
        { 1, 0, 0, 1, 0, 0 }, //
        { 0, 0, 1, 0, 0, 1 } //
    });

    QRDecomposition qr = new QRDecomposition(m);
    double[][] q = qr.getQ(), r = qr.getR();
    assertTrue(almostEquals(unitMatrix(q[0].length), transposeTimes(q, q)));
    checkTriangular(r);
    assertTrue("Not a proper decomposition.", almostEquals(m, times(q, r), 1e-14));
    assertEquals("Rank not as expected", 4, qr.rank(1e-14));
  }

  /**
   * Testing the getQ method of the QRDDecomposition class.
   */
  @Test
  public void testGetQ() {
    // assert that octave and numpy functions have almost same results
    assertTrue(almostEquals(Q_OCTAVE, Q_NUMPY, 1e-8));

    final double[][] q = new QRDecomposition(M).getQ();
    assertTrue(almostEquals(q, Q_OCTAVE, 1e-15));
    assertTrue(almostEquals(q, Q_NUMPY, 1e-8));
  }

  /**
   * Testing the getR method of the QRDDecomposition class.
   */
  @Test
  public void testGetR() {
    // assert that octave and numpy functions have almost same results
    assertTrue(almostEquals(R_OCTAVE, R_NUMPY, 1e-8));

    final double[][] r = new QRDecomposition(M).getR();
    assertTrue(almostEquals(r, R_OCTAVE, 1e-13));
    assertTrue(almostEquals(r, R_NUMPY, 1e-8));
  }

  /**
   * Testing the getH method of the QRDDecomposition class, but
   * only testing if method runs without exception.
   */
  @Test
  public void testGetH() {
    new QRDecomposition(M).getH();
  }

  /**
   * Testing the IsFullRank() method of the QRDDecomposition class.
   */
  @Test
  public void testIsFullRank() {
    final double[][] m1 = { { 1, 3 }, //
        { -1, 4 }, //
        { 1, 1 } };
    assertTrue(new QRDecomposition(m1).isFullRank());

    final double[][] m2 = { { 1, 1 }, //
        { 1, 1 }, //
        { 2, 2 } };
    assertFalse(new QRDecomposition(m2).isFullRank());
  }

  /**
   * Testing the Solve method of the QRDDecomposition class
   * with {@link M} and local B as matrix test data.
   */
  @Test
  public void testSolve() {
    final double[] lsq_octave = { 0.01076018062819501, -0.005427983208297543, -0.001572290955308337, 0.01219551340160215, -0.0009912695684353332, 0.004877478839811684, -0.01700610419895007, 0.01094414459995209, 0.001761189365626154, 0.0002632220376317611 };
    final double[] lsq_numpy = { 0.01076018, -0.00542798, -0.00157229, 0.01219551, -0.00099127, 0.00487748, -0.0170061, 0.01094414, 0.00176119, 0.00026322 };

    // assert that octave and numpy functions have almost same results
    assertTrue(almostEquals(lsq_numpy, lsq_octave, 1E-6));

    final double[] B = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
    // Matrix based API
    // Double transpose hack: to convert double[N] to double[N][1].
    final double[][] XM = new QRDecomposition(M).solve(transpose(transpose(B)));
    assertTrue(almostEquals(XM, transpose(transpose(lsq_octave)), 1e-16));
    assertTrue(almostEquals(XM, transpose(transpose(lsq_numpy)), 1e-8));

    // Vector-based API
    final double[] X = new QRDecomposition(M).solve(B);
    assertTrue(almostEquals(X, lsq_octave, 1e-16));
    assertTrue(almostEquals(X, lsq_numpy, 1e-8));
  }

  /**
   * Testing that the solve method of the QRDDecomposition class raises an
   * exception if
   * IsFullRank returns false.
   */
  @Test(expected = ArithmeticException.class)
  public void testSolveIsFullRank() {
    new QRDecomposition(new double[][] { { 1, 1 }, { 1, 1 } }).solve(new double[][] { { 1 }, { 1 } });
  }

  /**
   * Testing that the solve method of the QRDDecomposition class raises an
   * exception if
   * the row dimensions do not agree.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSolveRowDimensionMismatch() {
    new QRDecomposition(M).solve(new double[][] { {} });
  }
}
