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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.GeneralizedDBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.PreDeConCorePredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.PreDeConNeighborPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

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
    public double kappa = Parameterizer.KAPPA_DEFAULT;

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
    public static class Parameterizer extends AbstractParameterizer {
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
      public void makeOptions(Parameterization config) {
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
        DoubleParameter deltaP = new DoubleParameter(DELTA_ID) //
            .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
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
      public Settings makeInstance() {
        return settings;
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * PreDeConSettings.
     */
    protected PreDeCon.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      settings = config.tryInstantiate(PreDeCon.Settings.class);
    }

    @Override
    protected PreDeCon<V> makeInstance() {
      return new PreDeCon<>(settings);
    }
  }
}
