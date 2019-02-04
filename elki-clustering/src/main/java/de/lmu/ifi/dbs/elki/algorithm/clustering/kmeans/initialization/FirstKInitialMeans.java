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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Initialize K-means by using the first k objects as initial means.
 * <p>
 * Reference:
 * <p>
 * J. MacQueen<br>
 * Some Methods for Classification and Analysis of Multivariate Observations<br>
 * 5th Berkeley Symp. Math. Statist. Prob.
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @param <O> Object type for KMedoids
 */
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.FirstKInitialMeans")
@Reference(authors = "J. MacQueen", //
    title = "Some Methods for Classification and Analysis of Multivariate Observations", //
    booktitle = "5th Berkeley Symp. Math. Statist. Prob.", //
    url = "http://projecteuclid.org/euclid.bsmsp/1200512992", //
    bibkey = "conf/bsmsp/MacQueen67")
public class FirstKInitialMeans<O> implements KMeansInitialization, KMedoidsInitialization<O> {
  /**
   * Constructor.
   */
  public FirstKInitialMeans() {
    super();
  }

  @Override
  public double[][] chooseInitialMeans(Database database, Relation<? extends NumberVector> relation, int k, NumberVectorDistanceFunction<?> distanceFunction) {
    DBIDIter iter = relation.iterDBIDs();
    double[][] means = new double[k][];
    for(int i = 0; i < k && iter.valid(); i++, iter.advance()) {
      means[i] = relation.get(iter).toArray();
    }
    return means;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distanceFunction) {
    DBIDIter iter = ids.iter();
    ArrayModifiableDBIDs means = DBIDUtil.newArray(k);
    for(int i = 0; i < k && iter.valid(); i++, iter.advance()) {
      means.add(iter);
    }
    return means;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    @Override
    protected FirstKInitialMeans<V> makeInstance() {
      return new FirstKInitialMeans<>();
    }
  }
}
