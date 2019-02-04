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

import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write an object into the comments section, using the objects toString()
 * method.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class TextWriterObjectComment extends TextWriterWriterInterface<Object> {
  /**
   * Put an object into the comment section
   */
  @Override
  public void write(TextWriterStream out, String label, Object object) {
    StringBuilder buf = new StringBuilder(100);
    if(label != null) {
      buf.append(label).append('=');
    }
    if(object != null) {
      buf.append(object.toString());
    }
    out.commentPrintLn(buf);
  }
}
