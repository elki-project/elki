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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.PagedIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert.MTreeInsert;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert.MinimumEnlargementInsert;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MLBDistSplit;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MTreeSplit;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract factory for various MTrees
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - AbstractMTree
 * 
 * @param <O> Object type
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class AbstractMTreeFactory<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry, S extends MTreeSettings<O, N, E>> extends PagedIndexFactory<O> {
  /**
   * Tree settings.
   */
  protected S settings;

  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public AbstractMTreeFactory(PageFileFactory<?> pageFileFactory, S settings) {
    super(pageFileFactory);
    this.settings = settings;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return settings.distanceFunction.getInputTypeRestriction();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry, S extends MTreeSettings<O, N, E>> extends PagedIndexFactory.Parameterizer<O> {
    /**
     * Parameter to specify the distance function to determine the distance
     * between database objects, must extend
     * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
     */
    public static final OptionID DISTANCE_FUNCTION_ID = new OptionID("mtree.distancefunction", "Distance function to determine the distance between database objects.");

    /**
     * Parameter to specify the splitting strategy to construct the tree.
     */
    public static final OptionID SPLIT_STRATEGY_ID = new OptionID("mtree.split", "Split strategy to use for constructing the M-tree.");

    /**
     * Parameter to specify the insertion strategy to construct the tree.
     */
    public static final OptionID INSERT_STRATEGY_ID = new OptionID("mtree.insert", "Insertion strategy to use for constructing the M-tree.");

    /**
     * Tree settings.
     */
    protected S settings;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      settings = makeSettings();
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = new ObjectParameter<>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        settings.distanceFunction = distanceFunctionP.instantiateClass(config);
      }
      ObjectParameter<MTreeSplit<E, N>> splitStrategyP = new ObjectParameter<>(SPLIT_STRATEGY_ID, MTreeSplit.class, MLBDistSplit.class);
      if(config.grab(splitStrategyP)) {
        settings.splitStrategy = splitStrategyP.instantiateClass(config);
      }
      ObjectParameter<MTreeInsert<E, N>> insertStrategyP = new ObjectParameter<>(INSERT_STRATEGY_ID, MTreeInsert.class, MinimumEnlargementInsert.class);
      if(config.grab(insertStrategyP)) {
        settings.insertStrategy = insertStrategyP.instantiateClass(config);
      }
    }

    abstract protected S makeSettings();

    @Override
    protected abstract AbstractMTreeFactory<O, N, E, ?> makeInstance();
  }
}
