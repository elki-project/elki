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
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Measure the similarity of clusters via the Jaccard coefficient.
 * <p>
 * Reference:
 * <p>
 * P. Jaccard<br>
 * Distribution de la florine alpine dans la Bassin de Dranses et dans
 * quelques regiones voisines<br>
 * Bulletin del la Société Vaudoise des Sciences Naturelles
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "P. Jaccard", //
    title = "Distribution de la florine alpine dans la Bassin de Dranses et dans quelques regiones voisines", //
    booktitle = "Bulletin del la Société Vaudoise des Sciences Naturelles", //
    url = "http://data.rero.ch/01-R241574160", //
    bibkey = "journals/misc/Jaccard1902")
public class ClusterJaccardSimilarity implements PrimitiveDistance<Cluster<?>>, PrimitiveSimilarity<Cluster<?>> {
  /**
   * Static instance.
   */
  public static final ClusterJaccardSimilarity STATIC = new ClusterJaccardSimilarity();

  /**
   * Constructor - use the static instance {@link #STATIC}!
   */
  public ClusterJaccardSimilarity() {
    super();
  }

  @Override
  public double similarity(Cluster<?> o1, Cluster<?> o2) {
    int i = DBIDUtil.intersectionSize(o1.getIDs(), o2.getIDs());
    int union = o1.size() + o2.size() - i;
    return i / (double) union;
  }

  @Override
  public double distance(Cluster<?> o1, Cluster<?> o2) {
    int i = DBIDUtil.intersectionSize(o1.getIDs(), o2.getIDs());
    int union = o1.size() + o2.size() - i;
    return 1. - i / (double) union;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public boolean isMetric() {
    return true;
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
    public ClusterJaccardSimilarity make() {
      return STATIC;
    }
  }
}
