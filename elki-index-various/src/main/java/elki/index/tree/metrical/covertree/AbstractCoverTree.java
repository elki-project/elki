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
package elki.index.tree.metrical.covertree;

import elki.data.type.TypeInformation;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.index.AbstractIndex;
import elki.index.IndexFactory;
import elki.logging.Logging;
import elki.logging.LoggingUtil;
import elki.logging.statistics.LongStatistic;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

import net.jafama.FastMath;

/**
 * Abstract base class for cover tree variants.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
public abstract class AbstractCoverTree<O> extends AbstractIndex<O> {
  /**
   * Constant expansion rate. 2 would be the intuitive value, but the original
   * version used 1.3, so we copy this. This means that in every level, the
   * cover radius shrinks by 1.3.
   */
  final double expansion;

  /**
   * Logarithm base.
   */
  final double invLogExpansion;

  /**
   * Remaining points are likely identical. For 1.3 this yields: -2700
   */
  protected final int scaleBottom;

  /**
   * Holds the instance of the trees distance function.
   */
  protected Distance<? super O> distance;

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
   * @param distance Distance function
   * @param expansion Expansion rate
   * @param truncate Truncate branches with less than this number of instances.
   */
  public AbstractCoverTree(Relation<O> relation, Distance<? super O> distance, double expansion, int truncate) {
    super(relation);
    this.distance = distance;
    this.distanceQuery = distance.instantiate(relation);
    this.truncate = truncate;
    this.expansion = expansion;
    this.invLogExpansion = 1. / FastMath.log(expansion);
    this.scaleBottom = (int) Math.ceil(FastMath.log(Double.MIN_NORMAL) * invLogExpansion);
  }

  /**
   * Convert a scaling factor to a distance.
   * 
   * @param s Scaling factor
   * @return Distance
   */
  protected final double scaleToDist(int s) {
    return FastMath.pow(expansion, s);
  }

  /**
   * Convert a distance to an upper scaling bound-
   * 
   * @param d Distance
   * @return Scaling bound
   */
  protected final int distToScale(double d) {
    return (int) Math.ceil(FastMath.log(d) * invLogExpansion);
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
    assert (collect.size() == 0) : "Not empty";
    DoubleDBIDListIter it = candidates.iter().advance(); // Except first = cur!
    while(it.valid()) {
      assert (!DBIDUtil.equal(cur, it));
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
  public abstract static class Factory<O> implements IndexFactory<O> {
    /**
     * Holds the instance of the trees distance function.
     */
    protected Distance<? super O> distance;

    /**
     * Constant expansion rate. 2 would be the intuitive value, but the original
     * version used 1.3, so we copy this. This means that in every level, the
     * cover radius shrinks by 1.3.
     */
    protected double expansion;

    /**
     * Truncate tree at this height.
     */
    protected int truncate;

    /**
     * Constructor.
     *
     * @param distance Distance function
     * @param expansion Expansion rate
     * @param truncate Truncate branches with less than this number of
     *        instances.
     */
    public Factory(Distance<? super O> distance, double expansion, int truncate) {
      super();
      this.distance = distance;
      this.expansion = expansion;
      this.truncate = truncate;
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distance.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     */
    public abstract static class Par<O> implements Parameterizer {
      /**
       * Parameter to specify the distance function to determine the distance
       * between database objects, must extend
       * {@link elki.distance.Distance}.
       */
      public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("covertree.distancefunction", "Distance function to determine the distance between objects.");

      /**
       * Truncate branches when they have less than this number of instances.
       */
      public static final OptionID TRUNCATE_ID = new OptionID("covertree.truncate", "Truncate tree when branches have less than this number of instances.");

      /**
       * Expansion rate of the tree (going upward).
       */
      public static final OptionID EXPANSION_ID = new OptionID("covertree.expansionrate", "Expansion rate of the tree (Default: 1.3).");

      /**
       * Holds the instance of the trees distance function.
       */
      protected Distance<? super O> distance;

      /**
       * Truncate the tree.
       */
      protected int truncate = 10;

      /**
       * Expansion rate.
       */
      protected double expansion = 1.3;

      @Override
      public void configure(Parameterization config) {
        new ObjectParameter<Distance<O>>(DISTANCE_FUNCTION_ID, Distance.class) //
            .grab(config, x -> {
              distance = x;
              if(!distance.isMetric()) {
                LoggingUtil.warning("CoverTree requires a metric to be exact.");
              }
            });
        new IntParameter(TRUNCATE_ID, 10)//
            .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
            .grab(config, x -> truncate = x);
        new DoubleParameter(EXPANSION_ID, 1.3)//
            .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE) //
            .grab(config, x -> expansion = x);
      }
    }
  }
}
