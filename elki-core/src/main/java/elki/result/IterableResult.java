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
package elki.result;

import java.util.Iterator;

/**
 * Interface of an "iterable" result (e.g. a list, table) that can be printed one-by-one.
 * (At least when the class O is TextWriteable)
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 *
 * @param <O> object class.
 */
public interface IterableResult<O> extends Iterable<O> {
  /**
   * Retrieve an iterator for the result.
   * 
   * @return iterator
   */
  @Override
  Iterator<O> iterator();
}