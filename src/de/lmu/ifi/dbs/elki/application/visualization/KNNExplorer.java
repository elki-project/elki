package de.lmu.ifi.dbs.elki.application.visualization;

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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;
import de.lmu.ifi.dbs.elki.visualization.batikutil.NodeReplacer;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.style.PropertiesBasedStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPath;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * User application to explore the k Nearest Neighbors for a given data set and
 * distance function. When selecting one or more data entries, the nearest
 * neighbors each are determined and visualized.
 * 
 * <p>
 * Reference:<br/>
 * 
 * Elke Achtert, Thomas Bernecker, Hans-Peter Kriegel, Erich Schubert, Arthur
 * Zimek:<br/>
 * 
 * ELKI in Time: ELKI 0.2 for the Performance Evaluation of Distance Measures
 * for Time Series.<br/>
 * 
 * In Proc. 11th International Symposium on Spatial and Temporal Databases (SSTD
 * 2009), Aalborg, Denmark, 2009.
 * </p>
 * 
 * <h3>Usage example:</h3>
 * 
 * <p>
 * Main invocation:<br/>
 * 
 * <code>java -cp elki.jar de.lmu.ifi.dbs.elki.application.visualization.KNNExplorer</code>
 * </p>
 * 
 * <p>
 * The application supports the usual parameterization, in particular parameters
 * <code>-dbc.in</code> and <code>-explorer.distancefunction</code> to select an
 * input file and the distance function to explore.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf ExplorerWindow
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Reference(authors = "E. Achtert, T. Bernecker, H.-P. Kriegel, E. Schubert, A. Zimek", title = "ELKI in Time: ELKI 0.2 for the Performance Evaluation of Distance Measures for Time Series", booktitle = "Proceedings of the 11th International Symposium on Spatial and Temporal Databases (SSTD), Aalborg, Denmark, 2009", url = "http://dx.doi.org/10.1007/978-3-642-02982-0_35")
public class KNNExplorer<O extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractApplication {
  // TODO: replace by a wrapper creating appropriate visualization modules.

  /**
   * Parameter to specify the distance function to determine the distance
   * between database objects, must extend
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
   * <p>
   * Key: {@code -explorer.distancefunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction}
   * </p>
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("explorer.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private Database database;

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_ID}.
   */
  private DistanceFunction<O, D> distanceFunction;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param database Database
   * @param distanceFunction Distance function
   */
  public KNNExplorer(boolean verbose, Database database, DistanceFunction<O, D> distanceFunction) {
    super(verbose);
    this.database = database;
    this.distanceFunction = distanceFunction;
  }

  @Override
  public void run() throws IllegalStateException {
    database.initialize();
    Relation<O> relation = database.getRelation(distanceFunction.getInputTypeRestriction());
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(relation, distanceFunction);
    (new ExplorerWindow()).run(database, distFunc);
  }

  /**
   * Main window of KNN Explorer.
   * 
   * @author Erich Schubert
   */
  class ExplorerWindow {
    /**
     * Default Window Title
     */
    private static final String WINDOW_TITLE_BASE = "ELKI k Nearest Neighbors Explorer";

    /**
     * Maximum resolution for plotted lines to improve performance for long time
     * series.
     */
    private static final int MAXRESOLUTION = 1000;

    /**
     * SVG graph object ID (for replacing)
     */
    private static final String SERIESID = "series";

    // The frame.
    protected JFrame frame = new JFrame(WINDOW_TITLE_BASE);

    // The spinner
    protected JSpinner spinner;

    // The list of series
    private JList seriesList = new JList();

    // The "Quit" button, to close the application.
    protected JButton quitButton = new JButton("Quit");

    // The "Export" button, to save the image
    protected JButton saveButton = new JButton("Export");

    // The SVG canvas.
    protected JSVGSynchronizedCanvas svgCanvas = new JSVGSynchronizedCanvas();

    // The plot
    SVGPlot plot;

    // Viewport
    Element viewport;

    // Dimensionality
    protected int dim;

    // k
    protected int k = 20;

    // Scale
    protected LinearScale s;

    // The current database
    protected Relation<? extends O> data;

    // Distance cache
    protected HashMap<DBID, Double> distancecache = new HashMap<DBID, Double>();

    // Canvas scaling ratio
    protected double ratio;

    /**
     * Holds the instance of the distance function specified by
     * {@link #DISTANCE_FUNCTION_ID}.
     */
    private DistanceQuery<O, D> distanceQuery;

    /**
     * Holds the associated kNN query
     */
    private KNNQuery<O, D> knnQuery;

    /**
     * The label representation
     */
    protected Relation<String> labelRep;

    private Database db;

    /**
     * Constructor.
     */
    public ExplorerWindow() {
      super();

      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

      // ELKI icon
      try {
        frame.setIconImage(new ImageIcon(KDDTask.class.getResource("elki-icon.png")).getImage());
      }
      catch(Exception e) {
        // Ignore - icon not found is not fatal.
      }
      
      // Create a panel and add the button, status label and the SVG canvas.
      final JPanel bigpanel = new JPanel(new BorderLayout());

      // set up spinner
      SpinnerModel model = new SpinnerNumberModel(k, 1, 1000, 1);
      spinner = new JSpinner(model);
      JPanel spinnerPanel = new JPanel(new BorderLayout());
      spinnerPanel.add(BorderLayout.WEST, new JLabel("k"));
      spinnerPanel.add(BorderLayout.EAST, spinner);

      // button panel
      JPanel buttonPanel = new JPanel(new BorderLayout());
      buttonPanel.add(BorderLayout.WEST, saveButton);
      buttonPanel.add(BorderLayout.EAST, quitButton);

      // set up cell renderer
      seriesList.setCellRenderer(new SeriesLabelRenderer());

      JPanel sidepanel = new JPanel(new BorderLayout());
      sidepanel.add(BorderLayout.NORTH, spinnerPanel);
      sidepanel.add(BorderLayout.CENTER, new JScrollPane(seriesList));
      sidepanel.add(BorderLayout.SOUTH, buttonPanel);

      bigpanel.add(BorderLayout.WEST, sidepanel);
      bigpanel.add(BorderLayout.CENTER, svgCanvas);

      frame.getContentPane().add(bigpanel);

      spinner.addChangeListener(new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
          updateK((Integer) (spinner.getValue()));
          updateSelection();
        }
      });

      seriesList.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if(!e.getValueIsAdjusting()) {
            updateSelection();
          }
        }
      });

      saveButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
          SVGSaveDialog.showSaveDialog(plot, 512, 512);
        }
      });

      quitButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          frame.setVisible(false);
          frame.dispose();
        }
      });

      // display
      frame.setSize(600, 600);

      // resize listener
      LazyCanvasResizer listener = new LazyCanvasResizer(frame) {
        @Override
        public void executeResize(double newratio) {
          ratio = newratio;
          updateSize();
          updateSelection();
        }
      };
      ratio = listener.getActiveRatio();
      frame.addComponentListener(listener);
    }

    /**
     * Update the SVG plot size.
     */
    public void updateSize() {
      SVGUtil.setAtt(plot.getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + ratio + " 1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_WIDTH_ATTRIBUTE, ratio);
      SVGUtil.setAtt(viewport, SVGConstants.SVG_HEIGHT_ATTRIBUTE, "1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, (-0.1 * StyleLibrary.SCALE) + " " + (-0.1 * StyleLibrary.SCALE) + " " + (ratio + 0.2) * StyleLibrary.SCALE + " " + 1.2 * StyleLibrary.SCALE);
    }

    /**
     * Process the given Database and distance function.
     * 
     * @param db Database
     * @param distanceQuery Distance function
     */
    public void run(Database db, DistanceQuery<O, D> distanceQuery) {
      this.db = db;
      this.data = distanceQuery.getRelation();
      this.labelRep = DatabaseUtil.guessObjectLabelRepresentation(db);
      this.dim = DatabaseUtil.dimensionality(distanceQuery.getRelation());
      this.distanceQuery = distanceQuery;
      this.updateK(k);

      double min = Double.MAX_VALUE;
      double max = Double.MIN_VALUE;
      for(DBID objID : data.iterDBIDs()) {
        O vec = data.get(objID);
        DoubleMinMax mm = VectorUtil.getRangeDouble(vec);
        min = Math.min(min, mm.getMin());
        max = Math.max(max, mm.getMax());
      }
      this.s = new LinearScale(min, max);

      this.frame.setTitle(distanceQuery.getClass().getSimpleName() + " - " + WINDOW_TITLE_BASE);

      plot = new SVGPlot();
      viewport = plot.svgElement(SVGConstants.SVG_SVG_TAG);
      plot.getRoot().appendChild(viewport);
      updateSize();

      try {
        StyleLibrary style = new PropertiesBasedStyleLibrary();
        SVGSimpleLinearAxis.drawAxis(plot, viewport, this.s, 0.0, StyleLibrary.SCALE, 0.0, 0.0, true, false, style);
      }
      catch(CSSNamingConflict e) {
        LoggingUtil.exception(e);
      }
      plot.updateStyleElement();

      // insert the actual data series.
      Element egroup = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(egroup, SVGConstants.SVG_ID_ATTRIBUTE, SERIESID);
      viewport.appendChild(egroup);
      plot.putIdElement(SERIESID, egroup);

      svgCanvas.setPlot(plot);

      DefaultListModel m = new DefaultListModel();
      for(DBID dbid : data.iterDBIDs()) {
        m.addElement(dbid);
      }
      seriesList.setModel(m);

      frame.setVisible(true);
    }

    void updateK(int k) {
      this.k = k;
      knnQuery = db.getKNNQuery(distanceQuery, k);
    }

    /**
     * Process the users new selection.
     */
    protected void updateSelection() {
      Object[] sel = seriesList.getSelectedValues();
      // prepare replacement tag.
      Element newe = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(newe, SVGConstants.SVG_ID_ATTRIBUTE, SERIESID);

      distancecache.clear();

      for(Object o : sel) {
        DBID idx = (DBID) o;

        KNNResult<D> knn = knnQuery.getKNNForDBID(idx, k);

        double maxdist = knn.getKNNDistance().doubleValue();
        // avoid division by zero.
        if(maxdist == 0) {
          maxdist = 1;
        }

        for (int i = knn.size() - 1; i >= 0; i--) {
          DistanceResultPair<D> pair = knn.get(i);
          Element line = plotSeries(pair.getDBID(), MAXRESOLUTION);
          double dist = pair.getDistance().doubleValue() / maxdist;
          Color color = getColor(dist);
          String colstr = "#" + Integer.toHexString(color.getRGB()).substring(2);
          String width = (pair.getDBID().equals(idx)) ? "0.5%" : "0.2%";
          SVGUtil.setStyle(line, "stroke: " + colstr + "; stroke-width: " + width + "; fill: none");
          newe.appendChild(line);
          // put into cache
          Double known = distancecache.get(pair.getDBID());
          if(known == null || dist < known) {
            distancecache.put(pair.getDBID(), dist);
          }
        }
      }
      plot.scheduleUpdate(new NodeReplacer(newe, plot, SERIESID));
      seriesList.repaint();
    }

    /**
     * Get the appropriate color for the given distance.
     * 
     * @param dist Distance
     * @return Color
     */
    Color getColor(double dist) {
      Color color = new Color((int) (255 * dist), 0, (int) (255 * (1.0 - dist)));
      return color;
    }

    /**
     * Plot a single time series.
     * 
     * @param idx Object index
     * @param resolution Maximum number of steps to plot
     * @return SVG element
     */
    private Element plotSeries(DBID idx, int resolution) {
      O series = data.get(idx);

      double step = 1.0;
      if(resolution < dim) {
        step = (double) dim / (double) resolution;
      }

      SVGPath path = new SVGPath();
      for(double id = 0; id < dim; id += step) {
        int i = (int) Math.floor(id);
        path.drawTo(StyleLibrary.SCALE * ratio * (((double) i) / (dim - 1)), StyleLibrary.SCALE * (1.0 - s.getScaled(series.doubleValue(i + 1))));
      }
      Element p = path.makeElement(plot);
      return p;
    }

    /**
     * Renderer for the labels, with coloring as in the plot.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    private class SeriesLabelRenderer extends DefaultListCellRenderer {
      /**
       * Serial version
       */
      private static final long serialVersionUID = 1L;

      /**
       * Constructor.
       */
      public SeriesLabelRenderer() {
        super();
      }

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        String label = null;
        if(label == null || label == "") {
          label = labelRep.get((DBID) value);
        }
        if(label == null || label == "") {
          label = ((DBID) value).toString();
        }
        // setText(label);
        Component renderer = super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
        Double known = distancecache.get(value);
        if(known != null) {
          setBackground(getColor(known));
        }
        return renderer;
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractApplication.Parameterizer {
    protected Database database = null;

    protected DistanceFunction<O, D> distanceFunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // parameter database connection
      final ObjectParameter<Database> dbpar = new ObjectParameter<Database>(OptionID.DATABASE_CONNECTION, Database.class, StaticArrayDatabase.class);
      if(config.grab(dbpar)) {
        database = dbpar.instantiateClass(config);
      }

      // Distance function
      final ClassParameter<DistanceFunction<O, D>> dfpar = new ClassParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(dfpar)) {
        distanceFunction = dfpar.instantiateClass(config);
      }
    }

    @Override
    protected KNNExplorer<O, D> makeInstance() {
      return new KNNExplorer<O, D>(verbose, database, distanceFunction);
    }
  }

  /**
   * Main method to run this application.
   * 
   * @param args the arguments to run this application
   */
  public static void main(String[] args) {
    runCLIApplication(KNNExplorer.class, args);
  }
}