package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;


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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Initialize K-means by randomly choosing k existing elements as cluster
 * centers.
 *
 * This initialization is attributed to:
 * <p>
 * E. W. Forgy<br />
 * Cluster analysis of multivariate data : efficiency versus interpretability of
 * classifications<br />
 * Abstract published in Biometrics 21(3)
 * </p>
 * but we were unable to verify this so far (apparently, only an abstract is
 * available in Biometrics).
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Vector type
 */
@Reference(authors = "E. W. Forgy", //
title = "Cluster analysis of multivariate data: efficiency versus interpretability of classifications", //
booktitle = "Biometrics 21(3)")
public class RandomlyChosenInitialMeans<O> extends AbstractKMeansInitialization<NumberVector> implements KMedoidsInitialization<O> {
  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public RandomlyChosenInitialMeans(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public <T extends NumberVector> double[][] chooseInitialMeans(Database database, Relation<T> relation, int k, NumberVectorDistanceFunction<? super T> distanceFunction) {
    DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), k, rnd);
    if(ids.size() < k) {
      throw new AbortException("Could not choose k means.");
    }
    double[][] means = new double[k][];
    DBIDIter iter = ids.iter();
    for(int i = 0; i < k; i++, iter.advance()) {
      means[i] = relation.get(iter).toArray();
    }
    return means;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distanceFunction) {
    return DBIDUtil.randomSample(ids, k, rnd);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V> extends AbstractKMeansInitialization.Parameterizer {
    @Override
    protected RandomlyChosenInitialMeans<V> makeInstance() {
      return new RandomlyChosenInitialMeans<>(rnd);
    }
  }
}