package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.CombinedInsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.InsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow.LimitedReinsertOverflowTreatment;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow.OverflowTreatment;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.SplitStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.TopologicalSplitter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract factory for R*-Tree based trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses AbstractRStarTree oneway - - «create»
 * 
 * @param <O> Object type
 * @param <N> Node type
 * @param <E> Entry type
 * @param <I> Index type
 */
public abstract class AbstractRStarTreeFactory<O extends NumberVector<O, ?>, N extends AbstractRStarTreeNode<N, E>, E extends SpatialEntry, I extends AbstractRStarTree<N, E> & Index> extends TreeIndexFactory<O, I> {
  /**
   * Fast-insertion parameter. Optional.
   */
  public static OptionID INSERTION_STRATEGY_ID = OptionID.getOrCreateOptionID("rtree.insertionstrategy", "The strategy to use for object insertion.");

  /**
   * Split strategy parameter. Optional.
   */
  public static OptionID SPLIT_STRATEGY_ID = OptionID.getOrCreateOptionID("rtree.splitstrategy", "The strategy to use for node splitting.");

  /**
   * Parameter for bulk strategy
   */
  public static final OptionID BULK_SPLIT_ID = OptionID.getOrCreateOptionID("spatial.bulkstrategy", "The class to perform the bulk split with.");

  /**
   * Parameter for the relative minimum fill.
   */
  public static final OptionID MINIMUM_FILL_ID = OptionID.getOrCreateOptionID("rtree.minimum-fill", "Minimum relative fill required for data pages.");

  /**
   * Overflow treatment.
   */
  public static OptionID OVERFLOW_STRATEGY_ID = OptionID.getOrCreateOptionID("rtree.overflowtreatment", "The strategy to use for handling overflows.");

  /**
   * Strategy to find the insertion node with.
   */
  protected InsertionStrategy insertionStrategy;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit bulkSplitter;

  /**
   * The strategy for splitting nodes
   */
  protected SplitStrategy nodeSplitter;

  /**
   * Overflow treatment strategy
   */
  protected OverflowTreatment overflowTreatment;

  /**
   * Relative minimum fill
   */
  protected double minimumFill;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter the strategy to use for bulk splitting
   * @param insertionStrategy the strategy to find the insertion child
   * @param nodeSplitter the strategy to use for splitting nodes
   * @param overflowTreatment the strategy to use for overflow treatment
   * @param minimumFill the relative minimum fill
   */
  public AbstractRStarTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy, SplitStrategy nodeSplitter, OverflowTreatment overflowTreatment, double minimumFill) {
    super(fileName, pageSize, cacheSize);
    this.insertionStrategy = insertionStrategy;
    this.bulkSplitter = bulkSplitter;
    this.nodeSplitter = nodeSplitter;
    this.overflowTreatment = overflowTreatment;
    this.minimumFill = minimumFill;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O extends NumberVector<O, ?>> extends TreeIndexFactory.Parameterizer<O> {
    /**
     * Insertion strategy
     */
    protected InsertionStrategy insertionStrategy = null;

    /**
     * The strategy for splitting nodes
     */
    protected SplitStrategy nodeSplitter = null;

    /**
     * Bulk loading strategy
     */
    protected BulkSplit bulkSplitter = null;

    /**
     * Overflow treatment strategy
     */
    protected OverflowTreatment overflowTreatment = null;

    /**
     * Relative minimum fill
     */
    protected double minimumFill;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<InsertionStrategy> insertionStrategyP = new ObjectParameter<InsertionStrategy>(INSERTION_STRATEGY_ID, InsertionStrategy.class, CombinedInsertionStrategy.class);
      if(config.grab(insertionStrategyP)) {
        insertionStrategy = insertionStrategyP.instantiateClass(config);
      }
      ObjectParameter<SplitStrategy> splitStrategyP = new ObjectParameter<SplitStrategy>(SPLIT_STRATEGY_ID, SplitStrategy.class, TopologicalSplitter.class);
      if(config.grab(splitStrategyP)) {
        nodeSplitter = splitStrategyP.instantiateClass(config);
      }
      DoubleParameter minimumFillP = new DoubleParameter(MINIMUM_FILL_ID, new IntervalConstraint(0.0, IntervalBoundary.OPEN, 0.5, IntervalBoundary.OPEN), 0.4);
      if (config.grab(minimumFillP)) {
        minimumFill = minimumFillP.getValue();
      }
      ObjectParameter<OverflowTreatment> overflowP = new ObjectParameter<OverflowTreatment>(OVERFLOW_STRATEGY_ID, OverflowTreatment.class, LimitedReinsertOverflowTreatment.class);
      if(config.grab(overflowP)) {
        overflowTreatment = overflowP.instantiateClass(config);
      }
      configBulkLoad(config);
    }

    /**
     * Configure the bulk load parameters.
     * 
     * @param config Parameterization
     */
    protected void configBulkLoad(Parameterization config) {
      ObjectParameter<BulkSplit> bulkSplitP = new ObjectParameter<BulkSplit>(BULK_SPLIT_ID, BulkSplit.class, true);
      if(config.grab(bulkSplitP)) {
        bulkSplitter = bulkSplitP.instantiateClass(config);
      }
    }

    @Override
    protected abstract AbstractRStarTreeFactory<O, ?, ?, ?> makeInstance();
  }
}