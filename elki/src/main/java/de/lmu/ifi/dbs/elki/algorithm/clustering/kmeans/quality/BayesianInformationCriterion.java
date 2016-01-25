package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality;

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

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Bayesian Information Criterion (BIC), also known as Schwarz criterion (SBC,
 * SBIC) for the use with evaluating k-means results.
 *
 * Reference:
 * <p>
 * G. Schwarz<br />
 * Estimating the dimension of a model<br />
 * The annals of statistics 6.2.
 * </p>
 *
 * The use for k-means was popularized by:
 * <p>
 * D. Pelleg, A. Moore:<br />
 * X-means: Extending K-means with Efficient Estimation on the Number of
 * Clusters<br />
 * In: Proceedings of the 17th International Conference on Machine Learning
 * (ICML 2000)
 * </p>
 *
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "G. Schwarz", //
title = "Estimating the dimension of a model", //
booktitle = "The annals of statistics 6.2", //
url = "http://dx.doi.org/10.1214/aos/1176344136")
public class BayesianInformationCriterion extends AbstractKMeansQualityMeasure<NumberVector> {
  @Override
  public <V extends NumberVector> double quality(Clustering<? extends MeanModel> clustering, NumberVectorDistanceFunction<? super V> distanceFunction, Relation<V> relation) {
    return logLikelihood(relation, clustering, distanceFunction) //
        - (.5 * numberOfFreeParameters(relation, clustering)) * Math.log(numPoints(clustering));
  }

  @Override
  public boolean ascending() {
    return true;
  }

  @Override
  public boolean isBetter(double currentCost, double bestCost) {
    // Careful: bestCost may be NaN!
    return !(currentCost <= bestCost);
  }
}
