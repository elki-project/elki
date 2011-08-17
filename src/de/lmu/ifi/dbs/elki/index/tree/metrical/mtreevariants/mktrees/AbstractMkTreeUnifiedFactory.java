package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
 * @param <I> Index type
 */
public abstract class AbstractMkTreeUnifiedFactory<O, D extends Distance<D>, I extends AbstractMkTree<O, D, ?, ?> & Index> extends AbstractMTreeFactory<O, D, I> {
  /**
   * Parameter specifying the maximal number k of reverse k nearest neighbors to
   * be supported, must be an integer greater than 0.
   * <p>
   * Key: {@code -mktree.kmax}
   * </p>
   */
  public static final OptionID K_MAX_ID = OptionID.getOrCreateOptionID("mktree.kmax", "Specifies the maximal number k of reverse k nearest neighbors to be supported.");

  /**
   * Holds the value of parameter {@link #K_MAX_ID}.
   */
  protected int k_max;

  /**
   * Constructor.
   *
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param distanceFunction
   * @param k_max
   */
  public AbstractMkTreeUnifiedFactory(String fileName, int pageSize, long cacheSize, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(fileName, pageSize, cacheSize, distanceFunction);
    this.k_max = k_max;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O, D extends Distance<D>> extends AbstractMTreeFactory.Parameterizer<O, D> {
    protected int k_max;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter k_maxP = new IntParameter(K_MAX_ID, new GreaterConstraint(0));

      if (config.grab(k_maxP)) {
        k_max = k_maxP.getValue();
      }
    }

    @Override
    protected abstract AbstractMkTreeUnifiedFactory<O, D, ?> makeInstance();
  }
}