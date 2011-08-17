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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorMerge;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorNoise;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorSingleCluster;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairSortedGeneratorInterface;
import de.lmu.ifi.dbs.elki.utilities.pairs.Triple;

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
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairSortedGeneratorInterface
 * @apiviz.uses de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorNoise
 * @apiviz.uses de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorSingleCluster
 * @apiviz.uses de.lmu.ifi.dbs.elki.evaluation.paircounting.generator.PairGeneratorMerge
 */
public class PairCountingFMeasure {
  /**
   * Get a pair generator for the given Clustering
   * 
   * @param <R> Clustering result class
   * @param <M> Model type
   * @param clusters Clustering result
   * @param noiseSpecial Special handling for "noise clusters"
   * @param hierarchicalSpecial Special handling for hierarchical clusters
   * @return Sorted pair generator
   */
  public static <R extends Clustering<M>, M extends Model> PairSortedGeneratorInterface getPairGenerator(R clusters, boolean noiseSpecial, boolean hierarchicalSpecial) {
    // collect all clusters into a flat list.
    Collection<Cluster<M>> allclusters = clusters.getAllClusters();

    // Make generators for each cluster
    PairSortedGeneratorInterface[] gens = new PairSortedGeneratorInterface[allclusters.size()];
    int i = 0;
    for(Cluster<?> c : allclusters) {
      if(noiseSpecial && c.isNoise()) {
        gens[i] = new PairGeneratorNoise(c);
      }
      else {
        gens[i] = new PairGeneratorSingleCluster(c, hierarchicalSpecial);
      }
      i++;
    }
    return new PairGeneratorMerge(gens);
  }

  /**
   * Compare two clustering results.
   * 
   * @param <R> Result type
   * @param <M> Model type
   * @param <S> Result type
   * @param <N> Model type
   * @param result1 first result
   * @param result2 second result
   * @param beta Beta value for the F-Measure
   * @param noiseSpecial Noise receives special treatment
   * @param hierarchicalSpecial Special handling for hierarchical clusters
   * @return Pair counting F-Measure result.
   */
  public static <R extends Clustering<M>, M extends Model, S extends Clustering<N>, N extends Model> double compareClusterings(R result1, S result2, double beta, boolean noiseSpecial, boolean hierarchicalSpecial) {
    PairSortedGeneratorInterface first = getPairGenerator(result1, noiseSpecial, hierarchicalSpecial);
    PairSortedGeneratorInterface second = getPairGenerator(result2, noiseSpecial, hierarchicalSpecial);
    Triple<Integer, Integer, Integer> countedPairs = countPairs(first, second);
    return fMeasure(countedPairs.first, countedPairs.second, countedPairs.third, beta);
  }

  /**
   * Compare two clustering results.
   * 
   * @param <R> Result type
   * @param <M> Model type
   * @param <S> Result type
   * @param <N> Model type
   * @param result1 first result
   * @param result2 second result
   * @param beta Beta value for the F-Measure
   * @return Pair counting F-Measure result.
   */
  public static <R extends Clustering<M>, M extends Model, S extends Clustering<N>, N extends Model> double compareClusterings(R result1, S result2, double beta) {
    return compareClusterings(result1, result2, beta, false, false);
  }

  /**
   * Compare two clustering results.
   * 
   * @param <R> Result type
   * @param <M> Model type
   * @param <S> Result type
   * @param <N> Model type
   * @param result1 first result
   * @param result2 second result
   * @param noiseSpecial Noise receives special treatment
   * @return Pair counting F-1-Measure result.
   */
  public static <R extends Clustering<M>, M extends Model, S extends Clustering<N>, N extends Model> double compareClusterings(R result1, S result2, boolean noiseSpecial, boolean hierarchicalSpecial) {
    return compareClusterings(result1, result2, 1.0, noiseSpecial, hierarchicalSpecial);
  }

  /**
   * Compare two clustering results.
   * 
   * @param <R> Result type
   * @param <M> Model type
   * @param <S> Result type
   * @param <N> Model type
   * @param result1 first result
   * @param result2 second result
   * @return Pair counting F-1-Measure result.
   */
  public static <R extends Clustering<M>, M extends Model, S extends Clustering<N>, N extends Model> double compareClusterings(R result1, S result2) {
    return compareClusterings(result1, result2, 1.0, false, false);
  }

  /**
   * Compare two sets of generated pairs. It determines how many objects of the
   * first set are in both sets, just in the first set or just in the second
   * set.</p>
   * 
   * 
   * @param <R> Result type
   * @param <M> Model type
   * @param <S> Result type
   * @param <N> Model type
   * @param result1 first result
   * @param result2 second result
   * @return Returns a {@link Triple} that contains the number of objects that
   *         are in both sets (FIRST), the number of objects that are just in
   *         the first set (SECOND) and the number of object that are just in
   *         the second set (THIRD).
   * 
   */
  public static <R extends Clustering<M>, M extends Model, S extends Clustering<N>, N extends Model> Triple<Integer, Integer, Integer> countPairs(R result1, S result2) {
    PairSortedGeneratorInterface first = getPairGenerator(result1, false, false);
    PairSortedGeneratorInterface second = getPairGenerator(result2, false, false);
    return countPairs(first, second);
  }

  /**
   * Compare two sets of generated pairs. It determines how many objects of the
   * first set are in both sets, just in the first set or just in the second
   * set.</p>
   * 
   * @param first first set
   * @param second second set
   * @return Returns a {@link Triple} that contains the number of objects that
   *         are in both sets (FIRST), the number of objects that are just in
   *         the first set (SECOND) and the number of object that are just in
   *         the second set (THIRD).
   */
  public static Triple<Integer, Integer, Integer> countPairs(PairSortedGeneratorInterface first, PairSortedGeneratorInterface second) {
    int inboth = 0;
    int infirst = 0;
    int insecond = 0;

    while(first.current() != null && second.current() != null) {
      int cmp = first.current().compareTo(second.current());
      if(cmp == 0) {
        inboth++;
        first.next();
        second.next();
      }
      else if(cmp < 0) {
        infirst++;
        first.next();
      }
      else {
        insecond++;
        second.next();
      }
    }
    while(first.current() != null) {
      infirst++;
      first.next();
    }
    while(second.current() != null) {
      insecond++;
      second.next();
    }
    return new Triple<Integer, Integer, Integer>(inboth, infirst, insecond);
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
    double fmeasure = ((1 + beta * beta) * inBoth) / ((1 + beta * beta) * inBoth + (beta * beta) * inFirst + inSecond);
    return fmeasure;
  }
}
