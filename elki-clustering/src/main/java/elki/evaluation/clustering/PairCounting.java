/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.evaluation.clustering;

import elki.logging.LoggingUtil;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Reference;

/**
 * Pair-counting measures, with support for "noise" clusters and self-pairing
 * support.
 * <p>
 * Implementation note: this implementation will either use n² or n(n-1) pairs
 * for each cluster intersection; which means we use <i>ordered</i> pairs. In
 * literature, you will often find (n choose 2) pairs, which differs by a factor
 * of 2, but this factor will cancel out everywhere anyway. The raw pair counts
 * are not exposed as an API, only the derived. The Mirkin index removes this
 * factor of 2.
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
public class PairCounting {
  /**
   * Pairs in both clusterings.
   */
  protected long inBoth;

  /**
   * Pairs in first clustering only.
   */
  protected long inFirst;

  /**
   * Pairs in second clustering only.
   */
  protected long inSecond;

  /**
   * Pairs in neither clusterings.
   */
  protected long inNone;

  /**
   * Constructor.
   */
  protected PairCounting(ClusterContingencyTable table) {
    super();
    final int[][] contingency = table.contingency;
    final boolean breakNoise = table.breakNoiseClusters;
    final boolean selfPair = table.selfPairing;
    if(!table.isStrictPartitioning()) {
      LoggingUtil.warning("PairCounting F-Measure is not well defined for overlapping and incomplete clusterings. The number of elements are: " + contingency[table.size1][table.size2 + 1] + " != " + contingency[table.size1 + 1][table.size2] + " elements.");
    }
    // Aggregations
    long inB = 0, in1 = 0, in2 = 0;
    // Process first clustering:
    for(int i = 0; i < table.size1; i++) {
      final int size = contingency[i][table.size2 + 1];
      if(breakNoise && BitsUtil.get(table.noise1, i)) {
        if(selfPair) {
          in1 += size;
        } // else: 0
      }
      else {
        in1 += size * (long) (selfPair ? size : (size - 1));
      }
    }
    // Process second clustering:
    for(int j = 0; j < table.size2; j++) {
      final int size = contingency[table.size1 + 1][j];
      if(breakNoise && BitsUtil.get(table.noise2, j)) {
        if(selfPair) {
          in2 += size;
        } // else: 0
      }
      else {
        in2 += size * (long) (selfPair ? size : (size - 1));
      }
    }
    // Process combinations
    for(int i1 = 0; i1 < table.size1; i1++) {
      for(int i2 = 0; i2 < table.size2; i2++) {
        final int size = contingency[i1][i2];
        if(breakNoise && (BitsUtil.get(table.noise1, i1) || BitsUtil.get(table.noise2, i2))) {
          if(selfPair) {
            inB += size;
          } // else: 0
        }
        else {
          inB += size * (long) (selfPair ? size : (size - 1));
        }
      }
    }
    final int tsize = contingency[table.size1][table.size2];
    long total = tsize * (long) (selfPair ? tsize : (tsize - 1));
    this.inBoth = inB;
    this.inFirst = in1 - inB;
    this.inSecond = in2 - inB;
    this.inNone = total - (inB + inFirst + inSecond);
  }

  /**
   * Get the pair-counting F-Measure
   *
   * @param beta Beta value.
   * @return F-Measure
   */
  public double fMeasure(double beta) {
    final double beta2 = beta * beta;
    return ((1. + beta2) * inBoth) / ((1. + beta2) * inBoth + beta2 * inFirst + inSecond);
  }

  /**
   * Get the pair-counting F1-Measure.
   *
   * @return F1-Measure
   */
  public double f1Measure() {
    return 2. * inBoth / (2. * inBoth + inFirst + inSecond);
  }

  /**
   * Computes the pair-counting precision.
   *
   * @return pair-counting precision
   */
  public double precision() {
    return inBoth / (double) (inBoth + inSecond);
  }

  /**
   * Computes the pair-counting recall.
   *
   * @return pair-counting recall
   */
  public double recall() {
    return inBoth / (double) (inBoth + inFirst);
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
    return Math.sqrt(precision() * recall());
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
    return (inBoth + inNone) / (double) (inBoth + inFirst + inSecond + inNone);
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
    double d = Math.sqrt((double) inBoth + inFirst + inSecond + inNone);
    // Note: avoid (a+b)*(a+c) as this will cause long overflows easily
    // Because we have O(N^2) pairs, and thus this value is temporarily O(N^4)
    double exp = (inBoth + inFirst) / d * (inBoth + inSecond) / d;
    double opt = inBoth + 0.5 * (inFirst + inSecond);
    return (inBoth - exp) / (opt - exp);
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
    return inBoth / (double) (inBoth + inFirst + inSecond);
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
    // We *omit* the factor of 2 because we already counted each pair twice!
    return inFirst + inSecond;
  }
}
