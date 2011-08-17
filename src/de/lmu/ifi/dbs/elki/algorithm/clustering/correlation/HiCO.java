package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PCACorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Implementation of the HiCO algorithm, an algorithm for detecting hierarchies
 * of correlation clusters.
 * <p>
 * Reference: E. Achtert, C. Böhm, P. Kröger, A. Zimek: Mining Hierarchies of
 * Correlation Clusters. <br>
 * In: Proc. Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM'06), Vienna, Austria, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses KNNQueryFilteredPCAIndex
 * @apiviz.uses PCABasedCorrelationDistanceFunction
 * 
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Mining Hierarchies of Correlation Clusters")
@Description("Algorithm for detecting hierarchies of correlation clusters.")
@Reference(authors = "E. Achtert, C. Böhm, P. Kröger, A. Zimek", title = "Mining Hierarchies of Correlation Clusterse", booktitle = "Proc. Int. Conf. on Scientific and Statistical Database Management (SSDBM'06), Vienna, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.35")
public class HiCO<V extends NumberVector<V, ?>> extends OPTICS<V, PCACorrelationDistance> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(HiCO.class);

  /**
   * Parameter to specify the smoothing factor, must be an integer greater than
   * 0. The {link {@link #MU_ID}-nearest neighbor is used to compute the
   * correlation reachability of an object.
   * 
   * <p>
   * Key: {@code -hico.mu}
   * </p>
   */
  public static final OptionID MU_ID = OptionID.getOrCreateOptionID("hico.mu", "Specifies the smoothing factor. The mu-nearest neighbor is used to compute the correlation reachability of an object.");

  /**
   * Optional parameter to specify the number of nearest neighbors considered in
   * the PCA, must be an integer greater than 0. If this parameter is not set, k
   * is set to the value of {@link #MU_ID}.
   * <p>
   * Key: {@code -hico.k}
   * </p>
   * <p>
   * Default value: {@link #MU_ID}
   * </p>
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("hico.k", "Optional parameter to specify the number of nearest neighbors considered in the PCA. If this parameter is not set, k is set to the value of parameter mu.");

  /**
   * Parameter to specify the threshold of a distance between a vector q and a
   * given space that indicates that q adds a new dimension to the space, must
   * be a double equal to or greater than 0.
   * <p>
   * Default value: {@code 0.25}
   * </p>
   * <p>
   * Key: {@code -hico.delta}
   * </p>
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("hico.delta", "Threshold of a distance between a vector q and a given space that indicates that " + "q adds a new dimension to the space.");

  /**
   * The threshold for 'strong' eigenvectors: the 'strong' eigenvectors explain
   * a portion of at least alpha of the total variance.
   * <p>
   * Default value: {@link #DEFAULT_ALPHA}
   * </p>
   * <p>
   * Key: {@code -hico.alpha}
   * </p>
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hico.alpha", "The threshold for 'strong' eigenvectors: the 'strong' eigenvectors explain a portion of at least alpha of the total variance.");

  /**
   * The default value for {@link #DELTA_ID}.
   */
  public static final double DEFAULT_DELTA = 0.25;

  /**
   * The default value for {@link #ALPHA_ID}.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param mu Mu parameter
   */
  public HiCO(PCABasedCorrelationDistanceFunction distanceFunction, int mu) {
    super(distanceFunction, distanceFunction.getDistanceFactory().infiniteDistance(), mu);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    int mu = -1;
    
    PCABasedCorrelationDistanceFunction distance;
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter muP = new IntParameter(MU_ID, new GreaterConstraint(0));
      if (config.grab(muP)) {
        mu = muP.getValue();
      }

      IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0), true);
      final int k;
      if (config.grab(kP)) {
        k = kP.getValue();
      } else {
        k = mu;
      }

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0), DEFAULT_DELTA);
      double delta = DEFAULT_DELTA;
      if (config.grab(deltaP)) {
        delta = deltaP.getValue();
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), DEFAULT_ALPHA);
      double alpha = DEFAULT_ALPHA;
      if (config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }

      // Configure Distance function
      ListParameterization opticsParameters = new ListParameterization();
      // preprocessor
      opticsParameters.addParameter(IndexBasedDistanceFunction.INDEX_ID, KNNQueryFilteredPCAIndex.Factory.class);
      opticsParameters.addParameter(KNNQueryFilteredPCAIndex.Factory.K_ID, k);
      opticsParameters.addParameter(PercentageEigenPairFilter.ALPHA_ID, alpha);
      opticsParameters.addParameter(PCABasedCorrelationDistanceFunction.DELTA_ID, delta);

      ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
      chain.errorsTo(config);
      distance = chain.tryInstantiate(PCABasedCorrelationDistanceFunction.class);
    }

    @Override
    protected HiCO<V> makeInstance() {
      return new HiCO<V>(distance, mu);
    }
  }
}