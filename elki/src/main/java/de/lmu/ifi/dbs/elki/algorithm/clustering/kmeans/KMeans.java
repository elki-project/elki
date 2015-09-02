package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

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

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Some constants and options shared among kmeans family algorithms.
 *
 * @author Erich Schubert
 *
 * @param <V> Number vector type
 * @param <M> Actual model type
 */
public interface KMeans<V extends NumberVector, M extends Model> extends ClusteringAlgorithm<Clustering<M>>, DistanceBasedAlgorithm<V> {
  /**
   * Parameter to specify the initialization method
   */
  public static final OptionID INIT_ID = new OptionID("kmeans.initialization", "Method to choose the initial means.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   */
  public static final OptionID K_ID = new OptionID("kmeans.k", "The number of clusters to find.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater or equal to 0, where 0 means no limit.
   */
  public static final OptionID MAXITER_ID = new OptionID("kmeans.maxiter", "The maximum number of iterations to do. 0 means no limit.");

  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = new OptionID("kmeans.seed", "The random number generator seed.");

  /**
   * Run the clustering algorithm.
   *
   * @param database Database to run on.
   * @param rel Relation to process.
   * @return Clustering result
   */
  Clustering<M> run(Database database, Relation<V> rel);

  /**
   * Set the value of k. Needed for some types of nested k-means.
   *
   * @param k K parameter
   */
  void setK(int k);

  /**
   * Set the distance function to use.
   *
   * @param distanceFunction Distance function.
   */
  void setDistanceFunction(NumberVectorDistanceFunction<? super V> distanceFunction);
}
