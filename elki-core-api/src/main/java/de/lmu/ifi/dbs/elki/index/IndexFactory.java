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
package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Factory interface for indexes.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @stereotype factory,interface
 * @navhas - create - Index
 *
 * @param <V> Input object type
 */
public interface IndexFactory<V> {
  /**
   * Sets the database in the distance function of this index (if existing).
   * 
   * @param relation the relation to index
   */
  Index instantiate(Relation<V> relation);

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  TypeInformation getInputTypeRestriction();
}