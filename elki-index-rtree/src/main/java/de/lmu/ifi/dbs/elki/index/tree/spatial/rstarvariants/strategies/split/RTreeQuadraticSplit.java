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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Quadratic-time complexity greedy split as used by the original R-Tree.
 * <p>
 * Reference:
 * <p>
 * A. Guttman<br>
 * R-Trees: A Dynamic Index Structure For Spatial Searching<br>
 * Proc. 1984 ACM SIGMOD Int. Conf. Management of Data
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "A. Guttman", //
    title = "R-Trees: A Dynamic Index Structure For Spatial Searching", //
    booktitle = "Proc. 1984 ACM SIGMOD Int. Conf. on Management of Data", //
    url = "https://doi.org/10.1145/971697.602266", //
    bibkey = "doi:10.1145/971697.602266")
public class RTreeQuadraticSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final RTreeQuadraticSplit STATIC = new RTreeQuadraticSplit();

  @Override
  public <E extends SpatialComparable, A> long[] split(A entries, ArrayAdapter<E, A> getter, int minEntries) {
    final int num = getter.size(entries);
    // Object assignment, and processed objects
    long[] assignment = BitsUtil.zero(num);
    long[] assigned = BitsUtil.zero(num);
    // MBRs and Areas of current assignments
    ModifiableHyperBoundingBox mbr1, mbr2;
    double area1 = 0, area2 = 0;
    // PickSeeds - find worst pair
    {
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
      // Mark both as used
      BitsUtil.setI(assigned, w1);
      BitsUtil.setI(assigned, w2);
      // Assign second to second set
      BitsUtil.setI(assignment, w2);
      // Initial mbrs and areas
      area1 = areas[w1];
      area2 = areas[w2];
      mbr1 = new ModifiableHyperBoundingBox(getter.get(entries, w1));
      mbr2 = new ModifiableHyperBoundingBox(getter.get(entries, w2));
    }
    // Second phase, QS2+QS3
    {
      int in1 = 1, in2 = 1;
      int remaining = num - 2;
      while(remaining > 0) {
        // Shortcut when minEntries must be fulfilled
        if(in1 + remaining <= minEntries) {
          // No need to updated assigned, no changes to assignment.
          break;
        }
        if(in2 + remaining <= minEntries) {
          // Mark unassigned for second.
          // Don't bother to update assigned, though
          for(int pos = BitsUtil.nextClearBit(assigned, 0); pos < num; pos = BitsUtil.nextClearBit(assigned, pos + 1)) {
            BitsUtil.setI(assignment, pos);
          }
          break;
        }
        // PickNext
        double greatestPreference = Double.NEGATIVE_INFINITY;
        int best = -1;
        E best_i = null;
        boolean preferSecond = false;
        for(int pos = BitsUtil.nextClearBit(assigned, 0); pos < num; pos = BitsUtil.nextClearBit(assigned, pos + 1)) {
          // Cost of putting object into both mbrs
          final E pos_i = getter.get(entries, pos);
          final double d1 = SpatialUtil.volumeUnion(mbr1, pos_i) - area1;
          final double d2 = SpatialUtil.volumeUnion(mbr2, pos_i) - area2;
          // Preference
          final double preference = Math.abs(d1 - d2);
          if(preference > greatestPreference) {
            greatestPreference = preference;
            best = pos;
            best_i = pos_i;
            // Prefer smaller increase
            preferSecond = (d2 < d1);
          }
        }
        // QS3: tie handling
        if(greatestPreference == 0) {
          // Prefer smaller area
          if(area1 != area2) {
            preferSecond = (area2 < area1);
          }
          else {
            // Prefer smaller group size
            preferSecond = (in2 < in1);
          }
        }
        // Mark as used.
        BitsUtil.setI(assigned, best);
        remaining--;
        if(!preferSecond) {
          in1++;
          mbr1.extend(best_i);
          area1 = SpatialUtil.volume(mbr1);
        }
        else {
          in2++;
          BitsUtil.setI(assignment, best);
          mbr2.extend(best_i);
          area2 = SpatialUtil.volume(mbr2);
        }
        // Loop from QS2
      }
      // Note: "assigned" and "remaining" likely not updated!
    }
    return assignment;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected RTreeQuadraticSplit makeInstance() {
      return RTreeQuadraticSplit.STATIC;
    }
  }
}