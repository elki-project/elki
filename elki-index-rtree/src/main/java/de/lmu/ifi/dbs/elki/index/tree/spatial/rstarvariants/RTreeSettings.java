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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants;

import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.InsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.LeastOverlapInsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow.LimitedReinsertOverflowTreatment;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.overflow.OverflowTreatment;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.SplitStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.split.TopologicalSplitter;

/**
 * Class to wrap common Rtree settings.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @composed - - - BulkSplit
 * @composed - - - SplitStrategy
 * @composed - - - InsertionStrategy
 * @composed - - - OverflowTreatment
 */
public class RTreeSettings {
  /**
   * The strategy for bulk load.
   */
  protected BulkSplit bulkSplitter = null;

  /**
   * The split strategy.
   */
  protected SplitStrategy nodeSplitter = TopologicalSplitter.STATIC;

  /**
   * The insertion strategy to use.
   */
  protected InsertionStrategy insertionStrategy = LeastOverlapInsertionStrategy.STATIC;

  /**
   * Overflow treatment.
   */
  private OverflowTreatment overflowTreatment = LimitedReinsertOverflowTreatment.RSTAR_OVERFLOW;

  /**
   * Relative minimum fill.
   */
  protected double relativeMinFill = 0.4;

  /**
   * Constructor with default values.
   */
  public RTreeSettings() {
    super();
  }

  /**
   * Constructor with default values and bulk loader.
   */
  public RTreeSettings(BulkSplit bulkSplitter) {
    this();
    setBulkStrategy(bulkSplitter);
  }

  /**
   * Set the bulk loading strategy.
   * 
   * @param bulkSplitter Bulk loading strategy
   */
  public void setBulkStrategy(BulkSplit bulkSplitter) {
    this.bulkSplitter = bulkSplitter;
  }

  /**
   * Set the node splitting strategy.
   * 
   * @param nodeSplitter the split strategy to set
   */
  public void setNodeSplitStrategy(SplitStrategy nodeSplitter) {
    this.nodeSplitter = nodeSplitter;
  }

  /**
   * Set insertion strategy.
   * 
   * @param insertionStrategy the insertion strategy to set
   */
  public void setInsertionStrategy(InsertionStrategy insertionStrategy) {
    this.insertionStrategy = insertionStrategy;
  }

  /**
   * Set the overflow treatment strategy.
   * 
   * @param overflowTreatment overflow treatment strategy
   */
  public void setOverflowTreatment(OverflowTreatment overflowTreatment) {
    this.overflowTreatment = overflowTreatment;
  }

  /**
   * Set the relative minimum fill. (Only supported before the tree was used!)
   * 
   * @param relative Relative minimum fill
   */
  public void setMinimumFill(double relative) {
    this.relativeMinFill = relative;
  }

  /**
   * @return the overflowTreatment
   */
  public OverflowTreatment getOverflowTreatment() {
    return overflowTreatment;
  }
}
