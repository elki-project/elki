/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.optics;

import elki.Algorithm;
import elki.database.Database;

/**
 * Interface for OPTICS type algorithms, that can be analyzed by OPTICS Xi etc.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @navassoc - produces - ClusterOrder
 */
public interface OPTICSTypeAlgorithm extends Algorithm {
  @Override
  default ClusterOrder autorun(Database database) {
    return (ClusterOrder) Algorithm.Utils.autorun(this, database);
  }

  /**
   * Get the minpts value used. Needed for OPTICS Xi.
   *
   * @return minpts value
   */
  int getMinPts();
}
