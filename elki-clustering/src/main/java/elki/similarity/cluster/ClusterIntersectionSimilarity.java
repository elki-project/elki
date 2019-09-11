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
package elki.similarity.cluster;

import elki.data.Cluster;
import elki.data.type.SimpleTypeInformation;
import elki.database.ids.DBIDUtil;
import elki.database.query.DistanceSimilarityQuery;
import elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.PrimitiveDistance;
import elki.similarity.PrimitiveSimilarity;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Measure the similarity of clusters via the intersection size.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ClusterIntersectionSimilarity implements PrimitiveDistance<Cluster<?>>, PrimitiveSimilarity<Cluster<?>> {
  /**
   * Static instance.
   */
  public static final ClusterIntersectionSimilarity STATIC = new ClusterIntersectionSimilarity();

  /**
   * Constructor - use the static instance {@link #STATIC}!
   */
  public ClusterIntersectionSimilarity() {
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
  public static class Par implements Parameterizer {
    @Override
    public ClusterIntersectionSimilarity make() {
      return STATIC;
    }
  }
}
