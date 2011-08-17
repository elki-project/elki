package de.lmu.ifi.dbs.elki.data;
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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.utilities.FormatUtil;

/**
 * A list of string labels
 * 
 * @author Erich Schubert
 */
public class LabelList extends ArrayList<String> {
  /**
   * Serial number
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public LabelList() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity
   */
  public LabelList(int initialCapacity) {
    super(initialCapacity);
  }

  @Override
  public String toString() {
    return FormatUtil.format(this, " ");
  }
}
