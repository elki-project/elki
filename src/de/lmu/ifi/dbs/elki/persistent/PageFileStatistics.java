package de.lmu.ifi.dbs.elki.persistent;

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
 * Statistics API for a Page File.
 * 
 * See {@link PageFileUtil} for related utility functions for analysing this
 * data!
 * 
 * @author Erich Schubert
 */
public interface PageFileStatistics {
  /**
   * Returns the read I/O-Accesses of this file.
   * 
   * @return Number of physical read I/O accesses
   */
  public long getReadOperations();

  /**
   * Returns the write I/O-Accesses of this file.
   * 
   * @return Number of physical write I/O accesses
   */
  public long getWriteOperations();

  /**
   * Resets the counters for page accesses of this file and flushes the cache.
   */
  public void resetPageAccess();

  /**
   * Get statistics for the inner page file, if present.
   * 
   * @return Inner page file statistics, or null.
   */
  public PageFileStatistics getInnerStatistics();
}
