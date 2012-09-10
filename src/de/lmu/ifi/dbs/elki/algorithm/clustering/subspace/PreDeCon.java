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

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj.PreDeConSubspaceIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * <p/>
 * PreDeCon computes clusters of subspace preference weighted connected points.
 * The algorithm searches for local subgroups of a set of feature vectors having
 * a low variance along one or more (but not all) attributes.
 * </p>
 * <p/>
 * Reference: <br>
 * C. Böhm, K. Kailing, H.-P. Kriegel, P. Kröger: Density Connected Clustering
 * with Local Subspace Preferences. <br>
 * In Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04), Brighton, UK, 2004.
 * </p>
 * 
 * @author Peer Kröger
 * 
 * @apiviz.uses PreDeConSubspaceIndex
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("PreDeCon: Subspace Preference weighted Density Connected Clustering")
@Description("PreDeCon computes clusters of subspace preference weighted connected points. " + "The algorithm searches for local subgroups of a set of feature vectors having " + "a low variance along one or more (but not all) attributes.")
@Reference(authors = "C. Böhm, K. Kailing, H.-P. Kriegel, P. Kröger", title = "Density Connected Clustering with Local Subspace Preferences", booktitle = "Proc. 4th IEEE Int. Conf. on Data Mining (ICDM'04), Brighton, UK, 2004", url = "http://dx.doi.org/10.1109/ICDM.2004.10087")
public class PreDeCon<V extends NumberVector<?>> extends AbstractProjectedDBSCAN<Clustering<Model>, V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PreDeCon.class);

  /**
   * Constructor.
   * 
   * @param epsilon Epsilon value
   * @param minpts MinPts value
   * @param distanceFunction outer distance function
   * @param lambda Lambda value
   */
  public PreDeCon(DoubleDistance epsilon, int minpts, LocallyWeightedDistanceFunction<V> distanceFunction, int lambda) {
    super(epsilon, minpts, distanceFunction, lambda);
  }

  @Override
  public String getLongResultName() {
    return "PreDeCon Clustering";
  }

  @Override
  public String getShortResultName() {
    return "predecon-clustering";
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
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractProjectedDBSCAN.Parameterizer<V, DoubleDistance> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configInnerDistance(config);
      configEpsilon(config, innerdist);
      configMinPts(config);
      configOuterDistance(config, epsilon, minpts, PreDeConSubspaceIndex.Factory.class, innerdist);
      configLambda(config);
    }

    @Override
    protected PreDeCon<V> makeInstance() {
      return new PreDeCon<V>(epsilon, minpts, outerdist, lambda);
    }
  }
}