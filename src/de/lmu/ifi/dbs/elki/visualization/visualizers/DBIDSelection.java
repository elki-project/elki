package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

/**
 * Class representing selected Database-IDs and/or a selection range.
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 */
public class DBIDSelection {
  /**
   * Selected IDs
   */
  private DBIDs selectedIds = DBIDUtil.EMPTYDBIDS;
  
  /**
   * Constructor with new object IDs.
   * 
   * @param selectedIds selection IDs
   */
  public DBIDSelection(DBIDs selectedIds) {
    super();
    this.selectedIds = selectedIds;
  }

  /**
   * Getter for the selected IDs
   * 
   * @return DBIDs
   */
  public DBIDs getSelectedIds() {
    return selectedIds;
  }
}