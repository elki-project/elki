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
package de.lmu.ifi.dbs.elki.evaluation.similaritymatrix;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.PixmapResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
 * @since 0.4.0
 * 
 * @navhas - create - SimilarityMatrix
 * 
 * @param <O> Object class
 */
public class ComputeSimilarityMatrixImage<O> implements Evaluator {
  /**
   * The logger.
   */
  private static final Logging LOG = Logging.getLogger(ComputeSimilarityMatrixImage.class);

  /**
   * OptionID for the scaling function to use
   */
  public static final OptionID SCALING_ID = new OptionID("simmatrix.scaling", "Class to use as scaling function.");

  /**
   * OptionID to skip zero values when plotting to increase contrast.
   */
  public static final OptionID SKIPZERO_ID = new OptionID("simmatrix.skipzero", "Skip zero values when computing the colors to increase contrast.");

  /**
   * The distance function to use
   */
  private DistanceFunction<? super O> distanceFunction;

  /**
   * Scaling function to use
   */
  private ScalingFunction scaling;

  /**
   * Skip zero values.
   */
  private boolean skipzero = false;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function to use
   * @param scaling Scaling function to use for contrast
   * @param skipzero Skip zero values when scaling.
   */
  public ComputeSimilarityMatrixImage(DistanceFunction<? super O> distanceFunction, ScalingFunction scaling, boolean skipzero) {
    super();
    this.distanceFunction = distanceFunction;
    this.scaling = scaling;
    this.skipzero = skipzero;
  }

  /**
   * Compute the actual similarity image.
   * 
   * @param relation Relation
   * @param iter DBID iterator
   * @return result object
   */
  private SimilarityMatrix computeSimilarityMatrixImage(Relation<O> relation, DBIDIter iter) {
    ArrayModifiableDBIDs order = DBIDUtil.newArray(relation.size());
    for(; iter.valid(); iter.advance()) {
      order.add(iter);
    }
    if(order.size() != relation.size()) {
      throw new IllegalStateException("Iterable result doesn't match database size - incomplete ordering?");
    }
    DistanceQuery<O> dq = distanceFunction.instantiate(relation);
    final int size = order.size();

    // When the logging is in the outer loop, it's just 2*size (providing enough
    // resolution)
    final int ltotal = 2 * size; // size * (size + 1);
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Similarity Matrix Image", ltotal, LOG) : null;

    // Note: we assume that we have an efficient distance cache available,
    // since we are using 2*O(n*n) distance computations.
    DoubleMinMax minmax = new DoubleMinMax();
    {
      DBIDArrayIter id1 = order.iter();
      DBIDArrayIter id2 = order.iter();
      for(; id1.valid(); id1.advance()) {
        id2.seek(id1.getOffset());
        for(; id2.valid(); id2.advance()) {
          final double dist = dq.distance(id1, id2);
          if(!Double.isNaN(dist) && !Double.isInfinite(dist) /* && dist > 0.0 */) {
            if(!skipzero || dist > 0.0) {
              minmax.put(dist);
            }
          }
        }
        LOG.incrementProcessed(prog);
      }
    }

    double zoom = minmax.getMax() - minmax.getMin();
    if(zoom > 0.0) {
      zoom = 1. / zoom;
    }
    LinearScaling scale = new LinearScaling(zoom, -minmax.getMin() * zoom);
    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    {
      DBIDArrayIter id1 = order.iter();
      DBIDArrayIter id2 = order.iter();
      for(int x = 0; x < size && id1.valid(); x++, id1.advance()) {
        id2.seek(id1.getOffset());
        for(int y = x; y < size && id2.valid(); y++, id2.advance()) {
          double ddist = dq.distance(id1, id2);
          if(ddist > 0.0) {
            ddist = scale.getScaled(ddist);
          }
          // Apply extra scaling
          if(scaling != null) {
            ddist = scaling.getScaled(ddist);
          }
          int dist = 0xFF & (int) (255 * ddist);
          int col = 0xff000000 | (dist << 16) | (dist << 8) | dist;
          img.setRGB(x, y, col);
          img.setRGB(y, x, col);
        }
        LOG.incrementProcessed(prog);
      }
    }
    LOG.ensureCompleted(prog);

    return new SimilarityMatrix(img, relation, order);
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    Database db = ResultUtil.findDatabase(hier);
    boolean nonefound = true;
    List<OutlierResult> oresults = OutlierResult.getOutlierResults(result);
    List<OrderingResult> orderings = ResultUtil.getOrderingResults(result);
    // Outlier results are the main use case.
    for(OutlierResult o : oresults) {
      final OrderingResult or = o.getOrdering();
      Relation<O> relation = db.getRelation(distanceFunction.getInputTypeRestriction());
      db.getHierarchy().add(or, computeSimilarityMatrixImage(relation, or.order(relation.getDBIDs()).iter()));
      // Process them only once.
      orderings.remove(or);
      nonefound = false;
    }

    // FIXME: find appropriate place to add the derived result
    // otherwise apply an ordering to the database IDs.
    for(OrderingResult or : orderings) {
      Relation<O> relation = db.getRelation(distanceFunction.getInputTypeRestriction());
      DBIDIter iter = or.order(relation.getDBIDs()).iter();
      db.getHierarchy().add(or, computeSimilarityMatrixImage(relation, iter));
      nonefound = false;
    }

    if(nonefound) {
      // Use the database ordering.
      // But be careful to NOT cause a loop, process new databases only.
      List<Database> iter = ResultUtil.filterResults(hier, Database.class);
      for(Database database : iter) {
        // Get an arbitrary representation
        Relation<O> relation = database.getRelation(distanceFunction.getInputTypeRestriction());
        db.getHierarchy().add(db, computeSimilarityMatrixImage(relation, relation.iterDBIDs()));
      }
    }
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
    Relation<?> relation;

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
    public SimilarityMatrix(RenderedImage img, Relation<?> relation, ArrayDBIDs ids) {
      super();
      this.img = img;
      this.relation = relation;
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
     * Get the relation
     * 
     * @return the relation
     */
    public Relation<?> getRelation() {
      return relation;
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

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * The distance function to use
     */
    private DistanceFunction<O> distanceFunction;

    /**
     * Scaling function to use
     */
    private ScalingFunction scaling;

    /**
     * Skip zero values.
     */
    private boolean skipzero = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<O>> distanceFunctionP = new ObjectParameter<>(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distanceFunction = distanceFunctionP.instantiateClass(config);
      }

      ObjectParameter<ScalingFunction> scalingP = new ObjectParameter<>(SCALING_ID, ScalingFunction.class, true);
      if(config.grab(scalingP)) {
        scaling = scalingP.instantiateClass(config);
      }

      Flag skipzeroP = new Flag(SKIPZERO_ID);
      if(config.grab(skipzeroP)) {
        skipzero = skipzeroP.getValue();
      }
    }

    @Override
    protected ComputeSimilarityMatrixImage<O> makeInstance() {
      return new ComputeSimilarityMatrixImage<>(distanceFunction, scaling, skipzero);
    }
  }
}