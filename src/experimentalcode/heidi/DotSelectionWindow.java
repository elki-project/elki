package experimentalcode.heidi;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

public class DotSelectionWindow extends JFrame implements MouseListener {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  SelectionUpdateVisualizer<?> opsel;

  private JTable table;

  String[][] data;

  ArrayList<ArrayList<String>> tupels;

  SVGPlot svgp;

  VisualizationProjection proj;

  public DotSelectionWindow(VisualizerContext<?> context, SelectionUpdateVisualizer<?> ops, ArrayList<DBID> dbIDs, ArrayList<ArrayList<String>> t, SVGPlot s, VisualizationProjection p) {
    super("Dot Selection");
    svgp = s;
    proj = p;
    opsel = ops;
    tupels = t;

    Database<?> database = context.getDatabase();

    data = new String[tupels.size()][tupels.get(0).size() + 3];
    for(int i = 0; i < tupels.size(); i++) {

      ClassLabel classLabel = database.getClassLabel(dbIDs.get(i));
      data[i][0] = dbIDs.get(i).toString();
      if(classLabel == null) {
        data[i][1] = "none";
      }
      else {
        data[i][1] = classLabel.toString();
      }
      String objectLabel = database.getObjectLabel(dbIDs.get(i));
      data[i][2] = objectLabel;
      
      for(int j = 0; j < tupels.get(0).size(); j++) {
        data[i][j+3] = (String) tupels.get(i).get(j);
      }
    }
    //TODO: besser machen
    JPanel panel = new JPanel();
    String[] col = new String[tupels.get(0).size()+3];
    col[0] = "ID";
    col[1] = "ClassLabel";
    col[2] = "ObjectLabel";
    for(int j=1; j <= tupels.get(0).size(); j++) {
      col[j+2] = "Dim " + j;
    }
    table = new JTable(data, col);
    table.addMouseListener(this);
    JScrollPane pane = new JScrollPane(table);
    panel.add(pane);
    add(panel);
    setSize(500, 500);
    // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(true);
    setResizable(true);
  }

  public void mouseClicked(MouseEvent e) {
    int row = table.rowAtPoint(e.getPoint());
    opsel.selectPoint(svgp, proj, data[row][0]);
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub

  }
}
