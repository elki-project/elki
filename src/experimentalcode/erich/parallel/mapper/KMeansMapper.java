package experimentalcode.erich.parallel.mapper;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.VMath;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import experimentalcode.erich.parallel.MapExecutor;

/**
 * Parallel k-means implementation.
 * 
 * @author Erich Schubert
 */
public class KMeansMapper<V extends NumberVector> implements Mapper {
  /**
   * Data relation.
   */
  Relation<V> relation;

  /**
   * Distance function.
   */
  PrimitiveDistanceFunction<? super NumberVector> distance;

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
   * Whether the assignment changed during the last iteration.
   */
  boolean changed = false;

  /**
   * Constructor.
   * 
   * @param relation Data relation
   * @param distance Distance function
   * @param means Initial means
   * @param assignment Cluster assignment
   */
  public KMeansMapper(Relation<V> relation, PrimitiveDistanceFunction<? super NumberVector> distance, WritableIntegerDataStore assignment) {
    super();
    this.distance = distance;
    this.relation = relation;
    this.assignment = assignment;
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
  }

  @Override
  public Instance instantiate(MapExecutor exectutor) {
    return new Instance(relation, distance, assignment, means);
  }

  @Override
  public void cleanup(Mapper.Instance inst) {
    @SuppressWarnings("unchecked")
    Instance instance = (Instance) inst;
    synchronized (this) {
      changed |= instance.changed;
      for (int i = 0; i < centroids.length; i++) {
        int sizeb = instance.sizes[i];
        if (sizeb == 0) {
          continue;
        }
        int sizea = sizes[i];
        double sum = sizea + sizeb;
        double[] cent = centroids[i];
        if (sizea > 0) {
          VMath.timesEquals(cent, sizea / sum);
        }
        VMath.plusTimesEquals(cent, instance.centroids[i], 1. / sum);
        sizes[i] += sizeb;
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
    for (int i = 0; i < centroids.length; i++) {
      if (sizes[i] == 0) {
        newmeans.add(means.get(i)); // Keep old mean.
        continue;
      }
      newmeans.add(new Vector(centroids[i]));
    }
    return newmeans;
  }

  public class Instance implements Mapper.Instance {
    /**
     * Data relation.
     */
    private Relation<V> relation;

    /**
     * Distance function.
     */
    private PrimitiveDistanceFunction<? super NumberVector> distance;

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
    public Instance(Relation<V> relation, PrimitiveDistanceFunction<? super NumberVector> distance, WritableIntegerDataStore assignment, List<? extends NumberVector> means) {
      super();
      this.relation = relation;
      this.distance = distance;
      this.assignment = assignment;
      final int k = means.size();
      this.means = new Vector[k];
      Iterator<? extends NumberVector> iter = means.iterator();
      for (int i = 0; i < k; i++) {
        this.means[i] = iter.next().getColumnVector(); // Make local copy!
      }
      // Storage for updated means.
      final int dim = this.means[0].getDimensionality();
      this.centroids = new double[k][dim];
      this.sizes = new int[k];
    }

    @Override
    public void map(DBIDRef id) {
      final V fv = relation.get(id);
      // Find minimum:
      double mindist = Double.POSITIVE_INFINITY;
      int minIndex = 0;
      for (int i = 0; i < means.length; i++) {
        final double dist = distance.distance(fv, means[i]);
        if (dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      // Update assignment:
      int prev = assignment.putInt(id, minIndex);
      // Update changed flag:
      changed |= (prev != minIndex);
      double[] cent = centroids[minIndex];
      for (int d = 0; d < fv.getDimensionality(); d++) {
        // TODO: improve numerical stability via Kahan summation?
        cent[d] += fv.doubleValue(d);
      }
      ++sizes[minIndex];
    }
  }
}
