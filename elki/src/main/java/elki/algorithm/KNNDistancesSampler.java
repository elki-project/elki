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
package elki.algorithm;

import java.util.Arrays;

import elki.AbstractDistanceBasedAlgorithm;
import elki.algorithm.KNNDistancesSampler.KNNDistanceOrderResult;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.geometry.XYCurve;
import elki.result.Metadata;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 * <p>
 * This class can be used to estimate parameters for other algorithms, such as
 * estimating the epsilon parameter for DBSCAN: set k to minPts-1, and then
 * choose a percentile from the sample as epsilon, or plot the result as a graph
 * and look for a bend or knee in this plot.
 * <p>
 * Reference:
 * <p>
 * Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu<br>
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases
 * with Noise<br>
 * Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)
 * <p>
 * Further discussion:
 * <p>
 * Erich Schubert, Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu<br>
 * DBSCAN Revisited, Revisited: Why and How You Should (Still) Use DBSCAN<br>
 * ACM Trans. Database Systems (TODS)
 *
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @navassoc - produces - KNNDistanceOrderResult
 *
 * @param <O> the type of objects handled by this algorithm
 */
@Title("KNN-Distance-Order")
@Description("Assesses the knn distances for a specified k and orders them.")
@Reference(authors = "Martin Ester, Hans-Peter Kriegel, Jörg Sander, Xiaowei Xu", //
    title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", //
    booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96)", //
    url = "http://www.aaai.org/Library/KDD/1996/kdd96-037.php", //
    bibkey = "DBLP:conf/kdd/EsterKSX96")
@Reference(authors = "Erich Schubert, Jörg Sander, Martin Ester, Hans-Peter Kriegel, Xiaowei Xu", //
    title = "DBSCAN Revisited, Revisited: Why and How You Should (Still) Use DBSCAN", //
    booktitle = "ACM Trans. Database Systems (TODS)", //
    url = "https://doi.org/10.1145/3068335", //
    bibkey = "DBLP:journals/tods/SchubertSEKX17")
public class KNNDistancesSampler<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, KNNDistanceOrderResult> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(KNNDistancesSampler.class);

  /**
   * Parameter k.
   */
  protected int k;

  /**
   * Sampling percentage.
   */
  protected double sample;

  /**
   * Random number seeding.
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param k k Parameter
   * @param sample Sampling rate, or sample size (when &gt; 1)
   * @param rnd Random source.
   */
  public KNNDistancesSampler(Distance<? super O> distanceFunction, int k, double sample, RandomFactory rnd) {
    super(distanceFunction);
    this.k = k;
    this.sample = sample;
    this.rnd = rnd;
  }

  /**
   * Provides an order of the kNN-distances for all objects within the specified
   * database.
   *
   * @param relation Relation
   * @return Result
   */
  public KNNDistanceOrderResult run(Relation<O> relation) {
    final KNNQuery<O> knnQuery = relation.getKNNQuery(getDistance(), k + 1);
    final int size = (int) ((sample <= 1.) ? Math.ceil(relation.size() * sample) : sample);
    DBIDs sample = DBIDUtil.randomSample(relation.getDBIDs(), size, rnd);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Sampling kNN distances", size, LOG) : null;
    double[] knnDistances = new double[size];
    int i = 0;
    for(DBIDIter iditer = sample.iter(); iditer.valid(); iditer.advance()) {
      final KNNList neighbors = knnQuery.getKNNForDBID(iditer, k + 1);
      knnDistances[i++] = neighbors.getKNNDistance();
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return new KNNDistanceOrderResult(knnDistances, k);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Curve result for a list containing the knn distances.
   *
   * @author Arthur Zimek
   */
  public static class KNNDistanceOrderResult extends XYCurve {
    /**
     * Construct result
     *
     * @param knnDistances distance list to wrap.
     * @param k number of neighbors considered
     */
    public KNNDistanceOrderResult(double[] knnDistances, int k) {
      super("Objects", k + "-NN-distance", knnDistances.length + 1);
      Arrays.sort(knnDistances);
      for(int j = 0; j < knnDistances.length; j++) {
        this.addAndSimplify(knnDistances.length - j, knnDistances[j]);
      }
      Metadata.of(this).setLongName(k + "-NN distance order");
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * Parameter to specify the distance of the k-distant object to be assessed,
     * must be an integer greater than 0.
     */
    public static final OptionID K_ID = new OptionID("knndistanceorder.k", "Specifies the distance of the k-distant object to be assessed, ignoring the query object.");

    /**
     * Parameter to specify the average percentage of distances randomly chosen
     * to be provided in the result, must be a double greater than 0 and less
     * than or equal to 1.
     */
    public static final OptionID SAMPLING_ID = new OptionID("knndistanceorder.sample", "The percentage of objects to use for sampling, or the absolute number of samples.");

    /**
     * Random generator seed for distances.
     */
    public static final OptionID SEED_ID = new OptionID("knndistanceorder.seed", "Random generator seed for sampling.");

    /**
     * Parameter k.
     */
    protected int k;

    /**
     * Sampling percentage.
     */
    protected double percentage;

    /**
     * Random number seeding.
     */
    private RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new DoubleParameter(SAMPLING_ID, 1.) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> percentage = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public KNNDistancesSampler<O> make() {
      return new KNNDistancesSampler<>(distanceFunction, k, percentage, rnd);
    }
  }
}
