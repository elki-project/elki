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
package elki.data.model;

import elki.database.ids.DBID;
import elki.result.textwriter.TextWriteable;

/**
 * Cluster model that stores a mean for the cluster.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class MedoidModel extends SimplePrototypeModel<DBID> implements TextWriteable {
  /**
   * Constructor with medoid
   * 
   * @param medoid Cluster medoid
   */
  public MedoidModel(DBID medoid) {
    super(medoid);
  }

  /**
   * @return medoid
   */
  public DBID getMedoid() {
    return prototype;
  }

  @Override
  public String getPrototypeType() {
    return "Medoid";
  }
}
