package experimentalcode.heidi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

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
  private static final String NAME = "Heidi DotSelectionWindow";

  private static final long serialVersionUID = 1L;

  private SelectionUpdateVisualizer<?> opsel;

  private JTable table;

  private JButton resetButton;

  private JButton selectButton;

  private JButton changeButton;

  private JButton deleteButton;

  private String[][] dotArray;

  private String[] colArray;

  private DotTableModel dotTableModel;

  private static Logging logger = Logging.getLogger(NAME);

  private ArrayList<ArrayList<String>> tupels;

  public DotSelectionWindow(VisualizerContext<?> context, SelectionUpdateVisualizer<?> ops, ArrayList<DBID> dbIDs, ArrayList<ArrayList<String>> t, SVGPlot s, VisualizationProjection p) {
    super("Dot Selection");
    this.opsel = ops;
    tupels = t;

    Database<?> database = context.getDatabase();

    dotArray = new String[tupels.size()][tupels.get(0).size() + 3];
    for(int i = 0; i < tupels.size(); i++) {

      ClassLabel classLabel = database.getClassLabel(dbIDs.get(i));
      dotArray[i][0] = dbIDs.get(i).toString();
      if(classLabel == null) {
        dotArray[i][1] = "none";
      }
      else {
        dotArray[i][1] = classLabel.toString();
      }
      String objectLabel = database.getObjectLabel(dbIDs.get(i));
      dotArray[i][2] = objectLabel;

      for(int j = 0; j < tupels.get(0).size(); j++) {
        dotArray[i][j + 3] = (String) tupels.get(i).get(j);
      }
    }
    JPanel panel = new JPanel();
    colArray = new String[tupels.get(0).size() + 3];
    colArray[0] = "ID";
    colArray[1] = "ClassLabel";
    colArray[2] = "ObjectLabel";
    for(int j = 1; j <= tupels.get(0).size(); j++) {
      colArray[j + 2] = "Dim " + j;
    }

    dotTableModel = new DotTableModel(dotArray, colArray);

    table = new JTable(dotTableModel);
//    table.addMouseListener(this);
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
        opsel.selectPoint(dotArray[row][0]);      }
      }
    });
    changeButton = new JButton("change in DB");
    changeButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        // TODO: change
      }
    });
    resetButton = new JButton("reset");
    resetButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        logger.warning("reset");
        dotTableModel.setDotArray(dotArray);
      }
    });
    deleteButton = new JButton("delete");
    deleteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        logger.warning("delete");
        if(table.getSelectedRow() != -1) {
//          dotTableModel.removeRow(table.getSelectedRow());
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
    int row = e.getFirstRow();
    int column = e.getColumn();
    logger.warning("row: " + row);
    logger.warning("column: " + column);
    int c = 2;
    int r = 2;
    logger.warning("Value (2,2): " + table.getModel().getValueAt(r, c));
  }

//  public void mouseClicked(MouseEvent e) {
//    int row = table.rowAtPoint(e.getPoint());
//    opsel.selectPoint(dotArray[row][0]);
//  }
//
//  @Override
//  public void mouseEntered(MouseEvent e) {
//    // TODO Auto-generated method stub
//  }
//
//  @Override
//  public void mouseExited(MouseEvent e) {
//    // TODO Auto-generated method stub
//
//  }
//
//  @Override
//  public void mousePressed(MouseEvent e) {
//    // TODO Auto-generated method stub
//
//  }
//
//  @Override
//  public void mouseReleased(MouseEvent e) {
//    // TODO Auto-generated method stub
//
//  }
}
