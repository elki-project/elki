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

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedDBSCAN;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj.FourCSubspaceIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * 4C identifies local subgroups of data objects sharing a uniform correlation.
 * The algorithm is based on a combination of PCA and density-based clustering
 * (DBSCAN).
 * <p>
 * Reference: Christian Böhm, Karin Kailing, Peer Kröger, Arthur Zimek:
 * Computing Clusters of Correlation Connected Objects. <br>
 * In Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses FourCSubspaceIndex
 * 
 * @param <V> type of NumberVector handled by this Algorithm
 */
@Title("4C: Computing Correlation Connected Clusters")
@Description("4C identifies local subgroups of data objects sharing a uniform correlation. " + "The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).")
@Reference(authors = "C. Böhm, K. Kailing, P. Kröger, A. Zimek", title = "Computing Clusters of Correlation Connected Objects", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466", url = "http://dx.doi.org/10.1145/1007568.1007620")
public class FourC<V extends NumberVector<V, ?>> extends AbstractProjectedDBSCAN<Clustering<Model>, V> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(FourC.class);

  /**
   * Constructor.
   * 
   * @param epsilon Epsilon value
   * @param minpts MinPts value
   * @param distanceFunction Distance function
   * @param lambda Lambda value
   */
  public FourC(DoubleDistance epsilon, int minpts, LocallyWeightedDistanceFunction<V> distanceFunction, int lambda) {
    super(epsilon, minpts, distanceFunction, lambda);
  }

  @Override
  public String getLongResultName() {
    return "4C Clustering";
  }

  @Override
  public String getShortResultName() {
    return "4c-clustering";
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractProjectedDBSCAN.Parameterizer<O, DoubleDistance> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configInnerDistance(config);
      configEpsilon(config, innerdist);
      configMinPts(config);
      configOuterDistance(config, epsilon, minpts, FourCSubspaceIndex.Factory.class, innerdist);
      configLambda(config);
    }

    @Override
    protected FourC<O> makeInstance() {
      return new FourC<O>(epsilon, minpts, outerdist, lambda);
    }
  }
}