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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;

/**
 * Initialize K-means by randomly choosing k exsiting elements as cluster
 * centers.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class RandomlyChosenInitialMeans<V extends NumberVector<V, ?>> extends AbstractKMeansInitialization<V> {
  /**
   * Constructor.
   * 
   * @param seed Random seed.
   */
  public RandomlyChosenInitialMeans(Long seed) {
    super(seed);
  }

  @Override
  public List<Vector> chooseInitialMeans(Relation<V> relation, int k, PrimitiveDistanceFunction<? super V, ?> distanceFunction) {
    DBIDs ids = DBIDUtil.randomSample(relation.getDBIDs(), k, seed);
    List<Vector> means = new ArrayList<Vector>(k);
    for(DBID id : ids) {
      means.add(relation.get(id).getColumnVector());
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractKMeansInitialization.Parameterizer<V> {

    @Override
    protected RandomlyChosenInitialMeans<V> makeInstance() {
      return new RandomlyChosenInitialMeans<V>(seed);
    }
  }
}