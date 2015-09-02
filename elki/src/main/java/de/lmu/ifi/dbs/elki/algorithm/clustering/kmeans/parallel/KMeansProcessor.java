package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.parallel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.parallel.Executor;
import de.lmu.ifi.dbs.elki.parallel.processor.Processor;

/**
 * Parallel k-means implementation.
 *
 * @author Erich Schubert
 *
 * @apiviz.has Instance
 */
public class KMeansProcessor<V extends NumberVector> implements Processor {
  /**
   * Data relation.
   */
  Relation<V> relation;

  /**
   * Distance function.
   */
  NumberVectorDistanceFunction<? super V> distance;

  /**
   * Assignment storage.
   */
  WritableIntegerDataStore assignment;

  /**
   * Mean vectors.
   */
  List<Vector> means;

  /**
   * Updated cluster centroids
   */
  double[][] centroids;

  /**
   * (Partial) cluster sizes
   */
  int[] sizes;

  /**
   * Variance sum.
   */
  double[] varsum;

  /**
   * Whether the assignment changed during the last iteration.
   */
  boolean changed = false;

  /**
   * Constructor.
   *
   * @param relation Data relation
   * @param distance Distance function
   * @param assignment Cluster assignment
   * @param varsum Variance sums
   */
  public KMeansProcessor(Relation<V> relation, NumberVectorDistanceFunction<? super V> distance, WritableIntegerDataStore assignment, double[] varsum) {
    super();
    this.distance = distance;
    this.relation = relation;
    this.assignment = assignment;
    this.varsum = varsum;
  }

  /**
   * Get the "has changed" value.
   *
   * @return Changed flag.
   */
  public boolean changed() {
    return changed;
  }

  /**
   * Initialize for a new iteration.
   *
   * @param means New means.
   */
  public void nextIteration(List<Vector> means) {
    this.means = means;
    changed = false;
    final int k = means.size();
    final int dim = means.get(0).getDimensionality();
    centroids = new double[k][dim];
    sizes = new int[k];
    Arrays.fill(varsum, 0.);
  }

  @Override
  public Instance<V> instantiate(Executor exectutor) {
    return new Instance<>(relation, distance, assignment, means);
  }

  @Override
  public void cleanup(Processor.Instance inst) {
    @SuppressWarnings("unchecked")
    Instance<V> instance = (Instance<V>) inst;
    synchronized(this) {
      changed |= instance.changed;
      for(int i = 0; i < centroids.length; i++) {
        int sizeb = instance.sizes[i];
        if(sizeb == 0) {
          continue;
        }
        int sizea = sizes[i];
        double sum = sizea + sizeb;
        double[] cent = centroids[i];
        if(sizea > 0) {
          VMath.timesEquals(cent, sizea / sum);
        }
        VMath.plusTimesEquals(cent, instance.centroids[i], 1. / sum);
        sizes[i] += sizeb;
        VMath.plusEquals(varsum, instance.varsum);
      }
    }
  }

  /**
   * Get the new means.
   *
   * @return New means
   */
  public List<Vector> getMeans() {
    ArrayList<Vector> newmeans = new ArrayList<>(centroids.length);
    for(int i = 0; i < centroids.length; i++) {
      if(sizes[i] == 0) {
        newmeans.add(means.get(i)); // Keep old mean.
        continue;
      }
      newmeans.add(new Vector(centroids[i]));
    }
    return newmeans;
  }

  /**
   * Instance to process part of the data set, for a single iteration.
   *
   * @author Erich Schubert
   */
  public static class Instance<V extends NumberVector> implements Processor.Instance {
    /**
     * Data relation.
     */
    private Relation<V> relation;

    /**
     * Distance function.
     */
    private NumberVectorDistanceFunction<? super V> distance;

    /**
     * Cluster assignment storage.
     */
    private WritableIntegerDataStore assignment;

    /**
     * Current mean vectors.
     */
    private Vector[] means;

    /**
     * Updated cluster centroids
     */
    private double[][] centroids;

    /**
     * (Partial) cluster sizes
     */
    private int[] sizes;

    /**
     * Variance sum.
     */
    private double[] varsum;

    /**
     * Changed flag.
     */
    private boolean changed = false;

    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param distance Distance function
     * @param assignment Current assignment
     * @param means Previous mean vectors
     */
    public Instance(Relation<V> relation, NumberVectorDistanceFunction<? super V> distance, WritableIntegerDataStore assignment, List<? extends NumberVector> means) {
      super();
      this.relation = relation;
      this.distance = distance;
      this.assignment = assignment;
      final int k = means.size();
      this.means = new Vector[k];
      Iterator<? extends NumberVector> iter = means.iterator();
      for(int i = 0; i < k; i++) {
        this.means[i] = iter.next().getColumnVector(); // Make local copy!
      }
      // Storage for updated means.
      final int dim = this.means[0].getDimensionality();
      this.centroids = new double[k][dim];
      this.sizes = new int[k];
      this.varsum = new double[k];
    }

    @Override
    public void map(DBIDRef id) {
      final V fv = relation.get(id);
      // Find minimum:
      double mindist = Double.POSITIVE_INFINITY;
      int minIndex = 0;
      for(int i = 0; i < means.length; i++) {
        final double dist = distance.distance(fv, means[i]);
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      varsum[minIndex] += mindist;
      // Update assignment:
      int prev = assignment.putInt(id, minIndex);
      // Update changed flag:
      changed |= (prev != minIndex);
      double[] cent = centroids[minIndex];
      for(int d = 0; d < fv.getDimensionality(); d++) {
        // TODO: improve numerical stability via Kahan summation?
        cent[d] += fv.doubleValue(d);
      }
      ++sizes[minIndex];
    }
  }
}
