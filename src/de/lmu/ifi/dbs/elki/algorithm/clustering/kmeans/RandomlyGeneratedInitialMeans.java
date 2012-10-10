package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Initialize k-means by generating random vectors (within the data sets value
 * range).
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class RandomlyGeneratedInitialMeans<V extends NumberVector<?>> extends AbstractKMeansInitialization<V> {
  /**
   * Constructor.
   * 
   * @param rnd Random generator.
   */
  public RandomlyGeneratedInitialMeans(RandomFactory rnd) {
    super(rnd);
  }

  @Override
  public List<V> chooseInitialMeans(Relation<V> relation, int k, PrimitiveDistanceFunction<? super V, ?> distanceFunction) {
    final int dim = RelationUtil.dimensionality(relation);
    NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(relation);
    Pair<V, V> minmax = DatabaseUtil.computeMinMax(relation);
    List<V> means = new ArrayList<V>(k);
    final Random random = rnd.getRandom();
    for(int i = 0; i < k; i++) {
      double[] r = MathUtil.randomDoubleArray(dim, random);
      // Rescale
      for(int d = 0; d < dim; d++) {
        r[d] = minmax.first.doubleValue(d) + (minmax.second.doubleValue(d) - minmax.first.doubleValue(d)) * r[d];
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
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractKMeansInitialization.Parameterizer<V> {
    @Override
    protected RandomlyGeneratedInitialMeans<V> makeInstance() {
      return new RandomlyGeneratedInitialMeans<V>(rnd);
    }
  }
}