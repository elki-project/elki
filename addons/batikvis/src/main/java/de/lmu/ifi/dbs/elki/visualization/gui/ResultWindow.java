/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultListener;
import de.lmu.ifi.dbs.elki.result.ResultWriter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.visualization.VisualizationItem;
import de.lmu.ifi.dbs.elki.visualization.VisualizationListener;
import de.lmu.ifi.dbs.elki.visualization.VisualizationMenuAction;
import de.lmu.ifi.dbs.elki.visualization.VisualizationMenuToggle;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
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
   * Dynamic menu.
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
     * The "Export Image" button, to save the image
     */
    private JMenuItem exportItem;

    /**
     * The "Write to CSV" button, to invoce the text writer.
     */
    private JMenuItem writeItem;

    /**
     * The "tabular edit" item.
     */
    // private JMenuItem editItem;

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
        overviewItem.addActionListener((e) -> showOverview());
        filemenu.add(overviewItem);
      }

      exportItem = new JMenuItem("Export Plot");
      exportItem.setMnemonic(KeyEvent.VK_E);
      exportItem.setEnabled(false);
      exportItem.addActionListener((e) -> saveCurrentPlot());
      filemenu.add(exportItem);

      writeItem = new JMenuItem("Write Data to Folder");
      writeItem.setMnemonic(KeyEvent.VK_W);
      writeItem.addActionListener((e) -> invokeTextWriter());
      filemenu.add(writeItem);

      // editItem = new JMenuItem("Table View/Edit");
      // editItem.setMnemonic(KeyEvent.VK_T);
      // editItem.addActionListener((e) -> showTableView());
      // FIXME: re-add, only for dynamic database, when it is functional again.
      // filemenu.add(editItem);

      quitItem = new JMenuItem("Quit");
      quitItem.setMnemonic(KeyEvent.VK_Q);
      quitItem.addActionListener((e) -> close());

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
        visItem.addActionListener((e) -> action.activate());
        return visItem;
      }
      if(r instanceof VisualizationMenuToggle) {
        final VisualizationMenuToggle toggle = (VisualizationMenuToggle) r;
        final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(toggle.getMenuName(), toggle.active());
        visItem.setEnabled(toggle.enabled());
        visItem.addItemListener((e) -> toggle.toggle());
        return visItem;
      }
      if(!(r instanceof VisualizationTask)) {
        return null;
      }
      final VisualizationTask v = (VisualizationTask) r;
      JMenuItem item;

      // Currently enabled?
      final String name = v.getMenuName();
      boolean enabled = v.isVisible(), istool = v.isTool();
      if(!istool) {
        final JCheckBoxMenuItem visItem = new JCheckBoxMenuItem(name, enabled);
        visItem.addItemListener((e) -> VisualizationTree.setVisible(context, v, visItem.getState()));
        item = visItem;
      }
      else {
        final JRadioButtonMenuItem visItem = new JRadioButtonMenuItem(name, enabled);
        visItem.addItemListener((e) -> VisualizationTree.setVisible(context, v, visItem.isSelected()));
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

    /**
     * Enable / disable the writer menu.
     *
     * @param b Flag
     */
    public void enableWriter(boolean b) {
      writeItem.setEnabled(b);
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
    overview.addActionListener((e) -> {
      if(e instanceof DetailViewSelectedEvent) {
        showSubplot((DetailViewSelectedEvent) e);
      }
      if(OverviewPlot.OVERVIEW_REFRESHING == e.getActionCommand() && currentSubplot == null) {
        showPlot(null);
      }
      if(OverviewPlot.OVERVIEW_REFRESHED == e.getActionCommand() && currentSubplot == null) {
        showOverview();
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

    // key commands
    KeyStroke ctrle = KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK);
    KeyStroke ctrls = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlq = KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlw = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
    KeyStroke ctrlo = KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
    panel.registerKeyboardAction((e) -> saveCurrentPlot(), ctrle, JComponent.WHEN_IN_FOCUSED_WINDOW);
    panel.registerKeyboardAction((e) -> saveCurrentPlot(), ctrls, JComponent.WHEN_IN_FOCUSED_WINDOW);
    panel.registerKeyboardAction((e) -> close(), ctrlq, JComponent.WHEN_IN_FOCUSED_WINDOW);
    panel.registerKeyboardAction((e) -> close(), ctrlw, JComponent.WHEN_IN_FOCUSED_WINDOW);
    panel.registerKeyboardAction((e) -> showOverview(), ctrlo, JComponent.WHEN_IN_FOCUSED_WINDOW);

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
    if(currentPlot != null) {
      SVGSaveDialog.showSaveDialog(currentPlot, 512, 512);
    }
  }

  /**
   * Save/export the current plot.
   */
  protected void invokeTextWriter() {
    JOptionPane.showMessageDialog(this, //
        "This function is a minimal call to TextWriter with default options.\n" + //
            "You currently cannot select what data is written, or how.\n" + //
            "Some results cannot be written to text files at all.\n" + //
            "There will not be a 'success' or 'failure' message in the UI.\n" + //
            "For full control, please use the Java API.", "Notice", JOptionPane.WARNING_MESSAGE);

    JFileChooser fc = new JFileChooser(new File("."));
    fc.setDialogTitle("Choose Folder to Write to");
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    TextWriterPanel optionsPanel = new TextWriterPanel();
    fc.setAccessory(optionsPanel);
    int ret = fc.showSaveDialog(null);
    if(ret == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      boolean gzip = optionsPanel.compress.isSelected();
      String filtertext = optionsPanel.filterField.getText();
      try {
        Pattern filter = filtertext.isEmpty() ? null : Pattern.compile(filtertext);
        new ResultWriter(file, gzip, false, filter).processNewResult(context.getHierarchy(), context.getBaseResult());
      }
      catch(PatternSyntaxException e) {
        JOptionPane.showMessageDialog(this, "Filter pattern was not a valid regular expression.", //
            "Pattern error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /**
   * Simple configuration panel for the text output.
   *
   * @author Erich Schubert
   */
  private class TextWriterPanel extends JPanel {
    /**
     * Serial version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Compression option.
     */
    JCheckBox compress;

    /**
     * Filter text field.
     */
    JTextField filterField;

    /**
     * Constructor.
     */
    public TextWriterPanel() {
      this.setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.anchor = GridBagConstraints.WEST;
      c.ipadx = 2;

      this.add(compress = new JCheckBox("Compress output (gzip)"), c);
      compress.setSelected(true);

      this.add(new JLabel("Result filter:"), c);
      c.fill = GridBagConstraints.HORIZONTAL;
      this.add(filterField = new JTextField(), c);
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
    SwingUtilities.invokeLater(() -> menubar.updateVisualizerMenus());
  }
}
