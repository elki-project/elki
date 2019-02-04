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
package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Interface DBIDSimilarityFunction describes the requirements of any similarity
 * function defined over object IDs.
 *
 * @author Elke Achtert
 * @since 0.4.0
 *
 * @opt nodefillcolor LemonChiffon
 * @assoc - "defined on" - DBID
 */
public interface DBIDSimilarityFunction extends PrimitiveSimilarityFunction<DBID> {
  /**
   * Computes the similarity between two given DatabaseObjects according to this
   * similarity function.
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the similarity between two given DatabaseObjects according to this
   *         similarity function
   */
  double similarity(DBID id1, DBID id2);
}
