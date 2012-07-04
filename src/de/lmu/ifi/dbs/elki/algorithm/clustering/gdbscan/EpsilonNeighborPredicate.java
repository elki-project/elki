package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * The default DBSCAN and OPTICS neighbor predicate, using an
 * epsilon-neighborhood.
 * 
 * <p>
 * Reference: <br>
 * M. Ester, H.-P. Kriegel, J. Sander, and X. Xu: A Density-Based Algorithm for
 * Discovering Clusters in Large Spatial Databases with Noise. <br>
 * In Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96),
 * Portland, OR, 1996.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has NeighborListInstance
 * @apiviz.has DBIDInstance
 * 
 * @param <D> Distance type
 */
@Reference(authors = "M. Ester, H.-P. Kriegel, J. Sander, and X. Xu", title = "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise", booktitle = "Proc. 2nd Int. Conf. on Knowledge Discovery and Data Mining (KDD '96), Portland, OR, 1996", url = "http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.71.1980")
public class EpsilonNeighborPredicate<O, D extends Distance<D>> implements NeighborPredicate {
  /**
   * Range to query with
   */
  D epsilon;

  /**
   * Distance function to use
   */
  DistanceFunction<O, D> distFunc;

  /**
   * Full constructor.
   * 
   * @param epsilon Epsilon value
   * @param distFunc Distance function to use
   */
  public EpsilonNeighborPredicate(D epsilon, DistanceFunction<O, D> distFunc) {
    super();
    this.epsilon = epsilon;
    this.distFunc = distFunc;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> NeighborPredicate.Instance<T> instantiate(Database database, SimpleTypeInformation<?> type) {
    DistanceQuery<O, D> dq = QueryUtil.getDistanceQuery(database, distFunc);
    RangeQuery<O, D> rq = database.getRangeQuery(dq);
    return (NeighborPredicate.Instance<T>) new Instance<D>(epsilon, rq, dq.getRelation().getDBIDs());
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
  public static class Instance<D extends Distance<D>> implements NeighborPredicate.Instance<DistanceDBIDResult<D>> {
    /**
     * Range to query with
     */
    D epsilon;

    /**
     * Range query to use on the database.
     */
    RangeQuery<?, D> rq;

    /**
     * DBIDs to process
     */
    DBIDs ids;

    /**
     * Constructor.
     * 
     * @param epsilon Epsilon
     * @param rq Range query to use
     * @param ids DBIDs to process
     */
    public Instance(D epsilon, RangeQuery<?, D> rq, DBIDs ids) {
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
    public DistanceDBIDResult<D> getNeighbors(DBIDRef reference) {
      return rq.getRangeForDBID(reference, epsilon);
    }

    @Override
    public void addDBIDs(ModifiableDBIDs ids, DistanceDBIDResult<D> neighbors) {
      ids.addDBIDs(neighbors);
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractParameterizer {
    /**
     * Range to query with
     */
    D epsilon;

    /**
     * Distance function to use
     */
    DistanceFunction<O, D> distfun = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get a distance function.
      ObjectParameter<DistanceFunction<O, D>> distanceP = new ObjectParameter<DistanceFunction<O, D>>(AbstractDistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      D distanceFactory = null;
      if(config.grab(distanceP)) {
        distfun = distanceP.instantiateClass(config);
        distanceFactory = distfun.getDistanceFactory();
      }
      // Get the epsilon parameter
      DistanceParameter<D> epsilonP = new DistanceParameter<D>(de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN.EPSILON_ID, distanceFactory);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }
    }

    @Override
    protected EpsilonNeighborPredicate<O, D> makeInstance() {
      return new EpsilonNeighborPredicate<O, D>(epsilon, distfun);
    }
  }
}
