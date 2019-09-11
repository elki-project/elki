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

import elki.data.Clustering;
import elki.data.type.SimpleTypeInformation;
import elki.database.query.DistanceSimilarityQuery;
import elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import elki.database.relation.Relation;
import elki.similarity.NormalizedSimilarity;
import elki.evaluation.clustering.ClusterContingencyTable;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Measure the similarity of clusters via the BCubed F1 Index.
 * <p>
 * Reference:
 * <p>
 * A. Bagga, B. Baldwin<br>
 * Entity-based cross-document coreferencing using the Vector Space Model<br>
 * Proc. 17th Int. Conf. on Computational Linguistics (COLING '98)
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "A. Bagga, B. Baldwin", //
    title = "Entity-based cross-document coreferencing using the Vector Space Model", //
    booktitle = "Proc. 17th Int. Conf. on Computational Linguistics (COLING '98)", //
    url = "https://doi.org/10.3115/980451.980859", //
    bibkey = "doi:10.3115/980451.980859")
public class ClusteringBCubedF1Similarity implements ClusteringDistanceSimilarity, NormalizedSimilarity<Clustering<?>> {
  /**
   * Static instance.
   */
  public static final ClusteringBCubedF1Similarity STATIC = new ClusteringBCubedF1Similarity();

  /**
   * Constructor - use the static instance {@link #STATIC}!
   */
  public ClusteringBCubedF1Similarity() {
    super();
  }

  @Override
  public double similarity(Clustering<?> o1, Clustering<?> o2) {
    ClusterContingencyTable ct = new ClusterContingencyTable(false, true);
    ct.process(o1, o2);
    return ct.getBCubed().f1Measure();
  }

  @Override
  public double distance(Clustering<?> o1, Clustering<?> o2) {
    ClusterContingencyTable ct = new ClusterContingencyTable(false, true);
    ct.process(o1, o2);
    return 1. - ct.getBCubed().f1Measure();
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
  public static class Par implements Parameterizer {
    @Override
    public ClusteringBCubedF1Similarity make() {
      return STATIC;
    }
  }
}
