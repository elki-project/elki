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
package de.lmu.ifi.dbs.elki.evaluation.clustering;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Pair-counting measures.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class PairCounting {
  /**
   * This is the maximum size this implementation can support.
   *
   * Note: this is approximately sqrt(2) * Integer.MAX_VALUE as long = 63 bits
   * (+unused sign bit), int = 31 bits (+unused sign bit)
   */
  public static final long MAX_SIZE = (long) Math.floor(Math.sqrt(Long.MAX_VALUE));

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
        if(table.breakNoiseClusters && BitsUtil.get(table.noise1, i1)) {
          if(table.selfPairing) {
            in1 += size;
          } // else: 0
        }
        else {
          in1 += size * (long) (table.selfPairing ? size : (size - 1));
        }
      }
    }
    // Process second clustering:
    {
      for(int i2 = 0; i2 < table.size2; i2++) {
        final int size = table.contingency[table.size1 + 1][i2];
        if(table.breakNoiseClusters && BitsUtil.get(table.noise2, i2)) {
          if(table.selfPairing) {
            in2 += size;
          } // else: 0
        }
        else {
          in2 += size * (long) (table.selfPairing ? size : (size - 1));
        }
      }
    }
    // Process combinations
    for(int i1 = 0; i1 < table.size1; i1++) {
      for(int i2 = 0; i2 < table.size2; i2++) {
        final int size = table.contingency[i1][i2];
        if(table.breakNoiseClusters && (BitsUtil.get(table.noise1, i1) || BitsUtil.get(table.noise2, i2))) {
          if(table.selfPairing) {
            inBoth += size;
          } // else: 0
        }
        else {
          inBoth += size * (long) (table.selfPairing ? size : (size - 1));
        }
      }
    }
    // The official sum
    int tsize = table.contingency[table.size1][table.size2];
    if(table.contingency[table.size1][table.size2 + 1] != tsize || table.contingency[table.size1 + 1][table.size2] != tsize) {
      LoggingUtil.warning("PairCounting F-Measure is not well defined for overlapping and incomplete clusterings. The number of elements are: " + table.contingency[table.size1][table.size2 + 1] + " != " + table.contingency[table.size1 + 1][table.size2] + " elements.");
    }
    if(tsize < 0 || tsize >= MAX_SIZE) {
      LoggingUtil.warning("Your data set size probably is too big for this implementation, which uses only long precision.");
    }
    total = tsize * (long) (table.selfPairing ? tsize : (tsize - 1));
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
    return pairconfuse[0] / (double) (pairconfuse[0] + pairconfuse[2]);
  }

  /**
   * Computes the pair-counting recall.
   *
   * @return pair-counting recall
   */
  public double recall() {
    return pairconfuse[0] / (double) (pairconfuse[0] + pairconfuse[1]);
  }

  /**
   * Computes the pair-counting Fowlkes-mallows (flat only, non-hierarchical!)
   * <p>
   * E. B. Fowlkes, C. L. Mallows<br>
   * A method for comparing two hierarchical clusterings<br>
   * In: Journal of the American Statistical Association, Vol. 78 Issue 383
   *
   * @return pair-counting Fowlkes-mallows
   */
  @Reference(authors = "E. B. Fowlkes, C. L. Mallows", //
      title = "A method for comparing two hierarchical clusterings", //
      booktitle = "Journal of the American Statistical Association, Vol. 78 Issue 383", //
      url = "https://doi.org/10.2307/2288117", //
      bibkey = "doi:10.2307/2288117")
  public double fowlkesMallows() {
    return FastMath.sqrt(precision() * recall());
  }

  /**
   * Computes the Rand index (RI).
   * <p>
   * W. M. Rand<br>
   * Objective Criteria for the Evaluation of Clustering Methods<br>
   * Journal of the American Statistical Association, Vol. 66 Issue 336
   *
   * @return The Rand index (RI).
   */
  @Reference(authors = "W. M. Rand", //
      title = "Objective Criteria for the Evaluation of Clustering Methods", //
      booktitle = "Journal of the American Statistical Association, Vol. 66 Issue 336", //
      url = "https://doi.org/10.2307/2284239", //
      bibkey = "doi:10.2307/2284239")
  public double randIndex() {
    final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2] + pairconfuse[3];
    return (pairconfuse[0] + pairconfuse[3]) / sum;
  }

  /**
   * Computes the adjusted Rand index (ARI).
   * <p>
   * L. Hubert, P. Arabie<br>
   * Comparing partitions.<br>
   * Journal of Classification 2(193)
   *
   * @return The adjusted Rand index (ARI).
   */
  @Reference(authors = "L. Hubert, P. Arabie", //
      title = "Comparing partitions", //
      booktitle = "Journal of Classification 2(193)", //
      url = "https://doi.org/10.1007/BF01908075", //
      bibkey = "doi:10.1007/BF01908075")
  public double adjustedRandIndex() {
    double d = FastMath.sqrt(pairconfuse[0] + pairconfuse[1] + pairconfuse[2] + pairconfuse[3]);
    // Note: avoid (a+b)*(a+c) as this will cause long overflows easily
    // Because we have O(N^2) pairs, and thus this value is temporarily O(N^4)
    double exp = (pairconfuse[0] + pairconfuse[1]) / d * (pairconfuse[0] + pairconfuse[2]) / d;
    double opt = pairconfuse[0] + 0.5 * (pairconfuse[1] + pairconfuse[2]);
    return (pairconfuse[0] - exp) / (opt - exp);
  }

  /**
   * Computes the Jaccard index
   * <p>
   * P. Jaccard<br>
   * Distribution de la florine alpine dans la Bassin de Dranses et dans
   * quelques regiones voisines<br>
   * Bulletin del la Société Vaudoise des Sciences Naturelles
   *
   * @return The Jaccard index
   */
  @Reference(authors = "P. Jaccard", //
      title = "Distribution de la florine alpine dans la Bassin de Dranses et dans quelques regiones voisines", //
      booktitle = "Bulletin del la Société Vaudoise des Sciences Naturelles", //
      url = "http://data.rero.ch/01-R241574160", //
      bibkey = "journals/misc/Jaccard1902")
  public double jaccard() {
    final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2];
    return pairconfuse[0] / sum;
  }

  /**
   * Computes the Mirkin index, aka Equivalence Mismatch Distance.
   * <p>
   * This is a multiple of the Rand index.
   * <p>
   * B. Mirkin<br>
   * Mathematical Classification and Clustering<br>
   * Nonconvex Optimization and Its Applications
   *
   * @return The Mirkin index
   */
  @Reference(authors = "B. Mirkin", //
      title = "Mathematical Classification and Clustering", //
      booktitle = "Nonconvex Optimization and Its Applications", //
      url = "https://doi.org/10.1007/978-1-4613-0457-9", //
      bibkey = "doi:10.1007/978-1-4613-0457-9")
  public long mirkin() {
    return 2 * (pairconfuse[1] + pairconfuse[2]);
  }
}
