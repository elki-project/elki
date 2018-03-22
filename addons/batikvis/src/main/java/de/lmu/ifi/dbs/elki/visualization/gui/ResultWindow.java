/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2018
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.*;

import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.visualization.*;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;
import de.lmu.ifi.dbs.elki.visualization.gui.detail.DetailView;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.DetailViewSelectedEvent;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.OverviewPlot;
import de.lmu.ifi.dbs.elki.visualization.gui.overview.PlotItem;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.projector.Projector;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * Swing window to manage a particular result visualization.
 *
 * Yes, this is very basic and ad-hoc. Feel free to contribute something more
 * advanced to ELKI!
 *
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 * @since 0.3
 *
 * @composed - - - JSVGSynchronizedCanvas
 * @composed - - - OverviewPlot
 * @composed - - - SelectionTableWindow
 * @composed - - - SVGSaveDialog
 * @composed - - - LazyCanvasResizer
 * @has - - - VisualizerContext
 * @navassoc - - - DetailView
 * @navassoc - "reacts to" - DetailViewSelectedEvent
 */
public class ResultWindow extends JFrame implements ResultListener, VisualizationListener {
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
   * @hidden
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

    /**
     * Simplify the menu.
     */
    protected boolean simplify = true;

    /**
     * Constructor.
     */
    public DynamicMenu() {
      menubar = new JMenuBar();
      filemenu = new JMenu("File");
      filemenu.setMnemonic(KeyEvent.VK_F);

      // setup buttons
      if(!single) {
        overviewItem = new JMenuItem("Open Overview");
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

      exportItem = new JMenuItem("Export Plot");
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
    protected synchronized void updateVisualizerMenus() {
      Projection proj = null;
      if(svgCanvas.getPlot() instanceof DetailView) {
        PlotItem item = ((DetailView) svgCanvas.getPlot()).getPlotItem();
        proj = item.proj;
      }
      menubar.removeAll();
      menubar.add(filemenu);
      ResultHierarchy hier = context.getHierarchy();
      Hierarchy<Object> vistree = context.getVisHierarchy();
      Result start = context.getBaseResult();
      ArrayList<JMenuItem> items = new ArrayList<>();
      if(start == null) {
        for(It<Result> iter = hier.iterAll(); iter.valid(); iter.advance()) {
          if(hier.numParents(iter.get()) == 0) {
            recursiveBuildMenu(items, iter.get(), hier, vistree, proj);
          }
        }
      }
      else {
        for(It<Result> iter = hier.iterChildren(start); iter.valid(); iter.advance()) {
          recursiveBuildMenu(items, iter.get(), hier, vistree, proj);
        }
      }
      // Add all items.
      for(JMenuItem item : items) {
        menubar.add(item);
      }
      menubar.revalidate();
      menubar.repaint();
    }

    private void recursiveBuildMenu(Collection<JMenuItem> items, Object r, ResultHierarchy hier, Hierarchy<Object> vistree, Projection proj) {
      // Make a submenu for this element
      final String nam;
      if(r instanceof Result) {
        nam = ((Result) r).getLongName();
      }
      else if(r instanceof VisualizationItem) {
        nam = ((VisualizationItem) r).getMenuName();
      }
      else {
        return;
      }
      ArrayList<JMenuItem> subitems = new ArrayList<>();
      // Add menus for any child results
      if(r instanceof Result) {
        for(It<Result> iter = hier.iterChildren((Result) r); iter.valid(); iter.advance()) {
          recursiveBuildMenu(subitems, iter.get(), hier, vistree, proj);
        }
      }
      // Add visualizers:
      for(It<Object> iter = vistree.iterChildren(r); iter.valid(); iter.advance()) {
        recursiveBuildMenu(subitems, iter.get(), hier, vistree, proj);
      }

      // Item for the visualizer
      JMenuItem item = null;
      if(proj == null) {
        item = makeMenuItemForVisualizer(r);
      }
      else {
        // Only include items that belong to different projections:
        for(It<Projector> iter = vistree.iterAncestorsSelf(r).filter(Projector.class); iter.valid(); iter.advance()) {
          if(iter.get() == proj.getProjector()) {
            item = makeMenuItemForVisualizer(r);
            break;
          }
        }
      }
      final int numchild = subitems.size();
      if(numchild == 0) {
        if(item != null) {
          items.add(item);
        }
        return;
      }
      if(simplify && numchild == 1) {
        JMenuItem a = subitems.get(0);
        if(a instanceof JMenu) {
          if(nam != null) {
            a.setText(nam + " " + a.getText());
          }
          items.add(a);
          return;
        }
      }
      JMenu submenu = new JMenu((nam != null) ? nam : "unnamed");
      if(item != null) {
        submenu.add(item);
      }
      for(JMenuItem subitem : subitems) {
        submenu.add(subitem);
      }
      items.add(submenu);
    }

    private JMenuItem makeMenuItemForVisualizer(Object r) {
      if(r instanceof VisualizationMenuAction) {
        final VisualizationMenuAction action = (VisualizationMenuAction) r;
        JMenuItem visItem = new JMenuItem(action.getMenuName());
        visItem.setEnabled(action.enabled());
        visItem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            action.activate();
          }
        });
        return visItem;
      }
      if(r instanceof VisualizationMenuToggle) {
        final VisualizationMenuToggle toggle = (VisualizationMenuToggle) r;
        final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(toggle.getMenuName(), toggle.active());
        visItem.setEnabled(toggle.enabled());
        visItem.addItemListener(new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            toggle.toggle();
          }
        });
        return visItem;
      }
      if(!(r instanceof VisualizationTask)) {
        return null;
      }
      final VisualizationTask v = (VisualizationTask) r;
      JMenuItem item;

      // Currently enabled?
      final String name = v.getMenuName();
      boolean enabled = v.isVisible();
      boolean istool = v.isTool();
      if(!istool) {
        final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(name, enabled);
        visItem.addItemListener(new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            VisualizationTree.setVisible(context, v, visItem.getState());
          }
        });
        item = visItem;
      }
      else {
        final JRadioButtonMenuItem visItem = new JRadioButtonMenuItem(name, enabled);
        visItem.addItemListener(new ItemListener() {
          @Override
          public void itemStateChanged(ItemEvent e) {
            VisualizationTree.setVisible(context, v, visItem.isSelected());
          }
        });
        item = visItem;
      }
      return item;
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
   * Single view mode. No overview / detail view split
   */
  private boolean single = false;

  /**
   * Constructor.
   *
   * @param title Window title
   * @param context Visualizer context
   * @param single Single visualization mode
   */
  public ResultWindow(String title, VisualizerContext context, boolean single) {
    super(title);
    this.context = context;
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

    overview = new OverviewPlot(context, single);
    // when a subplot is clicked, show the selected subplot.
    overview.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(e instanceof DetailViewSelectedEvent) {
          showSubplot((DetailViewSelectedEvent) e);
        }
        if(OverviewPlot.OVERVIEW_REFRESHING == e.getActionCommand() && currentSubplot == null) {
          showPlot(null);
        }
        if(OverviewPlot.OVERVIEW_REFRESHED == e.getActionCommand() && currentSubplot == null) {
          showOverview();
        }
      }
    });

    // handle screen size
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    overview.screenwidth = dim.width;
    overview.screenheight = dim.height;

    // Maximize.
    this.setSize(dim.width - 50, dim.height - 50);
    this.setExtendedState(JFrame.MAXIMIZED_BOTH);

    // resize listener
    final LazyCanvasResizer listener = new LazyCanvasResizer(this, 0.1) {
      @Override
      public void executeResize(double newratio) {
        ResultWindow.this.handleResize(newratio);
      }
    };
    this.addComponentListener(listener);
    svgCanvas.addGVTTreeBuilderListener(new GVTTreeBuilderAdapter() {
      @Override
      public void gvtBuildCompleted(GVTTreeBuilderEvent arg0) {
        // Supposedly in Swing thread.
        menubar.updateVisualizerMenus();
      }
    });

    context.addResultListener(this);
    context.addVisualizationListener(this);
    overview.initialize(listener.getCurrentRatio());
  }

  @Override
  public void dispose() {
    context.removeResultListener(this);
    context.removeVisualizationListener(this);
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
  private void showPlot(final SVGPlot plot) {
    if(svgCanvas.getPlot() instanceof DetailView) {
      ((DetailView) svgCanvas.getPlot()).destroy();
    }
    svgCanvas.setPlot(plot);
    menubar.enableOverview(plot != overview.getPlot());
    menubar.enableExport(plot != null);
    updateVisualizerMenus();
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
    updateVisualizerMenus();
    if(currentSubplot != null) {
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

  @Override
  public void resultAdded(Result child, Result parent) {
    updateVisualizerMenus();
  }

  @Override
  public void resultChanged(Result current) {
    updateVisualizerMenus();
  }

  @Override
  public void resultRemoved(Result child, Result parent) {
    updateVisualizerMenus();
  }

  @Override
  public void visualizationChanged(VisualizationItem item) {
    updateVisualizerMenus();
  }

  /**
   * Update visualizer menus, but only from Swing thread.
   */
  private void updateVisualizerMenus() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        menubar.updateVisualizerMenus();
      }
    });
  }
}
