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
package elki.index.tree.metrical.mtreevariants.mktrees.mktab;

import elki.database.relation.Relation;
import elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import elki.persistent.PageFile;
import elki.persistent.PageFileFactory;
import elki.utilities.ClassGenericsUtil;

/**
 * Factory for MkTabTrees
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - MkTabTreeIndex
 * 
 * @param <O> Object type
 */
public class MkTabTreeFactory<O> extends AbstractMkTreeUnifiedFactory<O, MkTabTreeNode<O>, MkTabEntry, MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry>> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public MkTabTreeFactory(PageFileFactory<?> pageFileFactory, MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry> settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public MkTabTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<MkTabTreeNode<O>> pagefile = makePageFile(getNodeClass());
    return new MkTabTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<MkTabTreeNode<O>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkTabTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractMkTreeUnifiedFactory.Par<O, MkTabTreeNode<O>, MkTabEntry, MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry>> {
    @Override
    public MkTabTreeFactory<O> make() {
      return new MkTabTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MkTreeSettings<O, MkTabTreeNode<O>, MkTabEntry> makeSettings() {
      return new MkTreeSettings<>();
    }
  }
}
