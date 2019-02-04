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
package de.lmu.ifi.dbs.elki.algorithm.clustering.affinitypropagation;

import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Initialization methods for affinity propagation.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public interface AffinityPropagationInitialization<O> {
  /**
   * Quantile to use for the diagonal entries.
   */
  OptionID QUANTILE_ID = new OptionID("ap.quantile", "Quantile to use for diagonal entries.");

  /**
   * Compute the initial similarity matrix.
   * 
   * @param db Database
   * @param relation Data relation
   * @param ids indexed DBIDs
   * @return Similarity matrix
   */
  double[][] getSimilarityMatrix(Database db, Relation<O> relation, ArrayDBIDs ids);

  /**
   * Get the data type information for the similarity computations.
   * 
   * @return Data type
   */
  TypeInformation getInputTypeRestriction();
}
