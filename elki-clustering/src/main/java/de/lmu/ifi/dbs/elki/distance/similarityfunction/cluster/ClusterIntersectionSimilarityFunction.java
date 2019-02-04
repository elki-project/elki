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
package de.lmu.ifi.dbs.elki.distance.similarityfunction.cluster;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Measure the similarity of clusters via the intersection size.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ClusterIntersectionSimilarityFunction implements PrimitiveDistanceFunction<Cluster<?>>, PrimitiveSimilarityFunction<Cluster<?>> {
  /**
   * Static instance.
   */
  public static final ClusterIntersectionSimilarityFunction STATIC = new ClusterIntersectionSimilarityFunction();

  /**
   * Constructor - use the static instance {@link #STATIC}!
   */
  public ClusterIntersectionSimilarityFunction() {
    super();
  }

  @Override
  public double similarity(Cluster<?> o1, Cluster<?> o2) {
    return DBIDUtil.intersectionSize(o1.getIDs(), o2.getIDs());
  }

  @Override
  public double distance(Cluster<?> o1, Cluster<?> o2) {
    int i = DBIDUtil.intersectionSize(o1.getIDs(), o2.getIDs());
    return Math.max(o1.size(), o2.size()) - i;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public <T extends Cluster<?>> DistanceSimilarityQuery<T> instantiate(Relation<T> relation) {
    return new PrimitiveDistanceSimilarityQuery<>(relation, this, this);
  }

  @Override
  public SimpleTypeInformation<? super Cluster<?>> getInputTypeRestriction() {
    return new SimpleTypeInformation<>(Cluster.class);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ClusterIntersectionSimilarityFunction makeInstance() {
      return STATIC;
    }
  }
}
