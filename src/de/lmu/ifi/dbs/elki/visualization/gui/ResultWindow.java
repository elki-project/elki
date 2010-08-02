package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;
import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.DetailViewSelectedEvent;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.OverviewPlot;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerGroup;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerTreeItem;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.VisualizerChangedEvent;

/**
 * Swing window to manage a particular result visualization.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
public class ResultWindow extends JFrame implements ContextChangeListener {
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
   * The "Tools" button, to enable/disable visualizers
   */
  private JMenu toolsMenu;

  /**
   * The SVG canvas.
   */
  private JSVGSynchronizedCanvas svgCanvas;

  /**
   * The overview plot.
   */
  private OverviewPlot<DoubleVector> overview;

  /**
   * Visualizer context
   */
  private VisualizerContext<? extends DatabaseObject> context;

  /**
   * Currently selected subplot.
   */
  private DetailView currentSubplot = null;

  /**
   * Constructor.
   * 
   * @param title Window title
   * @param db Database
   * @param result Result to visualize
   * @param maxdim Maximal dimensionality to show.
   * @param context Visualizer context
   */
  public ResultWindow(String title, Database<? extends DatabaseObject> db, MultiResult result, int maxdim, VisualizerContext<? extends DatabaseObject> context) {
    super(title);
    this.context = context;

    // close handler
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // setup buttons
    exportItem = new JMenuItem("Export");
    exportItem.setMnemonic(KeyEvent.VK_E);
    exportItem.setEnabled(false);
    exportItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
        saveCurrentPlot();
      }
    });

    quitItem = new JMenuItem("Quit");
    quitItem.setMnemonic(KeyEvent.VK_Q);
    quitItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
        close();
      }
    });

    overviewItem = new JMenuItem("Overview");
    overviewItem.setMnemonic(KeyEvent.VK_O);
    overviewItem.setEnabled(false);
    overviewItem.addActionListener(new ActionListener() {
      @Override
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
    toolsMenu = new JMenu("Tools");
    toolsMenu.setMnemonic(KeyEvent.VK_T);
    menubar.add(toolsMenu);

    panel.add("North", menubar);

    svgCanvas = new JSVGSynchronizedCanvas();
    panel.add("Center", svgCanvas);

    this.getContentPane().add(panel);

    this.overview = new OverviewPlot<DoubleVector>(db, result, maxdim, context);
    // when a subplot is clicked, show the selected subplot.
    overview.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(e instanceof DetailViewSelectedEvent) {
          showSubplot((DetailViewSelectedEvent) e);
        }
      }
    });

    // handle screen size
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    this.overview.screenwidth = dim.width;
    this.overview.screenheight = dim.height;

    // Maximize.
    this.setSize(600, 600);
    this.setExtendedState(JFrame.MAXIMIZED_BOTH);

    // resize listener
    LazyCanvasResizer listener = new LazyCanvasResizer(this) {
      @Override
      public void executeResize(double newratio) {
        setRatio(newratio);
      }
    };
    setRatio(listener.getActiveRatio());
    this.addComponentListener(listener);

    context.addContextChangeListener(this);

    update();
  }

  /**
   * Change the plot ratio. Will only be applied to new plots for now.
   * 
   * @param newratio New ratio
   */
  protected void setRatio(double newratio) {
    ResultWindow.this.overview.setRatio(newratio);
  }

  @Override
  public void dispose() {
    context.removeContextChangeListener(this);
    svgCanvas.setPlot(null);
    overview.dispose();
    if(currentSubplot != null) {
      currentSubplot.dispose();
      currentSubplot = null;
    }
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
    if(currentSubplot != null) {
      currentSubplot.destroy();
    }
    currentSubplot = null;
    showPlot(overview);
  }

  /**
   * Navigate to a subplot.
   * 
   * @param e
   */
  protected void showSubplot(DetailViewSelectedEvent e) {
    currentSubplot = e.makeDetailView();
    showPlot(currentSubplot);
  }

  /**
   * Navigate to a particular plot.
   * 
   * @param plot Plot to show.
   */
  private void showPlot(SVGPlot plot) {
    if(svgCanvas.getPlot() instanceof DetailView) {
      ((DetailView) svgCanvas.getPlot()).destroy();
    }
    svgCanvas.setPlot(plot);
    overviewItem.setEnabled(plot != overview);
    exportItem.setEnabled(plot != null);
  }

  /**
   * Save/export the current plot.
   */
  public void saveCurrentPlot() {
    // TODO: exclude "do not export" layers!
    final SVGPlot currentPlot = svgCanvas.getPlot();
    if(currentPlot != null) {
      SVGSaveDialog.showSaveDialog(currentPlot, 512, 512);
    }
    else {
      logger.warning("saveCurrentPlot() called without a visible plot!");
    }
  }

  /**
   * Refresh the overview
   */
  protected void update() {
    updateVisualizerMenus();
    if(currentSubplot != null) {
      // FIXME: really need to refresh?
      // currentSubplot.redraw();
      showPlot(currentSubplot);
    }
    overview.refresh();
  }

  /**
   * Update the visualizer menus.
   */
  private void updateVisualizerMenus() {
    visualizersMenu.removeAll();
    toolsMenu.removeAll();
    recursiveBuildMenu(visualizersMenu, context.getVisualizers().topIterator());
  }

  private void recursiveBuildMenu(JMenu parent, Iterator<VisualizerTreeItem> iter) {
    while(iter.hasNext()) {
      final VisualizerTreeItem item = iter.next();
      if(item instanceof Visualizer) {
        final Visualizer v = (Visualizer) item;
        // Currently enabled?
        boolean enabled = VisualizerUtil.isVisible(v);
        boolean istool = VisualizerUtil.isTool(v);
        final String name = v.getMetadata().getGenerics(Visualizer.META_NAME, String.class);
        if(!istool) {
          final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(name, enabled);
          visItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(@SuppressWarnings("unused") ItemEvent e) {
              toggleVisibility(v, visItem.getState());
            }
          });
          parent.add(visItem);
        }
        else {
          final JRadioButtonMenuItem visItem = new JRadioButtonMenuItem(name, enabled);
          visItem.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(@SuppressWarnings("unused") ItemEvent e) {
              toggleVisibility(v, visItem.isSelected());
            }
          });
          toolsMenu.add(visItem);
        }
      }
      else if(item instanceof VisualizerGroup) {
        VisualizerGroup group = (VisualizerGroup) item;
        if(group.size() > 0) {
          JMenu submenu = new JMenu(group.getName());
          recursiveBuildMenu(submenu, group.iterator());
          parent.add(submenu);
        }
      }
      else {
        logger.warning("Encountered VisualizerTreeItem that is neither a group nor a Visualizer: " + item);
      }
    }
  }

  protected void toggleVisibility(Visualizer v, boolean visibility) {
    // Hide other tools
    if(visibility && VisualizerUtil.isTool(v)) {
      for(Visualizer other : context.getVisualizers().getTools()) {
        logger.debug("Testing tool: " + other);
        if(other != v && VisualizerUtil.isVisible(other)) {
          logger.debug("Hiding tool: " + other);
          other.getMetadata().put(Visualizer.META_VISIBLE, false);
          context.fireContextChange(new VisualizerChangedEvent(context, other));
        }
      }
    }
    v.getMetadata().put(Visualizer.META_VISIBLE, visibility);
    context.fireContextChange(new VisualizerChangedEvent(context, v));
    // update();
  }

  @Override
  public void contextChanged(ContextChangedEvent e) {
    if(e instanceof VisualizerChangedEvent) {
      updateVisualizerMenus();
    }
  }
}