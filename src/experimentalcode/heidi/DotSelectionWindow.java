package experimentalcode.heidi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class DotSelectionWindow<NV extends NumberVector<NV, ?>> extends JFrame implements TableModelListener {
  /**
   * A short name characterizing this Visualizer.
   */
  // TODO: kein Visualizer!
  private static final String NAME = "Heidi DotSelectionWindow";

  private static final long serialVersionUID = 1L;

  private SelectionUpdateVisualizer<?> opsel;

  private JTable table;

  private JButton resetButton;

  private JButton selectButton;

  private JButton changeButton;

  private JButton deleteButton;

  private Object[][] dataVector;

  private Object[] columnIdentifiers;

  private DefaultTableModel dotTableModel;

  private static Logging logger = Logging.getLogger(NAME);

  private ArrayList<Object> tupelOld;

  private ArrayList<Object> tupelNew;

  private ArrayList<DBID> dbids;

  private int columns;

  public DotSelectionWindow(VisualizerContext<? extends NV> context, SelectionUpdateVisualizer<?> ops, final ArrayList<DBID> dbIDs, SVGPlot s, VisualizationProjection p) {
    super("Dot Selection");
    this.opsel = ops;
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
      columnIdentifiers[j + 3] = "Dim " + j;
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
        int[] rowIDs = table.getSelectedRows();
        DBID[] rows = new DBID[rowIDs.length];
        for(int i = 0; i < rowIDs.length; i++) {
          rows[i] = dbIDs.get(rowIDs[i]);
        }
        opsel.selectPoints(rows);
      }
    });
    changeButton = new JButton("change in DB");
    changeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {

        java.util.Vector<java.util.Vector<String>> dataVectorNew = (java.util.Vector<java.util.Vector<String>>) dotTableModel.getDataVector();
        opsel.updateIDs(dbIDs, dataVectorNew);
      }
    });
    resetButton = new JButton("reset");
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        logger.warning("reset");
        dotTableModel.setDataVector(dataVector, columnIdentifiers);
        dotTableModel.fireTableDataChanged();
      }
    });
    deleteButton = new JButton("delete");
    deleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        logger.warning("delete");
        ArrayList<DBID> dbidList = new ArrayList<DBID>();
        int[] selRows = table.getSelectedRows();
        // logger.warning("selRows: "+ selRows[0]);
        // delete last selected row first!
        // for(int i = table.getSelectedRowCount() - 1; i >= 0; i--) {
        // dotTableModel.removeRow(selRows[i]);
        // }
        for(int i = 0; i < selRows.length; i++) {
          dbidList.add(dbids.get(selRows[i]));
        }
        opsel.deleteInDB(dbidList);
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
    // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }

  public void tableChanged(TableModelEvent e) {
    logger.warning("tableChanged");
    logger.warning("first row:  " + e.getFirstRow());
    logger.warning("last row:  " + e.getLastRow());
    logger.warning("column:  " + e.getColumn());
  }
}
