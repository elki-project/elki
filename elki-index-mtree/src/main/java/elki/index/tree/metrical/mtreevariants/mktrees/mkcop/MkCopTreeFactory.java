/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import elki.database.relation.Relation;
import elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import elki.index.tree.metrical.mtreevariants.mktrees.MkTreeSettings;
import elki.persistent.PageFile;
import elki.persistent.PageFileFactory;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

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
  public static class Par<O> extends AbstractMTreeFactory.Par<O, MkCoPTreeNode<O>, MkCoPEntry, MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry>> {
    /**
     * Parameter for k
     */
    public static final OptionID K_ID = new OptionID("mkcop.k", "positive integer specifying the maximum number k of reverse k nearest neighbors to be supported.");

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> settings.kmax = x);
    }

    @Override
    public MkCopTreeFactory<O> make() {
      return new MkCopTreeFactory<>(pageFileFactory, settings);
    }

    @Override
    protected MkTreeSettings<O, MkCoPTreeNode<O>, MkCoPEntry> makeSettings() {
      return new MkTreeSettings<>();
    }
  }
}
