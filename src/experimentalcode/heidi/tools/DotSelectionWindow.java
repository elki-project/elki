package experimentalcode.heidi.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualizes selected Objects in a JTable.<br>
 * Objects can be selected, changed and deleted
 * 
 * @author Heidi Kolb
 * 
 * @param <NV> Type of the NumberVector being visualized.
 */
public class DotSelectionWindow<NV extends NumberVector<NV, ?>> extends JFrame implements TableModelListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "DotSelectionWindow";

  private static final long serialVersionUID = 1L;

  /**
   * The ToolDBChangeVisualizer
   */
  private ToolDBChangeVisualizer<?> dbChanceVis;

  /**
   * The JTable
   */
  private JTable table;

  /**
   * Button to reset changes
   */
  private JButton resetButton;

  /**
   * Button to select the marked objects
   */
  private JButton selectButton;

  /**
   * Button to write the changes in the database
   */
  private JButton changeButton;

  /**
   * Button to delete the selected objects
   */
  private JButton deleteButton;

  /**
   * DataVector for JTable
   */
  private java.util.Vector<java.util.Vector<String>> dataVector;

  /**
   * DataVector before changes
   */
  private java.util.Vector<java.util.Vector<String>> dataVectorOld;

  /**
   * Column identifiers for JTable
   */
  private java.util.Vector<String> columnIdentifiers;

  /**
   * The table model
   */
  private DefaultTableModel dotTableModel;

  /**
   * The logger
   */
  private static Logging logger = Logging.getLogger(NAME);

  /**
   * The DBIDs to display
   */
  private ArrayModifiableDBIDs dbids;

  /**
   * The actual visualization instance, for a single projection
   * 
   * @param context The Context
   * @param toolDBChangeVisualizer The ToolDBChangeVisualizer
   * @param dbIDs The DBIDs to display
   */
  public DotSelectionWindow(VisualizerContext<? extends NV> context, ToolDBChangeVisualizer<NV> toolDBChangeVisualizer, final ArrayModifiableDBIDs dbIDs) {
    super(NAME);
    this.dbChanceVis = toolDBChangeVisualizer;
    this.dbids = dbIDs;

    Database<? extends NV> database = context.getDatabase();

    dataVector = new java.util.Vector<java.util.Vector<String>>();
    dataVectorOld = new java.util.Vector<java.util.Vector<String>>();

    columnIdentifiers = new java.util.Vector<String>();

    for(int i = 0; i < dbids.size(); i++) {
      java.util.Vector<String> data = new java.util.Vector<String>();

      data.add(dbids.get(i).toString());
      ClassLabel classLabel = database.getClassLabel(dbIDs.get(i));
      if(classLabel == null) {
        data.add("none");
      }
      else {
        data.add(classLabel.toString());
      }
      String objectLabel = database.getObjectLabel(dbIDs.get(i));
      data.add(objectLabel);

      Vector v = database.get(dbids.get(i)).getColumnVector();

      for(int j = 0; j < v.getDimensionality(); j++) {
        data.add(Double.toString(v.get(j)));
      }
      dataVector.add(data);

      java.util.Vector<String> dataOld = new java.util.Vector<String>();
      dataOld = (java.util.Vector<String>) data.clone();
      dataVectorOld.add(dataOld);
    }

    JPanel panel = new JPanel();
    columnIdentifiers.add("ID");
    columnIdentifiers.add("ClassLabel");
    columnIdentifiers.add("ObjectLabel");
    for(int j = 0; j < database.dimensionality(); j++) {
      columnIdentifiers.add("Dim " + (j + 1));
    }

    dotTableModel = new DefaultTableModel(dataVector, columnIdentifiers);

    table = new JTable(dotTableModel);
    table.getModel().addTableModelListener(this);

    // table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane pane = new JScrollPane(table);
    panel.add(pane);

    selectButton = new JButton("select");
    selectButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleSelect();
      }
    });
    changeButton = new JButton("change in DB");
    changeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        handleChange();
      }
    });
    resetButton = new JButton("reset");
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        handleReset();
      }
    });
    deleteButton = new JButton("delete");
    deleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        handleDelete();
      }
    });
    panel.add(selectButton);
    panel.add(changeButton);
    panel.add(resetButton);
    panel.add(deleteButton);

    setSize(500, 500);
    add(panel);
    setVisible(true);
    setResizable(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }

  /**
   * Handle select. <br>
   * Select the marked objects in ToolDBChangeVisualizer.
   */
  private void handleSelect() {
    if(table.getSelectedRowCount() > 0) {
      int[] rowIDs = table.getSelectedRows();
      ArrayModifiableDBIDs rows = DBIDUtil.newArray(rowIDs.length);
      for(int i = 0; i < rowIDs.length; i++) {
        rows.add(dbids.get(rowIDs[i]));
      }
      dbChanceVis.selectPoints(rows);
    }
    else {
      dbChanceVis.deleteMarker();
    }
    dbChanceVis.closeDotWindow();
  }

  /**
   * Handle change. <br>
   * Update the attributes of the modified objects in the database.
   */
  private void handleChange() {
    for(int i = 0; i < dataVector.size(); i++) {
      if(dataVector.get(i).get(0).equals(dataVectorOld.get(i).get(0))) {
        // dbids equal
        if(!dataVector.get(i).equals(dataVectorOld.get(i))) {
          dbChanceVis.updateDB(dbids.get(i), dataVector.get(i));
        }
      }
      else {
        logger.warning("Error: DBIDs not equal");
      }
    }
    dbChanceVis.closeDotWindow();
    dbChanceVis.deleteMarker();
  }

  /**
   * Handle reset. <br>
   * Reset the modifications in the table.
   */
  private void handleReset() {
    dotTableModel.setDataVector(dataVector, columnIdentifiers);
    dotTableModel.fireTableDataChanged();
  }

  /**
   * Handle delete. <br>
   * Delete the marked objects in the database.
   */
  private void handleDelete() {
    int[] selRows = table.getSelectedRows();
    for(int i = 0; i < selRows.length; i++) {
      dbChanceVis.deleteInDB(dbids.get(selRows[i]));
    }
    dbChanceVis.closeDotWindow();
    dbChanceVis.deleteMarker();
  }

  @Override
  public void tableChanged(TableModelEvent arg0) {
    // TODO Auto-generated method stub
    // nothing to do
  }

}
