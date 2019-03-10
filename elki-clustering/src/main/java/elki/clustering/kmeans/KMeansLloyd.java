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
package elki.clustering.kmeans;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.distance.distancefunction.NumberVectorDistance;
import elki.logging.Logging;
import elki.utilities.Alias;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * The standard k-means algorithm, using bulk iterations and commonly attributed
 * to Lloyd and Forgy (independently).
 * <p>
 * Reference:
 * <p>
 * S. Lloyd<br>
 * Least squares quantization in PCM<br>
 * IEEE Transactions on Information Theory 28 (2)<br>
 * previously published as Bell Telephone Laboratories Paper
 * <p>
 * E. W. Forgy<br>
 * Cluster analysis of multivariate data: efficiency versus interpretability of
 * classifications<br>
 * Abstract published in Biometrics 21(3)
 *
 * @author Arthur Zimek
 * @since 0.5.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Title("k-Means (Lloyd/Forgy Algorithm)")
@Reference(authors = "S. Lloyd", //
    title = "Least squares quantization in PCM", //
    booktitle = "IEEE Transactions on Information Theory 28 (2): 129–137.", //
    url = "https://doi.org/10.1109/TIT.1982.1056489", //
    bibkey = "DBLP:journals/tit/Lloyd82")
@Reference(authors = "E. W. Forgy", //
    title = "Cluster analysis of multivariate data: efficiency versus interpretability of classifications", //
    booktitle = "Biometrics 21(3)", //
    bibkey = "journals/biometrics/Forgy65")
@Alias({ "lloyd", "forgy" })
public class KMeansLloyd<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KMeansLloyd.class);

  /**
   * Constructor.
   *
   * @param distanceFunction distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Initialization method
   */
  public KMeansLloyd(NumberVectorDistance<? super V> distanceFunction, int k, int maxiter, KMeansInitialization initializer) {
    super(distanceFunction, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Database database, Relation<V> relation) {
    Instance instance = new Instance(relation, getDistance(), initialMeans(database, relation));
    instance.run(maxiter);
    return instance.buildResult();
  }

  /**
   * Inner instance, storing state for a single data set.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends AbstractKMeans.Instance {
    /**
     * Constructor.
     *
     * @param relation Relation
     * @param means Initial means
     */
    public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means) {
      super(relation, df, means);
    }

    @Override
    protected int iterate(int iteration) {
      means = iteration == 1 ? means : means(clusters, means, relation);
      return assignToNearestCluster();
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractKMeans.Parameterizer<V> {
    @Override
    protected KMeansLloyd<V> makeInstance() {
      return new KMeansLloyd<>(distanceFunction, k, maxiter, initializer);
    }
  }
}
