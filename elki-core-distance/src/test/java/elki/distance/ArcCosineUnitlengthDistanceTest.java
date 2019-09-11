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
package elki.distance;

import static elki.math.linearalgebra.VMath.normalizeEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.data.HyperBoundingBox;
import elki.data.SparseDoubleVector;
import elki.math.MathUtil;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for unit-length ArcCosine distance.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ArcCosineUnitlengthDistanceTest extends AbstractDistanceTest {
  @Test
  public void testSpatialConsistency() {
    // Also test the builder - we could have just used .STATIC
    ArcCosineUnitlengthDistance dist = new ELKIBuilder<>(ArcCosineUnitlengthDistance.class).build();
    basicChecks(dist);
    // Note: some of these are not well defined, as we have zero vectors.
    varyingLengthBasic(0, dist, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI, MathUtil.HALFPI);
    nonnegativeSpatialConsistency(dist);
  }

  @Test
  public void compareOnUnitSphere() {
    Random r = new Random(0L);
    double[][] data = new double[100][10];
    for(int i = 0; i < data.length; i++) {
      double[] row = data[i];
      for(int j = 0; j < row.length; j++) {
        row[j] = r.nextDouble();
      }
      normalizeEquals(row);
    }

    ArcCosineUnitlengthDistance cosnorm = ArcCosineUnitlengthDistance.STATIC;
    ArcCosineDistance cosfull = ArcCosineDistance.STATIC;
    for(int i = 0; i < data.length; i++) {
      for(int j = 0; j < data.length; j++) {
        assertSameDistance(cosfull, cosnorm, DoubleVector.wrap(data[i]), DoubleVector.wrap(data[j]), 1e-7);
        assertSameMinDist(cosfull, cosnorm, DoubleVector.wrap(data[i]), DoubleVector.wrap(data[j]), 1e-7);
        assertSameMinDist(cosfull, cosnorm, new HyperBoundingBox(data[i], data[i]), new HyperBoundingBox(data[j], data[j]), 1e-7);
      }
    }
  }

  @Test
  public void compareSparseUnitSphere() {
    Random r = new Random(0L);
    int dim = 100, set = 20;
    int[] dimset = MathUtil.sequence(0, dim);
    SparseDoubleVector[] sparse = new SparseDoubleVector[100];
    for(int i = 0; i < sparse.length; i++) {
      // Fisher-Yates shuffle to choose non-zero dimensions
      int[] idx = new int[set];
      for(int j = 0; j < set; j++) {
        int p = r.nextInt(dim - j) + j;
        int tmp = dimset[j]; // Swap
        idx[j] = dimset[j] = dimset[p];
        dimset[p] = tmp; // Swap
      }
      Arrays.sort(idx);
      // Choose values
      double[] vals = new double[set];
      for(int j = 0; j < set; j++) {
        vals[j] = r.nextDouble();
      }
      normalizeEquals(vals);
      sparse[i] = new SparseDoubleVector(idx, vals, dim);
    }
    DoubleVector[] dense = new DoubleVector[10];
    for(int i = 0; i < dense.length; i++) {
      double[] row = new double[dim];
      for(int j = 0; j < dim; j++) {
        row[j] = r.nextDouble();
      }
      dense[i] = DoubleVector.wrap(normalizeEquals(row));
    }

    ArcCosineUnitlengthDistance cosnorm = ArcCosineUnitlengthDistance.STATIC;
    ArcCosineDistance cosfull = ArcCosineDistance.STATIC;
    for(int i = 0; i < sparse.length; i++) {
      for(int j = 0; j < sparse.length; j++) {
        assertSameDistance(cosfull, cosnorm, sparse[i], sparse[j], 1e-7);
        assertSameMinDist(cosfull, cosnorm, sparse[i], sparse[j], 1e-7);
      }
      for(int j = 0; j < dense.length; j++) {
        assertSameDistance(cosfull, cosnorm, sparse[i], dense[j], 1e-7);
        assertSameMinDist(cosfull, cosnorm, sparse[i], dense[j], 1e-7);
        assertSameDistance(cosfull, cosnorm, dense[j], sparse[i], 1e-7);
        assertSameMinDist(cosfull, cosnorm, dense[j], sparse[i], 1e-7);
      }
    }
  }

  @Test
  public void testNotArcCosine() {
    DoubleVector d1 = DoubleVector.wrap(new double[] { 1, 0 });
    DoubleVector d2 = DoubleVector.wrap(new double[] { .5, 0 });
    ArcCosineUnitlengthDistance cosnorm = new ELKIBuilder<>(ArcCosineUnitlengthDistance.class).build();
    ArcCosineDistance cosfull = new ELKIBuilder<>(ArcCosineDistance.class).build();
    assertEquals("ArcCosine not ok", 0, cosfull.distance(d1, d2), 0);
    assertEquals("Length not ignored", 1.04719, cosnorm.distance(d1, d2), 1e-5);
  }
}
