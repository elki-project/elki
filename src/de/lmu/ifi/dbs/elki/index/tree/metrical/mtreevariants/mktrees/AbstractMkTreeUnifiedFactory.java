package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;

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

import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert.MTreeInsert;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MTreeSplit;
import de.lmu.ifi.dbs.elki.persistent.PageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract factory for various Mk-Trees
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses AbstractMkTree oneway - - «create»
 *
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Node type
 * @param <E> Entry type
 * @param <I> Index type
 */
public abstract class AbstractMkTreeUnifiedFactory<O, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>, I extends AbstractMkTree<O, D, N, E> & Index> extends AbstractMTreeFactory<O, D, N, E, I> {
  /**
   * Parameter specifying the maximal number k of reverse k nearest neighbors to
   * be supported, must be an integer greater than 0.
   * <p>
   * Key: {@code -mktree.kmax}
   * </p>
   */
  public static final OptionID K_MAX_ID = new OptionID("mktree.kmax", "Specifies the maximal number k of reverse k nearest neighbors to be supported.");

  /**
   * Holds the value of parameter {@link #K_MAX_ID}.
   */
  protected int k_max;

  /**
   * Constructor.
   *
   * @param pageFileFactory Data storage
   * @param distanceFunction Distance function
   * @param splitStrategy Split strategy
   * @param insertStrategy Insertion strategy
   * @param k_max Maximum k
   */
  public AbstractMkTreeUnifiedFactory(PageFileFactory<?> pageFileFactory, DistanceFunction<O, D> distanceFunction, MTreeSplit<O, D, N, E> splitStrategy, MTreeInsert<O, D, N, E> insertStrategy, int k_max) {
    super(pageFileFactory, distanceFunction, splitStrategy, insertStrategy);
    this.k_max = k_max;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer<O, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends AbstractMTreeFactory.Parameterizer<O, D, N, E> {
    protected int k_max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter k_maxP = new IntParameter(K_MAX_ID);
      k_maxP.addConstraint(new GreaterConstraint(0));

      if (config.grab(k_maxP)) {
        k_max = k_maxP.getValue();
      }
    }

    @Override
    protected abstract AbstractMkTreeUnifiedFactory<O, D, N, E, ?> makeInstance();
  }
}