package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split;

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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Linear-time complexity greedy split as used by the original R-Tree.
 * 
 * <p>
 * Antonin Guttman:<br/>
 * R-Trees: A Dynamic Index Structure For Spatial Searching<br />
 * in Proceedings of the 1984 ACM SIGMOD international conference on Management
 * of data.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "Antonin Guttman", title = "R-Trees: A Dynamic Index Structure For Spatial Searching", booktitle = "Proceedings of the 1984 ACM SIGMOD international conference on Management of data", url = "http://dx.doi.org/10.1145/971697.602266")
public class RTreeLinearSplit implements SplitStrategy {
  /**
   * Static instance.
   */
  public static final RTreeLinearSplit STATIC = new RTreeLinearSplit();

  @Override
  public <E extends SpatialComparable, A> BitSet split(A entries, ArrayAdapter<E, A> getter, int minEntries) {
    final int num = getter.size(entries);
    // Object assignment, and processed objects
    BitSet assignment = new BitSet(num);
    BitSet assigned = new BitSet(num);
    // MBRs and Areas of current assignments
    ModifiableHyperBoundingBox mbr1, mbr2;
    double area1 = 0, area2 = 0;
    // LinearPickSeeds - find worst pair
    {
      final int dim = getter.get(entries, 0).getDimensionality();
      // Best candidates
      double bestsep = Double.NEGATIVE_INFINITY;
      int w1 = -1, w2 = -1;
      // LPS1: find extreme rectangles
      for(int d = 0; d < dim; d++) {
        // We need to find two candidates each, in case of el==eh!
        double minlow = Double.POSITIVE_INFINITY;
        double maxlow = Double.NEGATIVE_INFINITY, maxlow2 = Double.NEGATIVE_INFINITY;
        double minhig = Double.POSITIVE_INFINITY, minhig2 = Double.POSITIVE_INFINITY;
        double maxhig = Double.NEGATIVE_INFINITY;
        int el = -1, el2 = -1;
        int eh = -1, eh2 = -1;
        for(int i = 0; i < num; i++) {
          E ei = getter.get(entries, i);
          final double low = ei.getMin(d);
          final double hig = ei.getMax(d);
          minlow = Math.min(minlow, low);
          maxhig = Math.max(maxhig, hig);
          if(low >= maxlow) {
            maxlow2 = maxlow;
            maxlow = low;
            el2 = el;
            el = i;
          }
          else if(low > maxlow2) {
            maxlow2 = low;
            el2 = i;
          }
          if(hig <= minhig) {
            minhig2 = minhig;
            minhig = hig;
            eh2 = eh;
            eh = i;
          }
          else if(hig < minhig2) {
            minhig2 = hig;
            eh2 = i;
          }
        }
        // Compute normalized separation
        final double normsep;
        if(el != eh) {
          normsep = minhig - maxlow / (maxhig - minlow);
        }
        else {
          // Resolve tie.
          double normsep1 = minhig - maxlow2 / (maxhig - minlow);
          double normsep2 = minhig2 - maxlow / (maxhig - minlow);
          if(normsep1 > normsep2) {
            el = el2;
            normsep = normsep1;
          }
          else {
            eh = eh2;
            normsep = normsep2;
          }
        }
        assert (eh != -1 && el != -1 && (eh != el));
        if(normsep > bestsep) {
          bestsep = normsep;
          w1 = el;
          w2 = eh;
        }
      }

      // Data to keep
      // Mark both as used
      assigned.set(w1);
      assigned.set(w2);
      // Assign second to second set
      assignment.set(w2);
      // Initial mbrs and areas
      final E w1i = getter.get(entries, w1);
      final E w2i = getter.get(entries, w2);
      area1 = SpatialUtil.volume(w1i);
      area2 = SpatialUtil.volume(w2i);
      mbr1 = new ModifiableHyperBoundingBox(w1i);
      mbr2 = new ModifiableHyperBoundingBox(w2i);
    }
    // Second phase, QS2+QS3
    {
      int in1 = 1, in2 = 1;
      int remaining = num - 2;
      // Choose any element, for example the next.
      for(int next = assigned.nextClearBit(0); remaining > 0 && next < num; next = assigned.nextClearBit(next + 1)) {
        // Shortcut when minEntries must be fulfilled
        if(in1 + remaining <= minEntries) {
          // No need to updated assigned, no changes to assignment.
          break;
        }
        if(in2 + remaining <= minEntries) {
          // Mark unassigned for second.
          // Don't bother to update assigned, though
          for(; next < num; next = assigned.nextClearBit(next + 1)) {
            assignment.set(next);
          }
          break;
        }
        // PickNext
        boolean preferSecond = false;

        // Cost of putting object into both mbrs
        final E next_i = getter.get(entries, next);
        final double d1 = SpatialUtil.volumeUnion(mbr1, next_i) - area1;
        final double d2 = SpatialUtil.volumeUnion(mbr2, next_i) - area2;
        // Prefer smaller increase
        preferSecond = (d2 < d1);
        // QS3: tie handling
        if(d1 == d2) {
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
        assigned.set(next);
        remaining--;
        // Assign
        if(!preferSecond) {
          in1++;
          mbr1.extend(next_i);
          area1 = SpatialUtil.volume(mbr1);
        }
        else {
          in2++;
          assignment.set(next);
          mbr2.extend(next_i);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected RTreeLinearSplit makeInstance() {
      return RTreeLinearSplit.STATIC;
    }
  }
}