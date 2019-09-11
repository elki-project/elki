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
package elki.clustering.subspace;

import elki.clustering.dbscan.DBSCAN;
import elki.clustering.dbscan.GeneralizedDBSCAN;
import elki.clustering.dbscan.predicates.PreDeConCorePredicate;
import elki.clustering.dbscan.predicates.PreDeConNeighborPredicate;
import elki.data.NumberVector;
import elki.logging.Logging;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * PreDeCon computes clusters of subspace preference weighted connected points.
 * The algorithm searches for local subgroups of a set of feature vectors having
 * a low variance along one or more (but not all) attributes.
 * <p>
 * Reference:
 * <p>
 * Christian Böhm, Karin Kailing, Hans-Peter Kriegel, Peer Kröger<br>
 * Density Connected Clustering with Local Subspace Preferences.<br>
 * Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04)
 *
 * @author Peer Kröger
 * @since 0.1
 *
 * @has - - - PreDeCon.Settings
 * @composed - - - PreDeConNeighborPredicate
 * @composed - - - PreDeConCorePredicate
 *
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("PreDeCon: Subspace Preference weighted Density Connected Clustering")
@Description("PreDeCon computes clusters of subspace preference weighted connected points. "//
    + "The algorithm searches for local subgroups of a set of feature vectors having " + "a low variance along one or more (but not all) attributes.")
@Reference(authors = "Christian Böhm, Karin Kailing, Hans-Peter Kriegel, Peer Kröger", //
    title = "Density Connected Clustering with Local Subspace Preferences", //
    booktitle = "Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04)", //
    url = "https://doi.org/10.1109/ICDM.2004.10087", //
    bibkey = "DBLP:conf/icdm/BohmKKK04")
public class PreDeCon<V extends NumberVector> extends GeneralizedDBSCAN {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PreDeCon.class);

  /**
   * Constructor.
   * 
   * @param settings PreDeCon settings.
   */
  public PreDeCon(PreDeCon.Settings settings) {
    super(new PreDeConNeighborPredicate<>(settings), new PreDeConCorePredicate(settings), false);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Class containing all the PreDeCon settings.
   * 
   * @author Erich Schubert
   */
  public static class Settings {
    /**
     * Query radius parameter epsilon.
     */
    public double epsilon;

    /**
     * The threshold for small eigenvalues.
     */
    public double delta;

    /**
     * The kappa penality factor for deviations in preferred dimensions.
     */
    public double kappa = Par.KAPPA_DEFAULT;

    /**
     * DBSCAN Minpts parameter, aka "mu".
     */
    public int minpts;

    /**
     * Lambda: Maximum subspace dimensionality.
     */
    public int lambda = Integer.MAX_VALUE;

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public static class Par implements Parameterizer {
      /**
       * Parameter Delta: maximum variance allowed
       */
      public static final OptionID DELTA_ID = new OptionID("predecon.delta", "A double specifying the variance threshold for small Eigenvalues.");

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
       * Settings to build.
       */
      Settings settings;

      @Override
      public void configure(Parameterization config) {
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
        new DoubleParameter(DBSCAN.Par.EPSILON_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
            .grab(config, x -> settings.epsilon = x);
      }

      /**
       * Configure the minPts aka "mu" parameter.
       * 
       * @param config Parameter source
       */
      protected void configMinPts(Parameterization config) {
        new IntParameter(DBSCAN.Par.MINPTS_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> settings.minpts = x);
      }

      /**
       * Configure the delta parameter.
       * 
       * @param config Parameter source
       */
      protected void configDelta(Parameterization config) {
        new DoubleParameter(DELTA_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
            .grab(config, x -> settings.delta = x);
      }

      /**
       * Configure the kappa parameter.
       * 
       * @param config Parameter source
       */
      protected void configKappa(Parameterization config) {
        new DoubleParameter(KAPPA_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE) //
            .setDefaultValue(KAPPA_DEFAULT) //
            .grab(config, x -> settings.kappa = x);
      }

      /**
       * Configure the delta parameter.
       * 
       * @param config Parameter source
       */
      protected void configLambda(Parameterization config) {
        new IntParameter(LAMBDA_ID) //
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .setOptional(true) //
            .grab(config, x -> settings.lambda = x);
      }

      @Override
      public Settings make() {
        return settings;
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * PreDeConSettings.
     */
    protected PreDeCon.Settings settings;

    @Override
    public void configure(Parameterization config) {
      settings = config.tryInstantiate(PreDeCon.Settings.class);
    }

    @Override
    public PreDeCon<V> make() {
      return new PreDeCon<>(settings);
    }
  }
}
