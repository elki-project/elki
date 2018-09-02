/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram.HistogramIntersectionDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;
import de.lmu.ifi.dbs.elki.utilities.random.FastNonThreadsafeRandom;

/**
 * Validate spatial distance functions by ensuring that mindist <= distance for
 * random objects.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public abstract class AbstractSpatialPrimitiveDistanceFunctionTest {
  /**
   * MBR consistency check, around 0.
   *
   * @param dis Distance function to check
   */
  public static void spatialConsistency(SpatialPrimitiveDistanceFunction<? super NumberVector> dis) {
    final Random rnd = new FastNonThreadsafeRandom(0);
    final int dim = 7, iters = 10000;

    double[] d1 = new double[dim], d2 = new double[dim];
    double[] d3 = new double[dim], d4 = new double[dim];
    DoubleVector v1 = DoubleVector.wrap(d1), v2 = DoubleVector.wrap(d4);
    HyperBoundingBox mbr = new HyperBoundingBox(d2, d3);
    for(int i = 0; i < iters; i++) {
      for(int d = 0; d < dim; d++) {
        d1[d] = (rnd.nextDouble() - .5) * 2E4;
        d2[d] = (rnd.nextDouble() - .5) * 2E4;
        d3[d] = (rnd.nextDouble() - .5) * 2E4;
        if(d2[d] > d3[d]) {
          double t = d2[d];
          d2[d] = d3[d];
          d3[d] = t;
        }
        double m = rnd.nextDouble();
        d4[d] = m * d2[d] + (1 - m) * d3[d];
      }
      compareDistances(v1, v2, mbr, dis);
    }
  }

  /**
   * Test not involving negative values.
   * 
   * @param dis Distance function to test
   */
  public static void nonnegativeSpatialConsistency(SpatialPrimitiveDistanceFunction<? super NumberVector> dis) {
    List<SpatialPrimitiveDistanceFunction<? super NumberVector>> dists = new ArrayList<>();
    dists.add(new ELKIBuilder<>(HistogramIntersectionDistanceFunction.class).build());

    final Random rnd = new FastNonThreadsafeRandom(1);
    final int dim = 7, iters = 10000;
    double[] d1 = new double[dim], d2 = new double[dim];
    double[] d3 = new double[dim], d4 = new double[dim];
    DoubleVector v1 = DoubleVector.wrap(d1), v2 = DoubleVector.wrap(d4);
    HyperBoundingBox mbr = new HyperBoundingBox(d2, d3);
    for(int i = 0; i < iters; i++) {
      for(int d = 0; d < dim; d++) {
        d1[d] = rnd.nextDouble() * 2E4;
        d2[d] = rnd.nextDouble() * 2E4;
        d3[d] = rnd.nextDouble() * 2E4;
        if(d2[d] > d3[d]) {
          double t = d2[d];
          d2[d] = d3[d];
          d3[d] = t;
        }
        double m = rnd.nextDouble();
        d4[d] = m * d2[d] + (1 - m) * d3[d];
      }
      compareDistances(v1, v2, mbr, dis);
    }
  }

  public static void compareDistances(NumberVector v1, NumberVector v2, HyperBoundingBox mbr2, SpatialPrimitiveDistanceFunction<? super NumberVector> dist) {
    double exact = dist.distance(v1, v2), mind = dist.minDist(v1, v2),
        mbrd = dist.minDist(v1, mbr2), zero = dist.minDist(v2, mbr2);
    assertEquals("Not same: " + dist.toString(), exact, mind, 1e-10);
    assertTrue("Not smaller:" + dist.toString() + " " + mbrd + " > " + exact + " " + mbr2 + " " + v1, mbrd <= exact);
    assertEquals("Not zero: " + dist.toString(), 0., zero, 1e-10);
  }
}