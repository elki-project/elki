package de.lmu.ifi.dbs.elki.application.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
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

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.AbstractJSVGComponent;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;
import de.lmu.ifi.dbs.elki.visualization.batikutil.NodeReplacer;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * User application to explore the k Nearest Neighbors for a given data set and
 * distance function. When selecting one or more data entries, the nearest
 * neighbors each are determined and visualized.
 * 
 * <p>Reference:<br/>
 * 
 * Elke Achtert, Thomas Bernecker, Hans-Peter Kriegel, Erich Schubert, Arthur
 * Zimek:<br/>
 * 
 * ELKI in Time: ELKI 0.2 for the Performance Evaluation of Distance Measures
 * for Time Series.<br/>
 * 
 * In Proc. 11th International Symposium on Spatial and Temporal Databases (SSTD
 * 2009), Aalborg, Denmark, 2009.</p>
 * 
 * <h3>Usage example:</h3>
 * 
 * <p>Main invocation:<br/>
 * 
 * <code>java -cp elki.jar de.lmu.ifi.dbs.elki.visualization.KNNExplorer</code></p>
 * 
 * <p>The application supports the usual parametrization, in particular parameters
 * <code>-dbc.in</code> and <code>-explorer.distancefunction</code> to select an input file
 * and the distance function to explore.</p>
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class KNNExplorer<O extends NumberVector<O, ?>, N extends NumberDistance<N, D>, D extends Number> extends AbstractApplication {
  /**
   * Parameter to specify the database connection to be used, must extend
   * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection}.
   * <p>
   * Key: {@code -dbc}
   * </p>
   * <p>
   * Default value: {@link FileBasedDatabaseConnection}
   * </p>
   */
  private final ClassParameter<DatabaseConnection<O>> DATABASE_CONNECTION_PARAM = new ClassParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class.getName());

  /**
   * Optional Parameter to specify a normalization in order to use a database
   * with normalized values.
   * <p>
   * Key: {@code -norm}
   * </p>
   */
  private final ClassParameter<Normalization<O>> NORMALIZATION_PARAM = new ClassParameter<Normalization<O>>(OptionID.NORMALIZATION, Normalization.class, true);

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("explorer.distancefunction", "Distance function to determine the distance between database objects.");

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
  protected final ClassParameter<DistanceFunction<O, N>> DISTANCE_FUNCTION_PARAM = new ClassParameter<DistanceFunction<O, N>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class.getName());

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection<O> databaseConnection;

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_PARAM}.
   */
  private DistanceFunction<O, N> distanceFunction;

  /**
   * A normalization - per default no normalization is used.
   */
  private Normalization<O> normalization = null;

  public KNNExplorer() {
    super();

    // parameter database connection
    addOption(DATABASE_CONNECTION_PARAM);

    // Distance function
    addOption(DISTANCE_FUNCTION_PARAM);

    // parameter normalization
    addOption(NORMALIZATION_PARAM);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // database connection
    databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass();
    addParameterizable(databaseConnection);
    remainingParameters = databaseConnection.setParameters(remainingParameters);

    // instanciate distance function.
    distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass();
    addParameterizable(distanceFunction);
    remainingParameters = distanceFunction.setParameters(remainingParameters);

    // normalization
    if(NORMALIZATION_PARAM.isSet()) {
      normalization = NORMALIZATION_PARAM.instantiateClass();
      addParameterizable(normalization);
      remainingParameters = normalization.setParameters(remainingParameters);
    }

    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  @Override
  public void run() throws IllegalStateException {
    Database<O> db = databaseConnection.getDatabase(normalization);
    (new ExplorerWindow()).run(db, distanceFunction);
  }
  
  /**
   * Main method to run this wrapper.
   * 
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    new KNNExplorer<DoubleVector, DoubleDistance, Double>().runCLIApplication(args);
  }


  class ExplorerWindow extends AbstractLoggable {
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
    protected JFrame frame = new JFrame("ELKI k Nearest Neighbors Explorer");

    // The spinner
    protected JSpinner spinner;

    // The list of series
    private JList seriesList = new JList();

    // The "Quit" button, to close the application.
    protected JButton quitButton = new JButton("Quit");

    // The "Export" button, to save the image
    protected JButton saveButton = new JButton("Export");

    // The SVG canvas.
    protected JSVGCanvas svgCanvas = new JSVGCanvas();

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
    protected Database<O> db;

    // Distance cache
    protected HashMap<Integer, Double> distancecache = new HashMap<Integer, Double>();
    
    // Canvas scaling ratio
    protected double ratio;

    /**
     * Holds the instance of the distance function specified by
     * {@link #DISTANCE_FUNCTION_PARAM}.
     */
    private DistanceFunction<O, N> distanceFunction;

    public ExplorerWindow() {
      super(false);

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
        public void stateChanged(@SuppressWarnings("unused") ChangeEvent e) {
          k = (Integer) (spinner.getValue());
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
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae) {
          SVGSaveDialog.showSaveDialog(plot, 512, 512);
        }
      });

      quitButton.addActionListener(new ActionListener() {
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
          System.exit(0);
        }
      });

      // close handler
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(@SuppressWarnings("unused") WindowEvent e) {
          System.exit(0);
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
    
    public void updateSize() {
      SVGUtil.setAtt(plot.getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 "+ratio+" 1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_WIDTH_ATTRIBUTE, ratio);
      SVGUtil.setAtt(viewport, SVGConstants.SVG_HEIGHT_ATTRIBUTE, "1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "-0.05 -0.05 "+(ratio+0.1)+" 1.1");     
    }

    public void run(Database<O> db, DistanceFunction<O, N> distanceFunction) {
      this.db = db;
      this.dim = db.dimensionality();
      this.distanceFunction = distanceFunction;

      double min = Double.MAX_VALUE;
      double max = Double.MIN_VALUE;
      for(Integer objID : db) {
        O vec = db.get(objID);
        double[] mm = vec.getRange();
        min = Math.min(min, mm[0]);
        max = Math.max(max, mm[1]);
      }
      this.s = new LinearScale(min, max);

      plot = new SVGPlot();
      viewport = plot.svgElement(SVGConstants.SVG_SVG_TAG);
      plot.getRoot().appendChild(viewport);      
      updateSize();

      try {
        SVGSimpleLinearAxis.drawAxis(plot, viewport, this.s, 0.0, 1.0, 0.0, 0.0, true, false);
      }
      catch(CSSNamingConflict e) {
        logger.exception(e);
      }
      plot.updateStyleElement();

      // insert the actual data series.
      Element egroup = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(egroup, SVGConstants.SVG_ID_ATTRIBUTE, SERIESID);
      viewport.appendChild(egroup);
      plot.putIdElement(SERIESID, egroup);
      
      svgCanvas.setDocumentState(AbstractJSVGComponent.ALWAYS_DYNAMIC);
      svgCanvas.setDocument(plot.getDocument());

      DefaultListModel m = new DefaultListModel();
      for(Integer dbid : db) {
        m.addElement(dbid);
      }
      seriesList.setModel(m);

      frame.setVisible(true);
    }

    protected void updateSelection() {
      Object[] sel = seriesList.getSelectedValues();
      // prepare replacement tag.
      Element newe = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(newe, SVGConstants.SVG_ID_ATTRIBUTE, SERIESID);

      distancecache.clear();

      for(Object o : sel) {
        int idx = (Integer) o;

        List<DistanceResultPair<N>> knn = db.kNNQueryForID(idx, k, distanceFunction);

        double maxdist = knn.get(knn.size() - 1).getDistance().getValue().doubleValue();
        // avoid division by zero.
        if(maxdist == 0) {
          maxdist = 1;
        }

        for(ListIterator<DistanceResultPair<N>> iter = knn.listIterator(knn.size()); iter.hasPrevious();) {
          DistanceResultPair<N> pair = iter.previous();
          Element line = plotSeries(pair.getID(), MAXRESOLUTION);
          double dist = pair.getDistance().getValue().doubleValue() / maxdist;
          Color color = getColor(dist);
          String colstr = "#" + Integer.toHexString(color.getRGB()).substring(2);
          String width = (pair.getID() == idx) ? "0.5%" : "0.2%";
          SVGUtil.setStyle(line, "stroke: " + colstr + "; stroke-width: " + width + "; fill: none");
          newe.appendChild(line);
          // put into cache
          Double known = distancecache.get(pair.getID());
          if(known == null || dist < known) {
            distancecache.put(pair.getID(), dist);
          }
        }
      }
      new NodeReplacer(newe, plot, SERIESID).hook(svgCanvas);
    }

    Color getColor(double dist) {
      Color color = new Color((int) (255 * dist), 0, (int) (255 * (1.0 - dist)));
      return color;
    }

    private Element plotSeries(int idx, int resolution) {
      O series = db.get(idx);

      double step = 1.0;
      if(resolution < dim) {
        step = (double) dim / (double) resolution;
      }

      StringBuffer path = new StringBuffer();
      for(double id = 0; id < dim; id += step) {
        int i = (int) Math.floor(id);
        if(i == 0) {
          path.append(SVGConstants.PATH_MOVE);
        }
        path.append(ratio * (((double) i) / (dim - 1)));
        path.append(" ");
        path.append(1.0 - s.getScaled(series.getValue(i + 1).doubleValue()));
        path.append(" ");
        if(i == 0) {
          path.append(SVGConstants.PATH_LINE_TO);
        }
      }
      // path.append(SVGConstants.PATH_CLOSE);
      Element p = plot.svgElement(SVGConstants.SVG_PATH_TAG);
      p.setAttribute(SVGConstants.SVG_D_ATTRIBUTE, path.toString());
      return p;
    }

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
          label = db.getAssociation(AssociationID.LABEL, (Integer) value);
        }
        if(label == null || label == "") {
          label = db.getAssociation(AssociationID.CLASS, (Integer) value).toString();
        }
        if(label == null || label == "") {
          label = Integer.toString((Integer) value);
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
}
