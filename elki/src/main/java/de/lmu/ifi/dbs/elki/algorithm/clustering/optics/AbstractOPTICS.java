package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * The OPTICS algorithm for density-based hierarchical clustering.
 * 
 * This is the abstract base class, providing the shared parameters only.
 * 
 * Reference:
 * <p>
 * M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander:<br />
 * OPTICS: Ordering Points to Identify the Clustering Structure. <br/>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> the type of DatabaseObjects handled by the algorithm
 */
@Title("OPTICS: Density-Based Hierarchical Clustering")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minPts' and 'epsilon' (specifying a volume). These two parameters determine a density threshold for clustering.")
@Reference(authors = "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander", //
title = "OPTICS: Ordering Points to Identify the Clustering Structure", //
booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", //
url = "http://dx.doi.org/10.1145/304181.304187")
@Alias({ "OPTICS", "de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS" })
public abstract class AbstractOPTICS<O> extends AbstractDistanceBasedAlgorithm<O, ClusterOrder> implements OPTICSTypeAlgorithm {
  /**
   * Holds the maximum distance to search for objects (performance parameter)
   */
  protected double epsilon;

  /**
   * The density threshold, in number of points.
   */
  protected int minpts;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public AbstractOPTICS(DistanceFunction<? super O> distanceFunction, double epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Run OPTICS on the database.
   * 
   * @param db Database
   * @param relation Relation
   * @return Result
   */
  public abstract ClusterOrder run(Database db, Relation<O> relation);

  @Override
  public int getMinPts() {
    return minpts;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <O> Object type
   */
  public static abstract class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered, must be suitable to the distance function specified.
     */
    public static final OptionID EPSILON_ID = new OptionID("optics.epsilon", "The maximum radius of the neighborhood to be considered.");

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("optics.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

    /**
     * Epsilon radius.
     */
    protected double epsilon = Double.POSITIVE_INFINITY;

    /**
     * Minimum number of points.
     */
    protected int minpts = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID) //
      .setOptional(true);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minpts = minptsP.intValue();
      }
    }
  }
}