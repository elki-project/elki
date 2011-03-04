package de.lmu.ifi.dbs.elki.evaluation.similaritymatrix;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.IterableResult;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.PixmapResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;

/**
 * Compute a similarity matrix for a distance function.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object class
 */
public class ComputeSimilarityMatrixImage<O extends DatabaseObject> implements Evaluator<O> {
  /**
   * The logger.
   */
  static final Logging logger = Logging.getLogger(ComputeSimilarityMatrixImage.class);

  /**
   * OptionID for {@link #SCALING_PARAM}
   */
  public static final OptionID SCALING_ID = OptionID.getOrCreateOptionID("simmatrix.scaling", "Class to use as scaling function.");

  /**
   * OptionID for {@link #SKIPZERO_PARAM}
   */
  public static final OptionID SKIPZERO_ID = OptionID.getOrCreateOptionID("simmatrix.skipzero", "Skip zero values when computing the colors to increase contrast.");

  /**
   * The distance function to use
   */
  private DistanceFunction<O, ? extends NumberDistance<?, ?>> distanceFunction;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;
  
  /**
   * Skip zero values.
   */
  private boolean skipzero = false;

  /**
   * Constructor
   * 
   * @param config Parameters
   */
  public ComputeSimilarityMatrixImage(Parameterization config) {
    super();
    config = config.descend(this);
    distanceFunction = AbstractAlgorithm.getParameterDistanceFunction(config);
    scaling = getScalingFunction(config);
    Flag skipzero_param = new Flag(SKIPZERO_ID);
    if (config.grab(skipzero_param)) {
      skipzero = skipzero_param.getValue();
    }
  }

  /**
   * Get the scaling function parameter.
   * 
   * @param config Parameterization
   * @return Scaling function or null
   */
  private static ScalingFunction getScalingFunction(Parameterization config) {
    final ObjectParameter<ScalingFunction> param = new ObjectParameter<ScalingFunction>(SCALING_ID, ScalingFunction.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    } else {
      return null;
    }
  }

  /**
   * Compute the actual similarity image.
   * 
   * @param database Database
   * @param iter DBID iterator
   * @return result object
   */
  private SimilarityMatrix computeSimilarityMatrixImage(Database<O> database, Iterator<DBID> iter) {
    ArrayModifiableDBIDs order = DBIDUtil.newArray(database.size());
    while(iter.hasNext()) {
      Object o = iter.next();
      if(!(o instanceof DBID)) {
        throw new IllegalStateException("Iterable result contained non-DBID - result didn't satisfy requirements");
      }
      else {
        order.add((DBID) o);
      }
    }
    if(order.size() != database.size()) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }
    DistanceQuery<O, ? extends NumberDistance<?, ?>> dq = distanceFunction.instantiate(database);
    final int size = order.size();

    // When the logging is in the outer loop, it's just 2*size (providing enough
    // resolution)
    final int ltotal = 2 * size; // size * (size + 1);
    FiniteProgress prog = logger.isVerbose() ? new FiniteProgress("Similarity Matrix Image", ltotal, logger) : null;

    // Note: we assume that we have an efficient distance cache available,
    // since we are using 2*O(n*n) distance computations.
    DoubleMinMax minmax = new DoubleMinMax();
    for(int x = 0; x < size; x++) {
      DBID id1 = order.get(x);
      for(int y = x; y < size; y++) {
        DBID id2 = order.get(y);
        final double dist = dq.distance(id1, id2).doubleValue();
        if(!Double.isNaN(dist) && !Double.isInfinite(dist) /* && dist > 0.0 */) {
          if (!skipzero || dist != 0.0) {
            minmax.put(dist);
          }
        }
      }
      if(prog != null) {
        prog.incrementProcessed(logger);
      }
    }

    double zoom = minmax.getMax() - minmax.getMin();
    if(zoom > 0.0) {
      zoom = 1. / zoom;
    }
    LinearScaling scale = new LinearScaling(zoom, -minmax.getMin() * zoom);
    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    for(int x = 0; x < size; x++) {
      DBID id1 = order.get(x);
      for(int y = x; y < size; y++) {
        DBID id2 = order.get(y);
        double ddist = dq.distance(id1, id2).doubleValue();
        if(ddist > 0.0) {
          ddist = scale.getScaled(ddist);
        }
        // Apply extra scaling
        if (scaling != null) {
          ddist = scaling.getScaled(ddist);
        }
        int dist = 0xFF & (int) (255 * ddist);
        int col = 0xff000000 | (dist << 16) | (dist << 8) | dist;
        img.setRGB(x, y, col);
        img.setRGB(y, x, col);
      }
      if(prog != null) {
        prog.incrementProcessed(logger);
      }
    }
    if(prog != null) {
      prog.ensureCompleted(logger);
    }

    return new SimilarityMatrix(img, database, order);
  }

  /**
   * Wrap the uncheckable cast with the manual check.
   * 
   * @param ir Interable result
   * @return Iterator if Integer iterable, null otherwise.
   */
  @SuppressWarnings("unchecked")
  private Iterator<DBID> getDBIDIterator(IterableResult<?> ir) {
    Iterator<?> testit = ir.iterator();
    if(testit.hasNext() && (testit.next() instanceof DBID)) {
      // note: we DO want a fresh iterator here!
      return (Iterator<DBID>) ir.iterator();
    }
    return null;
  }

  @Override
  public void processResult(Database<O> db, Result result, ResultHierarchy hierarchy) {
    boolean nonefound = true;
    List<OutlierResult> oresults = ResultUtil.getOutlierResults(result);
    List<IterableResult<?>> iterables = ResultUtil.getIterableResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      final OrderingResult or = o.getOrdering();
      hierarchy.add(or, computeSimilarityMatrixImage(db, or.iter(db.getIDs())));
      // Process them only once.
      orderings.remove(or);
      nonefound = false;
    }

    // try iterable results first
    // FIXME: find the appropriate place to call addDerivedResult
    for(IterableResult<?> ir : iterables) {
      Iterator<DBID> iter = getDBIDIterator(ir);
      if(iter != null) {
        hierarchy.add(ir, computeSimilarityMatrixImage(db, iter));
        nonefound = false;
      }
    }
    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      Iterator<DBID> iter = or.iter(db.getIDs());
      hierarchy.add(or, computeSimilarityMatrixImage(db, iter));
      nonefound = false;
    }

    if(nonefound) {
      // Use the database ordering.
      // But be careful to NOT cause a loop, process new databases only.
      Iterable<Database<?>> iter = ResultUtil.filteredResults(result, Database.class);
      for(Database<?> d : iter) {
        @SuppressWarnings("unchecked")
        Database<O> database = (Database<O>) d;
        hierarchy.add(db, computeSimilarityMatrixImage(database, database.iterator()));
      }
    }
  }

  @Override
  public void setNormalization(@SuppressWarnings("unused") Normalization<O> normalization) {
    // Normalizations are ignored
  }

  /**
   * Similarity matrix image.
   * 
   * @author Erich Schubert
   */
  public static class SimilarityMatrix implements PixmapResult {
    /**
     * Prefix for filenames
     */
    private static final String IMGFILEPREFIX = "elki-pixmap-";
    
    /**
     * The database
     */
    Database<?> database;
    
    /**
     * The database IDs used
     */
    ArrayDBIDs ids;

    /**
     * Our image
     */
    RenderedImage img;

    /**
     * The file we have written the image to
     */
    File imgfile = null;

    /**
     * Constructor
     * 
     * @param img Image data
     */
    public SimilarityMatrix(RenderedImage img, Database<?> database, ArrayDBIDs ids) {
      super();
      this.img = img;
      this.database = database;
      this.ids = ids;
    }

    @Override
    public RenderedImage getImage() {
      return img;
    }

    @Override
    public File getAsFile() {
      if(imgfile == null) {
        try {
          imgfile = File.createTempFile(IMGFILEPREFIX, ".png");
          imgfile.deleteOnExit();
          ImageIO.write(img, "PNG", imgfile);
        }
        catch(IOException e) {
          LoggingUtil.exception("Could not generate OPTICS plot.", e);
        }
      }
      return imgfile;
    }
    
    /**
     * Get the database
     * 
     * @return the database
     */
    public Database<?> getDatabase() {
      return database;
    }

    /**
     * Get the IDs
     * 
     * @return the ids
     */
    public ArrayDBIDs getIDs() {
      return ids;
    }

    @Override
    public String getLongName() {
      return "Similarity Matrix";
    }

    @Override
    public String getShortName() {
      return "sim-matrix";
    }
  }
}