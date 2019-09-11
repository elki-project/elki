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
package elki.index;

/**
 * Interface defining the minimum requirements for all index classes.
 * 
 * See also: {@link IndexFactory}, {@link DynamicIndex}
 * 
 * @author Elke Achtert
 * @since 0.1
 * 
 */
public interface Index {
  /**
   * Initialize the index. For static indexes, this is the moment the index is
   * bulk loaded.
   */
  void initialize();

  /**
   * Send statistics to the logger, if enabled.
   * 
   * Note: you must have set the logging level appropriately before initializing
   * the index! Otherwise, the index might not have collected the desired
   * statistics.
   */
  void logStatistics();
  
  /**
   * Get a long (human-readbale) name of the index.
   *
   * @return Name
   */
  // @Override // Used to be in Result
  String getLongName();

  /**
   * Get a short (suitable for filenames) name of the index.
   * 
   * FIXME: remove.
   *
   * @return Name
   */
  // @Override // Used to be in Result
  String getShortName();
}
