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
package de.lmu.ifi.dbs.elki.application.greedyensemble;

import java.awt.image.BufferedImage;

import org.apache.batik.util.SVGConstants;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.evaluation.scores.ROCEvaluation;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.DecreasingVectorIter;
import de.lmu.ifi.dbs.elki.evaluation.scores.adapter.VectorNonZero;
import de.lmu.ifi.dbs.elki.evaluation.similaritymatrix.ComputeSimilarityMatrixImage;
import de.lmu.ifi.dbs.elki.evaluation.similaritymatrix.ComputeSimilarityMatrixImage.SimilarityMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.ensemble.EnsembleVoting;
import de.lmu.ifi.dbs.elki.utilities.ensemble.EnsembleVotingMean;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.scaling.LinearScaling;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTree;
import de.lmu.ifi.dbs.elki.visualization.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.VisualizerParameterizer;
import de.lmu.ifi.dbs.elki.visualization.gui.SimpleSVGViewer;
import de.lmu.ifi.dbs.elki.visualization.gui.VisualizationPlot;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.visunproj.SimilarityMatrixVisualizer;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Class to load an outlier detection summary file, as produced by
 * {@link ComputeKNNOutlierScores}, and compute a matrix with the pairwise
 * gains. It will have one column / row obtained for each combination.
 * <p>
 * The gain is always computed in relation to the better of the two input
 * methods. Green colors indicate the result has improved, red indicate it
 * became worse.
 * <p>
 * Reference:
 * <p>
 * Erich Schubert, Remigius Wojdanowski, Arthur Zimek, Hans-Peter Kriegel<br>
 * On Evaluation of Outlier Rankings and Outlier Scores<br>
 * Proc. 12th SIAM Int. Conf. on Data Mining (SDM 2012)
 *
 * @author Erich Schubert
 * @since 0.5.0
 *
 * @composed - - - VisualizerParameterizer
 * @composed - - - SimilarityMatrixVisualizer
 */
@Reference(authors = "Erich Schubert, Remigius Wojdanowski, Arthur Zimek, Hans-Peter Kriegel", //
    title = "On Evaluation of Outlier Rankings and Outlier Scores", //
    booktitle = "Proc. 12th SIAM Int. Conf. on Data Mining (SDM 2012)", //
    url = "https://doi.org/10.1137/1.9781611972825.90", //
    bibkey = "DBLP:conf/sdm/SchubertWZK12")
public class VisualizePairwiseGainMatrix extends AbstractApplication {
  /**
   * Get static logger.
   */
  private static final Logging LOG = Logging.getLogger(VisualizePairwiseGainMatrix.class);

  /**
   * The data input part.
   */
  private InputStep inputstep;

  /**
   * Parameterizer for visualizers.
   */
  private VisualizerParameterizer vispar;

  /**
   * Outlier scaling to apply during preprocessing.
   */
  private ScalingFunction prescaling;

  /**
   * Ensemble voting function.
   */
  private EnsembleVoting voting;

  /**
   * Constructor.
   *
   * @param inputstep Input step
   * @param prescaling Scaling function for input scores.
   * @param voting Voting function
   * @param vispar Visualizer parameterizer
   */
  public VisualizePairwiseGainMatrix(InputStep inputstep, ScalingFunction prescaling, EnsembleVoting voting, VisualizerParameterizer vispar) {
    super();
    this.inputstep = inputstep;
    this.prescaling = prescaling;
    this.voting = voting;
    this.vispar = vispar;
  }

  @Override
  public void run() {
    final Database database = inputstep.getDatabase();
    ResultHierarchy hier = database.getHierarchy();
    Relation<NumberVector> relation = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    final Relation<String> labels = DatabaseUtil.guessLabelRepresentation(database);
    final DBID firstid = DBIDUtil.deref(labels.iterDBIDs());
    final String firstlabel = labels.get(firstid);
    if(!firstlabel.matches(".*by.?label.*")) {
      throw new AbortException("No 'by label' reference outlier found, which is needed for weighting!");
    }
    relation = GreedyEnsembleExperiment.applyPrescaling(prescaling, relation, firstid);

    // Dimensionality and reference vector
    final int dim = RelationUtil.dimensionality(relation);
    final NumberVector refvec = relation.get(firstid);

    // Build the truth vector
    VectorNonZero pos = new VectorNonZero(refvec);

    ArrayModifiableDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    ids.remove(firstid);
    ids.sort();
    final int size = ids.size();

    double[][] data = new double[size][size];
    DoubleMinMax minmax = new DoubleMinMax(), commax = new DoubleMinMax();

    {
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing ensemble gain.", size * (size + 1) >> 1, LOG) : null;
      double[] buf = new double[2]; // Vote combination buffer.
      int a = 0;
      for(DBIDIter id = ids.iter(); id.valid(); id.advance(), a++) {
        final NumberVector veca = relation.get(id);
        // Direct AUC score:
        {
          double auc = ROCEvaluation.computeROCAUC(pos, new DecreasingVectorIter(veca));
          data[a][a] = auc;
          // minmax.put(auc);
          LOG.incrementProcessed(prog);
        }
        // Compare to others, exploiting symmetry
        DBIDArrayIter id2 = ids.iter();
        id2.seek(a + 1);
        for(int b = a + 1; b < size; b++, id2.advance()) {
          final NumberVector vecb = relation.get(id2);
          double[] combined = new double[dim];
          for(int d = 0; d < dim; d++) {
            buf[0] = veca.doubleValue(d);
            buf[1] = vecb.doubleValue(d);
            combined[d] = voting.combine(buf);
          }
          double auc = ROCEvaluation.computeROCAUC(pos, new DecreasingVectorIter(DoubleVector.wrap(combined)));
          // logger.verbose(auc + " " + labels.get(ids.get(a)) + " " +
          // labels.get(ids.get(b)));
          data[a][b] = auc;
          data[b][a] = auc;
          commax.put(data[a][b]);
          // minmax.put(auc);
          LOG.incrementProcessed(prog);
        }
      }
      LOG.ensureCompleted(prog);
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

    LOG.verbose("Gain: " + minmax.toString() + " AUC: " + commax.toString());

    boolean hasneg = (minmax.getMin() < -1E-3);
    LinearScaling scale;
    if(!hasneg) {
      scale = LinearScaling.fromMinMax(0., minmax.getMax());
    }
    else {
      scale = LinearScaling.fromMinMax(0.0, Math.max(minmax.getMax(), -minmax.getMin()));
    }
    scale = LinearScaling.fromMinMax(0., .5);

    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    for(int x = 0; x < size; x++) {
      for(int y = x; y < size; y++) {
        double val = data[x][y];
        val = Math.max(-1, Math.min(1., scale.getScaled(val)));
        // Compute color:
        final int col;
        {
          if(val >= 0) {
            int ival = 0xFF & (int) (255 * val);
            col = 0xff000000 | (ival << 8);
          }
          else {
            int ival = 0xFF & (int) (255 * -val);
            col = 0xff000000 | (ival << 16);
          }
        }
        img.setRGB(x, y, col);
        img.setRGB(y, x, col);
      }
    }
    SimilarityMatrix smat = new ComputeSimilarityMatrixImage.SimilarityMatrix(img, relation, ids);
    hier.add(database, smat);

    VisualizerContext context = vispar.newContext(hier, smat);

    // Attach visualizers to results
    SimilarityMatrixVisualizer factory = new SimilarityMatrixVisualizer();
    factory.processNewResult(context, database);

    VisualizationTree.findVis(context).filter(VisualizationTask.class).forEach(task -> {
      if(task.getFactory() == factory) {
        showVisualization(context, factory, task);
      }
    });
  }

  /**
   * Show a single visualization.
   *
   * @param context Visualizer context
   * @param factory Visualizer factory
   * @param task Visualization task
   */
  private void showVisualization(VisualizerContext context, SimilarityMatrixVisualizer factory, VisualizationTask task) {
    VisualizationPlot plot = new VisualizationPlot();
    Visualization vis = factory.makeVisualization(context, task, plot, 1.0, 1.0, null);
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
   */
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * Data source.
     */
    private InputStep inputstep;

    /**
     * Parameterizer for visualizers.
     */
    private VisualizerParameterizer vispar;

    /**
     * Outlier scaling to apply during preprocessing.
     */
    private ScalingFunction prescaling;

    /**
     * Voring function.
     */
    private EnsembleVoting voting;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Data input
      inputstep = config.tryInstantiate(InputStep.class);
      // Visualization options
      vispar = config.tryInstantiate(VisualizerParameterizer.class);

      // Prescaling
      ObjectParameter<ScalingFunction> prescalingP = new ObjectParameter<>(GreedyEnsembleExperiment.Parameterizer.PRESCALING_ID, ScalingFunction.class);
      prescalingP.setOptional(true);
      if(config.grab(prescalingP)) {
        prescaling = prescalingP.instantiateClass(config);
      }

      ObjectParameter<EnsembleVoting> votingP = new ObjectParameter<>(GreedyEnsembleExperiment.Parameterizer.VOTING_ID, EnsembleVoting.class, EnsembleVotingMean.class);
      if(config.grab(votingP)) {
        voting = votingP.instantiateClass(config);
      }
    }

    @Override
    protected VisualizePairwiseGainMatrix makeInstance() {
      return new VisualizePairwiseGainMatrix(inputstep, prescaling, voting, vispar);
    }
  }

  /**
   * Main method.
   *
   * @param args Command line parameters.
   */
  public static void main(String[] args) {
    runCLIApplication(VisualizePairwiseGainMatrix.class, args);
  }
}
