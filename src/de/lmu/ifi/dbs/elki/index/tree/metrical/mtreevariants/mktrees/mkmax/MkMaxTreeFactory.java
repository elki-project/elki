package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

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
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTreeUnifiedFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Factory for MkMaxTrees
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses MkMaxTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkMaxTreeFactory<O, D extends NumberDistance<D, ?>> extends AbstractMkTreeUnifiedFactory<O, D, MkMaxTreeNode<O, D>, MkMaxEntry, MkMaxTreeIndex<O, D>, MkTreeSettings<O, D, MkMaxTreeNode<O, D>, MkMaxEntry>> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public MkMaxTreeFactory(PageFileFactory<?> pageFileFactory, MkTreeSettings<O, D, MkMaxTreeNode<O, D>, MkMaxEntry> settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public MkMaxTreeIndex<O, D> instantiate(Relation<O> relation) {
    PageFile<MkMaxTreeNode<O, D>> pagefile = makePageFile(getNodeClass());
    return new MkMaxTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<MkMaxTreeNode<O, D>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkMaxTreeNode.class);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractMkTreeUnifiedFactory.Parameterizer<O, D, MkMaxTreeNode<O, D>, MkMaxEntry, MkTreeSettings<O, D, MkMaxTreeNode<O, D>, MkMaxEntry>> {
    @Override
    protected MkMaxTreeFactory<O, D> makeInstance() {
      return new MkMaxTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MkTreeSettings<O, D, MkMaxTreeNode<O, D>, MkMaxEntry> makeSettings() {
      return new MkTreeSettings<>();
    }
  }
}
