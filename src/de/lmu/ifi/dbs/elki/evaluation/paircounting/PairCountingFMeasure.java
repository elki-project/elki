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

/**
 * Compare two clustering results using a pair-counting F-Measure.
 * 
 * A pair are any two objects that belong to the same cluster.
 * 
 * Two clusterings are compared by comparing their pairs; if two clusterings
 * completely agree, they also agree on every pair; even when the clusters and
 * points are ordered differently.
 * 
 * An empty clustering will of course have no pairs, the trivial all-in-one
 * clustering of course has n^2 pairs. Therefore neither recall nor precision
 * itself are useful, however their combination -- the F-Measure -- is useful.
 * 
 * @author Erich Schubert
 */
public class PairCountingFMeasure {
  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @param noiseSpecial Noise receives special treatment
   * @return Pair counts
   */
  public static int[] countPairs(Clustering<?> result1, Clustering<?> result2, boolean noiseSpecial) {
    final int self = 0;
    final List<? extends Cluster<?>> cs1 = result1.getAllClusters();
    final List<? extends Cluster<?>> cs2 = result2.getAllClusters();
    // Fill overlap matrix
    final int[][] overlapmat = new int[cs1.size()][cs2.size()];
    final BitSet noise1 = new BitSet(cs1.size());
    final BitSet noise2 = new BitSet(cs2.size());
    {
      final Iterator<? extends Cluster<?>> it1 = cs1.iterator();
      for(int i1 = 0; it1.hasNext(); i1++) {
        final Cluster<?> c1 = it1.next();
        if(noiseSpecial && c1.isNoise()) {
          noise1.set(i1);
        }
        final DBIDs ids = DBIDUtil.ensureSet(c1.getIDs());
        final Iterator<? extends Cluster<?>> it2 = cs2.iterator();
        for(int i2 = 0; it2.hasNext(); i2++) {
          final Cluster<?> c2 = it2.next();
          if(noiseSpecial && i1 == 0 && c2.isNoise()) {
            noise2.set(i2);
          }
          int count = 0;
          for(DBID id : c2.getIDs()) {
            if(ids.contains(id)) {
              count++;
            }
          }
          overlapmat[i1][i2] = count;
        }
      }
    }
    // Pair counting
    int sum1 = 0, sum2 = 0;
    int in1 = 0, in2 = 0, inboth = 0;
    for(Cluster<?> c1 : cs1) {
      if(noiseSpecial && c1.isNoise()) {
        in1 += c1.size() * (1 - self);
      }
      else {
        in1 += c1.size() * (c1.size() - self);
      }
      sum1 += c1.size();
    }
    for(Cluster<?> c2 : cs2) {
      if(noiseSpecial && c2.isNoise()) {
        in2 += c2.size() * (1 - self);
      }
      else {
        in2 += c2.size() * (c2.size() - self);
      }
      sum2 += c2.size();
    }
    for(int i1 = 0; i1 < cs1.size(); i1++) {
      for(int i2 = 0; i2 < cs2.size(); i2++) {
        final int s = overlapmat[i1][i2];
        if(noiseSpecial && (noise1.get(i1) || noise2.get(i2))) {
          inboth += s * (1 - self);
        }
        else {
          inboth += s * (s - self);
        }
      }
    }
    if(sum1 != sum2) {
      LoggingUtil.warning("PairCounting F-Measure is not well defined for overlapping and incomplete clusterings.");
    }
    return new int[] { inboth, in1 - inboth, in2 - inboth };
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @param beta Beta value for the F-Measure
   * @param noiseSpecial Noise receives special treatment
   * @return Pair counting F-Measure result.
   */
  public static double compareClusterings(Clustering<?> result1, Clustering<?> result2, double beta, boolean noiseSpecial) {
    int[] counts = countPairs(result1, result2, noiseSpecial);
    return fMeasure(counts[0], counts[1], counts[2], beta);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @param beta Beta value for the F-Measure
   * @return Pair counting F-Measure result.
   */
  public static double compareClusterings(Clustering<?> result1, Clustering<?> result2, double beta) {
    return compareClusterings(result1, result2, beta, false);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @param noiseSpecial Noise receives special treatment
   * @return Pair counting F-1-Measure result.
   */
  public static double compareClusterings(Clustering<?> result1, Clustering<?> result2, boolean noiseSpecial) {
    return compareClusterings(result1, result2, 1.0, noiseSpecial);
  }

  /**
   * Compare two clustering results.
   * 
   * @param result1 first result
   * @param result2 second result
   * @return Pair counting F-1-Measure result.
   */
  public static double compareClusterings(Clustering<?> result1, Clustering<?> result2) {
    return compareClusterings(result1, result2, 1.0, false);
  }

  /**
   * Computes the F-measure of the given parameters.</p>
   * <p>
   * Returns
   * <code>((1+beta*beta) * inBoth) / ((1+beta*beta) * inBoth + (beta*beta)*inFirst + inSecond)</code>
   * </p>
   * 
   * @param inBoth The number of objects that are in both sets.
   * @param inFirst The number of objects that are in the first set.
   * @param inSecond The number of objects that are in the second set.
   * @param beta The beta values for the f-measure.
   * @return The F-measure.
   */
  public static double fMeasure(int inBoth, int inFirst, int inSecond, double beta) {
    // System.out.println("Both: "+inboth+" First: "+infirst+" Second: "+insecond);
    final double beta2 = beta * beta;
    double fmeasure = ((1 + beta2) * inBoth) / ((1 + beta2) * inBoth + beta2 * inFirst + inSecond);
    return fmeasure;
  }
}