package de.lmu.ifi.dbs.elki.visualization.gui;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import javax.swing.JComponent;
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
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
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
 * Yes, this is very basic and ad-hoc. Feel free to contribute something more
 * advanced to ELKI!
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
  private static final Logging LOG = Logging.getLogger(ResultWindow.class);

  /**
   * Dynamic menu.
   *
   * @apiviz.exclude
   */
  public class DynamicMenu {
    /**
     * Menubar component
     */
    private JMenuBar menubar;

    /**
     * File menu.
     */
    private JMenu filemenu;

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

    public DynamicMenu() {
      menubar = new JMenuBar();
      filemenu = new JMenu("File");
      filemenu.setMnemonic(KeyEvent.VK_F);

      // setup buttons
      if(!single) {
        overviewItem = new JMenuItem("Overview");
        overviewItem.setMnemonic(KeyEvent.VK_O);
        overviewItem.setEnabled(false);
        overviewItem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent ae) {
            showOverview();
          }
        });
        filemenu.add(overviewItem);
      }

      exportItem = new JMenuItem("Export");
      exportItem.setMnemonic(KeyEvent.VK_E);
      exportItem.setEnabled(false);
      exportItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
          saveCurrentPlot();
        }
      });
      filemenu.add(exportItem);

      editItem = new JMenuItem("Table View/Edit");
      editItem.setMnemonic(KeyEvent.VK_T);
      editItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
          showTableView();
        }
      });
      // FIXME: re-add when it is working again.
      // filemenu.add(editItem);

      quitItem = new JMenuItem("Quit");
      quitItem.setMnemonic(KeyEvent.VK_Q);
      quitItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          close();
        }
      });

      filemenu.add(quitItem);
      menubar.add(filemenu);

      visualizersMenu = new JMenu("Visualizers");
      visualizersMenu.setMnemonic(KeyEvent.VK_V);
      menubar.add(visualizersMenu);
    }

    /**
     * Update the visualizer menus.
     */
    protected void updateVisualizerMenus() {
      menubar.removeAll();
      menubar.add(filemenu);
      ResultHierarchy hier = context.getHierarchy();
      for(Hierarchy.Iter<Result> iter = hier.iterChildren(result); iter.valid(); iter.advance()) {
        recursiveBuildMenu(menubar, iter.get());
      }
    }

    private boolean recursiveBuildMenu(JComponent parent, Result r) {
      ResultHierarchy hier = context.getHierarchy();

      // Skip "adapter" results that do not have visualizers
      if(r instanceof ResultAdapter) {
        if(hier.numChildren(r) <= 0) {
          return false;
        }
      }
      // Make a submenu for this element
      boolean nochildren = true;
      JMenu submenu = new JMenu((r.getLongName() != null) ? r.getLongName() : "unnamed");
      // Add menus for any children
      for(Hierarchy.Iter<Result> iter = hier.iterChildren(r); iter.valid(); iter.advance()) {
        if(recursiveBuildMenu(submenu, iter.get())) {
          nochildren = false;
        }
      }

      // Item for the visualizer
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

    /**
     * Get the menu bar component.
     * 
     * @return Menu bar component
     */
    public JMenuBar getMenuBar() {
      return menubar;
    }

    /**
     * Enable / disable the overview menu.
     * 
     * @param b Flag
     */
    public void enableOverview(boolean b) {
      if(overviewItem != null) {
        overviewItem.setEnabled(b);
      }
    }

    /**
     * Enable / disable the export menu.
     * 
     * @param b Flag
     */
    public void enableExport(boolean b) {
      exportItem.setEnabled(b);
    }
  }

  private DynamicMenu menubar;

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
   * Single view mode. No overview / detail view split
   */
  private boolean single = false;

  /**
   * Constructor.
   * 
   * @param title Window title
   * @param result Result to visualize
   * @param context Visualizer context
   * @param single Single visualization mode
   */
  public ResultWindow(String title, HierarchicalResult result, VisualizerContext context, boolean single) {
    super(title);
    this.context = context;
    this.result = result;
    this.single = single;

    // close handler
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    // ELKI icon
    try {
      setIconImage(new ImageIcon(KDDTask.class.getResource("elki-icon.png")).getImage());
    }
    catch(Exception e) {
      // Ignore - icon not found is not fatal.
    }

    // Create a panel and add the button, status label and the SVG canvas.
    final JPanel panel = new JPanel(new BorderLayout());

    menubar = new DynamicMenu();
    panel.add("North", menubar.getMenuBar());

    svgCanvas = new JSVGSynchronizedCanvas();
    panel.add("Center", svgCanvas);

    this.getContentPane().add(panel);

    this.overview = new OverviewPlot(result, context, single);
    // when a subplot is clicked, show the selected subplot.
    overview.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(e instanceof DetailViewSelectedEvent) {
          showSubplot((DetailViewSelectedEvent) e);
        }
        if(OverviewPlot.OVERVIEW_REFRESHED.equals(e.getActionCommand())) {
          if(currentSubplot == null) {
            svgCanvas.setPlot(overview.getPlot());
          }
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
    LazyCanvasResizer listener = new LazyCanvasResizer(this, 0.1) {
      @Override
      public void executeResize(double newratio) {
        ResultWindow.this.handleResize(newratio);
      }
    };
    this.overview.initialize(listener.getCurrentRatio());

    this.addComponentListener(listener);

    context.addResultListener(this);

    // update();
    menubar.updateVisualizerMenus();
  }

  @Override
  public void dispose() {
    context.removeResultListener(this);
    svgCanvas.setPlot(null);
    overview.destroy();
    if(currentSubplot != null) {
      currentSubplot.dispose();
      currentSubplot = null;
    }
    super.dispose();
  }

  /**
   * Close the visualizer window.
   */
  protected void close() {
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
    showPlot(overview.getPlot());
  }

  /**
   * Navigate to a subplot.
   * 
   * @param e
   */
  protected void showSubplot(DetailViewSelectedEvent e) {
    if(!single) {
      currentSubplot = e.makeDetailView();
      showPlot(currentSubplot);
    }
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
    menubar.enableOverview(plot != overview.getPlot());
    menubar.enableExport(plot != null);
  }

  /**
   * Save/export the current plot.
   */
  protected void saveCurrentPlot() {
    final SVGPlot currentPlot = svgCanvas.getPlot();
    if(currentPlot == null) {
      LOG.warning("saveCurrentPlot() called without a visible plot!");
      return;
    }
    SVGSaveDialog.showSaveDialog(currentPlot, 512, 512);
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
    menubar.updateVisualizerMenus();
    if(currentSubplot != null) {
      // FIXME: really need to refresh?
      // currentSubplot.redraw();
      showPlot(currentSubplot);
    }
    overview.lazyRefresh();
  }

  /**
   * Handle a resize event.
   *
   * @param newratio New window size ratio.
   */
  protected void handleResize(double newratio) {
    if(currentSubplot == null) {
      ResultWindow.this.overview.setRatio(newratio);
    }
  }

  private JMenuItem makeMenuItemForVisualizer(Result r) {
    if(VisualizationTask.class.isInstance(r)) {
      final VisualizationTask v = (VisualizationTask) r;
      JMenuItem item;

      // Currently enabled?
      final String name = v.getLongName();
      boolean enabled = v.visible;
      boolean istool = v.tool;
      if(!istool) {
        final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(name, enabled);
        visItem.addItemListener(new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            // We need SwingUtilities to avoid a deadlock!
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                VisualizerUtil.setVisible(context, v, visItem.getState());
              }
            });
          }
        });
        item = visItem;
      }
      else {
        final JRadioButtonMenuItem visItem = new JRadioButtonMenuItem(name, enabled);
        visItem.addItemListener(new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            // We need SwingUtilities to avoid a deadlock!
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                VisualizerUtil.setVisible(context, v, visItem.isSelected());
              }
            });
          }
        });
        item = visItem;
      }
      if(v.hasoptions) {
        final JMenu menu = new JMenu(name);
        menu.add(item);
        // TODO: build a menu for the visualizer!
        return menu;
      }
      else {
        return item;
      }
    }
    return null;
  }

  @Override
  public void resultAdded(Result child, Result parent) {
    menubar.updateVisualizerMenus();
  }

  @Override
  public void resultChanged(Result current) {
    menubar.updateVisualizerMenus();
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    menubar.updateVisualizerMenus();
  }
}