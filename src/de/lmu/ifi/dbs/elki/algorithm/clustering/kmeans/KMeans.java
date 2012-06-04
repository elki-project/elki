package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

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

/**
 * Some constants and options shared among kmeans family algorithms.
 * 
 * @author Erich Schubert
 */
public interface KMeans {
  /**
   * Parameter to specify the initialization method
   */
  public static final OptionID INIT_ID = OptionID.getOrCreateOptionID("kmeans.initialization", "Method to choose the initial means.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("kmeans.k", "The number of clusters to find.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater or equal to 0, where 0 means no limit.
   */
  public static final OptionID MAXITER_ID = OptionID.getOrCreateOptionID("kmeans.maxiter", "The maximum number of iterations to do. 0 means no limit.");

  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("kmeans.seed", "The random number generator seed.");
}