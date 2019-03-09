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
package elki.distance.similarityfunction.cluster;

import elki.data.Clustering;
import elki.data.type.SimpleTypeInformation;
import elki.database.query.DistanceSimilarityQuery;
import elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import elki.database.relation.Relation;
import elki.distance.similarityfunction.NormalizedSimilarity;
import elki.evaluation.clustering.ClusterContingencyTable;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Measure the similarity of clusters via the Adjusted Rand Index.
 *
 * References:
 * <p>
 * L. Hubert, P. Arabie<br>
 * Comparing partitions.<br>
 * Journal of Classification 2(193)
 * <p>
 * W. M. Rand<br>
 * Objective Criteria for the Evaluation of Clustering Methods<br>
 * Journal of the American Statistical Association, Vol. 66 Issue 336
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "L. Hubert, P. Arabie", //
    title = "Comparing partitions", //
    booktitle = "Journal of Classification 2(193)", //
    url = "https://doi.org/10.1007/BF01908075", //
    bibkey = "doi:10.1007/BF01908075")
public class ClusteringAdjustedRandIndexSimilarity implements ClusteringDistanceSimilarity, NormalizedSimilarity<Clustering<?>> {
  /**
   * Static instance.
   */
  public static final ClusteringAdjustedRandIndexSimilarity STATIC = new ClusteringAdjustedRandIndexSimilarity();

  /**
   * Constructor - use the static instance {@link #STATIC}!
   */
  public ClusteringAdjustedRandIndexSimilarity() {
    super();
  }

  @Override
  public double similarity(Clustering<?> o1, Clustering<?> o2) {
    ClusterContingencyTable ct = new ClusterContingencyTable(false, true);
    ct.process(o1, o2);
    return ct.getPaircount().adjustedRandIndex();
  }

  @Override
  public double distance(Clustering<?> o1, Clustering<?> o2) {
    ClusterContingencyTable ct = new ClusterContingencyTable(false, true);
    ct.process(o1, o2);
    return 1. - ct.getPaircount().adjustedRandIndex();
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public <T extends Clustering<?>> DistanceSimilarityQuery<T> instantiate(Relation<T> relation) {
    return new PrimitiveDistanceSimilarityQuery<>(relation, this, this);
  }

  @Override
  public SimpleTypeInformation<? super Clustering<?>> getInputTypeRestriction() {
    return new SimpleTypeInformation<>(Clustering.class);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ClusteringAdjustedRandIndexSimilarity makeInstance() {
      return STATIC;
    }
  }
}
