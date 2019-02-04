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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Factory for MkMaxTrees
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - MkMaxTreeIndex
 * 
 * @param <O> Object type
 */
public class MkMaxTreeFactory<O> extends AbstractMkTreeUnifiedFactory<O, MkMaxTreeNode<O>, MkMaxEntry, MkTreeSettings<O, MkMaxTreeNode<O>, MkMaxEntry>> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public MkMaxTreeFactory(PageFileFactory<?> pageFileFactory, MkTreeSettings<O, MkMaxTreeNode<O>, MkMaxEntry> settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public MkMaxTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<MkMaxTreeNode<O>> pagefile = makePageFile(getNodeClass());
    return new MkMaxTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<MkMaxTreeNode<O>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkMaxTreeNode.class);
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
  public static class Parameterizer<O> extends AbstractMkTreeUnifiedFactory.Parameterizer<O, MkMaxTreeNode<O>, MkMaxEntry, MkTreeSettings<O, MkMaxTreeNode<O>, MkMaxEntry>> {
    @Override
    protected MkMaxTreeFactory<O> makeInstance() {
      return new MkMaxTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MkTreeSettings<O, MkMaxTreeNode<O>, MkMaxEntry> makeSettings() {
      return new MkTreeSettings<>();
    }
  }
}
