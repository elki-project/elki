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

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;

/**
 * Selection result wrapper.
 * 
 * Note: we did not make the DBIDSelection a result in itself. Instead, the
 * DBIDSelection object should be seen as static contents of this result.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - DBIDSelection
 */
public class SelectionResult {
  /**
   * The actual selection
   */
  DBIDSelection selection = null;

  /**
   * Constructor.
   */
  public SelectionResult() {
    super();
  }

  /**
   * @return the selection
   */
  public DBIDSelection getSelection() {
    return selection;
  }

  /**
   * @param selection the selection to set
   */
  public void setSelection(DBIDSelection selection) {
    this.selection = selection;
  }

  // @Override
  public String getLongName() {
    return "Selection";
  }

  // @Override
  public String getShortName() {
    return "selection";
  }

  /**
   * Ensure that there also is a selection container object.
   *
   * @param db Database
   * @return selection result
   */
  public static SelectionResult ensureSelectionResult(final Database db) {
    It<SelectionResult> it = Metadata.hierarchyOf(db).iterDescendantsSelf()//
        .filter(SelectionResult.class);
    if(it.valid()) {
      return it.get();
    }
    SelectionResult sel = new SelectionResult();
    Metadata.hierarchyOf(db).addChild(sel);
    return sel;
  }
}
