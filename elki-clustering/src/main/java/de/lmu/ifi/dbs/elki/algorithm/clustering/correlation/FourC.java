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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.FourCCorePredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.FourCNeighborPredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.GeneralizedDBSCAN;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.LimitEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * 4C identifies local subgroups of data objects sharing a uniform correlation.
 * The algorithm is based on a combination of PCA and density-based clustering
 * (DBSCAN).
 * <p>
 * Reference:
 * <p>
 * Christian Böhm, Karin Kailing, Peer Kröger, Arthur Zimek<br>
 * Computing Clusters of Correlation Connected Objects<br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2004)
 *
 * @author Arthur Zimek
 * @since 0.1
 *
 * @composed - - - Settings
 * @composed - - - FourCNeighborPredicate
 * @composed - - - FourCCorePredicate
 *
 * @param <V> type of NumberVector handled by this Algorithm
 */
@Title("4C: Computing Correlation Connected Clusters")
@Description("4C identifies local subgroups of data objects sharing a uniform correlation. " //
    + "The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).")
@Reference(authors = "Christian Böhm, Karin Kailing, Peer Kröger, Arthur Zimek", //
    title = "Computing Clusters of Correlation Connected Objects", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2004)", //
    url = "https://doi.org/10.1145/1007568.1007620", //
    bibkey = "DBLP:conf/sigmod/BohmKKZ04")
public class FourC<V extends NumberVector> extends GeneralizedDBSCAN {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FourC.class);

  /**
   * Constructor.
   *
   * @param settings FourC settings.
   */
  public FourC(FourC.Settings settings) {
    super(new FourCNeighborPredicate<V>(settings), new FourCCorePredicate(settings), false);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Class wrapping the 4C parameter settings.
   *
   * @author Erich Schubert
   */
  public static class Settings {
    /**
     * Query radius epsilon.
     */
    public double epsilon;

    /**
     * Use absolute variance, not relative variance.
     */
    public boolean absolute = false;

    /**
     * Delta parameter, for selecting strong Eigenvectors.
     */
    public double delta = 0.0;

    /**
     * Kappa penalty parameter, to punish deviation in low-variance
     * Eigenvectors.
     */
    public double kappa = 50.;

    /**
     * Maximum subspace dimensionality lambda.
     */
    public int lambda = Integer.MAX_VALUE;

    /**
     * MinPts / mu parameter.
     */
    public int minpts;

    /**
     * Parameterization class for 4C settings.
     *
     * @author Erich Schubert
     */
    public static class Parameterizer extends AbstractParameterizer {
      /**
       * The default value for delta.
       */
      public static final double DEFAULT_DELTA = .1;

      /**
       * Parameter Kappa: penalty for deviations in preferred dimensions.
       */
      public static final OptionID KAPPA_ID = new OptionID("predecon.kappa", "Penalty factor for deviations in preferred (low-variance) dimensions.");

      /**
       * Default for kappa parameter.
       */
      public static final double KAPPA_DEFAULT = 20.;

      /**
       * Parameter Lambda: maximum dimensionality allowed.
       */
      public static final OptionID LAMBDA_ID = new OptionID("predecon.lambda", "Maximum dimensionality to consider for core points.");

      /**
       * Settings storage.
       */
      Settings settings;

      @Override
      protected void makeOptions(Parameterization config) {
        settings = new Settings();
        configEpsilon(config);
        configMinPts(config);
        configDelta(config);
        configKappa(config);
        configLambda(config);
      }

      /**
       * Configure the epsilon radius parameter.
       *
       * @param config Parameter source
       */
      protected void configEpsilon(Parameterization config) {
        DoubleParameter epsilonP = new DoubleParameter(DBSCAN.Parameterizer.EPSILON_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(config.grab(epsilonP)) {
          settings.epsilon = epsilonP.doubleValue();
        }
      }

      /**
       * Configure the minPts aka "mu" parameter.
       *
       * @param config Parameter source
       */
      protected void configMinPts(Parameterization config) {
        IntParameter minptsP = new IntParameter(DBSCAN.Parameterizer.MINPTS_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(minptsP)) {
          settings.minpts = minptsP.intValue();
        }
      }

      /**
       * Configure the delta parameter.
       *
       * @param config Parameter source
       */
      protected void configDelta(Parameterization config) {
        // Flag for using absolute variances
        Flag absoluteF = new Flag(LimitEigenPairFilter.Parameterizer.EIGENPAIR_FILTER_ABSOLUTE);
        if(config.grab(absoluteF)) {
          settings.absolute = absoluteF.isTrue();
        }

        // Parameter delta
        DoubleParameter deltaP = new DoubleParameter(LimitEigenPairFilter.Parameterizer.EIGENPAIR_FILTER_DELTA) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(!settings.absolute) {
          deltaP.setDefaultValue(DEFAULT_DELTA);
        }
        else {
          deltaP.addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
        }
        if(config.grab(deltaP)) {
          settings.delta = deltaP.doubleValue();
        }
      }

      /**
       * Configure the kappa parameter.
       *
       * @param config Parameter source
       */
      protected void configKappa(Parameterization config) {
        DoubleParameter kappaP = new DoubleParameter(KAPPA_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE) //
            .setDefaultValue(KAPPA_DEFAULT);
        if(config.grab(kappaP)) {
          settings.kappa = kappaP.doubleValue();
        }
      }

      /**
       * Configure the delta parameter.
       *
       * @param config Parameter source
       */
      protected void configLambda(Parameterization config) {
        IntParameter lambdaP = new IntParameter(LAMBDA_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .setOptional(true);
        if(config.grab(lambdaP)) {
          settings.lambda = lambdaP.intValue();
        }
      }

      @Override
      protected Object makeInstance() {
        return settings;
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * Settings storage.
     */
    Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      settings = config.tryInstantiate(FourC.Settings.class);
    }

    @Override
    protected FourC<O> makeInstance() {
      return new FourC<>(settings);
    }
  }
}
