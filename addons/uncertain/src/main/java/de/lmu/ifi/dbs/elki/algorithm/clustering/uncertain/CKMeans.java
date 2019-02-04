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
package de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansHamerly;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
@Reference(authors = "S. D. Lee, B. Kao, R. Cheng", //
    title = "Reducing UK-means to K-means", //
    booktitle = "ICDM Data Mining Workshops, 2007", //
    url = "https://doi.org/10.1109/ICDMW.2007.40", //
    bibkey = "DBLP:conf/icdm/LeeKC07")
public class CKMeans extends CenterOfMassMetaClustering<Clustering<KMeansModel>> {
  /**
   * CLass logger.
   */
  private static final Logging LOG = Logging.getLogger(CKMeans.class);

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
   * @param distanceFunction Distance functions for centers
   * @param k K parameter
   * @param maxiter Maximum number of iterations
   * @param initializer Initializer
   */
  public CKMeans(NumberVectorDistanceFunction<? super NumberVector> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(new KMeansLloyd<>(distanceFunction, k, maxiter, initializer));
  }

  @Override
  protected Logging getLogger() {
    return CKMeans.LOG;
  }

  /**
   * Parameterization class, based on k-means.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * K-means instance to use.
     */
    private KMeans<?, KMeansModel> kmeans;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<KMeans<?, KMeansModel>> kmeansP = new ObjectParameter<>(AbstractAlgorithm.ALGORITHM_ID, KMeans.class, KMeansHamerly.class);
      if(config.grab(kmeansP)) {
        kmeans = kmeansP.instantiateClass(config);
      }
    }

    @Override
    protected CKMeans makeInstance() {
      return new CKMeans(kmeans);
    }
  }
}
