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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Factory for a MkCoPTree-Tree
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - MkCoPTreeIndex
 * 
 * @param <O> Object type
 */
public class MkCopTreeFactory<O> extends AbstractMTreeFactory<O, MkCoPTreeNode<O>, MkCoPEntry, MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry>> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public MkCopTreeFactory(PageFileFactory<?> pageFileFactory, MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry> settings) {
    super(pageFileFactory, settings);
  }

  @Override
  public MkCoPTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<MkCoPTreeNode<O>> pagefile = makePageFile(getNodeClass());
    return new MkCoPTreeIndex<>(relation, pagefile, settings);
  }

  protected Class<MkCoPTreeNode<O>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(MkCoPTreeNode.class);
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
  public static class Parameterizer<O> extends AbstractMTreeFactory.Parameterizer<O, MkCoPTreeNode<O>, MkCoPEntry, MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry>> {
    /**
     * Parameter for k
     */
    public static final OptionID K_ID = new OptionID("mkcop.k", "positive integer specifying the maximum number k of reverse k nearest neighbors to be supported.");

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter k_maxP = new IntParameter(K_ID);
      k_maxP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(k_maxP)) {
        settings.kmax = k_maxP.intValue();
      }
    }

    @Override
    protected MkCopTreeFactory<O> makeInstance() {
      return new MkCopTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry> makeSettings() {
      return new MkTreeSettings<>();
    }
  }
}
