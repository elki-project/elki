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
package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.UpdatableDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.ModifiableRelation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;

/**
 * Visualizes selected Objects in a JTable, objects can be selected, changed and
 * deleted
 *
 * @author Heidi Kolb
 * @author Erich Schubert
 * @since 0.4.0
 */
// FIXME: INCOMPLETE TRANSITION TO MULTI-REPRESENTED DATA
public class SelectionTableWindow extends JFrame implements DataStoreListener, ResultListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Selected data objects";

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * The JTable
   */
  private JTable table;

  /**
   * Button to close the window
   */
  private JButton closeButton;

  /**
   * Button to delete the selected objects
   */
  private JButton deleteButton;

  /**
   * The table model
   */
  private DatabaseTableModel dotTableModel = new DatabaseTableModel();

  /**
   * The logger
   */
  private static final Logging LOG = Logging.getLogger(SelectionTableWindow.class);

  /**
   * The DBIDs to display
   */
  ArrayModifiableDBIDs dbids;

  /**
   * The database we use
   */
  UpdatableDatabase database;

  /**
   * Class label representation
   */
  ModifiableRelation<ClassLabel> crep;

  /**
   * Object label representation
   */
  ModifiableRelation<String> orep;

  /**
   * Our context
   */
  final protected VisualizerContext context;

  /**
   * The actual visualization instance, for a single projection
   *
   * @param context The Context
   */
  public SelectionTableWindow(VisualizerContext context) {
    super(NAME);
    // ELKI icon
    try {
      setIconImage(new ImageIcon(KDDTask.class.getResource("elki-icon.png")).getImage());
    }
    catch(Exception e) {
      // Ignore - icon not found is not fatal.
    }

    this.context = context;
    this.database = (UpdatableDatabase) ResultUtil.findDatabase(context.getHierarchy());
    // FIXME: re-add labels
    this.crep = null; // database.getClassLabelQuery();
    this.orep = null; // database.getObjectLabelQuery();
    updateFromSelection();

    JPanel panel = new JPanel(new BorderLayout());

    table = new JTable(dotTableModel);

    // table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane pane = new JScrollPane(table);
    panel.add(pane, BorderLayout.CENTER);

    JPanel buttons = new JPanel();
    panel.add(buttons, BorderLayout.SOUTH);

    closeButton = new JButton("close");
    closeButton.addActionListener((e) -> dispose());
    deleteButton = new JButton("delete");
    deleteButton.addActionListener((e) -> handleDelete());
    buttons.add(closeButton);
    buttons.add(deleteButton);

    setSize(500, 500);
    add(panel);
    setVisible(true);
    setResizable(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // Listen for Selection and Database changes.
    context.addResultListener(this);
    context.addDataStoreListener(this);
  }

  @Override
  public void dispose() {
    context.removeDataStoreListener(this);
    context.removeResultListener(this);
    super.dispose();
  }

  /**
   * Update our selection
   */
  protected void updateFromSelection() {
    DBIDSelection sel = context.getSelection();
    if(sel != null) {
      this.dbids = DBIDUtil.newArray(sel.getSelectedIds());
      this.dbids.sort();
    }
    else {
      this.dbids = DBIDUtil.newArray();
    }
  }

  /**
   * Handle delete. <br>
   * Delete the marked objects in the database.
   */
  protected void handleDelete() {
    ModifiableDBIDs todel = DBIDUtil.newHashSet();
    ModifiableDBIDs remain = DBIDUtil.newHashSet(dbids);
    DBIDArrayIter it = dbids.iter();
    for(int row : table.getSelectedRows()) {
      it.seek(row);
      todel.add(it);
      remain.remove(it);
    }
    // Unselect first ...
    context.setSelection(new DBIDSelection(remain));
    // Now delete them.
    for(DBIDIter iter = todel.iter(); iter.valid(); iter.advance()) {
      database.delete(iter);
    }
  }

  /**
   * View onto the database
   *
   * @author Erich Schubert
   */
  class DatabaseTableModel extends AbstractTableModel {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int getColumnCount() {
      return 3; // RelationUtil.dimensionality(database) + 3;
    }

    @Override
    public int getRowCount() {
      return dbids.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      DBIDRef id = dbids.iter().seek(rowIndex);
      if(columnIndex == 0) {
        return DBIDUtil.toString(id);
      }
      if(columnIndex == 1) {
        return orep.get(id);
      }
      if(columnIndex == 2) {
        return crep.get(id);
      }
      /*
       * NV obj = database.get(id);
       * if(obj == null) {
       * return null;
       * }
       * return obj.getValue(columnIndex - 3 + 1);
       */
      return null;
    }

    @Override
    public String getColumnName(int column) {
      if(column == 0) {
        return "DBID";
      }
      if(column == 1) {
        return "Object label";
      }
      if(column == 2) {
        return "Class label";
      }
      return "Dim " + (column - 3 + 1);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex > 0;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if(columnIndex == 0) {
        LOG.warning("Tried to edit DBID, this is not allowed.");
        return;
      }
      final DBIDRef id = dbids.iter().seek(rowIndex);
      if(columnIndex == 1 && aValue instanceof String) {
        orep.insert(id, (String) aValue);
      }
      if(columnIndex == 2 && aValue instanceof String) {
        // FIXME: better class label handling!
        SimpleClassLabel lbl = new SimpleClassLabel((String) aValue);
        crep.insert(id, lbl);
      }
      if(!(aValue instanceof String)) {
        LOG.warning("Was expecting a String value from the input element, got: " + aValue.getClass());
        return;
      }
      throw new AbortException("FIXME: INCOMPLETE TRANSITION");
      /*
       * NV obj = database.get(id);
       * if(obj == null) {
       * logger.warning("Tried to edit removed object?");
       * return;
       * }
       * final int dimensionality = RelationUtil.dimensionality(database);
       * double[] vals = new double[dimensionality];
       * for(int d = 0; d < dimensionality; d++) {
       * if(d == columnIndex - 3) {
       * vals[d] = FormatUtil.parseDouble((String) aValue);
       * }
       * else {
       * vals[d] = obj.doubleValue(d + 1);
       * }
       * }
       * NV newobj = obj.newInstance(vals);
       * newobj.setID(id);
       * final Representation<DatabaseObjectMetadata> mrep =
       * database.getMetadataQuery();
       * DatabaseObjectMetadata meta = mrep.get(id);
       * try {
       * database.delete(id);
       * database.insert(new Pair<NV, DatabaseObjectMetadata>(newobj, meta));
       * }
       * catch(UnableToComplyException e) {
       * de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
       * }
       */
      // TODO: refresh wrt. range selection!
    }
  }

  @Override
  public void contentChanged(DataStoreEvent e) {
    if(e.getInserts().isEmpty() && e.getRemovals().isEmpty() && !e.getUpdates().isEmpty()) {
      // Updates only.
      dotTableModel.fireTableDataChanged();
    }
    else {
      dotTableModel.fireTableStructureChanged();
    }
  }

  @Override
  public void resultAdded(Result child, Result parent) {
    // TODO Auto-generated method stub
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    // TODO Auto-generated method stub
  }

  @Override
  public void resultChanged(Result current) {
    if(current instanceof SelectionResult || current instanceof Database) {
      updateFromSelection();
      dotTableModel.fireTableStructureChanged();
    }
  }
}