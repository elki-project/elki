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

import java.io.IOException;

/**
 * Base class for object writers.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @param <O> Object type (usually the class itself)
 */
public abstract class TextWriterWriterInterface<O> {
  /**
   * Write a given object to the output stream.
   *
   * @param out Output stream
   * @param label Label to prefix
   * @param object object to output
   * @throws IOException on IO errors
   */
  public abstract void write(TextWriterStream out, String label, O object) throws IOException;

  /**
   * Non-type-checking version.
   *
   * @param out Output stream
   * @param label Label to prefix
   * @param object object to output
   * @throws IOException on IO errors
   */
  @SuppressWarnings("unchecked")
  public final void writeObject(TextWriterStream out, String label, Object object) throws IOException {
    write(out, label, (O) object);
  }
}