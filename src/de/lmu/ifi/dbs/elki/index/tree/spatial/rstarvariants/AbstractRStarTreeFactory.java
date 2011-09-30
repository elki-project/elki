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
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.InsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.LeastOverlapInsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.SplitStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.TopologicalSplitter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
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
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter the strategy to use for bulk splitting
   * @param insertionStrategy the strategy to find the insertion child
   * @param nodeSplitter the strategy to use for splitting nodes
   */
  public AbstractRStarTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy, SplitStrategy nodeSplitter) {
    super(fileName, pageSize, cacheSize);
    this.insertionStrategy = insertionStrategy;
    this.bulkSplitter = bulkSplitter;
    this.nodeSplitter = nodeSplitter;
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

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ClassParameter<InsertionStrategy> insertionStrategyP = new ClassParameter<InsertionStrategy>(INSERTION_STRATEGY_ID, InsertionStrategy.class, LeastOverlapInsertionStrategy.class);
      if(config.grab(insertionStrategyP)) {
        insertionStrategy = insertionStrategyP.instantiateClass(config);
      }
      ClassParameter<SplitStrategy> splitStrategyP = new ClassParameter<SplitStrategy>(SPLIT_STRATEGY_ID, SplitStrategy.class, TopologicalSplitter.class);
      if(config.grab(splitStrategyP)) {
        nodeSplitter = splitStrategyP.instantiateClass(config);
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