package de.lmu.ifi.dbs.elki.result.textwriter.writers;
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

import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterWriterInterface;

/**
 * Write an object into the inline section, using the objects toString method.
 * 
 * @author Erich Schubert
 */
public class TextWriterVector extends TextWriterWriterInterface<Vector> {
  /**
   * Serialize an object into the inline section.
   */
  @Override
  public void write(TextWriterStream out, String label, Vector v) {
    String res = "";
    if(label != null) {
      res = res + label + "=";
    }
    if(v != null) {
      res = res + v.toStringNoWhitespace();
    }
    out.inlinePrintNoQuotes(res);
  }
}
