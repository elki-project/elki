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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.outlier.meta.HiCS;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.FastNonThreadsafeRandom;

/**
 * Ensure basic integrity.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class HiCSDependenceMeasureTest {
  @Test
  public void testBasic() {
    DependenceMeasure cor = new ELKIBuilder<>(HiCSDependenceMeasure.class) //
        .with(HiCS.Parameterizer.SEED_ID, 0) //
        .build();
    // Note: only positive correlations are accepted.
    checkPerfectLinear(cor, 1000, 0.800, 0.280, 0.05);
    checkUniform(cor, 1000, 0.950, 0.02, 0.50, 0.08);
  }

  // Ugly duplication, but necessary because we cannot access the *test* classes
  // of other modules easily
  public static void checkPerfectLinear(DependenceMeasure m, int len, double expectp, double expectn, double tol) {
    Random r = new FastNonThreadsafeRandom(0L);
    double[] x = new double[len], y = new double[len], z = new double[len];
    for(int i = 0; i < len; i++) {
      z[i] = -(y[i] = (x[i] = r.nextGaussian() * Math.PI + Math.E) * MathUtil.SQRTTHIRD + MathUtil.LOG2);
    }
    double[] res = m.dependence(DoubleArrayAdapter.STATIC, Arrays.asList(new double[][] { x, y, z }));
    assertEquals("Perfect positive linear", expectp, m.dependence(x, y), tol);
    assertEquals("Perfect positive linear", expectp, res[0], tol);
    assertEquals("Perfect negative linear", expectn, m.dependence(x, z), tol);
    assertEquals("Perfect negative linear", expectn, res[1], tol);
    assertEquals("Perfect negative linear", expectn, m.dependence(y, z), tol);
    assertEquals("Perfect negative linear", expectn, res[2], tol);
  }

  public static void checkUniform(DependenceMeasure m, int len, double expectSelf, double tolSelf, double expectCross, double tolCross) {
    Random r = new FastNonThreadsafeRandom(0L);
    double[] x = new double[len], y = new double[len];
    for(int i = 0; i < len; i++) {
      x[i] = r.nextDouble() * Math.PI - MathUtil.SQRT3;
      y[i] = r.nextDouble() * -Math.E + MathUtil.LOGLOG2;
    }
    assertEquals("Uniform", expectCross, m.dependence(x, y), tolCross);
    assertEquals("Uniform-self1", expectSelf, m.dependence(x, x), tolSelf);
    assertEquals("Uniform-self2", expectSelf, m.dependence(y, y), tolSelf);
  }
}
