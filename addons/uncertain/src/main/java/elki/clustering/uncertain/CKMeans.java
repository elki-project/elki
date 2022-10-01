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
package elki.clustering.uncertain;

import elki.Algorithm;
import elki.clustering.kmeans.HamerlyKMeans;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmeans.LloydKMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.distance.NumberVectorDistance;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Run k-means on the centers of each uncertain object.
 * <p>
 * This is a baseline reference method, that computes the center of mass
 * (centroid) of each object, then runs k-means on this.
 * <p>
 * References:
 * <p>
 * This algorithm was introduced as CK-Means in:
 * <p>
 * S. D. Lee, B. Kao, R. Cheng<br>
 * Reducing UK-means to K-means<br>
 * ICDM Data Mining Workshops, 2007
 * <p>
 * and was shown to be equivalent to UK-Means.
 * <p>
 * In summary, the expected distance used by UK-Means can be decomposed using
 * Steiner/König-Huygens into the sum of squares between the centroids, and the
 * sum of squared deviations within the uncertain object itself. This last term,
 * however, is constant.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Title("CK-means")
@Reference(authors = "S. D. Lee, B. Kao, R. Cheng", //
    title = "Reducing UK-means to K-means", //
    booktitle = "ICDM Data Mining Workshops, 2007", //
    url = "https://doi.org/10.1109/ICDMW.2007.40", //
    bibkey = "DBLP:conf/icdm/LeeKC07")
public class CKMeans extends CenterOfMassMetaClustering<Clustering<KMeansModel>> {
  /**
   * Constructor that uses an arbitrary k-means algorithm.
   *
   * @param kmeans K-Means algorithm to use.
   */
  public CKMeans(KMeans<?, KMeansModel> kmeans) {
    super(kmeans);
  }

  /**
   * Constructor that uses Lloyd's k-means algorithm.
   *
   * @param distance Distance functions for centers
   * @param k K parameter
   * @param maxiter Maximum number of iterations
   * @param initializer Initializer
   */
  public CKMeans(NumberVectorDistance<? super NumberVector> distance, int k, int maxiter, KMeansInitialization initializer) {
    super(new LloydKMeans<>(distance, k, maxiter, initializer));
  }

  /**
   * Parameterization class, based on k-means.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * K-means instance to use.
     */
    private KMeans<?, KMeansModel> kmeans;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<KMeans<?, KMeansModel>>(Algorithm.Utils.ALGORITHM_ID, KMeans.class, HamerlyKMeans.class) //
          .grab(config, x -> kmeans = x);
    }

    @Override
    public CKMeans make() {
      return new CKMeans(kmeans);
    }
  }
}
