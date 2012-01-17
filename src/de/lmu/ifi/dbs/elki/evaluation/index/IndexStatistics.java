package de.lmu.ifi.dbs.elki.evaluation.index;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.index.tree.IndexTree;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Simple index analytics, which includes the toString() dump of index
 * information.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has IndexMetaResult oneway - - «create»
 */
public class IndexStatistics implements Evaluator {
  /**
   * Constructor.
   */
  public IndexStatistics() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    Database db = ResultUtil.findDatabase(baseResult);
    Collection<String> header = null;
    final ArrayList<IndexTree<?, ?>> indexes = ResultUtil.filterResults(result, IndexTree.class);
    if (indexes == null || indexes.size() <= 0) {
      return;
    }
    for(IndexTree<?, ?> index : indexes) {
      header = new java.util.Vector<String>();
      header.add(index.toString());
    }
    Collection<Pair<String, String>> col = new java.util.Vector<Pair<String, String>>();
    IndexMetaResult analysis = new IndexMetaResult(col, header);
    db.getHierarchy().add(db, analysis);
  }

  /**
   * Result class.
   * 
   * @author Erich Schubert
   */
  public class IndexMetaResult extends CollectionResult<Pair<String, String>> {
    /**
     * Constructor.
     * 
     * @param col key value pairs
     * @param header header
     */
    public IndexMetaResult(Collection<Pair<String, String>> col, Collection<String> header) {
      super("Index Statistics", "index-meta", col, header);
    }
  }
}