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
package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Class representing selected Database-IDs and/or a selection range.
 *
 * @author Heidi Kolb
 * @author Erich Schubert
 * @since 0.4.0
 */
public class RangeSelection extends DBIDSelection {
  /**
   * Selection range
   */
  private ModifiableHyperBoundingBox range;

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
  public RangeSelection(DBIDs selection, ModifiableHyperBoundingBox ranges) {
    super(selection);
    this.range = ranges;
  }

  /**
   * Get the selection range.
   *
   * @return Selected range. May be null!
   */
  public ModifiableHyperBoundingBox getRanges() {
    return range;
  }
}