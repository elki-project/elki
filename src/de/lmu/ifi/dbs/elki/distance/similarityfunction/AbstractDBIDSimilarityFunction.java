package de.lmu.ifi.dbs.elki.distance.similarityfunction;
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * 
 * @param <D> distance type
 */
public abstract class AbstractDBIDSimilarityFunction<D extends Distance<D>> extends AbstractPrimitiveSimilarityFunction<DBID, D> implements DBIDSimilarityFunction<D> {
  /**
   * The database we work on
   */
  protected Relation<? extends DBID> database;

  /**
   * Constructor.
   * 
   * @param database Database
   */
  public AbstractDBIDSimilarityFunction(Relation<? extends DBID> database) {
    super();
    this.database = database;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }
}