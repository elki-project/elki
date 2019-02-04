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
package de.lmu.ifi.dbs.elki.result.textwriter;

/**
 * Interface for objects providing a text serialization suitable for
 * human reading and storage in CSV files.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
//TODO: split TextWriteable interface into data writing and metadata writing?
public interface TextWriteable {
  /**
   * Write self to the given {@link TextWriterStream}
   * @param out Output writer
   * @param label Label
   */
  void writeToText(TextWriterStream out, String label);
}
