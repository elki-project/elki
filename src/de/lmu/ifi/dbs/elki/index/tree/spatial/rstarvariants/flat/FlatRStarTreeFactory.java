package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

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
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.InsertionStrategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.SplitStrategy;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Factory for flat R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses FlatRStarTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 */
public class FlatRStarTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, FlatRStarTreeNode, SpatialEntry, FlatRStarTreeIndex<O>> {
  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter Bulk loading strategy
   * @param insertionStrategy the strategy to find the insertion child
   * @param nodeSplitter the strategy for splitting nodes.
   */
  public FlatRStarTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy, SplitStrategy nodeSplitter) {
    super(fileName, pageSize, cacheSize, bulkSplitter, insertionStrategy, nodeSplitter);
  }

  @Override
  public FlatRStarTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<FlatRStarTreeNode> pagefile = makePageFile(getNodeClass());
    return new FlatRStarTreeIndex<O>(relation, pagefile, bulkSplitter, insertionStrategy, nodeSplitter);
  }

  protected Class<FlatRStarTreeNode> getNodeClass() {
    return FlatRStarTreeNode.class;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory.Parameterizer<O> {
    @Override
    protected FlatRStarTreeFactory<O> makeInstance() {
      return new FlatRStarTreeFactory<O>(fileName, pageSize, cacheSize, bulkSplitter, insertionStrategy, nodeSplitter);
    }
  }
}