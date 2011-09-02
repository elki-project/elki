package de.lmu.ifi.dbs.elki.persistent;

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


/**
 * Page file statistic utility functions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.uses PageFileStatistics oneway - - analyses
 */
public final class PageFileUtil {
  /**
   * Append the page file statistics to the output buffer.
   * 
   * @param buffer Buffer to append to
   */
  public static void appendPageFileStatistics(StringBuffer buffer, PageFileStatistics statistics) {
    if(statistics != null) {
      buffer.append("Page File Layer: ").append(statistics.getClass()).append("\n");
      buffer.append("Read Operations: ").append(statistics.getReadOperations()).append("\n");
      buffer.append("Write Operations: ").append(statistics.getWriteOperations()).append("\n");
      PageFileStatistics inner = statistics.getInnerStatistics();
      if(inner != null) {
        appendPageFileStatistics(buffer, inner);
      }
    }
  }

  /**
   * Get the number of (logical) read operations (without caching).
   * 
   * @param statistics Statistics.
   * @return logical read operations.
   */
  public static long getLogicalReadOperations(PageFileStatistics statistics) {
    return statistics.getReadOperations();
  }

  /**
   * Get the number of (logical) write operations (without caching).
   * 
   * @param statistics Statistics.
   * @return logical write operations.
   */
  public static long getLogicalWriteOperations(PageFileStatistics statistics) {
    return statistics.getWriteOperations();
  }

  /**
   * Get the number of (physical) read operations (with caching).
   * 
   * @param statistics Statistics.
   * @return physical read operations.
   */
  public static long getPhysicalReadOperations(PageFileStatistics statistics) {
    PageFileStatistics inner = statistics.getInnerStatistics();
    if(inner != null) {
      return getPhysicalReadOperations(inner);
    }
    return statistics.getReadOperations();
  }

  /**
   * Get the number of (physical) write operations (with caching).
   * 
   * @param statistics Statistics.
   * @return physical write operations.
   */
  public static long getPhysicalWriteOperations(PageFileStatistics statistics) {
    PageFileStatistics inner = statistics.getInnerStatistics();
    if(inner != null) {
      return getPhysicalWriteOperations(inner);
    }
    return statistics.getWriteOperations();
  }
}