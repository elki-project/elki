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
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Line-time complexity split proposed by Ang and Tan.
 * 
 * This split strategy tries to minimize overlap only, which can however
 * degenerate to "slices".
 * 
 * <p>
 * C. H. Ang and T. C. Tan:<br />
 * New linear node splitting algorithm for R-trees<br />
 * In: Proceedings of the 5th International Symposium on Advances in Spatial
 * Databases
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "C. H. Ang and T. C. Tan", title = "New linear node splitting algorithm for R-trees", booktitle = "Proceedings of the 5th International Symposium on Advances in Spatial Databases", url = "http://dx.doi.org/10.1007/3-540-63238-7_38")
public class AngTanLinearSplit implements SplitStrategy {
  /**
   * Logger class
   */
  private static final Logging LOG = Logging.getLogger(AngTanLinearSplit.class);

  /**
   * Static instance.
   */
  public static final AngTanLinearSplit STATIC = new AngTanLinearSplit();

  @Override
  public <E extends SpatialComparable, A> BitSet split(A entries, ArrayAdapter<E, A> getter, int minEntries) {
    final int num = getter.size(entries);
    // We need the overall MBR for computing edge preferences
    ModifiableHyperBoundingBox total = new ModifiableHyperBoundingBox(getter.get(entries, 0));
    {
      for(int i = 1; i < num; i++) {
        total.extend(getter.get(entries, i));
      }
    }
    final int dim = total.getDimensionality();
    // Prepare the axis lists (we use bitsets)
    BitSet[] closer = new BitSet[dim];
    {
      for(int d = 0; d < dim; d++) {
        closer[d] = new BitSet();
      }
      for(int i = 0; i < num; i++) {
        E e = getter.get(entries, i);
        for(int d = 1; d <= dim; d++) {
          double low = e.getMin(d) - total.getMin(d);
          double hig = total.getMax(d) - e.getMax(d);
          if(low >= hig) {
            closer[d - 1].set(i);
          }
        }
      }
    }
    // Find the most even split
    {
      int axis = -1;
      int bestcard = Integer.MAX_VALUE;
      BitSet bestset = null;
      double bestover = Double.NaN;
      for(int d = 0; d < dim; d++) {
        BitSet cand = closer[d];
        int card = cand.cardinality();
        card = Math.max(card, num - card);
        if(card == num) {
          continue;
        }
        if(card < bestcard) {
          axis = d + 1;
          bestcard = card;
          bestset = cand;
          bestover = Double.NaN;
        }
        else if(card == bestcard) {
          // Tie handling
          if(Double.isNaN(bestover)) {
            bestover = computeOverlap(entries, getter, bestset);
          }
          double overlap = computeOverlap(entries, getter, cand);
          if(overlap < bestover) {
            axis = d + 1;
            bestcard = card;
            bestset = cand;
            bestover = overlap;
          }
          else if(overlap == bestover) {
            double bestlen = total.getMax(axis) - total.getMin(axis);
            double candlen = total.getMax(d + 1) - total.getMin(d + 1);
            if(candlen < bestlen) {
              axis = d + 1;
              bestcard = card;
              bestset = cand;
              bestover = overlap;
            }
          }
        }
      }
      if(bestset == null) {
        LOG.warning("No Ang-Tan-Split found. Probably all points are the same? Returning random split.");
        return Util.randomBitSet(num / 2, num, new Random());
      }
      return bestset;
    }
  }

  /**
   * Compute overlap of assignment
   * 
   * @param entries Entries
   * @param getter Entry accessor
   * @param assign Assignment
   * @return Overlap amount
   */
  protected <E extends SpatialComparable, A> double computeOverlap(A entries, ArrayAdapter<E, A> getter, BitSet assign) {
    ModifiableHyperBoundingBox mbr1 = null, mbr2 = null;
    for(int i = 0; i < getter.size(entries); i++) {
      E e = getter.get(entries, i);
      if(assign.get(i)) {
        if(mbr1 == null) {
          mbr1 = new ModifiableHyperBoundingBox(e);
        }
        else {
          mbr1.extend(e);
        }
      }
      else {
        if(mbr2 == null) {
          mbr2 = new ModifiableHyperBoundingBox(e);
        }
        else {
          mbr2.extend(e);
        }
      }
    }
    if(mbr1 == null || mbr2 == null) {
      throw new AbortException("Invalid state in split: one of the sets is empty.");
    }
    return SpatialUtil.overlap(mbr1, mbr2);
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
    protected AngTanLinearSplit makeInstance() {
      return AngTanLinearSplit.STATIC;
    }
  }
}