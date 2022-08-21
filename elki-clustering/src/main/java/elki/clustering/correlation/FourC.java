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
package elki.clustering.correlation;

import elki.clustering.dbscan.DBSCAN;
import elki.clustering.dbscan.GeneralizedDBSCAN;
import elki.clustering.dbscan.predicates.FourCCorePredicate;
import elki.clustering.dbscan.predicates.FourCNeighborPredicate;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.math.linearalgebra.pca.filter.LimitEigenPairFilter;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;

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
 */
@Title("4C: Computing Correlation Connected Clusters")
@Description("4C identifies local subgroups of data objects sharing a uniform correlation. " //
    + "The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).")
@Reference(authors = "Christian Böhm, Karin Kailing, Peer Kröger, Arthur Zimek", //
    title = "Computing Clusters of Correlation Connected Objects", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2004)", //
    url = "https://doi.org/10.1145/1007568.1007620", //
    bibkey = "DBLP:conf/sigmod/BohmKKZ04")
public class FourC extends GeneralizedDBSCAN {
  /**
   * Constructor.
   *
   * @param settings FourC settings.
   */
  public FourC(FourC.Settings settings) {
    super(new FourCNeighborPredicate(settings), new FourCCorePredicate(settings), false);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
    public static class Par implements Parameterizer {
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
      public void configure(Parameterization config) {
        settings = new Settings();
        new DoubleParameter(DBSCAN.Par.EPSILON_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
            .grab(config, x -> settings.epsilon = x);
        new IntParameter(DBSCAN.Par.MINPTS_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> settings.minpts = x);
        // Flag for using absolute variances
        new Flag(LimitEigenPairFilter.Par.EIGENPAIR_FILTER_ABSOLUTE) //
            .grab(config, x -> settings.absolute = x);
        // Parameter delta
        DoubleParameter deltaP = new DoubleParameter(LimitEigenPairFilter.Par.EIGENPAIR_FILTER_DELTA) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(!settings.absolute) {
          deltaP.setDefaultValue(DEFAULT_DELTA);
        }
        else {
          deltaP.addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);
        }
        deltaP.grab(config, x -> settings.delta = x);
        new DoubleParameter(KAPPA_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE) //
            .setDefaultValue(KAPPA_DEFAULT) //
            .grab(config, x1 -> settings.kappa = x1);
        new IntParameter(LAMBDA_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .setOptional(true) //
            .grab(config, x -> settings.lambda = x);
      }

      @Override
      public Object make() {
        return settings;
      }
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Settings storage.
     */
    Settings settings;

    @Override
    public void configure(Parameterization config) {
      settings = config.tryInstantiate(FourC.Settings.class);
    }

    @Override
    public FourC make() {
      return new FourC(settings);
    }
  }
}
