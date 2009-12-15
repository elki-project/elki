package de.lmu.ifi.dbs.elki.application.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.visualization.batikutil.JSVGSynchronizedCanvas;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;
import de.lmu.ifi.dbs.elki.visualization.batikutil.NodeReplacer;
import de.lmu.ifi.dbs.elki.visualization.savedialog.SVGSaveDialog;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * User application to explore the k Nearest Neighbors for a given image data
 * set and distance function. When selecting one or more data entries, the
 * nearest neighbors each are determined and visualized.
 * 
 * <h3>Usage example:</h3>
 * 
 * <p>
 * Main invocation:<br/>
 * 
 * <code>java -cp elki.jar de.lmu.ifi.dbs.elki.visualization.ImageExplorer</code>
 * </p>
 * 
 * <p>
 * The application supports the usual parametrization, in particular parameters
 * <code>-dbc.in</code> and <code>-explorer.distancefunction</code> to select an
 * input file and the distance function to explore.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class ImageExplorer<O extends NumberVector<?, ?>, N extends NumberDistance<N, D>, D extends Number> extends AbstractApplication {
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
   * OptionID for {@link #BASEDIR_PARAM}
   */
  public static final OptionID BASEDIR_ID = OptionID.getOrCreateOptionID("explorer.basedir", "Base directory to locate image files.");

  /**
   * Parameter to specify the base directory to locate images in.
   * <p>
   * Key: {@code -explorer.explorer.basedir}
   * </p>
   */
  protected final PatternParameter BASEDIR_PARAM = new PatternParameter(BASEDIR_ID, true);

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

  /**
   * Store the base directory.
   */
  private String basedir = "";

  public ImageExplorer() {
    super();
    // parameter base directory
    addOption(BASEDIR_PARAM);

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

    // base directory
    if(BASEDIR_PARAM.isSet()) {
      basedir = BASEDIR_PARAM.getValue();
    }

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
    new ImageExplorer<DoubleVector, DoubleDistance, Double>().runCLIApplication(args);
  }

  /**
   * Try to locate an image.
   * 
   * @param name ID string
   * @return file, if the image could be found.
   */
  public File locateImage(String name) {
    // Try exact match first.
    File f = new File(name);
    if(f.exists()) {
      return f;
    }
    // Try with base directory
    if(basedir != null) {
      f = new File(basedir, name);
      //logger.warning("Trying: "+f.getAbsolutePath());
      if(f.exists()) {
        return f;
      }
    }
    // try stripping whitespace
    {
      String name2 = name.trim();
      if(!name.equals(name2)) {
        //logger.warning("Trying without whitespace.");
        f = locateImage(name2);
        if(f != null) {
          return f;
        }
      }
    }
    // try stripping extra characters, such as quotes.
    if(name.length() > 2 && name.charAt(0) == '"' && name.charAt(name.length() - 1) == '"') {
      //logger.warning("Trying without quotes.");
      f = locateImage(name.substring(1, name.length() - 1));
      if(f != null) {
        return f;
      }
    }
    return null;
  }

  class ExplorerWindow extends AbstractLoggable {
    /**
     * Default Window Title
     */
    private static final String WINDOW_TITLE_BASE = "ELKI Image Explorer";

    /**
     * Object ID used for replacing plot parts
     */
    private static final String IMAGESID = "images";

    /**
     * Object ID used for background element.
     */
    private static final String BACKGROUNDID = "background";

    /**
     * Filter ID for blending
     */
    private static final String FILTERID = "blend";

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

      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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

    public void updateSize() {
      SVGUtil.setAtt(plot.getRoot(), SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + ratio + " 1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_WIDTH_ATTRIBUTE, ratio);
      SVGUtil.setAtt(viewport, SVGConstants.SVG_HEIGHT_ATTRIBUTE, "1");
      SVGUtil.setAtt(viewport, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "-0.1 -0.1 " + (ratio + 0.2) + " 1.2");
    }

    public void run(Database<O> db, DistanceFunction<O, N> distanceFunction) {
      this.db = db;
      this.dim = db.dimensionality();
      this.distanceFunction = distanceFunction;
      this.distanceFunction.setDatabase(this.db, false, false);

      this.frame.setTitle(distanceFunction.getClass().getSimpleName() + " - " + WINDOW_TITLE_BASE);

      plot = new SVGPlot();
      viewport = plot.svgElement(SVGConstants.SVG_SVG_TAG);
      SVGUtil.setAtt(viewport, SVGConstants.SVG_STYLE_ATTRIBUTE, "fill: black");
      plot.getRoot().appendChild(viewport);
      updateSize();
      
      // setup blending
      Element filter = plot.svgElement(SVGConstants.SVG_FILTER_TAG);
      SVGUtil.setAtt(filter, SVGConstants.SVG_ID_ATTRIBUTE, FILTERID);
      Element blend = plot.svgElement(SVGConstants.SVG_FE_BLEND_TAG);
      SVGUtil.setAtt(blend, SVGConstants.SVG_MODE_ATTRIBUTE, SVGConstants.SVG_LIGHTEN_VALUE);
      SVGUtil.setAtt(blend, SVGConstants.SVG_IN_ATTRIBUTE, SVGConstants.SVG_SOURCE_GRAPHIC_VALUE);
      SVGUtil.setAtt(blend, SVGConstants.SVG_IN2_ATTRIBUTE, SVGConstants.SVG_BACKGROUND_IMAGE_VALUE);
      filter.appendChild(blend);
      plot.getDefs().appendChild(filter);
      
      // background
      Element background = plot.svgElement(SVGConstants.SVG_RECT_TAG);
      SVGUtil.setAtt(background,SVGConstants.SVG_X_ATTRIBUTE, 0);
      SVGUtil.setAtt(background,SVGConstants.SVG_Y_ATTRIBUTE, 0);
      SVGUtil.setAtt(background,SVGConstants.SVG_WIDTH_ATTRIBUTE, "100%");
      SVGUtil.setAtt(background,SVGConstants.SVG_HEIGHT_ATTRIBUTE, "100%");
      viewport.appendChild(background);

      // insert the actual data series.
      Element egroup = plot.svgElement(SVGConstants.SVG_G_TAG);
      SVGUtil.setAtt(egroup, SVGConstants.SVG_ID_ATTRIBUTE, IMAGESID);
      viewport.appendChild(egroup);
      plot.putIdElement(IMAGESID, egroup);

      svgCanvas.setPlot(plot);

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
      // need background buffer for Filter effects to work.
      //SVGUtil.setAtt(newe, SVGConstants.SVG_STYLE_ATTRIBUTE, "enable-background: new;");
      SVGUtil.setAtt(newe, SVGConstants.SVG_ID_ATTRIBUTE, IMAGESID);

      distancecache.clear();

      for(Object o : sel) {
        int idx = (Integer) o;

        List<DistanceResultPair<N>> knn = db.kNNQueryForID(idx, k, distanceFunction);

        double maxdist = knn.get(knn.size() - 1).getDistance().doubleValue();
        // avoid division by zero.
        if(maxdist == 0) {
          maxdist = 1;
        }

        for(ListIterator<DistanceResultPair<N>> iter = knn.listIterator(knn.size()); iter.hasPrevious();) {
          DistanceResultPair<N> pair = iter.previous();
          //Element line = plotSeries(pair.getID(), MAXRESOLUTION);
          double dist = pair.getDistance().doubleValue() / maxdist;
          
          String name = db.getAssociation(AssociationID.LABEL, pair.getID());
          File f = (name != null) ? locateImage(name) : null;
          if (f != null) {
            Element img = plot.svgElement(SVGConstants.SVG_IMAGE_TAG);
            double size = 0.3;
            SVGUtil.setAtt(img, SVGConstants.SVG_X_ATTRIBUTE, dist * (1 / (ratio + size)));
            SVGUtil.setAtt(img, SVGConstants.SVG_Y_ATTRIBUTE, 0.5 + 0.25 * ((double)name.hashCode() / Integer.MAX_VALUE));
            SVGUtil.setAtt(img, SVGConstants.SVG_WIDTH_ATTRIBUTE, size);
            SVGUtil.setAtt(img, SVGConstants.SVG_HEIGHT_ATTRIBUTE, size * 0.75);
            img.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, f.toURI().toString());
            SVGUtil.setAtt(img, SVGConstants.SVG_IMAGE_RENDERING_ATTRIBUTE, SVGConstants.SVG_OPTIMIZE_SPEED_VALUE);
            //SVGUtil.setAtt(img, SVGConstants.SVG_STYLE_ATTRIBUTE, "filter:url(#"+FILTERID+");");
            newe.appendChild(img);
          } else {
            logger.warning("Image not found: "+name);
          }
          
          //Color color = getColor(dist);
          //String colstr = "#" + Integer.toHexString(color.getRGB()).substring(2);
          //String width = (pair.getID() == idx) ? "0.5%" : "0.2%";
          //SVGUtil.setStyle(line, "stroke: " + colstr + "; stroke-width: " + width + "; fill: none");
          //newe.appendChild(line);
          // put into cache
          Double known = distancecache.get(pair.getID());
          if(known == null || dist < known) {
            distancecache.put(pair.getID(), dist);
          }
        }
      }
      plot.scheduleUpdate(new NodeReplacer(newe, plot, IMAGESID));
      seriesList.repaint();
    }

    Color getColor(double dist) {
      Color color = new Color((int) (255 * dist), 0, (int) (255 * (1.0 - dist)));
      return color;
    }

    /**
     * FIXME: add JavaDoc
     * 
     * @author Erich Schubert
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
          label = db.getAssociation(AssociationID.LABEL, (Integer) value);
        }
        if(label == null || label == "") {
          ClassLabel cls = db.getAssociation(AssociationID.CLASS, (Integer) value);
          if(cls != null) {
            label = cls.toString();
          }
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
