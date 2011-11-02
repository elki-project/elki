package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.Arrays;
import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Quadratic-time complexity split as used by Diane Greene for the R-Tree.
 * 
 * Seed selection is quadratic, distribution is O(n log n).
 * 
 * This contains a slight modification to improve performance with point data:
 * with points as seeds, the normalized separation is always 1, so we choose the
 * raw separation then.
 * 
 * <p>
 * Diane Greene:<br />
 * An implementation and performance analysis of spatial data access methods<br />
 * In: Proceedings of the Fifth International Conference on Data Engineering
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "Diane Greene", title = "An implementation and performance analysis of spatial data access methods", booktitle = "Proceedings of the Fifth International Conference on Data Engineering", url = "http://dx.doi.org/10.1109/ICDE.1989.47268")
public class GreeneSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final GreeneSplit STATIC = new GreeneSplit();

  @Override
  public <E extends SpatialComparable, A> BitSet split(A entries, ArrayAdapter<E, A> getter, int minEntries) {
    final int num = getter.size(entries);
    // Choose axis by best normalized separation
    int axis = -1;
    {
      // PickSeeds - find the two most distant rectangles
      double worst = Double.NEGATIVE_INFINITY;
      int w1 = 0, w2 = 0;

      // Compute individual areas
      double[] areas = new double[num];
      for(int e1 = 0; e1 < num - 1; e1++) {
        final E e1i = getter.get(entries, e1);
        areas[e1] = SpatialUtil.volume(e1i);
      }
      // Compute area increase
      for(int e1 = 0; e1 < num - 1; e1++) {
        final E e1i = getter.get(entries, e1);
        for(int e2 = e1 + 1; e2 < num; e2++) {
          final E e2i = getter.get(entries, e2);
          final double areaJ = SpatialUtil.volumeUnion(e1i, e2i);
          final double d = areaJ - areas[e1] - areas[e2];
          if(d > worst) {
            worst = d;
            w1 = e1;
            w2 = e2;
          }
        }
      }
      // Data to keep
      // Initial mbrs and areas
      E m1 = getter.get(entries, w1);
      E m2 = getter.get(entries, w2);

      double bestsep = Double.NEGATIVE_INFINITY;
      double bestsep2 = Double.NEGATIVE_INFINITY;
      for(int d = 1; d <= m1.getDimensionality(); d++) {
        final double s1 = m1.getMin(d) - m2.getMax(d);
        final double s2 = m2.getMin(d) - m1.getMax(d);
        final double sm = Math.max(s1, s2);
        final double no = Math.max(m1.getMax(d), m2.getMax(d)) - Math.min(m1.getMin(d), m2.getMin(d));
        final double sep = sm / no;
        if(sep > bestsep || (sep == bestsep && sm > bestsep2)) {
          bestsep = sep;
          bestsep2 = sm;
          axis = d;
        }
      }
    }
    // Sort by minimum value
    DoubleIntPair[] data = new DoubleIntPair[num];
    for(int i = 0; i < num; i++) {
      data[i] = new DoubleIntPair(getter.get(entries, i).getMin(axis), i);
    }
    Arrays.sort(data);
    // Object assignment
    final BitSet assignment = new BitSet(num);
    final int half = (num + 1) / 2;
    // Put the first half into second node
    for(int i = 0; i < half; i++) {
      assignment.set(data[i].second);
    }
    // Tie handling
    if(num % 2 == 0) {
      // We need to compute the bounding boxes
      ModifiableHyperBoundingBox mbr1 = new ModifiableHyperBoundingBox(getter.get(entries, data[0].second));
      for(int i = 1; i < half; i++) {
        mbr1.extend(getter.get(entries, data[i].second));
      }
      ModifiableHyperBoundingBox mbr2 = new ModifiableHyperBoundingBox(getter.get(entries, data[num - 1].second));
      for(int i = half + 1; i < num - 1; i++) {
        mbr2.extend(getter.get(entries, data[i].second));
      }
      E e = getter.get(entries, data[half].second);
      double inc1 = SpatialUtil.volumeUnion(mbr1, e) - SpatialUtil.volume(mbr1);
      double inc2 = SpatialUtil.volumeUnion(mbr2, e) - SpatialUtil.volume(mbr2);
      if(inc1 < inc2) {
        assignment.set(data[half].second);
      }
    }
    return assignment;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected GreeneSplit makeInstance() {
      return GreeneSplit.STATIC;
    }
  }
}