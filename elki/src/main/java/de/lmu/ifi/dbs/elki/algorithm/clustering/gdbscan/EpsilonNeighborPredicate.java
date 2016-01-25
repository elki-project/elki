package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

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

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The default DBSCAN and OPTICS neighbor predicate, using an
 * epsilon-neighborhood.
 *
 * Reference:
 * <p>
 * M. Ester, H.-P. Kriegel, J. Sander, X. Xu<br />
 * A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases
 * with Noise<br />
 * In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96),
 * Portland, OR, 1996.
 * </p>
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @apiviz.has Instance
 *
 * @param <O> object type
 */
@Reference(authors = "M. Ester, H.-P. Kriegel, J. Sander, X. Xu", //
title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", //
booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996", //
url = "http://www.aaai.org/Papers/KDD/1996/KDD96-037")
public class EpsilonNeighborPredicate<O> implements NeighborPredicate {
  /**
   * Range to query with
   */
  protected double epsilon;

  /**
   * Distance function to use
   */
  protected DistanceFunction<? super O> distFunc;

  /**
   * Full constructor.
   *
   * @param epsilon Epsilon value
   * @param distFunc Distance function to use
   */
  public EpsilonNeighborPredicate(double epsilon, DistanceFunction<? super O> distFunc) {
    super();
    this.epsilon = epsilon;
    this.distFunc = distFunc;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> NeighborPredicate.Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    DistanceQuery<O> dq = QueryUtil.getDistanceQuery(database, distFunc);
    RangeQuery<O> rq = database.getRangeQuery(dq);
    return (NeighborPredicate.Instance<T>) new Instance(epsilon, rq, dq.getRelation().getDBIDs());
  }

  @Override
  public SimpleTypeInformation<?>[] getOutputType() {
    return new SimpleTypeInformation<?>[] { TypeUtil.DBIDS, TypeUtil.NEIGHBORLIST };
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return distFunc.getInputTypeRestriction();
  }

  /**
   * Instance for a particular data set.
   *
   * @author Erich Schubert
   */
  public static class Instance implements NeighborPredicate.Instance<DoubleDBIDList> {
    /**
     * Range to query with
     */
    protected double epsilon;

    /**
     * Range query to use on the database.
     */
    protected RangeQuery<?> rq;

    /**
     * DBIDs to process
     */
    protected DBIDs ids;

    /**
     * Constructor.
     *
     * @param epsilon Epsilon
     * @param rq Range query to use
     * @param ids DBIDs to process
     */
    public Instance(double epsilon, RangeQuery<?> rq, DBIDs ids) {
      super();
      this.epsilon = epsilon;
      this.rq = rq;
      this.ids = ids;
    }

    @Override
    public DBIDs getIDs() {
      return ids;
    }

    @Override
    public DoubleDBIDList getNeighbors(DBIDRef reference) {
      return rq.getRangeForDBID(reference, epsilon);
    }

    @Override
    public DBIDIter iterDBIDs(DoubleDBIDList neighbors) {
      return neighbors.iter();
    }
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   *
   * @param <O> object type
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Range to query with
     */
    protected double epsilon;

    /**
     * Distance function to use
     */
    protected DistanceFunction<O> distfun = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get a distance function.
      ObjectParameter<DistanceFunction<O>> distanceP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceP)) {
        distfun = distanceP.instantiateClass(config);
      }
      // Get the epsilon parameter
      DoubleParameter epsilonP = new DoubleParameter(DBSCAN.Parameterizer.EPSILON_ID) //
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }
    }

    @Override
    protected EpsilonNeighborPredicate<O> makeInstance() {
      return new EpsilonNeighborPredicate<>(epsilon, distfun);
    }
  }
}
