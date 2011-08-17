package de.lmu.ifi.dbs.elki.result;
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

import java.util.Collection;

/**
 * Histogram result.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class (e.g. {@link de.lmu.ifi.dbs.elki.data.DoubleVector})
 */
public class HistogramResult<O> extends CollectionResult<O> {
  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection
   */
  public HistogramResult(String name, String shortname, Collection<O> col) {
    super(name, shortname, col);
  }

  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection
   * @param header Header information
   */
  public HistogramResult(String name, String shortname, Collection<O> col, Collection<String> header) {
    super(name, shortname, col, header);
  }
}