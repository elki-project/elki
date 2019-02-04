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
package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert.MTreeInsert;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MTreeSplit;

/**
 * Class to store the MTree settings.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @param <O> Object type
 * @param <N> Node type
 * @param <E> Entry type
 */
public class MTreeSettings<O, N extends AbstractMTreeNode<O, N, E>, E extends MTreeEntry> {
  /**
   * Holds the instance of the trees distance function.
   */
  protected DistanceFunction<? super O> distanceFunction;

  /**
   * Splitting strategy.
   */
  protected MTreeSplit<E, N> splitStrategy;

  /**
   * Insertion strategy.
   */
  protected MTreeInsert<E, N> insertStrategy;
}
