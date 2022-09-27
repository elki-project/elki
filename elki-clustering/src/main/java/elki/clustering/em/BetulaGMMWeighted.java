/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.clustering.em;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import elki.clustering.em.models.BetulaClusterModel;
import elki.clustering.em.models.BetulaClusterModelFactory;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.features.ClusterFeature;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;

import net.jafama.FastMath;

/**
 * Clustering by expectation maximization (EM-Algorithm), also known as Gaussian
 * Mixture Modeling (GMM), with optional MAP regularization. This version
 * performs
 * a more complex weighting based on the overlap of Gaussians &ndash; this is
 * more expensive, and experimentally did not produce much better results than
 * {@link BetulaGMM}.
 * <p>
 * Reference:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Andreas Lang
 * @since 0.8.0
 */
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class BetulaGMMWeighted extends BetulaGMM {
  /**
   * Constructor.
   *
   * @param cffactory CFTree factory
   * @param k Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initialization Initialization method
   * @param prior MAP prior
   */
  public BetulaGMMWeighted(CFTree.Factory<?> cffactory, double delta, int k, int maxiter, boolean soft, BetulaClusterModelFactory<?> initialization, double prior) {
    super(cffactory, delta, k, maxiter, soft, initialization, prior);
  }

  @Override
  public double assignProbabilitiesToInstances(ArrayList<? extends ClusterFeature> cfs, List<? extends BetulaClusterModel> models, Map<ClusterFeature, double[]> probClusterIGivenX) {
    final int k = models.size();
    double emSum = 0.;
    int n = 0;
    for(int i = 0; i < cfs.size(); i++) {
      ClusterFeature cfsi = cfs.get(i);
      double[] probs = new double[k];
      for(int j = 0; j < k; j++) {
        final double v = models.get(j).estimateLogDensity(cfsi);
        probs[j] = v > MIN_LOGLIKELIHOOD ? v : MIN_LOGLIKELIHOOD;
      }
      final double logP = EM.logSumExp(probs);
      for(int j = 0; j < k; j++) {
        probs[j] = FastMath.exp(probs[j] - logP);
      }
      probClusterIGivenX.put(cfsi, probs);
      emSum += logP * cfsi.getWeight();
      n += cfsi.getWeight();
    }
    return emSum / n;
  }

  /**
   * Parameterizer
   * 
   * @author Andreas Lang
   */
  public static class Par extends BetulaGMM.Par {
    @Override
    public BetulaGMMWeighted make() {
      return new BetulaGMMWeighted(cffactory, delta, k, maxiter, soft, initialization, prior);
    }
  }
}
