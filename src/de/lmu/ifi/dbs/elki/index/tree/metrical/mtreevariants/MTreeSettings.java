package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

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
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.insert.MTreeInsert;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.strategies.split.MTreeSplit;

/**
 * Class to store the MTree settings.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Node type
 * @param <E> Entry type
 */
public class MTreeSettings<O, D extends NumberDistance<D, ?>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry> {
  /**
   * Holds the instance of the trees distance function.
   */
  protected DistanceFunction<? super O, D> distanceFunction;

  /**
   * Splitting strategy.
   */
  protected MTreeSplit<O, D, N, E> splitStrategy;

  /**
   * Insertion strategy.
   */
  protected MTreeInsert<O, D, N, E> insertStrategy;
}
