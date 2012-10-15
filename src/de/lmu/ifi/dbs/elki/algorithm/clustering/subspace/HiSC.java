package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.HiSCDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.HiSCPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Implementation of the HiSC algorithm, an algorithm for detecting hierarchies
 * of subspace clusters.
 * <p>
 * Reference: E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman,
 * A. Zimek: Finding Hierarchies of Subspace Clusters. <br>
 * In: Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery
 * in Databases (PKDD'06), Berlin, Germany, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses HiSCPreferenceVectorIndex
 * @apiviz.uses HiSCDistanceFunction
 * 
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Finding Hierarchies of Subspace Clusters")
@Description("Algorithm for detecting hierarchies of subspace clusters.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek", title = "Finding Hierarchies of Subspace Clusters", booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06), Berlin, Germany, 2006", url = "http://www.dbs.ifi.lmu.de/Publikationen/Papers/PKDD06-HiSC.pdf")
public class HiSC<V extends NumberVector<?>> extends OPTICS<V, PreferenceVectorBasedCorrelationDistance> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HiSC.class);

  /**
   * Constructor.
   *
   * @param distanceFunction HiSC distance function used
   */
  public HiSC(HiSCDistanceFunction<V> distanceFunction) {
    super(distanceFunction, distanceFunction.getDistanceFactory().infiniteDistance(), 2);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    HiSCDistanceFunction<V> distanceFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter alphaP = new DoubleParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, HiSCPreferenceVectorIndex.Factory.DEFAULT_ALPHA);
      alphaP.addConstraint(new GreaterConstraint(0.0));
      alphaP.addConstraint(new LessConstraint(1.0));
      double alpha = 0.0;
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      // Configure HiSC distance function
      ListParameterization opticsParameters = new ListParameterization();

      // distance function
      opticsParameters.addParameter(HiSCDistanceFunction.EPSILON_ID, alpha);
      // preprocessor
      opticsParameters.addParameter(IndexBasedDistanceFunction.INDEX_ID, HiSCPreferenceVectorIndex.Factory.class);
      opticsParameters.addParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, alpha);

      ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
      chain.errorsTo(config);
      Class<HiSCDistanceFunction<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(HiSCDistanceFunction.class);
      distanceFunction = chain.tryInstantiate(cls);
    }

    @Override
    protected HiSC<V> makeInstance() {
      return new HiSC<V>(distanceFunction);
    }
  }
}