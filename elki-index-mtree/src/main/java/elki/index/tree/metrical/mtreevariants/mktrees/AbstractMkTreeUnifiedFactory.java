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
package elki.index.tree.metrical.mtreevariants.mktrees;

import elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import elki.index.tree.metrical.mtreevariants.MTreeEntry;
import elki.persistent.PageFileFactory;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract factory for various Mk-Trees
 *
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory
 * @navassoc - create - AbstractMkTreeUnified
 * 
 * @param <O> Object type
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class AbstractMkTreeUnifiedFactory<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry, S extends MkTreeSettings<O, N, E>> extends AbstractMTreeFactory<O, N, E, S> {
  /**
   * Constructor.
   * 
   * @param pageFileFactory Data storage
   * @param settings Tree settings
   */
  public AbstractMkTreeUnifiedFactory(PageFileFactory<?> pageFileFactory, S settings) {
    super(pageFileFactory, settings);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Par<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry, S extends MkTreeSettings<O, N, E>> extends AbstractMTreeFactory.Par<O, N, E, S> {
    /**
     * Parameter specifying the maximal number k of reverse k nearest neighbors
     * to be supported, must be an integer greater than 0.
     */
    public static final OptionID K_MAX_ID = new OptionID("mktree.kmax", "Specifies the maximal number k of reverse k nearest neighbors to be supported.");

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_MAX_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> settings.kmax = x);
    }

    @Override
    public abstract AbstractMkTreeUnifiedFactory<O, N, E, S> make();
  }
}
