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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.Alias;

/**
 * Initialize k-means by generating random vectors (within the data sets value
 * range).
 *
 * @author Erich Schubert
 * @since 0.5.0
 */
@Alias("de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.RandomlyGeneratedInitialMeans")
public class RandomlyGeneratedInitialMeans extends AbstractKMeansInitialization<NumberVector> {
  /**
   * Constructor.
   *
   * @param rnd Random generator.
   */
  public RandomlyGeneratedInitialMeans(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public <T extends NumberVector, V extends NumberVector> List<V> chooseInitialMeans(Database database, Relation<T> relation, int k, NumberVectorDistanceFunction<? super T> distanceFunction, NumberVector.Factory<V> factory) {
    final int dim = RelationUtil.dimensionality(relation);
    double[][] minmax = RelationUtil.computeMinMax(relation);
    double[] min = minmax[0], scale = minmax[1];
    for(int d = 0; d < dim; d++) {
      scale[d] = scale[d] - min[d];
    }
    List<V> means = new ArrayList<>(k);
    final Random random = rnd.getSingleThreadedRandom();
    for(int i = 0; i < k; i++) {
      double[] r = MathUtil.randomDoubleArray(dim, random);
      // Rescale
      for(int d = 0; d < dim; d++) {
        r[d] = min[d] + scale[d] * r[d];
      }
      means.add(factory.newNumberVector(r));
    }
    return means;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractKMeansInitialization.Parameterizer {
    @Override
    protected RandomlyGeneratedInitialMeans makeInstance() {
      return new RandomlyGeneratedInitialMeans(rnd);
    }
  }
}
