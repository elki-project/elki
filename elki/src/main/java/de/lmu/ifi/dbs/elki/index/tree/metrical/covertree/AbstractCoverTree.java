package de.lmu.ifi.dbs.elki.index.tree.metrical.covertree;
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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

public abstract class AbstractCoverTree<O> extends AbstractIndex<O> {
  /**
   * Constant expansion rate. 2 would be the intuitive value, but the original
   * version used 1.3, so we copy this. This means that in every level, the
   * cover radius shrinks by 1.3.
   */
  final double BASE = 1.3;

  /**
   * Logarithm base.
   */
  final double INV_LOG_BASE = 1. / Math.log(BASE);

  /**
   * Remaining points are likely identical. For 1.3 this yields: -2700
   */
  protected final int SCALE_BOTTOM = (int) Math.ceil(Math.log(Double.MIN_NORMAL) * INV_LOG_BASE);

  /**
   * Holds the instance of the trees distance function.
   */
  protected DistanceFunction<? super O> distanceFunction;

  /**
   * Distance query, on the data relation.
   */
  private DistanceQuery<O> distanceQuery;

  /**
   * Distance computations performed.
   */
  protected long distComputations = 0L;

  /**
   * Stop refining the tree at this size, but build a leaf.
   */
  protected int truncate = 10;

  /**
   * Constructor.
   *
   * @param relation Data relation
   * @param distanceFunction Distance function
   * @param truncate Truncate branches with less than this number of instances.
   */
  public AbstractCoverTree(Relation<O> relation, DistanceFunction<? super O> distanceFunction, int truncate) {
    super(relation);
    this.distanceFunction = distanceFunction;
    this.distanceQuery = distanceFunction.instantiate(relation);
    this.truncate = truncate;
  }

  /**
   * Convert a scaling factor to a distance.
   * 
   * @param s Scaling factor
   * @return Distance
   */
  protected final double scaleToDist(int s) {
    return Math.pow(BASE, s);
  }

  /**
   * Convert a distance to an upper scaling bound-
   * 
   * @param d Distance
   * @return Scaling bound
   */
  protected final int distToScale(double d) {
    return (int) Math.ceil(Math.log(d) * INV_LOG_BASE);
  }

  /**
   * Find maximum in a list via scanning.
   * 
   * @param elems Elements
   * @return Maximum distance
   */
  protected double maxDistance(DoubleDBIDList elems) {
    double max = 0;
    for(DoubleDBIDListIter it = elems.iter(); it.valid(); it.advance()) {
      final double v = it.doubleValue();
      max = max > v ? max : v;
    }
    return max;
  }

  /**
   * Compute a distance (and count).
   * 
   * @param a Object reference
   * @param b Object reference
   * @return Distance
   */
  protected double distance(DBIDRef a, DBIDRef b) {
    ++distComputations;
    return distanceQuery.distance(a, b);
  }

  /**
   * Compute a distance (and count).
   * 
   * @param a Object reference
   * @param b Object reference
   * @return Distance
   */
  protected double distance(O a, DBIDRef b) {
    ++distComputations;
    return distanceQuery.distance(a, b);
  }

  /**
   * Retain all elements within the current cover.
   * 
   * @param candidates Candidates
   * @param fmax Maximum distance
   * @param collect Far neighbors
   */
  protected void excludeNotCovered(ModifiableDoubleDBIDList candidates, double fmax, ModifiableDoubleDBIDList collect) {
    for(DoubleDBIDListIter it = candidates.iter(); it.valid();) {
      if(it.doubleValue() > fmax) {
        collect.add(it.doubleValue(), it);
        candidates.removeSwap(it.getOffset());
      }
      else {
        it.advance(); // Keep in candidates
      }
    }
  }

  /**
   * Collect all elements with respect to a new routing object.
   * 
   * @param cur Routing object
   * @param candidates Candidate list
   * @param fmax Maximum distance
   * @param collect Output list
   */
  protected void collectByCover(DBIDRef cur, ModifiableDoubleDBIDList candidates, double fmax, ModifiableDoubleDBIDList collect) {
    assert(collect.size() == 0) : "Not empty";
    DoubleDBIDListIter it = candidates.iter().advance(); // Except first = cur!
    while(it.valid()) {
      assert(!DBIDUtil.equal(cur, it));
      final double dist = distance(cur, it);
      if(dist <= fmax) { // Collect
        collect.add(dist, it);
        candidates.removeSwap(it.getOffset());
      }
      else {
        it.advance(); // Keep in candidates, outside cover radius.
      }
    }
  }

  @Override
  public void logStatistics() {
    getLogger().statistics(new LongStatistic(this.getClass().getName() + ".distance-computations", distComputations));
  }

  /**
   * Get the class logger.
   * 
   * @return Logger
   */
  protected abstract Logging getLogger();

  @Override
  public String getLongName() {
    return "Cover Tree";
  }

  @Override
  public String getShortName() {
    return "cover-tree";
  }

  /**
   * Index factory.
   * 
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public abstract static class Factory<O, I extends AbstractCoverTree<O>> implements IndexFactory<O, I> {
    /**
     * Holds the instance of the trees distance function.
     */
    protected DistanceFunction<? super O> distanceFunction;

    /**
     * Truncate tree at this height.
     */
    protected int truncate;

    /**
     * Constructor.
     *
     * @param distanceFunction Distance function
     * @param truncate Truncate branches with less than this number of
     *        instances.
     */
    public Factory(DistanceFunction<? super O> distanceFunction, int truncate) {
      super();
      this.distanceFunction = distanceFunction;
      this.truncate = truncate;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distanceFunction.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     *
     * @apiviz.exclude
     */
    public abstract static class Parameterizer<O> extends AbstractParameterizer {
      /**
       * Parameter to specify the distance function to determine the distance
       * between database objects, must extend
       * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
       * <p>
       * Key: {@code -covertree.distancefunction}
       * </p>
       */
      public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("covertree.distancefunction", "Distance function to determine the distance between objects.");

      /**
       * Truncate branches when they have less than this number of instances.
       * <p>
       * Key: {@code -covertree.truncate}
       * </p>
       */
      public static final OptionID TRUNCATE_ID = new OptionID("covertree.truncate", "Truncate tree when branches have less than this number of instances.");

      /**
       * Holds the instance of the trees distance function.
       */
      protected DistanceFunction<? super O> distanceFunction;

      /**
       * Truncate the tree.
       */
      protected int truncate = 10;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<DistanceFunction<O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DistanceFunction.class);
        if(config.grab(distanceFunctionP)) {
          distanceFunction = distanceFunctionP.instantiateClass(config);
        }
        IntParameter truncateP = new IntParameter(TRUNCATE_ID, 10)//
        .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(truncateP)) {
          truncate = truncateP.intValue();
        }
      }
    }
  }
}
