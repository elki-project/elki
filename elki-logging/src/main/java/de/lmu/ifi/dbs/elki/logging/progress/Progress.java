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
package de.lmu.ifi.dbs.elki.logging.progress;

/**
 * Generic Progress logging interface.
 * 
 * @author Erich Schubert
 * @since 0.1
 */
public interface Progress {
  /**
   * Serialize a description into a String buffer.
   * 
   * @param buf Buffer to serialize to
   * @return Buffer the data was serialized to.
   */
  StringBuilder appendToBuffer(StringBuilder buf);

  /**
   * Test whether a progress is complete (and thus doesn't need to be shown anymore)
   * 
   * @return Whether the progress was completed.
   */
  boolean isComplete();
  
  /**
   * Returns a String representation of the progress suitable as a message for
   * printing to the command line interface.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  String toString();
}