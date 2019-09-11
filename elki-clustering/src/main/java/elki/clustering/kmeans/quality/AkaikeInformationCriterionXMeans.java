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
package elki.clustering.kmeans.quality;

import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.utilities.documentation.Reference;

/**
 * Akaike Information Criterion (AIC).
 * <p>
 * Reference:
 * <p>
 * The use for k-means was briefly mentioned in:
 * <p>
 * D. Pelleg, A. Moore<br>
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters<br>
 * In: Proceedings of the 17th International Conference on Machine Learning
 * (ICML 2000)
 *
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 */
@Reference(authors = "D. Pelleg, A. Moore", //
    title = "X-means: Extending K-means with Efficient Estimation on the Number of Clusters", //
    booktitle = "Proc. 17th Int. Conf. on Machine Learning (ICML 2000)", //
    url = "http://www.pelleg.org/shared/hp/download/xmeans.ps", //
    bibkey = "DBLP:conf/icml/PellegM00")
public class AkaikeInformationCriterionXMeans extends AbstractKMeansQualityMeasure<NumberVector> {
  @Override
  public <V extends NumberVector> double quality(Clustering<? extends MeanModel> clustering, NumberVectorDistance<? super V> distanceFunction, Relation<V> relation) {
    return BayesianInformationCriterionXMeans.logLikelihoodXMeans(relation, clustering, distanceFunction) - numberOfFreeParameters(relation, clustering);
  }

  @Override
  public boolean isBetter(double currentCost, double bestCost) {
    // Careful: bestCost may be NaN!
    return !(currentCost <= bestCost);
  }
}
