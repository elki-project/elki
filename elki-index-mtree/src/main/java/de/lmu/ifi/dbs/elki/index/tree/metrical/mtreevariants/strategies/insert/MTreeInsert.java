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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert;

import de.lmu.ifi.dbs.elki.index.tree.IndexTreePath;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;

/**
 * Default insertion strategy for the M-tree.
 * 
 * <b>Warning:</b> as of now, insertion strategies <b>will already modify the
 * tree</b>, i.e. adjust cover radiuses.
 * 
 * FIXME: move this to the actual insert.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface MTreeInsert<E extends MTreeEntry, N extends AbstractMTreeNode<?, N, E>> {
  /**
   * Choose the subpath to insert into.
   * 
   * @param tree Tree to process
   * @param object Object to insert
   * @return Path to insertion node
   */
  IndexTreePath<E> choosePath(AbstractMTree<?, N, E, ?> tree, E object);
}
