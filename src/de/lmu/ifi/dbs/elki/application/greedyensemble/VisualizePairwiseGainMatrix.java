package de.lmu.ifi.dbs.elki.application.greedyensemble;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.evaluation.similaritymatrix.ComputeSimilarityMatrixImage;
import de.lmu.ifi.dbs.elki.evaluation.similaritymatrix.ComputeSimilarityMatrixImage.SimilarityMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.VisualizerParameterizer;
import de.lmu.ifi.dbs.elki.visualization.gui.SimpleSVGViewer;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.SimilarityMatrixVisualizer;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Class to load an outlier detection summary file, as produced by
 * {@link ComputeKNNOutlierScores}, and compute a matrix with the pairwise
 * gains. It will have one column / row obtained for each combination.
 * 
 * The gain is always computed in relation to the better of the two input
 * methods. Green colors indicate the result has improved, red indicate it
 * became worse.
 * 
 * Reference:
 * <p>
 * E. Schubert, R. Wojdanowski, A. Zimek, H.-P. Kriegel<br />
 * On Evaluation of Outlier Rankings and Outlier Scores<br/>
 * In Proceedings of the 12th SIAM International Conference on Data Mining
 * (SDM), Anaheim, CA, 2012.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf VisualizerParameterizer
 * @apiviz.composedOf SimilarityMatrixVisualizer
 */
@Reference(authors = "E. Schubert, R. Wojdanowski, A. Zimek, H.-P. Kriegel", title = "On Evaluation of Outlier Rankings and Outlier Scores", booktitle = "Proc. 12th SIAM International Conference on Data Mining (SDM), Anaheim, CA, 2012.")
public class VisualizePairwiseGainMatrix extends AbstractApplication {
  /**
   * Get static logger
   */
  private static final Logging logger = Logging.getLogger(VisualizePairwiseGainMatrix.class);

  /**
   * The data input part.
   */
  private InputStep inputstep;

  /**
   * Parameterizer for visualizers
   */
  private VisualizerParameterizer vispar;

  /**
   * Constructor.
   * 
   * @param verbose Verbosity
   * @param inputstep Input step
   * @param vispar Visualizer parameterizer
   */
  public VisualizePairwiseGainMatrix(boolean verbose, InputStep inputstep, VisualizerParameterizer vispar) {
    super(verbose);
    this.inputstep = inputstep;
    this.vispar = vispar;
  }

  @Override
  public void run() {
    final Database database = inputstep.getDatabase();
    final Relation<NumberVector<?, ?>> relation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    final Relation<String> labels = DatabaseUtil.guessLabelRepresentation(database);
    final DBID firstid = DBIDUtil.deref(labels.iterDBIDs());
    final String firstlabel = labels.get(firstid);
    if(!firstlabel.matches("bylabel")) {
      throw new AbortException("No 'by label' reference outlier found, which is needed for weighting!");
    }

    // Dimensionality and reference vector
    final int dim = DatabaseUtil.dimensionality(relation);
    final NumberVector<?, ?> refvec = relation.get(firstid);

    // Build the truth vector
    Set<Integer> pos = new TreeSet<Integer>();
    final DoubleIntPair[] combined = new DoubleIntPair[dim];
    {
      for(int d = 0; d < dim; d++) {
        combined[d] = new DoubleIntPair(0, d);
        if(refvec.doubleValue(d + 1) > 0) {
          pos.add(d);
        }
      }
    }

    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    ids.remove(firstid);
    final int size = ids.size();

    double[][] data = new double[size][size];
    DoubleMinMax minmax = new DoubleMinMax();

    {
      int a = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), a++) {
        final NumberVector<?, ?> veca = relation.get(id);
        // Direct AUC score:
        {
          for(int d = 0; d < dim; d++) {
            combined[d].first = veca.doubleValue(d + 1);
            combined[d].second = d;
          }
          Arrays.sort(combined, Collections.reverseOrder(DoubleIntPair.BYFIRST_COMPARATOR));
          double auc = XYCurve.areaUnderCurve(ROC.materializeROC(dim, pos, Arrays.asList(combined).iterator()));
          data[a][a] = auc;
          // minmax.put(auc);
          // logger.verbose(auc + " " + labels.get(ids.get(a)));
        }
        // Compare to others, exploiting symmetry
        DBIDArrayIter id2 = ids.iter();
        id2.seek(a + 1);
        for(int b = a + 1; b < size; b++, id2.advance()) {
          final NumberVector<?, ?> vecb = relation.get(id2);
          for(int d = 0; d < dim; d++) {
            combined[d].first = veca.doubleValue(d + 1) + vecb.doubleValue(d + 1);
            combined[d].second = d;
          }
          Arrays.sort(combined, Collections.reverseOrder(DoubleIntPair.BYFIRST_COMPARATOR));
          double auc = XYCurve.areaUnderCurve(ROC.materializeROC(dim, pos, Arrays.asList(combined).iterator()));
          // logger.verbose(auc + " " + labels.get(ids.get(a)) + " " +
          // labels.get(ids.get(b)));
          data[a][b] = auc;
          data[b][a] = auc;
          // minmax.put(auc);
        }
      }
    }
    for(int a = 0; a < size; a++) {
      for(int b = a + 1; b < size; b++) {
        double ref = Math.max(data[a][a], data[b][b]);
        data[a][b] = (data[a][b] - ref) / (1 - ref);
        data[b][a] = (data[b][a] - ref) / (1 - ref);
        // logger.verbose(data[a][b] + " " + labels.get(ids.get(a)) + " " +
        // labels.get(ids.get(b)));
        minmax.put(data[a][b]);
      }
    }
    for(int a = 0; a < size; a++) {
      data[a][a] = 0;
    }

    logger.verbose(minmax.toString());

    boolean hasneg = (minmax.getMin() < -1E-3);
    LinearScaling scale;
    if(!hasneg) {
      scale = new LinearScaling(minmax);
    }
    else {
      scale = LinearScaling.fromMinMax(0.0, Math.max(minmax.getMax(), -minmax.getMin()));
    }

    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    for(int x = 0; x < size; x++) {
      for(int y = x; y < size; y++) {
        double val = data[x][y];
        val = scale.getScaled(val);
        // Compute color:
        final int col;
        {
          if(!hasneg) {
            int ival = 0xFF & (int) (255 * Math.max(0, val));
            col = 0xff000000 | (ival << 16) | (ival << 8) | ival;
          }
          else {
            if(val >= 0) {
              int ival = 0xFF & (int) (255 * val);
              col = 0xff000000 | (ival << 8);
            }
            else {
              int ival = 0xFF & (int) (255 * -val);
              col = 0xff000000 | (ival << 16);
            }
          }
        }
        img.setRGB(x, y, col);
        img.setRGB(y, x, col);
      }
    }
    SimilarityMatrix smat = new ComputeSimilarityMatrixImage.SimilarityMatrix(img, relation, ids);
    database.getHierarchy().add(database, smat);

    VisualizerContext context = vispar.newContext(database);

    // Attach visualizers to results
    SimilarityMatrixVisualizer factory = new SimilarityMatrixVisualizer();
    factory.processNewResult(database, database);

    List<VisualizationTask> tasks = ResultUtil.filterResults(database, VisualizationTask.class);
    for(VisualizationTask task : tasks) {
      if(task.getFactory() == factory) {
        showVisualization(context, factory, task);
      }
    }
  }

  /**
   * Show a single visualization.
   * 
   * @param context
   * 
   * @param factory
   * @param task
   */
  private void showVisualization(VisualizerContext context, SimilarityMatrixVisualizer factory, VisualizationTask task) {
    SVGPlot plot = new SVGPlot();
    Visualization vis = factory.makeVisualization(task.clone(plot, context, null, 1.0, 1.0));
    plot.getRoot().appendChild(vis.getLayer());
    plot.getRoot().setAttribute(SVGConstants.SVG_WIDTH_ATTRIBUTE, "20cm");
    plot.getRoot().setAttribute(SVGConstants.SVG_HEIGHT_ATTRIBUTE, "20cm");
    plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 1 1");
    plot.updateStyleElement();

    (new SimpleSVGViewer()).setPlot(plot);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Data source
     */
    InputStep inputstep;

    /**
     * Parameterizer for visualizers
     */
    private VisualizerParameterizer vispar;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      // Visualization options
      vispar = config.tryInstantiate(VisualizerParameterizer.class);
    }

    @Override
    protected AbstractApplication makeInstance() {
      return new VisualizePairwiseGainMatrix(verbose, inputstep, vispar);
    }
  }

  /**
   * Main method.
   * 
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    VisualizePairwiseGainMatrix.runCLIApplication(VisualizePairwiseGainMatrix.class, args);
  }
}