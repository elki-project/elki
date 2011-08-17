package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util;
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

import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Generic interface for split strategies.
 * 
 * @author Erich Schubert
 * 
 * @param <B> Base type that can be processed by this strategy
 */
public interface SplitStrategy<B> {
  /**
   * Split a page
   * 
   * @param <E> Actual data type
   * @param entries Entries to split
   * @param minEntries Minimum number of entries in each part
   * @return Pair containing the two sets of objects
   */
  public <E extends B> Pair<List<E>, List<E>> split(List<E> entries, int minEntries);
}
