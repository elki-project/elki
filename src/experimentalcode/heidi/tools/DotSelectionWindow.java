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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class DotSelectionWindow<NV extends NumberVector<NV, ?>> extends JFrame implements TableModelListener {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Heidi DotSelectionWindow";

  private static final long serialVersionUID = 1L;

  private ToolDBChangeVisualizer<?> opsel;

  private JTable table;

  private JButton resetButton;

  private JButton selectButton;

  private JButton changeButton;

  private JButton deleteButton;

  private Object[][] dataVector;

  private Object[] columnIdentifiers;

  private DefaultTableModel dotTableModel;

  private static Logging logger = Logging.getLogger(NAME);

  private ArrayModifiableDBIDs dbids;

  private int columns;

  public DotSelectionWindow(VisualizerContext<? extends NV> context, ToolDBChangeVisualizer<NV> toolDBChangeVisualizer, final ArrayModifiableDBIDs dbIDs) {
    super("Dot Selection");
    this.opsel = toolDBChangeVisualizer;
    this.dbids = dbIDs;

    Database<? extends NV> database = context.getDatabase();
    columns = database.dimensionality() + 3;

    dataVector = new String[dbIDs.size()][columns];

    for(int i = 0; i < dbids.size(); i++) {
      dataVector[i][0] = dbids.get(i).toString();
      ClassLabel classLabel = database.getClassLabel(dbIDs.get(i));
      if(classLabel == null) {
        dataVector[i][1] = "none";
      }
      else {
        dataVector[i][1] = classLabel.toString();
      }
      String objectLabel = database.getObjectLabel(dbIDs.get(i));
      dataVector[i][2] = objectLabel;

      Vector v = database.get(dbids.get(i)).getColumnVector();

      for(int j = 0; j < v.getDimensionality(); j++) {
        dataVector[i][j + 3] = Double.toString(v.get(j));
      }
    }
    JPanel panel = new JPanel();
    columnIdentifiers = new String[columns];
    columnIdentifiers[0] = "ID";
    columnIdentifiers[1] = "ClassLabel";
    columnIdentifiers[2] = "ObjectLabel";
    for(int j = 0; j < database.dimensionality(); j++) {
      columnIdentifiers[j + 3] = "Dim " + (j + 1);
    }

    dotTableModel = new DefaultTableModel(dataVector, columnIdentifiers);

    table = new JTable(dotTableModel);
    // table.addMouseListener(this);
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
        java.util.Vector<java.util.Vector<String>> dataVectorNew = (java.util.Vector<java.util.Vector<String>>) dotTableModel.getDataVector();
        for(int i = 0; i < dataVector.length; i++) {
          if(dataVector[i][0].equals(dataVectorNew.get(i).get(0))) {
            // dbids equal
            if(!dataVector[i].equals(dataVectorNew.get(i))) {
              logger.warning("data not equal");
              opsel.updateDB(dbids.get(i), dataVectorNew.get(i));
            } else {
              logger.warning("data equal");
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
    logger.warning("tableChanged");
    logger.warning("first row:  " + e.getFirstRow());
    logger.warning("last row:  " + e.getLastRow());
    logger.warning("column:  " + e.getColumn());
  }
}
