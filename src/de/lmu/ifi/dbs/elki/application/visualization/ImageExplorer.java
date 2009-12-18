package de.lmu.ifi.dbs.elki.application.visualization;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.images.BlendComposite;
import de.lmu.ifi.dbs.elki.data.images.ImageUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.visualization.batikutil.LazyCanvasResizer;

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
public class ImageExplorer<O extends NumberVector<O, ?>, N extends NumberDistance<N, D>, D extends Number> extends AbstractApplication {
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
  String basedir = "";

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

  protected static GraphicsConfiguration getSystemGraphicsConfiguration() {
    // setup JFrame with optimal graphics settings.
    GraphicsEnvironment graphEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice graphDevice = graphEnv.getDefaultScreenDevice();
    GraphicsConfiguration graphicConf = graphDevice.getDefaultConfiguration();
    return graphicConf;
  }

  class ExplorerWindow extends JFrame {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default Window Title
     */
    private static final String WINDOW_TITLE_BASE = "ELKI Image Explorer";

    // The canvas
    protected ExplorerCanvas canvas;

    // The spinner
    protected JSpinner spinner;

    // The list of series
    private JList seriesList = new JList();

    // The "Quit" button, to close the application.
    protected JButton quitButton = new JButton("Quit");

    // The "Export" button, to save the image
    protected JButton saveButton = new JButton("Export");

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

    // Distance function
    private DistanceFunction<O, N> distanceFunction;

    public ExplorerWindow() {
      super(WINDOW_TITLE_BASE, getSystemGraphicsConfiguration());
      this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      this.setIgnoreRepaint(true);

      // setup Canvas accordingly
      canvas = new ExplorerCanvas(getGraphicsConfiguration());

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

      bigpanel.add(BorderLayout.CENTER, canvas);

      this.getContentPane().add(bigpanel);

      spinner.addChangeListener(new ChangeListener() {
        @Override
        public void stateChanged(@SuppressWarnings("unused") ChangeEvent e) {
          k = (Integer) (spinner.getValue());
          canvas.setK(k);
          refresh();
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

      saveButton.setEnabled(false);
      // saveButton.addActionListener(new ActionListener() {
      // public void actionPerformed(@SuppressWarnings("unused") ActionEvent ae)
      // {
      // logger.warning("Not yet implemented: save dialog");
      // // FIXME: implement.
      // }
      // });

      quitButton.addActionListener(new ActionListener() {
        public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
          ExplorerWindow.this.setVisible(false);
          ExplorerWindow.this.dispose();
        }
      });

      // display
      this.setSize(600, 600);

      // resize listener
      LazyCanvasResizer listener = new LazyCanvasResizer(this) {
        @Override
        public void executeResize(double newratio) {
          ratio = newratio;
        }
      };
      ratio = listener.getActiveRatio();
      this.addComponentListener(listener);
    }

    public void run(Database<O> db, DistanceFunction<O, N> distanceFunction) {
      this.db = db;
      this.dim = db.dimensionality();
      this.distanceFunction = distanceFunction;
      this.distanceFunction.setDatabase(this.db, false, false);

      this.canvas.setDatabase(db);
      this.canvas.setDistanceFunction(distanceFunction);
      this.canvas.setDistanceCache(distancecache);
      this.canvas.setK(k);

      this.setTitle(distanceFunction.getClass().getSimpleName() + " - " + WINDOW_TITLE_BASE);

      DefaultListModel m = new DefaultListModel();
      for(Integer dbid : db) {
        m.addElement(dbid);
      }
      seriesList.setModel(m);

      updateSelection();

      this.setVisible(true);
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

    public void updateSelection() {
      Object[] sel = seriesList.getSelectedValues();
      canvas.setSelection(sel);
      refresh();
    }

    public void refresh() {
      canvas.repaint();
      seriesList.repaint();
    }
  }
  
  class ImgObj {
    double x;
    double y;
    Integer id;
    BufferedImage image;
    
    public ImgObj(Integer id) {
      super();
      this.id = id;
    }
  }

  private class ExplorerCanvas extends JPanel {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Logger
     */
    private Logging logger = Logging.getLogger(ExplorerCanvas.class);

    // Distance function
    private DistanceFunction<O, N> distanceFunction;

    // Object Selection
    private Object[] selection;

    // Database
    private Database<O> db;

    // Distance cache
    private HashMap<Integer, Double> distancecache;

    // k
    private int k = 20;

    // objects to display
    private ArrayList<ImgObj> objects;
    
    /**
     * @param k the k to set
     */
    protected void setK(int k) {
      this.k = k;
      if(distancecache != null) {
        distancecache.clear();
      }
    }

    /**
     * Constructor.
     * 
     * @param graphicConf Graphics configuration.
     */
    public ExplorerCanvas(GraphicsConfiguration graphicConf) {
      super();
      this.setIgnoreRepaint(true);
    }

    /**
     * @param distancecache the distance cache to use
     */
    protected void setDistanceCache(HashMap<Integer, Double> distancecache) {
      this.distancecache = distancecache;
    }

    /**
     * @param db the Database to set
     */
    protected void setDatabase(Database<O> db) {
      this.db = db;
      if(distancecache != null) {
        distancecache.clear();
      }
    }

    /**
     * @param distanceFunction the distanceFunction to set
     */
    protected void setDistanceFunction(DistanceFunction<O, N> distanceFunction) {
      this.distanceFunction = distanceFunction;
      if(distancecache != null) {
        distancecache.clear();
      }
    }

    /**
     * @param selection the selection to set
     */
    protected void setSelection(Object[] selection) {
      this.selection = selection;
      if(distancecache != null) {
        distancecache.clear();
      }
      updateObjects();
    }

    public void updateObjects() {
      objects = new ArrayList<ImgObj>(this.selection.length * k);
      ArrayList<Integer> objids = new ArrayList<Integer>();
      for(Object o : this.selection) {
        int idx = (Integer) o;
        List<DistanceResultPair<N>> knn = db.kNNQueryForID(idx, k, distanceFunction);
        for(DistanceResultPair<N> pair : knn) {
          Integer id = pair.getID();
          objids.add(id);
          objects.add(new ImgObj(id));
          Double known = distancecache.get(pair.getID());
          double dist = pair.getDistance().doubleValue();
          if(known == null || dist < known) {
            distancecache.put(pair.getID(), dist);
          }
        }
      }
      if(objects.size() > 0) {
        // TODO: use center when there is more than one obj selected?
        Vector center = db.get(objects.get(0).id).getColumnVector();
        PCARunner<O, N> pcar = new PCARunner<O, N>();
        try {
          pcar.setParameters(new ArrayList<String>(0));
        }
        catch(ParameterException e) {
          logger.exception(e);
        }
        PCAResult pcares = pcar.processIds(objids, db);
        Matrix projm = pcares.getEigenvectors().transpose();

        double[] stddevs = pcares.getEigenvalues();
        for(int i = 0; i < stddevs.length; i++) {
          stddevs[i] = Math.sqrt(stddevs[i]);
        }
        for(int i = 0; i < db.dimensionality(); i++) {
          for(int j = 0; j < db.dimensionality(); j++) {
            // Note: rows on backward transformation!
            projm.set(j, i, projm.get(j, i) / stddevs[j]);
          }
        }

        for(ImgObj object : objects) {
          Vector centered = db.get(object.id).getColumnVector().minus(center);
          Vector p = projm.times(centered).getColumnVector(0);
          object.x = p.get(0);
          object.y = p.get(1);
        }
      }
    }

    @Override
    public void paintComponent(Graphics g) {
      int width = this.getWidth();
      int height = this.getHeight();
      int size = 256;
      // fill background
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, width, height);
      Graphics2D destG = (Graphics2D) g;
      destG.setComposite(new BlendComposite(BlendComposite.LIGHTEN, 1.0));

      int xrange = (width - size) / 2;
      int yrange = (height - size) / 2;

      for(ImgObj object : objects) {
        int x = (int) (object.x * xrange + xrange);
        int y = (int) (object.y * yrange + yrange);

        drawThumbnail(g, object, x, y);
      }
    }

    private void drawThumbnail(Graphics g, ImgObj object, int x, int y) {
      BufferedImage img = object.image;
      if (img == null) {
        String name = db.getAssociation(AssociationID.LABEL, object.id);
        File f = (name != null) ? FileUtil.locateFile(name, basedir) : null;
        if(f != null) {
          try {
            img = ImageUtil.loadImage(f);
            if (img != null) {
              object.image = img;
            }
          }
          catch(IOException e) {
            logger.exception("Exception drawing image.", e);
          }
        }
        else {
          logger.warning("Image not found: " + name);
        }
      }
      if (img != null) {
        g.drawImage(img, x, y, null);
      }
    }
  }
}
