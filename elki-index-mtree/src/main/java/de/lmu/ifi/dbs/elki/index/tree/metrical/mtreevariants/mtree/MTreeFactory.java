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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeSettings;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Factory for a M-Tree
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - MTreeIndex
 * 
 * @param <O> Object type
 */
@Alias({ "mtree", "m" })
public class MTreeFactory<O> extends AbstractMTreeFactory<O, MTreeNode<O>, MTreeEntry, MTreeSettings<O, MTreeNode<O>, MTreeEntry>> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public MTreeFactory(PageFileFactory<?> pageFileFactory, MTreeSettings<O, MTreeNode<O>, MTreeEntry> settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public MTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<MTreeNode<O>> pagefile = makePageFile(getNodeClass());
    return new MTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<MTreeNode<O>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractMTreeFactory.Parameterizer<O, MTreeNode<O>, MTreeEntry, MTreeSettings<O, MTreeNode<O>, MTreeEntry>> {
    @Override
    protected MTreeFactory<O> makeInstance() {
      return new MTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MTreeSettings<O, MTreeNode<O>, MTreeEntry> makeSettings() {
      return new MTreeSettings<>();
    }
  }
}
