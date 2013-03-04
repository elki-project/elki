package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MMRadSplit;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MTreeSplit;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract factory for various MTrees
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses AbstractMTree oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Node type
 * @param <E> Entry type
 * @param <I> Index type
 */
public abstract class AbstractMTreeFactory<O, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>, I extends AbstractMTree<O, D, N, E> & Index> extends TreeIndexFactory<O, I> {
  /**
   * Holds the instance of the distance function we are building the index for.
   */
  protected DistanceFunction<O, D> distanceFunction;

  /**
   * Split strategy
   */
  protected MTreeSplit<O, D, N, E> splitStrategy;

  /**
   * Constructor.
   * 
   * @param fileName File name
   * @param pageSize Page size
   * @param cacheSize Cache size
   * @param distanceFunction Distance function
   * @param splitStrategy Split strategy
   */
  public AbstractMTreeFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, MTreeSplit<O, D, N, E> splitStrategy) {
    super(fileName, pageSize, cacheSize);
    this.distanceFunction = distanceFunction;
    this.splitStrategy = splitStrategy;
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
  public abstract static class Parameterizer<O, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends TreeIndexFactory.Parameterizer<O> {
    /**
     * Parameter to specify the distance function to determine the distance
     * between database objects, must extend
     * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
     * <p>
     * Key: {@code -mtree.distancefunction}
     * </p>
     * <p>
     * Default value:
     * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction}
     * </p>
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("mtree.distancefunction", "Distance function to determine the distance between database objects.");

    /**
     * Parameter to specify the splitting strategy to construct the tree.
     * <p>
     * Key: {@code -mtree.split}
     * </p>
     */
    public static final OptionID SPLIT_STRATEGY_ID = new OptionID("mtree.split", "Split strategy to use for constructing the M-tree.");

    /**
     * Distance function to use for the index.
     */
    protected DistanceFunction<O, D> distanceFunction = null;

    /**
     * Splitting strategy.
     */
    protected MTreeSplit<O, D, N, E> splitStrategy = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<O, D>> distanceFunctionP = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if (config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }
      ObjectParameter<MTreeSplit<O, D, N, E>> splitStrategyP = new ObjectParameter<>(SPLIT_STRATEGY_ID, MTreeSplit.class, MMRadSplit.class);
      if (config.grab(splitStrategyP)) {
        splitStrategy = splitStrategyP.instantiateClass(config);
      }
    }

    @Override
    protected abstract AbstractMTreeFactory<O, D, N, E, ?> makeInstance();
  }
}
