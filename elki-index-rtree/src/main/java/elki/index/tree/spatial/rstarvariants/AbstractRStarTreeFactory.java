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
package elki.index.tree.spatial.rstarvariants;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.index.PagedIndexFactory;
import elki.index.tree.spatial.SpatialEntry;
import elki.index.tree.spatial.rstarvariants.strategies.bulk.BulkSplit;
import elki.index.tree.spatial.rstarvariants.strategies.insert.CombinedInsertionStrategy;
import elki.index.tree.spatial.rstarvariants.strategies.insert.InsertionStrategy;
import elki.index.tree.spatial.rstarvariants.strategies.overflow.LimitedReinsertOverflowTreatment;
import elki.index.tree.spatial.rstarvariants.strategies.overflow.OverflowTreatment;
import elki.index.tree.spatial.rstarvariants.strategies.split.SplitStrategy;
import elki.index.tree.spatial.rstarvariants.strategies.split.TopologicalSplitter;
import elki.persistent.PageFileFactory;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract factory for R*-Tree based trees.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - AbstractRStarTree
 * 
 * @param <O> Object type
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class AbstractRStarTreeFactory<O extends NumberVector, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry, S extends RTreeSettings> extends PagedIndexFactory<O> {
  /**
   * Tree settings
   */
  protected S settings;

  /**
   * Constructor.
   * 
   * @param pageFileFactory Page file factory
   * @param settings Tree settings
   */
  public AbstractRStarTreeFactory(PageFileFactory<?> pageFileFactory, S settings) {
    super(pageFileFactory);
    this.settings = settings;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return NumberVector.FIELD;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> Object type
   * @param <S> Settings class
   */
  public abstract static class Par<O extends NumberVector, S extends RTreeSettings> extends PagedIndexFactory.Par<O> {
    /**
     * Fast-insertion parameter. Optional.
     */
    public static OptionID INSERTION_STRATEGY_ID = new OptionID("rtree.insertionstrategy", "The strategy to use for object insertion.");

    /**
     * Split strategy parameter. Optional.
     */
    public static OptionID SPLIT_STRATEGY_ID = new OptionID("rtree.splitstrategy", "The strategy to use for node splitting.");

    /**
     * Parameter for bulk strategy
     */
    public static final OptionID BULK_SPLIT_ID = new OptionID("spatial.bulkstrategy", "The class to perform the bulk split with.");

    /**
     * Parameter for the relative minimum fill.
     */
    public static final OptionID MINIMUM_FILL_ID = new OptionID("rtree.minimum-fill", "Minimum relative fill required for data pages.");

    /**
     * Overflow treatment.
     */
    public static OptionID OVERFLOW_STRATEGY_ID = new OptionID("rtree.overflowtreatment", "The strategy to use for handling overflows.");

    /**
     * Tree settings
     */
    protected S settings;

    /**
     * Create the settings object
     * 
     * @return Settings instance.
     */
    abstract protected S createSettings();

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      settings = createSettings();
      new ObjectParameter<InsertionStrategy>(INSERTION_STRATEGY_ID, InsertionStrategy.class, CombinedInsertionStrategy.class) //
          .grab(config, x -> settings.insertionStrategy = x);
      new ObjectParameter<SplitStrategy>(SPLIT_STRATEGY_ID, SplitStrategy.class, TopologicalSplitter.class) //
          .grab(config, x -> settings.nodeSplitter = x);
      new DoubleParameter(MINIMUM_FILL_ID, 0.4) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_HALF_DOUBLE) //
          .grab(config, x -> settings.relativeMinFill = x);
      new ObjectParameter<OverflowTreatment>(OVERFLOW_STRATEGY_ID, OverflowTreatment.class, LimitedReinsertOverflowTreatment.class) //
          .grab(config, x -> settings.setOverflowTreatment(x));
      configBulkLoad(config);
    }

    /**
     * Configure the bulk load parameters.
     * 
     * @param config Parameterization
     */
    protected void configBulkLoad(Parameterization config) {
      new ObjectParameter<BulkSplit>(BULK_SPLIT_ID, BulkSplit.class) //
          .setOptional(true) //
          .grab(config, x -> settings.bulkSplitter = x);
    }

    @Override
    public abstract AbstractRStarTreeFactory<O, ?, ?, ?> make();
  }
}
