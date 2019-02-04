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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;

/**
 * Trivial combination interface.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface IntegerArrayDBIDs extends IntegerDBIDs, ArrayDBIDs {
  @Override
  IntegerDBIDArrayIter iter();
  
  // TODO: add a default implementation that works for most
  @Override
  IntegerArrayDBIDs slice(int begin, int end);
}
