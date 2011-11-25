package de.lmu.ifi.dbs.elki.evaluation.paircounting;

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
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * Class storing the contingency table and related data on two clusterings.
 * 
 * @author Erich Schubert
 */
public class ClusterContingencyTable {
  /**
   * Noise cluster handling
   */
  boolean breakNoiseClusters = false;

  /**
   * Self pairing
   */
  boolean selfPairing = true;

  /**
   * Number of clusters in first
   */
  int size1 = -1;

  /**
   * Number of clusters in second
   */
  int size2 = -1;

  /**
   * Contingency matrix
   */
  int[][] contingency = null;

  /**
   * Noise flags
   */
  BitSet noise1 = null;

  /**
   * Noise flags
   */
  BitSet noise2 = null;

  /**
   * Pair counting confusion matrix (flat: inBoth, inFirst, inSecond, inNone)
   */
  long[] pairconfuse = null;

  /**
   * Constructor.
   * 
   * @param selfPairing Build self-pairs
   * @param breakNoiseClusters Break noise clusters into individual objects
   */
  public ClusterContingencyTable(boolean selfPairing, boolean breakNoiseClusters) {
    super();
    this.selfPairing = selfPairing;
    this.breakNoiseClusters = breakNoiseClusters;
  }

  /**
   * Process two clustering results.
   * 
   * @param result1 First clustering
   * @param result2 Second clustering
   */
  public void process(Clustering<?> result1, Clustering<?> result2) {
    // Get the clusters
    final List<? extends Cluster<?>> cs1 = result1.getAllClusters();
    final List<? extends Cluster<?>> cs2 = result2.getAllClusters();

    // Initialize
    size1 = cs1.size();
    size2 = cs2.size();
    contingency = new int[size1 + 2][size2 + 2];
    noise1 = new BitSet(size1);
    noise2 = new BitSet(size2);

    // Fill main part of matrix
    {
      {
        final Iterator<? extends Cluster<?>> it2 = cs2.iterator();
        for(int i2 = 0; it2.hasNext(); i2++) {
          final Cluster<?> c2 = it2.next();
          if(c2.isNoise()) {
            noise2.set(i2);
          }
          contingency[size1 + 1][i2] = c2.size();
          contingency[size1 + 1][size2] += c2.size();
        }
      }
      final Iterator<? extends Cluster<?>> it1 = cs1.iterator();
      for(int i1 = 0; it1.hasNext(); i1++) {
        final Cluster<?> c1 = it1.next();
        if(c1.isNoise()) {
          noise1.set(i1);
        }
        final DBIDs ids = DBIDUtil.ensureSet(c1.getIDs());
        contingency[i1][size2 + 1] = c1.size();
        contingency[size1][size2 + 1] += c1.size();

        final Iterator<? extends Cluster<?>> it2 = cs2.iterator();
        for(int i2 = 0; it2.hasNext(); i2++) {
          final Cluster<?> c2 = it2.next();
          int count = 0;
          for(DBID id : c2.getIDs()) {
            if(ids.contains(id)) {
              count++;
            }
          }
          contingency[i1][i2] = count;
          contingency[i1][size2] += count;
          contingency[size1][i2] += count;
          contingency[size1][size2] += count;
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    if(contingency != null) {
      for(int i1 = 0; i1 < size1 + 2; i1++) {
        if(i1 >= size1) {
          buf.append("------\n");
        }
        for(int i2 = 0; i2 < size2 + 2; i2++) {
          if(i2 >= size2) {
            buf.append("| ");
          }
          buf.append(contingency[i1][i2]).append(" ");
        }
        buf.append("\n");
      }
    }
    if(pairconfuse != null) {
      buf.append(FormatUtil.format(pairconfuse));
    }
    return buf.toString();
  }

  /**
   * Compute the pair sizes
   */
  protected void computePairSizes() {
    assert (contingency != null) : "No clustering loaded.";
    // Aggregations
    long inBoth = 0, in1 = 0, in2 = 0, total = 0;
    // Process first clustering:
    {
      for(int i1 = 0; i1 < size1; i1++) {
        final int size = contingency[i1][size2 + 1];
        if(breakNoiseClusters && noise1.get(i1)) {
          if(selfPairing) {
            in1 += size;
          } // else: 0
        }
        else {
          if(selfPairing) {
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
      for(int i2 = 0; i2 < size2; i2++) {
        final int size = contingency[size1 + 1][i2];
        if(breakNoiseClusters && noise2.get(i2)) {
          if(selfPairing) {
            in2 += size;
          } // else: 0
        }
        else {
          if(selfPairing) {
            in2 += size * size;
          }
          else {
            in2 += size * (size - 1);
          }
        }
      }
    }
    // Process combinations
    for(int i1 = 0; i1 < size1; i1++) {
      for(int i2 = 0; i2 < size2; i2++) {
        final int size = contingency[i1][i2];
        if(breakNoiseClusters && (noise1.get(i1) || noise2.get(i2))) {
          if(selfPairing) {
            inBoth += size;
          } // else: 0
        }
        else {
          if(selfPairing) {
            inBoth += size * size;
          }
          else {
            inBoth += size * (size - 1);
          }
        }
      }
    }
    // The official sum
    int tsize = contingency[size1][size2];
    if(contingency[size1][size2 + 1] != tsize || contingency[size1 + 1][size2] != tsize) {
      LoggingUtil.warning("PairCounting F-Measure is not well defined for overlapping and incomplete clusterings.");
    }
    if(tsize >= Math.sqrt(Long.MAX_VALUE)) {
      LoggingUtil.warning("Your data set size probably is too big for this implementation, which uses only long precision.");
    }
    if(selfPairing) {
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
   * Get the pair-counting confusion matrix.
   * 
   * @return Confusion matrix
   */
  public long[] getPairConfusionMatrix() {
    if(pairconfuse == null) {
      computePairSizes();
    }
    return pairconfuse;
  }

  /**
   * Get the pair-counting F-Measure
   * 
   * @param beta Beta value.
   * @return F-Measure
   */
  public double pairFMeasure(double beta) {
    final double beta2 = beta * beta;
    getPairConfusionMatrix();
    double fmeasure = ((1 + beta2) * pairconfuse[0]) / ((1 + beta2) * pairconfuse[0] + beta2 * pairconfuse[1] + pairconfuse[2]);
    return fmeasure;
  }

  /**
   * Get the pair-counting F1-Measure.
   * 
   * @return F1-Measure
   */
  public double pairF1Measure() {
    return pairFMeasure(1.0);
  }

  /**
   * Computes the pair-counting precision.
   * 
   * @return pair-counting precision
   */
  public double pairPrecision() {
    getPairConfusionMatrix();
    return ((double) pairconfuse[0]) / (pairconfuse[0] + pairconfuse[2]);
  }

  /**
   * Computes the pair-counting recall.
   * 
   * @return pair-counting recall
   */
  public double pairRecall() {
    getPairConfusionMatrix();
    return ((double) pairconfuse[0]) / (pairconfuse[0] + pairconfuse[1]);
  }

  /**
   * Computes the pair-counting Fowlkes-mallows.
   * 
   * @return pair-counting Fowlkes-mallows
   */
  public double pairFowlkesMallows() {
    return Math.sqrt(pairPrecision() * pairRecall());
  }

  /**
   * Computes the Rand index (RI).
   * 
   * @return The Rand index (RI).
   */
  public double pairRandIndex() {
    getPairConfusionMatrix();
    final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2] + pairconfuse[3];
    return (pairconfuse[0] + pairconfuse[3]) / sum;
  }

  /**
   * Computes the adjusted Rand index (ARI).
   * 
   * @return The adjusted Rand index (ARI).
   */
  public double pairAdjustedRandIndex() {
    getPairConfusionMatrix();
    final double nom = pairconfuse[0] * pairconfuse[3] - pairconfuse[1] * pairconfuse[2];
    final long d1 = (pairconfuse[0] + pairconfuse[1]) * (pairconfuse[1] + pairconfuse[3]);
    final long d2 = (pairconfuse[0] + pairconfuse[2]) * (pairconfuse[2] + pairconfuse[3]);
    return 2 * nom / (d1 + d2);
  }

  /**
   * Computes the Jaccard index
   * 
   * @return The Jaccard index
   */
  public double pairJaccard() {
    getPairConfusionMatrix();
    final double sum = pairconfuse[0] + pairconfuse[1] + pairconfuse[2];
    return pairconfuse[0] / sum;
  }

  /**
   * Computes the Mirkin index
   * 
   * @return The Mirkin index
   */
  public long pairMirkin() {
    getPairConfusionMatrix();
    return 2 * (pairconfuse[1] + pairconfuse[2]);
  }

  /**
   * Compute the average Gini for each cluster (in both clusterings -
   * symmetric).
   * 
   * @return Mean and variance of Gini
   */
  public MeanVariance averageSymmetricGini() {
    MeanVariance mv = new MeanVariance();
    for(int i1 = 0; i1 < size1; i1++) {
      double purity = 0.0;
      if(contingency[i1][size2] > 0) {
        final double cs = contingency[i1][size2]; // sum, as double.
        for(int i2 = 0; i2 < size2; i2++) {
          double rel = contingency[i1][i2] / cs;
          purity += rel * rel;
        }
      }
      mv.put(purity);
    }
    for(int i2 = 0; i2 < size2; i2++) {
      double purity = 0.0;
      if(contingency[size1][i2] > 0) {
        final double cs = contingency[size1][i2]; // sum, as double.
        for(int i1 = 0; i1 < size1; i1++) {
          double rel = contingency[i1][i2] / cs;
          purity += rel * rel;
        }
      }
      mv.put(purity);
    }
    return mv;
  }
}