package experimentalcode.heidi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class DotSelectionWindow extends JFrame implements TableModelListener {
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

  private ArrayList<ArrayList<String>> tupels;

  public DotSelectionWindow(VisualizerContext<?> context, SelectionUpdateVisualizer<?> ops, ArrayList<DBID> dbIDs, ArrayList<ArrayList<String>> t, SVGPlot s, VisualizationProjection p) {
    super("Dot Selection");
    this.opsel = ops;
    tupels = t;

    Database<?> database = context.getDatabase();

    dataVector = new String[tupels.size()][tupels.get(0).size() + 3];
    for(int i = 0; i < tupels.size(); i++) {

      ClassLabel classLabel = database.getClassLabel(dbIDs.get(i));
      dataVector[i][0] = dbIDs.get(i).toString();
      if(classLabel == null) {
        dataVector[i][1] = "none";
      }
      else {
        dataVector[i][1] = classLabel.toString();
      }
      String objectLabel = database.getObjectLabel(dbIDs.get(i));
      dataVector[i][2] = objectLabel;

      for(int j = 0; j < tupels.get(0).size(); j++) {
        dataVector[i][j + 3] = (String) tupels.get(i).get(j);
      }
    }
    JPanel panel = new JPanel();
    columnIdentifiers = new String[tupels.get(0).size() + 3];
    columnIdentifiers[0] = "ID";
    columnIdentifiers[1] = "ClassLabel";
    columnIdentifiers[2] = "ObjectLabel";
    for(int j = 1; j <= tupels.get(0).size(); j++) {
      columnIdentifiers[j + 2] = "Dim " + j;
    }

    dotTableModel = new DefaultTableModel(dataVector, columnIdentifiers);

    table = new JTable(dotTableModel);
    // table.addMouseListener(this);
    table.getModel().addTableModelListener(this);

    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JScrollPane pane = new JScrollPane(table);
    panel.add(pane);

    selectButton = new JButton("select Point");
    selectButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int row = table.getSelectedRow();
        if(table.getSelectedRow() != -1) {
         opsel.selectPoint(dotTableModel.getValueAt(row, 0));
        }
      }
    });
    changeButton = new JButton("change in DB");
    changeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {

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
        if(table.getSelectedRow() != -1) {
          dotTableModel.removeRow(table.getSelectedRow());
        }
      }
    });
    panel.add(selectButton);
    panel.add(changeButton);
    panel.add(resetButton);
    panel.add(deleteButton);

    setSize(500, 500);
    add(panel);
    // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(true);
    setResizable(true);
  }

  public void tableChanged(TableModelEvent e) {
    logger.warning("tableChanged");
  }
}
