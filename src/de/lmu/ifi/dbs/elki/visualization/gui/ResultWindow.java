package de.lmu.ifi.dbs.elki.visualization.gui;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultAdapter;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;
import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.DetailViewSelectedEvent;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.OverviewPlot;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Swing window to manage a particular result visualization.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * 
 * @apiviz.composedOf JSVGSynchronizedCanvas
 * @apiviz.composedOf OverviewPlot
 * @apiviz.composedOf SelectionTableWindow
 * @apiviz.composedOf SVGSaveDialog
 * @apiviz.composedOf LazyCanvasResizer
 * @apiviz.has VisualizerContext
 * @apiviz.uses DetailView oneway
 * @apiviz.uses DetailViewSelectedEvent oneway - - reacts to
 */
public class ResultWindow extends JFrame implements ResultListener {
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
   * The "tabular edit" item.
   */
  private JMenuItem editItem;

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
  private OverviewPlot overview;

  /**
   * Visualizer context
   */
  protected VisualizerContext context;

  /**
   * Currently selected subplot.
   */
  private DetailView currentSubplot = null;

  /**
   * Result to visualize
   */
  private HierarchicalResult result;

  /**
   * Constructor.
   * 
   * @param title Window title
   * @param result Result to visualize
   * @param context Visualizer context
   */
  public ResultWindow(String title, HierarchicalResult result, VisualizerContext context) {
    super(title);
    this.context = context;
    this.result = result;

    // close handler
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // ELKI icon
    try {
      setIconImage(new ImageIcon(KDDTask.class.getResource("elki-icon.png")).getImage());
    }
    catch(Exception e) {
      // Ignore - icon not found is not fatal.
    }
    
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

    editItem = new JMenuItem("Table View/Edit");
    editItem.setMnemonic(KeyEvent.VK_T);
    editItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
        showTableView();
      }
    });

    // Create a panel and add the button, status label and the SVG canvas.
    final JPanel panel = new JPanel(new BorderLayout());

    JMenuBar menubar = new JMenuBar();
    JMenu filemenu = new JMenu("File");
    filemenu.setMnemonic(KeyEvent.VK_F);
    filemenu.add(overviewItem);
    filemenu.add(exportItem);
    // FIXME: re-add when it is working again.
    // filemenu.add(editItem);
    filemenu.add(quitItem);
    menubar.add(filemenu);

    visualizersMenu = new JMenu("Visualizers");
    visualizersMenu.setMnemonic(KeyEvent.VK_V);
    menubar.add(visualizersMenu);

    panel.add("North", menubar);

    svgCanvas = new JSVGSynchronizedCanvas();
    panel.add("Center", svgCanvas);

    this.getContentPane().add(panel);

    this.overview = new OverviewPlot(result, context);
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
    this.setSize(dim.width - 50, dim.height - 50);
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

    context.addResultListener(this);

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
    context.removeResultListener(this);
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
   * Show a tabular view
   */
  protected void showTableView() {
    (new SelectionTableWindow(context)).setVisible(true);
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
    overview.lazyRefresh();
  }

  /**
   * Update the visualizer menus.
   */
  private void updateVisualizerMenus() {
    visualizersMenu.removeAll();
    ResultHierarchy hier = context.getHierarchy();
    for(Result child : hier.getChildren(result)) {
      recursiveBuildMenu(visualizersMenu, child);
    }
  }

  private boolean recursiveBuildMenu(JMenu parent, Result r) {
    ResultHierarchy hier = context.getHierarchy();

    // Skip "adapter" results that do not have visualizers
    if(r instanceof ResultAdapter) {
      if(hier.getChildren(r).size() <= 0) {
        return false;
      }
    }
    // Make a submenu for this element
    boolean nochildren = true;
    JMenu submenu = new JMenu((r.getLongName() != null) ? r.getLongName() : "unnamed");
    // Add menus for any children
    for(Result child : hier.getChildren(r)) {
      if(recursiveBuildMenu(submenu, child)) {
        nochildren = false;
      }
    }
    // Item for a visualizer
    JMenuItem item = makeMenuItemForVisualizer(r);
    if(nochildren) {
      if(item != null) {
        parent.add(item);
      }
      else {
        JMenuItem noresults = new JMenuItem("no visualizers");
        noresults.setEnabled(false);
        submenu.add(noresults);
      }
    }
    else {
      if(item != null) {
        submenu.add(item, 0);
      }
      parent.add(submenu);
    }
    return true;
  }

  public JMenuItem makeMenuItemForVisualizer(Result r) {
    if(VisualizationTask.class.isInstance(r)) {
      final VisualizationTask v = (VisualizationTask) r;
      // Currently enabled?
      final String name = v.getLongName();
      boolean enabled = VisualizerUtil.isVisible(v);
      boolean istool = VisualizerUtil.isTool(v);
      if(!istool) {
        final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(name, enabled);
        visItem.addItemListener(new ItemListener() {
          @Override
          public void itemStateChanged(@SuppressWarnings("unused") ItemEvent e) {
            // We need SwingUtilities to avoid a deadlock!
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                context.setVisualizationVisibility(v, visItem.getState());
              }
            });
          }
        });
        return visItem;
      }
      else {
        final JRadioButtonMenuItem visItem = new JRadioButtonMenuItem(name, enabled);
        visItem.addItemListener(new ItemListener() {
          @Override
          public void itemStateChanged(@SuppressWarnings("unused") ItemEvent e) {
            // We need SwingUtilities to avoid a deadlock!
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                context.setVisualizationVisibility(v, visItem.isSelected());
              }
            });
          }
        });
        return visItem;
      }
    }
    return null;
  }

  @SuppressWarnings("unused")
  @Override
  public void resultAdded(Result child, Result parent) {
    updateVisualizerMenus();
  }

  @SuppressWarnings("unused")
  @Override
  public void resultChanged(Result current) {
    updateVisualizerMenus();
  }

  @SuppressWarnings("unused")
  @Override
  public void resultRemoved(Result child, Result parent) {
    updateVisualizerMenus();
  }
}