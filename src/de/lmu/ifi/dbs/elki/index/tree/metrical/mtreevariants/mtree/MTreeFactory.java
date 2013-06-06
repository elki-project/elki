package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
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
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Alias({ "mtree", "m" })
public class MTreeFactory<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory<O, D, MTreeNode<O, D>, MTreeEntry, MTreeIndex<O, D>, MTreeSettings<O, D, MTreeNode<O, D>, MTreeEntry>> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public MTreeFactory(PageFileFactory<?> pageFileFactory, MTreeSettings<O, D, MTreeNode<O, D>, MTreeEntry> settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public MTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<MTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractMTreeFactory.Parameterizer<O, D, MTreeNode<O, D>, MTreeEntry, MTreeSettings<O, D, MTreeNode<O, D>, MTreeEntry>> {
    @Override
    protected MTreeFactory<O, D> makeInstance() {
      return new MTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MTreeSettings<O, D, MTreeNode<O, D>, MTreeEntry> makeSettings() {
      return new MTreeSettings<>();
    }
  }
}
