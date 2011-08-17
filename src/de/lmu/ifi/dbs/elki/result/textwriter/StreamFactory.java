package de.lmu.ifi.dbs.elki.result.textwriter;
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

import java.io.IOException;
import java.io.PrintStream;

/**
 * Interface for output handling (single file, multiple files, ...)
 * 
 * @author Erich Schubert
 */
public interface StreamFactory {
  /**
   * Retrieve a print stream for output using the given label.
   * Note that multiple labels MAY result in the same PrintStream, so you
   * should be printing to only one stream at a time to avoid mixing outputs.
   * 
   * @param label Output label.
   * @return stream object for the given label
   * @throws IOException on IO error
   */
  public PrintStream openStream(String label) throws IOException;
  
  /**
   * Close (and forget) all streams the factory has opened.
   */
  public void closeAllStreams();
}