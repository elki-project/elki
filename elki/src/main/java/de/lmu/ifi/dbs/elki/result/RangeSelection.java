package de.lmu.ifi.dbs.elki.result;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleDoublePair;

/**
 * Class representing selected Database-IDs and/or a selection range.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 */
public class RangeSelection extends DBIDSelection {
  /**
   * Selection range
   */
  private DoubleDoublePair[] ranges = null;

  /**
   * Constructor.
   * 
   * @param selectedIds selected IDs
   */
  public RangeSelection(DBIDs selectedIds) {
    super(selectedIds);
  }
  
  /**
   * Constructor.
   * 
   * @param selection
   * @param ranges
   */
  public RangeSelection(DBIDs selection, DoubleDoublePair[] ranges) {
    super(selection);
    this.ranges = ranges;
  }

  /**
   * Get the selection range.
   * 
   * @return Selected range. May be null!
   */
  public DoubleDoublePair[] getRanges() {
    return ranges;
  }

  /**
   * Get a single selection range.
   * 
   * @return Selected range. May be null!
   */
  public DoubleDoublePair getRange(int dim) {
    return ranges[dim];
  }
}