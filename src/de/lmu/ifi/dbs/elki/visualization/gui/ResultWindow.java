package de.lmu.ifi.dbs.elki.visualization.gui;

import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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
   * The "Visualizers" button, to enable/disable visualizers
   */
  private JButton visualizersButton;

  /**
   * The Popup menu for Visualizers
   */
  private PopupMenu popup;

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

    visualizersButton = new JButton("Visualizers");
    visualizersButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        selectVisualizers(e);
      }
    });

    // Create a panel and add the button, status label and the SVG canvas.
    final JPanel panel = new JPanel(new BorderLayout());

    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
    p.add(overviewButton);
    p.add(visualizersButton);
    p.add(saveButton);
    p.add(quitButton);

    panel.add("North", p);

    svgCanvas = new JSVGSynchronizedCanvas();
    panel.add("Center", svgCanvas);

    this.getContentPane().add(panel);

    this.setSize(600, 600);
    // Maximize.
    this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);

    this.overview = new OverviewPlot<DoubleVector>(db, result);
    // when a subplot is clicked, show the selected subplot.
    overview.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(e instanceof SubplotSelectedEvent) {
          SubplotSelectedEvent se = (SubplotSelectedEvent) e;
          showPlot(se.makeSubplot());
        }
      }
    });

    // Prepare popup menu
    this.popup = new PopupMenu("Visualizers");
    this.add(popup);

    // handle screen size
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    this.overview.screenwidth = dim.width;
    this.overview.screenheight = dim.height;

    // Visualizers
    this.visualizers = new ArrayList<Visualizer>();
  }

  /**
   * Visualization popup button triggered.
   * 
   * @param event
   */
  protected void selectVisualizers(MouseEvent event) {
    popup.show(event.getComponent(), event.getX(), event.getY());
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
    overview.refresh();
    updatePopupMenu();
  }

  /**
   * Update the popup menu.
   */
  private void updatePopupMenu() {
    popup.removeAll();
    for(final Visualizer v : visualizers) {
      Boolean enabled = v.getMetadata().getGenerics(Visualizer.META_VISIBLE, Boolean.class);
      if (enabled == null) {
        enabled = v.getMetadata().getGenerics(Visualizer.META_VISIBLE_DEFAULT, Boolean.class);
      }
      if (enabled == null) {
        enabled = true;
      }
      final String name = v.getMetadata().getGenerics(Visualizer.META_NAME, String.class);
      final CheckboxMenuItem visItem = new CheckboxMenuItem(name, enabled);
      visItem.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(@SuppressWarnings("unused") ItemEvent e) {
          v.getMetadata().put(Visualizer.META_VISIBLE, visItem.getState());
          update();
        }
      });
      popup.add(visItem);
    }
  }
}