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
package elki.index.tree.spatial.rstarvariants.xtree;

import elki.data.NumberVector;
import elki.database.relation.Relation;
import elki.persistent.PageFile;
import elki.persistent.PageFileFactory;

/**
 * Factory for an xtree.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class XTreeFactory<O extends NumberVector> extends AbstractXTreeFactory<O, XTreeNode> {
  public XTreeFactory(PageFileFactory<?> pageFileFactory, XTreeSettings settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public XTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<XTreeNode> pagefile = makePageFile(getNodeClass());
    XTreeIndex<O> index = new XTreeIndex<>(relation, pagefile, settings);
    return index;
  }

  protected Class<XTreeNode> getNodeClass() {
    return XTreeNode.class;
  }

  public static class Par<O extends NumberVector> extends AbstractXTreeFactory.Par<O> {
    @Override
    public XTreeFactory<O> make() {
      return new XTreeFactory<>(pageFileFactory, settings);
    }
  }
}
