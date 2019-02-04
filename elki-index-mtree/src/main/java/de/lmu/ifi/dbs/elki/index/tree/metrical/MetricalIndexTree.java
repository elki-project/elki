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
package de.lmu.ifi.dbs.elki.index.tree.metrical;

import java.util.List;

import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.Entry;
import de.lmu.ifi.dbs.elki.index.tree.IndexTree;
import de.lmu.ifi.dbs.elki.index.tree.Node;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Abstract super class for all metrical index classes.
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 * @param <O> the type of objects stored in the index
 * @param <N> the type of nodes used in the metrical index
 * @param <E> the type of entries used in the metrical index
 */
public abstract class MetricalIndexTree<O, N extends Node<E>, E extends Entry> extends IndexTree<N, E> {
  /**
   * Constructor.
   * 
   * @param pagefile Page file
   */
  public MetricalIndexTree(PageFile<N> pagefile) {
    super(pagefile);
  }

  /**
   * Returns the distance function of this metrical index.
   * 
   * @return the distance function of this metrical index
   */
  public abstract DistanceFunction<? super O> getDistanceFunction();

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   * 
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();
}