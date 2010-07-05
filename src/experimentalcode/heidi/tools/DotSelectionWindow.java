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
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class DotSelectionWindow<NV extends NumberVector<NV, ?>> extends JFrame implements TableModelListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "DotSelectionWindow";

  private static final long serialVersionUID = 1L;

  private ToolDBChangeVisualizer<?> opsel;

  private JTable table;

  private JButton resetButton;

  private JButton selectButton;

  private JButton changeButton;

  private JButton deleteButton;

  private java.util.Vector<java.util.Vector<String>> dataVector;
  private java.util.Vector<java.util.Vector<String>> dataVectorOld;

  private java.util.Vector<String> columnIdentifiers;

  private DefaultTableModel dotTableModel;

  private static Logging logger = Logging.getLogger(NAME);

  private ArrayModifiableDBIDs dbids;


  public DotSelectionWindow(VisualizerContext<? extends NV> context, ToolDBChangeVisualizer<NV> toolDBChangeVisualizer, final ArrayModifiableDBIDs dbIDs) {
    super(NAME);
    this.opsel = toolDBChangeVisualizer;
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
      dataOld = (java.util.Vector<String>)data.clone();
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
        if(table.getSelectedRowCount() > 0) {
          int[] rowIDs = table.getSelectedRows();
          DBID[] rows = new DBID[rowIDs.length];
          for(int i = 0; i < rowIDs.length; i++) {
            rows[i] = dbIDs.get(rowIDs[i]);
          }
          opsel.selectPoints(rows);
        }
        else {
          opsel.deleteMarker();
        }
        opsel.closeDotWindow();
      }
    });
    changeButton = new JButton("change in DB");
    changeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        for(int i = 0; i < dataVector.size(); i++) {
          if(dataVector.get(i).get(0).equals(dataVectorOld.get(i).get(0))) {
            // dbids equal
            if(!dataVector.get(i).equals(dataVectorOld.get(i))) {
              opsel.updateDB(dbids.get(i), dataVector.get(i));
            }
          }
          else {
            // dbids not equal --> delete
            logger.warning("Error: DBIDs not equal");
          }
        }
        opsel.closeDotWindow();
        opsel.deleteMarker();
      }
    });
    resetButton = new JButton("reset");
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        dotTableModel.setDataVector(dataVector, columnIdentifiers);
        dotTableModel.fireTableDataChanged();
      }
    });
    deleteButton = new JButton("delete");
    deleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        int[] selRows = table.getSelectedRows();
        for(int i = 0; i < selRows.length; i++) {
          opsel.deleteInDB(dbids.get(selRows[i]));
        }
        opsel.closeDotWindow();
        opsel.deleteMarker();
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

  public void tableChanged(TableModelEvent e) {
  }
}
