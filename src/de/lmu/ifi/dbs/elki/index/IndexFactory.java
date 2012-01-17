package de.lmu.ifi.dbs.elki.index;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Factory interface for indexes.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory,interface
 * @apiviz.has Index oneway - - «create»
 *
 * @param <V> Input object type
 * @param <I> Index type
 */
public interface IndexFactory<V, I extends Index> extends Parameterizable {
  /**
   * Sets the database in the distance function of this index (if existing).
   * 
   * @param relation the relation to index
   */
  public I instantiate(Relation<V> relation);

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  public TypeInformation getInputTypeRestriction();
}