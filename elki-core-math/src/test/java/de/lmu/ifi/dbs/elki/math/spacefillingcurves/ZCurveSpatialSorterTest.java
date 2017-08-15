/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Class to unit test the z-curve based spatial sorter.
 * 
 * Note that it is not bit based, but it repeatedly subdivides [min;max].
 * 
 * @author Erich Schubert
 */
public class ZCurveSpatialSorterTest {
  @Test
  public void testBasic() {
    ArrayList<V> vs = new ArrayList<>();
    vs.add(new V(0, 0, 0));
    vs.add(new V(0, .1, 0));
    vs.add(new V(0, .1, .1));
    vs.add(new V(0, 0, .25));
    vs.add(new V(0, 0, .5));
    vs.add(new V(0, 0, 1));
    vs.add(new V(0, 1, 0));
    vs.add(new V(1, 0, 0));
    vs.add(new V(0, 0, 2));
    vs.add(new V(0, 0, 3));
    vs.add(new V(0, 2, 0));
    vs.add(new V(0, 3, 0));
    vs.add(new V(2, 0, 0));
    vs.add(new V(3, 0, 0));
    ArrayList<V> x = new ArrayList<>(vs);
    Collections.shuffle(x, new Random(0L));
    boolean shuffled = false;
    for(int i = 0; i < vs.size(); i++) {
      if(vs.get(i) != x.get(i)) {
        shuffled = true;
        break;
      }
    }
    assertTrue("Data was not shuffled.", shuffled);
    ZCurveSpatialSorter.STATIC.sort(x);
    for(int i = 0; i < vs.size(); i++) {
      assertSame("Data was not ordered correctly, at position " + i, vs.get(i), x.get(i));
    }
  }

  /**
   * Trivial pseudo-vector to use for testing. Because of module dependencies,
   * we do not have access to our usual DoubleVector here!
   * 
   * @author Erich Schubert
   */
  private static class V implements SpatialComparable {
    double[] data;

    public V(double... data) {
      this.data = data;
    }

    @Override
    public int getDimensionality() {
      return data.length;
    }

    @Override
    public double getMin(int dimension) {
      return data[dimension];
    }

    @Override
    public double getMax(int dimension) {
      return data[dimension];
    }

    @Override
    public String toString() {
      return Arrays.toString(data);
    }
  }
}
