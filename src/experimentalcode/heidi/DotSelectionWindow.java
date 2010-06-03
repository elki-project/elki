package experimentalcode.heidi;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

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

  public DotSelectionWindow(SelectionUpdateVisualizer<?> ops, ArrayList<ArrayList<String>> t, SVGPlot s, VisualizationProjection p) {
    super("Dot Selection");
    svgp = s;
    proj = p;
    opsel = ops;
    tupels = t;
    data = new String[tupels.size()][tupels.get(0).size()];
    for(int i = 0; i < tupels.size(); i++) {
      for(int j = 0; j < tupels.get(0).size(); j++) {
        data[i][j] = (String) tupels.get(i).get(j);
      }
    }
    JPanel panel = new JPanel();
    String[] col = new String[tupels.get(0).size()];
    col[0] = "ID";
    for(int i = 1; i < tupels.get(0).size(); i++) {
      col[i] = "Dim " + i;
    }
    table = new JTable(data, col);
    table.addMouseListener(this);
    JScrollPane pane = new JScrollPane(table);
    panel.add(pane);
    add(panel);
    setSize(500, 500);
//    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
