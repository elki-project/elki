/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.outlier.clustering;

import elki.clustering.kmeans.KMeansMinusMinus;
import elki.database.Database;
import elki.result.Metadata;
import elki.result.outlier.OutlierResult;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * k-means--: A Unified Approach to Clustering and Outlier Detection.
 * <p>
 * This implementation assigns the outlier label to all points that were
 * identified as noise according to the {@code KMeans--} algorithm.
 * <p>
 * Reference:
 * <p>
 * S. Chawla, A. Gionis<br>
 * k-means--: A Unified Approach to Clustering and Outlier Detection<br>
 * Proc. 13th SIAM Int. Conf. on Data Mining (SDM 2013)
 *
 * @author Braulio V.S. Vinces (ELKIfication)
 * @since 0.8.0
 */
@Title("K-Means--")
@Reference(authors = "S. Chawla, A. Gionis", //
    title = "k-means--: A Unified Approach to Clustering and Outlier Detection", //
    booktitle = "Proc. 13th SIAM Int. Conf. on Data Mining (SDM 2013)", //
    url = "https://doi.org/10.1137/1.9781611972832.21", //
    bibkey = "DBLP:conf/sdm/ChawlaG13")
public class KMeansMinusMinusOutlierDetection extends NoiseAsOutliers {
  /**
   * Constructor.
   *
   * @param kmeansminusminus Clustering algorithm
   */
  public KMeansMinusMinusOutlierDetection(KMeansMinusMinus<?> kmeansminusminus) {
    super(kmeansminusminus);
  }

  @Override
  public OutlierResult autorun(Database database) {
    OutlierResult res = super.autorun(database);
    Metadata.of(res).setLongName("K-means-- outlier labels");
    return res;
  }

  /**
   * Parameterizer.
   *
   * @author Braulio V.S. Vinces
   */
  public static class Par implements Parameterizer {
    /**
     * Clustering algorithm to run.
     */
    KMeansMinusMinus<?> kmeansminusminus;

    @Override
    public void configure(Parameterization config) {
      ChainedParameterization list = new ChainedParameterization(new ListParameterization() //
          .addFlag(KMeansMinusMinus.Par.NOISE_FLAG_ID), config);
      list.errorsTo(config);
      kmeansminusminus = list.tryInstantiate(KMeansMinusMinus.class);
    }

    @Override
    public KMeansMinusMinusOutlierDetection make() {
      return new KMeansMinusMinusOutlierDetection(kmeansminusminus);
    }
  }
}
