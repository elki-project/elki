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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * The choose subtree method proposed by the R*-Tree for leaf nodes.
 * <p>
 * Reference:
 * <p>
 * Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger<br>
 * The R*-tree: an efficient and robust access method for points and
 * rectangles<br>
 * Proc. 1990 ACM SIGMOD Int. Conf. Management of Data
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger", //
    title = "The R*-tree: an efficient and robust access method for points and rectangles", //
    booktitle = "Proc. 1990 ACM SIGMOD Int. Conf. Management of Data", //
    url = "https://doi.org/10.1145/93597.98741", //
    bibkey = "DBLP:conf/sigmod/BeckmannKSS90")
public class LeastOverlapInsertionStrategy implements InsertionStrategy {
  /**
   * Static instance.
   */
  public static final LeastOverlapInsertionStrategy STATIC = new LeastOverlapInsertionStrategy();

  /**
   * Constructor.
   */
  public LeastOverlapInsertionStrategy() {
    super();
  }

  @Override
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth) {
    final int size = getter.size(options);
    assert (size > 0) : "Choose from empty set?";
    // R*-Tree: overlap increase for leaves.
    int best = -1;
    double least_overlap = Double.POSITIVE_INFINITY;
    double least_areainc = Double.POSITIVE_INFINITY;
    double least_area = Double.POSITIVE_INFINITY;
    // least overlap increase, on reduced candidate set:
    for(int i = 0; i < size; i++) {
      // Existing object and extended rectangle:
      SpatialComparable entry = getter.get(options, i);
      HyperBoundingBox mbr = SpatialUtil.union(entry, obj);
      // Compute relative overlap increase.
      double overlap_wout = 0.0;
      double overlap_with = 0.0;
      for(int k = 0; k < size; k++) {
        if(i != k) {
          SpatialComparable other = getter.get(options, k);
          overlap_wout += SpatialUtil.relativeOverlap(entry, other);
          overlap_with += SpatialUtil.relativeOverlap(mbr, other);
        }
      }
      double inc_overlap = overlap_with - overlap_wout;
      if(inc_overlap < least_overlap) {
        final double area = SpatialUtil.volume(entry);
        final double inc_area = SpatialUtil.volume(mbr) - area;
        // Volume increase and overlap increase:
        least_overlap = inc_overlap;
        least_areainc = inc_area;
        least_area = area;
        best = i;
      }
      else if(inc_overlap == least_overlap) {
        final double area = SpatialUtil.volume(entry);
        final double inc_area = SpatialUtil.volume(mbr) - area;
        if(inc_area < least_areainc || (inc_area == least_areainc && area < least_area)) {
          least_overlap = inc_overlap;
          least_areainc = inc_area;
          least_area = area;
          best = i;
        }
      }
    }
    assert (best > -1) : "No split found? Volume outside of double precision?";
    return best;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LeastOverlapInsertionStrategy makeInstance() {
      return LeastOverlapInsertionStrategy.STATIC;
    }
  }
}