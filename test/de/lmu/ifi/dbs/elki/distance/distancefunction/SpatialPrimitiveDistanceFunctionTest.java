package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.colorhistogram.HistogramIntersectionDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Validate spatial distance functions by ensuring that mindist <= distance for
 * random objects.
 * 
 * @author Erich Schubert
 * 
 */
public class SpatialPrimitiveDistanceFunctionTest implements JUnit4Test {
  @Test
  public void testSpatialDistanceConsistency() {
    final Random rnd = new Random(0);
    final int dim = 7;
    final int iters = 10000;

    List<SpatialPrimitiveDistanceFunction<? super NumberVector<?, ?>, ?>> dists = new ArrayList<SpatialPrimitiveDistanceFunction<? super NumberVector<?, ?>, ?>>();
    dists.add(EuclideanDistanceFunction.STATIC);
    dists.add(ManhattanDistanceFunction.STATIC);
    dists.add(MaximumDistanceFunction.STATIC);
    dists.add(MinimumDistanceFunction.STATIC);
    dists.add(new LPNormDistanceFunction(3));
    dists.add(new LPNormDistanceFunction(.5));
    dists.add(CanberraDistanceFunction.STATIC);
    // Histogram intersection distance isn't proper for negative values
    // dists.add(HistogramIntersectionDistanceFunction.STATIC);
    dists.add(SquaredEuclideanDistanceFunction.STATIC);
    dists.add(ArcCosineDistanceFunction.STATIC);
    dists.add(CosineDistanceFunction.STATIC);

    double[] d1 = new double[dim];
    double[] d2 = new double[dim];
    double[] d3 = new double[dim];
    double[] d4 = new double[dim];
    Vector v1 = new Vector(d1);
    ModifiableHyperBoundingBox mbr = new ModifiableHyperBoundingBox(d2, d3);
    Vector v2 = new Vector(d4);
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
      for(SpatialPrimitiveDistanceFunction<? super NumberVector<?, ?>, ?> dis : dists) {
        compareDistances(v1, mbr, v2, dis);
      }
    }
  }

  @Test
  public void testSpatialDistanceConsistencyPositive() {
    final Random rnd = new Random(1);
    final int dim = 7;
    final int iters = 10000;

    List<SpatialPrimitiveDistanceFunction<? super NumberVector<?, ?>, ?>> dists = new ArrayList<SpatialPrimitiveDistanceFunction<? super NumberVector<?, ?>, ?>>();
    dists.add(EuclideanDistanceFunction.STATIC);
    dists.add(ManhattanDistanceFunction.STATIC);
    dists.add(MaximumDistanceFunction.STATIC);
    dists.add(MinimumDistanceFunction.STATIC);
    dists.add(new LPNormDistanceFunction(3));
    dists.add(new LPNormDistanceFunction(.5));
    dists.add(CanberraDistanceFunction.STATIC);
    dists.add(HistogramIntersectionDistanceFunction.STATIC);
    dists.add(SquaredEuclideanDistanceFunction.STATIC);
    dists.add(ArcCosineDistanceFunction.STATIC);
    dists.add(CosineDistanceFunction.STATIC);

    double[] d1 = new double[dim];
    double[] d2 = new double[dim];
    double[] d3 = new double[dim];
    double[] d4 = new double[dim];
    Vector v1 = new Vector(d1);
    ModifiableHyperBoundingBox mbr = new ModifiableHyperBoundingBox(d2, d3);
    Vector v2 = new Vector(d4);
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
      for(SpatialPrimitiveDistanceFunction<? super NumberVector<?, ?>, ?> dis : dists) {
        compareDistances(v1, mbr, v2, dis);
      }
    }
  }

  protected <D extends Distance<D>> void compareDistances(Vector v1, ModifiableHyperBoundingBox mbr, Vector v2, SpatialPrimitiveDistanceFunction<? super NumberVector<?, ?>, D> dist) {
    D exact = dist.distance(v1, v2);
    D mind = dist.minDist(v1, v2);
    D mbrd = dist.minDist(v1, mbr);
    assertEquals("Not same: " + dist.toString(), exact, mind);
    assertTrue("Not smaller:" + dist.toString() + " " + mbrd + " > " + exact + " " + mbr + " " + v1, mbrd.compareTo(exact) <= 0);
  }
}