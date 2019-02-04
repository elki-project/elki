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
package de.lmu.ifi.dbs.elki.data.model;

/**
 * Generic cluster model. Does not supply additional meta information except
 * that it is a cluster. Since there is no meta information, you should use the
 * static {@link #CLUSTER} object.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 */
public final class ClusterModel implements Model {
  /**
   * Static cluster model that can be shared for all clusters (since the object
   * doesn't include meta information.
   */
  public static final ClusterModel CLUSTER = new ClusterModel();

  @Override
  public String toString() {
    return "ClusterModel";
  }
}