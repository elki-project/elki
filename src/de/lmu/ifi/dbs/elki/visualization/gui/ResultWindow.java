package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.OverviewPlot;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.SubplotSelectedEvent;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;

/**
 * Swing window to manage a particular result visualization.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
public class ResultWindow extends JFrame {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Get a logger for this class.
   */
  protected final static Logging logger = Logging.getLogger(ResultWindow.class);

  /**
   * The "Overview" button, which goes to the overview view.
   */
  private JMenuItem overviewItem;

  /**
   * The "Quit" button, to close the application.
   */
  private JMenuItem quitItem;

  /**
   * The "Export" button, to save the image
   */
  private JMenuItem exportItem;

  /**
   * The "Visualizers" button, to enable/disable visualizers
   */
  private JMenu visualizersMenu;

  /**
   * The SVG canvas.
   */
  private JSVGSynchronizedCanvas svgCanvas;

  /**
   * The overview plot.
   */
  private OverviewPlot<DoubleVector> overview;

  /**
   * Visualizers
   */
  private ArrayList<Visualizer> visualizers;

  /**
   * Currently selected subplot.
   */
  private SubplotSelectedEvent currentSubplot = null;

  /**
   * Constructor.
   * 
   * @param title Window title 
   * @param db Database
   * @param result Result to visualize
   * @param maxdim Maximal dimensionality to show.
   */
  public ResultWindow(String title, Database<? extends DatabaseObject> db, MultiResult result, int maxdim) {
    super(title);

    // close handler
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // setup buttons
    exportItem = new JMenuItem("Export");
    exportItem.setMnemonic(KeyEvent.VK_E);
    exportItem.setEnabled(false);
    exportItem.addActionListener(new ActionListener() {
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
        saveCurrentPlot();
      }
    });

    quitItem = new JMenuItem("Quit");
    quitItem.setMnemonic(KeyEvent.VK_Q);
    quitItem.addActionListener(new ActionListener() {
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        close();
      }
    });

    overviewItem = new JMenuItem("Overview");
    overviewItem.setMnemonic(KeyEvent.VK_O);
    overviewItem.setEnabled(false);
    overviewItem.addActionListener(new ActionListener() {
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
        showOverview();
      }
    });

    // Create a panel and add the button, status label and the SVG canvas.
    final JPanel panel = new JPanel(new BorderLayout());

    JMenuBar menubar = new JMenuBar();
    JMenu filemenu = new JMenu("File");
    filemenu.setMnemonic(KeyEvent.VK_F);
    filemenu.add(overviewItem);
    filemenu.add(exportItem);
    filemenu.add(quitItem);
    menubar.add(filemenu);
    visualizersMenu = new JMenu("Visualizers");
    visualizersMenu.setMnemonic(KeyEvent.VK_V);
    menubar.add(visualizersMenu);

    panel.add("North", menubar);

    svgCanvas = new JSVGSynchronizedCanvas();
    panel.add("Center", svgCanvas);

    this.getContentPane().add(panel);

    this.overview = new OverviewPlot<DoubleVector>(db, result, maxdim);
    // when a subplot is clicked, show the selected subplot.
    overview.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(e instanceof SubplotSelectedEvent) {
          showSubplot((SubplotSelectedEvent) e);
        }
      }
    });

    // handle screen size
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    this.overview.screenwidth = dim.width;
    this.overview.screenheight = dim.height;

    // Visualizers
    this.visualizers = new ArrayList<Visualizer>();

    // Maximize.
    this.setSize(600, 600);
    this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
  }

  @Override
  public void dispose() {
    svgCanvas.setPlot(null);
    overview.dispose();
    super.dispose();
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
    currentSubplot = null;
    showPlot(overview);
  }

  /**
   * Navigate to a subplot.
   * 
   * @param e
   */
  protected void showSubplot(SubplotSelectedEvent e) {
    currentSubplot = e;
    showPlot(e.makeSubplot());
  }

  /**
   * Navigate to a particular plot.
   * 
   * @param plot Plot to show.
   */
  private void showPlot(SVGPlot plot) {
    svgCanvas.setPlot(plot);
    overviewItem.setEnabled(plot != overview);
    exportItem.setEnabled(plot != null);
  }

  /**
   * Save/export the current plot.
   */
  public void saveCurrentPlot() {
    final SVGPlot currentPlot = svgCanvas.getPlot();
    if(currentPlot != null) {
      SVGSaveDialog.showSaveDialog(currentPlot, 512, 512);
    }
    else {
      logger.warning("saveCurrentPlot() called without a visible plot!");
    }
  }

  /**
   * Add visualizations.
   * 
   * @param vs Visualizations
   */
  public void addVisualizations(Collection<Visualizer> vs) {
    visualizers.addAll(vs);
    overview.addVisualizations(vs);
    update();
    showPlot(overview);
  }

  /**
   * Refresh the overview
   */
  protected void update() {
    updatePopupMenu();
    if(currentSubplot != null) {
      showPlot(currentSubplot.makeSubplot());
    }
    overview.refresh();
  }

  /**
   * Update the popup menu.
   */
  private void updatePopupMenu() {
    visualizersMenu.removeAll();
    for(final Visualizer v : visualizers) {
      Boolean enabled = v.getMetadata().getGenerics(Visualizer.META_VISIBLE, Boolean.class);
      if(enabled == null) {
        enabled = v.getMetadata().getGenerics(Visualizer.META_VISIBLE_DEFAULT, Boolean.class);
      }
      if(enabled == null) {
        enabled = true;
      }
      final String name = v.getMetadata().getGenerics(Visualizer.META_NAME, String.class);
      final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(name, enabled);
      visItem.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(@SuppressWarnings("unused") ItemEvent e) {
          v.getMetadata().put(Visualizer.META_VISIBLE, visItem.getState());
          update();
        }
      });
      visualizersMenu.add(visItem);
    }
  }
}