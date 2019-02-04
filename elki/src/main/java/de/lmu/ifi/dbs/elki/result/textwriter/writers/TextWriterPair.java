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
package de.lmu.ifi.dbs.elki.result.textwriter.writers;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Write a pair
 *
 * @author Erich Schubert
 * @since 0.2
 */
public class TextWriterPair extends TextWriterWriterInterface<Pair<?,?>> {
  /**
   * Serialize a pair, component-wise
   */
  @Override
  @SuppressWarnings("unchecked")
  public void write(TextWriterStream out, String label, Pair<?,?> object) throws IOException {
    if (object != null) {
      Object first = object.getFirst();
      if (first != null) {
        TextWriterWriterInterface<Object> tw = (TextWriterWriterInterface<Object>) out.getWriterFor(first);
        if (tw == null) {
          throw new IOException("No handler for database object itself: " + first.getClass().getSimpleName());
        }
        tw.write(out, label, first);
      }
      Object second = object.getSecond();
      if (second != null) {
        TextWriterWriterInterface<Object> tw = (TextWriterWriterInterface<Object>) out.getWriterFor(second);
        if (tw == null) {
          throw new IOException("No handler for database object itself: " + second.getClass().getSimpleName());
        }
        tw.write(out, label, second);
      }
    }
  }
}
