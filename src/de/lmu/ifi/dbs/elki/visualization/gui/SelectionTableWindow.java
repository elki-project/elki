package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import de.lmu.ifi.dbs.elki.visualization.visualizers.DBIDSelection;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.SelectionChangedEvent;

/**
 * Visualizes selected Objects in a JTable, objects can be selected, changed and
 * deleted
 * 
 * @author Heidi Kolb
 * @author Erich Schubert
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class SelectionTableWindow<NV extends NumberVector<NV, ?>> extends JFrame implements DataStoreListener<NV>, ContextChangeListener {
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
  static final Logging logger = Logging.getLogger(SelectionTableWindow.class);

  /**
   * The DBIDs to display
   */
  ArrayModifiableDBIDs dbids;

  /**
   * The database we use
   */
  Database<NV> database;

  /**
   * Our context
   */
  final protected VisualizerContext<? extends NV> context;

  /**
   * The actual visualization instance, for a single projection
   * 
   * @param context The Context
   */
  @SuppressWarnings("unchecked")
  public SelectionTableWindow(VisualizerContext<? extends NV> context) {
    super(NAME);
    this.context = context;
    this.database = (Database<NV>) context.getDatabase();
    updateFromSelection();

    JPanel panel = new JPanel(new BorderLayout());

    table = new JTable(dotTableModel);

    // table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane pane = new JScrollPane(table);
    panel.add(pane, BorderLayout.CENTER);

    JPanel buttons = new JPanel();
    panel.add(buttons, BorderLayout.SOUTH);

    closeButton = new JButton("close");
    closeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent arg0) {
        dispose();
      }
    });
    deleteButton = new JButton("delete");
    deleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent arg0) {
        handleDelete();
      }
    });
    buttons.add(closeButton);
    buttons.add(deleteButton);

    setSize(500, 500);
    add(panel);
    setVisible(true);
    setResizable(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // Listen for Selection and Database changes.
    context.addContextChangeListener(this);
    context.addDataStoreListener(this);
  }

  @Override
  public void dispose() {
    context.removeDataStoreListener(this);
    context.removeContextChangeListener(this);
    super.dispose();
  }

  /**
   * Update our selection
   */
  protected void updateFromSelection() {
    DBIDSelection sel = context.getSelection();
    if(sel != null) {
      this.dbids = DBIDUtil.newArray(sel.getSelectedIds());
      Collections.sort(this.dbids);
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
    for(int row : table.getSelectedRows()) {
      final DBID id = dbids.get(row);
      todel.add(id);
      remain.remove(id);
    }
    // Unselect first ...
    context.setSelection(new DBIDSelection(remain));
    // Now delete them.
    for(DBID id : todel) {
      database.delete(id);
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
      return database.dimensionality() + 3;
    }

    @Override
    public int getRowCount() {
      return dbids.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      DBID id = dbids.get(rowIndex);
      if(columnIndex == 0) {
        return id.toString();
      }
      if(columnIndex == 1) {
        return database.getObjectLabel(id);
      }
      if(columnIndex == 2) {
        return database.getClassLabel(id);
      }
      NV obj = database.get(id);
      if(obj == null) {
        return null;
      }
      return obj.getValue(columnIndex - 3 + 1);
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
    public boolean isCellEditable(@SuppressWarnings("unused") int rowIndex, int columnIndex) {
      if(columnIndex == 0) {
        return false;
      }
      return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if(columnIndex == 0) {
        logger.warning("Tried to edit DBID, this is not allowed.");
        return;
      }
      final DBID id = dbids.get(rowIndex);
      if(columnIndex == 1 && aValue instanceof String) {
        database.setObjectLabel(id, (String) aValue);
      }
      if(columnIndex == 2 && aValue instanceof String) {
        // FIXME: better class label handling!
        SimpleClassLabel lbl = new SimpleClassLabel();
        lbl.init((String) aValue);
        database.setClassLabel(id, lbl);
      }
      if(!(aValue instanceof String)) {
        logger.warning("Was expecting a String value from the input element, got: " + aValue.getClass());
        return;
      }
      NV obj = database.get(id);
      if(obj == null) {
        logger.warning("Tried to edit removed object?");
        return;
      }
      double[] vals = new double[database.dimensionality()];
      for(int d = 0; d < database.dimensionality(); d++) {
        if(d == columnIndex - 3) {
          vals[d] = Double.valueOf((String) aValue);
        }
        else {
          vals[d] = obj.doubleValue(d + 1);
        }
      }
      NV newobj = obj.newInstance(vals);
      newobj.setID(id);
      DatabaseObjectMetadata meta = new DatabaseObjectMetadata(database, id);
      try {
        database.delete(id);
        database.insert(new Pair<NV, DatabaseObjectMetadata>(newobj, meta));
      }
      catch(UnableToComplyException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      }
      // TODO: refresh wrt. range selection!
    }
  }
  
  @Override
  public void contentChanged(DataStoreEvent<NV> e) {
    if (e.isUpdateEvent()) {
      dotTableModel.fireTableDataChanged(); 
    }
    else {
      dotTableModel.fireTableStructureChanged();  
    }
  }

  @Override
  public void contextChanged(ContextChangedEvent e) {
    if(e instanceof SelectionChangedEvent) {
      updateFromSelection();
      dotTableModel.fireTableStructureChanged();
    }
  }
}