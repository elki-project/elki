package de.lmu.ifi.dbs.elki.evaluation.clustering;
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

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Pair-counting measures.
 * 
 * @author Erich Schubert
 */
public class PairCounting {
  /**
   * Pair counting confusion matrix (flat: inBoth, inFirst, inSecond, inNone)
   */
  protected long[] pairconfuse = null;

  /**
   * Constructor.
   */
  protected PairCounting(ClusterContingencyTable table) {
    super();
    // Aggregations
    long inBoth = 0, in1 = 0, in2 = 0, total = 0;
    // Process first clustering:
    {
      for(int i1 = 0; i1 < table.size1; i1++) {
        final int size = table.contingency[i1][table.size2 + 1];
        if(table.breakNoiseClusters && table.noise1.get(i1)) {
          if(table.selfPairing) {
            in1 += size;
          } // else: 0
        }
        else {
          if(table.selfPairing) {
            in1 += size * size;
          }
          else {
            in1 += size * (size - 1);
          }
        }
      }
    }
    // Process second clustering:
    {
      for(int i2 = 0; i2 < table.size2; i2++) {
        final int size = table.contingency[table.size1 + 1][i2];
        if(table.breakNoiseClusters && table.noise2.get(i2)) {
          if(table.selfPairing) {
            in2 += size;
          } // else: 0
        }
        else {
          if(table.selfPairing) {
            in2 += size * size;
          }
          else {
            in2 += size * (size - 1);
          }
        }
      }
    }
    // Process combinations
    for(int i1 = 0; i1 < table.size1; i1++) {
      for(int i2 = 0; i2 < table.size2; i2++) {
        final int size = table.contingency[i1][i2];
        if(table.breakNoiseClusters && (table.noise1.get(i1) || table.noise2.get(i2))) {
          if(table.selfPairing) {
            inBoth += size;
          } // else: 0
        }
        else {
          if(table.selfPairing) {
            inBoth += size * size;
          }
          else {
            inBoth += size * (size - 1);
          }
        }
      }
    }
    // The official sum
    int tsize = table.contingency[table.size1][table.size2];
    if(table.contingency[table.size1][table.size2 + 1] != tsize || table.contingency[table.size1 + 1][table.size2] != tsize) {
      LoggingUtil.warning("PairCounting F-Measure is not well defined for overlapping and incomplete clusterings.");
    }
    if(tsize >= Math.sqrt(Long.MAX_VALUE)) {
      LoggingUtil.warning("Your data set size probably is too big for this implementation, which uses only long precision.");
    }
    if(table.selfPairing) {
      total = tsize * tsize;
    }
    else {
      total = tsize * (tsize - 1);
    }
    long inFirst = in1 - inBoth, inSecond = in2 - inBoth;
    long inNone = total - (inBoth + inFirst + inSecond);
    pairconfuse = new long[] { inBoth, inFirst, inSecond, inNone };
  }

  /**
   * Get the pair-counting F-Measure
   * 
   * @param beta Beta value.
   * @return F-Measure
   */
  public double fMeasure(double beta) {
    final double beta2 = beta * beta;
    double fmeasure = ((1 + beta2) * pairconfuse[0]) / ((1 + beta2) * pairconfuse[0] + beta2 * pairconfuse[1] + pairconfuse[2]);
    return fmeasure;
  }

  /**
   * Get the pair-counting F1-Measure.
   * 
   * @return F1-Measure
   */
  public double f1Measure() {
    return fMeasure(1.0);
  }

  /**
   * Computes the pair-counting precision.
   * 
   * @return pair-counting precision
   */
  public double precision() {
    return ((double) pairconfuse[0]) / (pairconfuse[0] + pairconfuse[2]);
  }

  /**
   * Computes the pair-counting recall.
   * 
   * @return pair-counting recall
   */
  public double recall() {
    return ((double) pairconfuse[0]) / (pairconfuse[0] + pairconfuse[1]);
  }

  /**
   * Computes the pair-counting Fowlkes-mallows (flat only, non-hierarchical!)
   * 
   * <p>
   * Fowlkes, E.B. and Mallows, C.L.<br />
   * A method for comparing two hierarchical clusterings<br />
   * In: Journal of the American Statistical Association, Vol. 78 Issue 383
   * </p>
   * 
   * @return pair-counting Fowlkes-mallows
   */
  // TODO: implement for non-flat clusterings!
  @Reference(authors = "Fowlkes, E.B. and Mallows, C.L.", title = "A method for comparing two hierarchical clusterings", booktitle = "Journal of the American Statistical Association, Vol. 78 Issue 383")
  public double fowlkesMallows() {
    return Math.sqrt(precision() * recall());
  }

  /**
   * Computes the Rand index (RI).
   * 
   * <p>
   * Rand, W. M.<br />
   * Objective Criteria for the Evaluation of Clustering Methods<br />
   * Journal of the American Statistical Association, Vol. 66 Issue 336
   * </p>
   * 
   * @return The Rand index (RI).
   */
  @Reference(authors = "Rand, W. M.", title = "Objective Criteria for the Evaluation of Clustering Methods", booktitle = "Journal of the American Statistical Association, Vol. 66 Issue 336", url = "http://www.jstor.org/stable/10.2307/2284239")
  public double randIndex() {
    final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2] + pairconfuse[3];
    return (pairconfuse[0] + pairconfuse[3]) / sum;
  }

  /**
   * Computes the adjusted Rand index (ARI).
   * 
   * @return The adjusted Rand index (ARI).
   */
  public double adjustedRandIndex() {
    final double nom = pairconfuse[0] * pairconfuse[3] - pairconfuse[1] * pairconfuse[2];
    final long d1 = (pairconfuse[0] + pairconfuse[1]) * (pairconfuse[1] + pairconfuse[3]);
    final long d2 = (pairconfuse[0] + pairconfuse[2]) * (pairconfuse[2] + pairconfuse[3]);
    return (d1 + d2 > 0) ? 2 * nom / (d1 + d2) : 0;
  }

  /**
   * Computes the Jaccard index
   * 
   * @return The Jaccard index
   */
  public double jaccard() {
    final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2];
    return pairconfuse[0] / sum;
  }

  /**
   * Computes the Mirkin index
   * 
   * @return The Mirkin index
   */
  public long mirkin() {
    return 2 * (pairconfuse[1] + pairconfuse[2]);
  }
}