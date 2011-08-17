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
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface DBIDSimilarityFunction describes the requirements of any similarity
 * function defined over object IDs.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.uses DBID oneway - - defined on
 * 
 * @param <D> distance type
 */
public interface DBIDSimilarityFunction<D extends Distance<D>> extends SimilarityFunction<DBID, D> {
  /**
   * Computes the similarity between two given DatabaseObjects according to this
   * similarity function.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the similarity between two given DatabaseObjects according to this
   *         similarity function
   */
  D similarity(DBID id1, DBID id2);
}