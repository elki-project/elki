package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.AbstractDatabaseDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A distance that is at least the distance to the kth nearest neighbor.
 * 
 * This is essentially the "reachability distance" of LOF, but with arguments
 * reversed!
 * 
 * Reachability of B <em>from</em> A, i.e.
 * 
 * <pre>
 *   reachability-distance(A,B) = max( k-distance(A), distance(A,B) )
 * </pre>
 * 
 * Where <tt>k-distance(A)</tt> is the distance to the k nearest neighbor of A,
 * and <tt>distance</tt> is the actual distance of A and B.
 * 
 * This distance is NOT symmetric. You need to pay attention to the order of
 * arguments!
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class MinKDistance<O, D extends Distance<D>> extends AbstractDatabaseDistanceFunction<O, D> {
  /**
   * OptionID for the base distance used to compute reachability
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("reachdist.basedistance", "Base distance function to use.");

  /**
   * OptionID for the KNN query class to use (preprocessor, approximation, ...)
   */
  public static final OptionID KNNQUERY_ID = OptionID.getOrCreateOptionID("reachdist.knnquery", "kNN query to use");

  /**
   * OptionID for the "k" parameter.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("reachdist.k", "The number of nearest neighbors of an object to be considered for computing its reachability distance.");

  /**
   * The distance function to determine the exact distance.
   */
  protected DistanceFunction<? super O, D> parentDistance;

  /**
   * The value of k
   */
  private int k;

  /**
   * Include object itself in kNN neighborhood.
   * 
   * In the official LOF publication, the point itself is not considered to be
   * part of its k nearest neighbors.
   */
  static boolean objectIsInKNN = false;

  /**
   * Full constructor. See {@link Parameterizer} for factory.
   * 
   * @param parentDistance distance function to use
   * @param k K parameter
   */
  public MinKDistance(DistanceFunction<? super O, D> parentDistance, int k) {
    super();
    this.parentDistance = parentDistance;
    this.k = k;
  }

  @Override
  public <T extends O> DistanceQuery<T, D> instantiate(Relation<T> relation) {
    return new Instance<T>(relation, k, parentDistance);
  }

  /**
   * Instance for an actual database.
   * 
   * @author Erich Schubert
   */
  public class Instance<T extends O> extends AbstractDatabaseDistanceQuery<T, D> {
    /**
     * KNN query instance
     */
    private KNNQuery<T, D> knnQuery;
    
    /**
     * Value for k
     */
    private int k;

    /**
     * Constructor.
     * 
     * @param relation Database
     * @param k Value of k
     */
    public Instance(Relation<T> relation, int k, DistanceFunction<? super O, D> parentDistance) {
      super(relation);
      this.k = k;
      this.knnQuery = QueryUtil.getKNNQuery(relation, parentDistance, k, DatabaseQuery.HINT_HEAVY_USE);
    }

    @Override
    public D distance(DBID id1, DBID id2) {
      List<DistanceResultPair<D>> neighborhood = knnQuery.getKNNForDBID(id1, k);
      D truedist = knnQuery.getDistanceQuery().distance(id1, id2);
      return computeReachdist(neighborhood, truedist);
    }

    @Override
    public DistanceFunction<? super T, D> getDistanceFunction() {
      return MinKDistance.this;
    }
  }

  /**
   * Actually compute the distance, whichever way we obtained the neighborhood
   * above.
   * 
   * @param neighborhood Neighborhood
   * @param truedist True distance
   * @return Reachability distance
   */
  protected D computeReachdist(List<DistanceResultPair<D>> neighborhood, D truedist) {
    // TODO: need to check neighborhood size?
    // TODO: Do we need to check we actually got the object itself in the
    // neighborhood?
    D kdist = neighborhood.get(neighborhood.size() - 1).getDistance();
    return DistanceUtil.max(kdist, truedist);
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public boolean isSymmetric() {
    return false;
  }

  @Override
  public D getDistanceFactory() {
    return parentDistance.getDistanceFactory();
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return parentDistance.getInputTypeRestriction();
  }
  
  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    MinKDistance<?,?> other = (MinKDistance<?, ?>) obj;
    return this.parentDistance.equals(other.parentDistance) && this.k == other.k;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractParameterizer {
    /**
     * The distance function to determine the exact distance.
     */
    protected DistanceFunction<? super O, D> parentDistance = null;

    /**
     * The value of k
     */
    private int k = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(1));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
      
      final ObjectParameter<DistanceFunction<? super O, D>> parentDistanceP = new ObjectParameter<DistanceFunction<? super O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if (config.grab(parentDistanceP)) {
        parentDistance = parentDistanceP.instantiateClass(config);
      }
   }

    @Override
    protected MinKDistance<O, D> makeInstance() {
      return new MinKDistance<O, D>(parentDistance, k + (objectIsInKNN ? 0 : 1));
    }
  }
}