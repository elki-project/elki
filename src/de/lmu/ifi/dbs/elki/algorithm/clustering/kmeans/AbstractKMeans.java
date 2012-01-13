package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractPrimitiveDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

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

public abstract class AbstractKMeans<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractPrimitiveDistanceBasedAlgorithm<V, D, Clustering<MeanModel<V>>> {
  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("kmeans.k", "The number of clusters to find.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater or equal to 0, where 0 means no limit.
   */
  public static final OptionID MAXITER_ID = OptionID.getOrCreateOptionID("kmeans.maxiter", "The maximum number of iterations to do. 0 means no limit.");

  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("kmeans.seed", "The random number generator seed.");

  /**
   * Parameter to specify the initialization method
   */
  public static final OptionID INIT_ID = OptionID.getOrCreateOptionID("kmeans.initialization", "Method to choose the initial means.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  protected int k;

  /**
   * Holds the value of {@link #MAXITER_ID}.
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMeansInitialization<V> initializer;

  /**
   * Constructor.
   * 
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   */
  public AbstractKMeans(PrimitiveDistanceFunction<? super V, D> distanceFunction, int k, int maxiter, KMeansInitialization<V> initializer) {
    super(distanceFunction);
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * 
   * @param relation the database to cluster
   * @param means a list of k means
   * @param clusters cluster assignment
   * @return true when the object was reassigned
   */
  protected boolean assignToNearestCluster(Relation<V> relation, List<V> means, List<? extends ModifiableDBIDs> clusters) {
    final PrimitiveDistanceFunction<? super V, D> df = getDistanceFunction();
    boolean changed = false;

    for(DBID id : relation.iterDBIDs()) {
      D mindist = df.getDistanceFactory().infiniteDistance();
      V fv = relation.get(id);
      int minIndex = 0;
      for(int i = 0; i < k; i++) {
        D dist = df.distance(fv, means.get(i));
        if(dist.compareTo(mindist) < 0) {
          minIndex = i;
          mindist = dist;
        }
      }
      if(clusters.get(minIndex).add(id)) {
        changed = true;
        // Remove from previous cluster
        // TODO: keep a list of cluster assignments to save this search?
        for(int i = 0; i < k; i++) {
          if(i != minIndex) {
            if(clusters.get(i).remove(id)) {
              break;
            }
          }
        }
      }
    }
    return changed;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   * 
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected List<V> means(List<? extends ModifiableDBIDs> clusters, List<V> means, Relation<V> database) {
    List<V> newMeans = new ArrayList<V>(k);
    for(int i = 0; i < k; i++) {
      ModifiableDBIDs list = clusters.get(i);
      V mean = null;
      for(Iterator<DBID> clusterIter = list.iterator(); clusterIter.hasNext();) {
        if(mean == null) {
          mean = database.get(clusterIter.next());
        }
        else {
          mean = mean.plus(database.get(clusterIter.next()));
        }
      }
      if(list.size() > 0) {
        assert mean != null;
        mean = mean.multiplicate(1.0 / list.size());
      }
      else {
        mean = means.get(i);
      }
      newMeans.add(mean);
    }
    return newMeans;
  }

  /**
   * Compute an incremental update for the mean
   * 
   * @param oldmean Previous mean
   * @param vec Object vector
   * @param newsize (New) size of cluster
   * @param op Cluster size change / Weight change
   * @return New mean
   */
  protected V incrementalUpdateMean(V oldmean, V vec, int newsize, double op) {
    if(newsize == 0) {
      return oldmean; // Keep old mean
    }
    Vector mv = oldmean.getColumnVector();
    Vector delta = vec.getColumnVector();
    // Compute difference from mean
    delta.minusEquals(mv);
    delta.timesEquals(op / newsize);
    mv.plusEquals(delta);
    return oldmean.newNumberVector(mv.getArrayRef());
  }

}