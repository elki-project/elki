/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.quality;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Different version of the BIC criterion.
 *
 * Reference:
 * <p>
 * Q. Zhao, M. Xu, P. Fränti:<br />
 * Knee Point Detection on Bayesian Information Criterion<br />
 * 20th IEEE International Conference on Tools with Artificial Intelligence
 * </p>
 *
 * @author Tibor Goldschwendt
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "Q. Zhao, M. Xu, P. Fränti", //
    title = "Knee Point Detection on Bayesian Information Criterion", //
    booktitle = "20th IEEE International Conference on Tools with Artificial Intelligence", //
    url = "https://doi.org/10.1109/ICTAI.2008.154", //
    bibkey = "DBLP:conf/ictai/ZhaoXF08")
public class BayesianInformationCriterionZhao extends AbstractKMeansQualityMeasure<NumberVector> {
  @Override
  public <V extends NumberVector> double quality(Clustering<? extends MeanModel> clustering, NumberVectorDistanceFunction<? super V> distanceFunction, Relation<V> relation) {
    return logLikelihoodAlternate(relation, clustering, distanceFunction) //
        - (.5 * clustering.getAllClusters().size()) * FastMath.log(numPoints(clustering));
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
