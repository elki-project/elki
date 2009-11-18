package experimentalcode.erich.visualization;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.Visualizers.Visualizer;

/**
 * Swing window to manage a particular result visualization.
 * 
 * @author Erich Schubert
 */
public class ResultWindow extends JFrame {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(ResultHistogramVisualizer.class);
  
  /** 
   * The "Overview" button, which goes to the overview view. 
   */
  private JButton overviewButton;

  /** 
   * The "Quit" button, to close the application. 
   */
  private JButton quitButton;

  /** 
   * The "Export" button, to save the image
   */
  private JButton saveButton;

  /** 
   * The SVG canvas. 
   */
  private JSVGSynchronizedCanvas svgCanvas = new JSVGSynchronizedCanvas();

  /**
   * The overview plot.
   */
  private OverviewPlot<DoubleVector> overview;

  /**
   * Constructor.
   */
  public ResultWindow(Database<? extends DatabaseObject> db, MultiResult result) {
    super("ELKI Result Visualization");

    // close handler
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // setup buttons
    saveButton = new JButton("Export");
    saveButton.setEnabled(false);
    saveButton.addActionListener(new ActionListener() {
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
        saveCurrentPlot();
      }
    });

    quitButton = new JButton("Quit");
    quitButton.addActionListener(new ActionListener() {
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        close();
      }
    });

    overviewButton = new JButton("Overview");
    overviewButton.setEnabled(false);
    overviewButton.addActionListener(new ActionListener() {
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
        showOverview();
      }
    });

    // Create a panel and add the button, status label and the SVG canvas.
    final JPanel panel = new JPanel(new BorderLayout());

    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
    p.add(saveButton);
    p.add(overviewButton);
    p.add(quitButton);

    panel.add("North", p);
    panel.add("Center", svgCanvas);

    this.getContentPane().add(panel);

    this.setSize(600, 600);
    
    this.overview = new OverviewPlot<DoubleVector>(db, result);
  }

  /**
   * Close the visualizer window.
   */
  public void close() {
    this.setVisible(false);
    this.dispose();
  }

  /**
   * Navigate to the overview plot.
   */
  public void showOverview() {
    showPlot(overview);
  }

  /**
   * Navigate to a particular plot.
   * 
   * @param plot Plot to show.
   */
  protected void showPlot(SVGPlot plot) {
    svgCanvas.setPlot(plot);
    overviewButton.setEnabled(plot != overview);
    saveButton.setEnabled(plot != null);
  }

  /**
   * Save/export the current plot.
   */
  public void saveCurrentPlot() {
    final SVGPlot currentPlot = svgCanvas.getPlot();
    if (currentPlot != null) {
      SVGSaveDialog.showSaveDialog(currentPlot,512,512);
    } else {
      logger.warning("saveCurrentPlot() called without a visible plot!");
    }
  }

  /**
   * Add visualizations.
   * 
   * @param vs Visualizations
   */
  public void addVisualizations(Collection<Visualizer> vs) {
    overview.addVisualizations(vs);
    overview.refresh();
  }
}