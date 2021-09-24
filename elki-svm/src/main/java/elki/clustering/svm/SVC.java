/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.clustering.svm;

import java.util.ArrayList;

import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;
import elki.similarity.PrimitiveSimilarity;
import elki.similarity.kernel.RadialBasisFunctionKernel;
import elki.svm.SVDD;
import elki.svm.SVDD.RadiusAcceptor;
import elki.svm.data.SimilarityQueryAdapter;
import elki.svm.model.RegressionModel;
import elki.utilities.datastructures.unionfind.UnionFind;
import elki.utilities.datastructures.unionfind.UnionFindUtil;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Support Vector Clustering works on SVDD, which tries to find the smallest
 * sphere enclosing all objects in kernel space.
 * SVC then checks if the line between two data points stay inside the sphere in
 * kernel space. Clusters are those points connected by enclosed lines.
 * 
 * <p>
 * Reference:
 * <p>
 * Asa Ben-Hur, David Horn, Hava T. Siegelmann, Vladimir Vapnik<br>
 * Support Vector Clustering<br>
 * Journal of Machine Learning Research 2 (2001)
 * 
 * @author Robert Gehde
 *
 */
@Reference(authors = "Asa Ben-Hur, David Horn, Hava T. Siegelmann, Vladimir Vapnik", //
    booktitle = "Journal of Machine Learning Research (2001)", //
    url = "https://jmlr.csail.mit.edu/papers/v2/", //
    title = "Support Vector Clustering")
public class SVC implements RadiusAcceptor, ClusteringAlgorithm<Clustering<? extends Model>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SVC.class);

  /**
   * Kernel function.
   */
  PrimitiveSimilarity<? super NumberVector> kernel;

  /**
   * C parameter.
   */
  double C = 5;

  /**
   * Sample size for line check.
   * (lsz-1 are between the points, last is on point)
   */
  int lsz = 21;

  /**
   * Squared radius of model.
   */
  double r_square = Double.NaN;

  /**
   * 
   * Constructor.
   *
   * @param kernel kernel to use
   * @param C C parameter
   */
  public SVC(PrimitiveSimilarity<? super NumberVector> kernel, double C) {
    this.kernel = kernel;
    this.C = C;
  }

  /**
   * perform clustering
   * 
   * @param relation relation to cluster
   * @return clustering
   */
  public Clustering<Model> run(Relation<NumberVector> relation) {
    final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final StaticDBIDs sids = DBIDUtil.makeUnmodifiable(ids);

    SimilarityQuery<NumberVector> sim = new QueryBuilder<>(relation, kernel).similarityQuery();

    if(LOG.isVerbose()) {
      LOG.verbose("Training one-class SVM...");
    }
    SimilarityQueryAdapter adapter = new SimilarityQueryAdapter(sim, ids);
    SVDD svm = new SVDD(1e-4, true, 1000 /* MB */, C > 0 ? C : 20. / ids.size(), this);
    RegressionModel model = svm.train(adapter);
    LOG.statistics(new LongStatistic(getClass().getCanonicalName() + ".numsv", model.l));

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Outer loop", relation.size(), LOG) : null;

    if(LOG.isVerbose()) {
      LOG.verbose("Checking connectivity...");
    }
    
    assert r_square != Double.NaN : "Model not trained correctly!";
    
    if(prog != null) {
      prog.setProcessed(0, LOG);
    }
    UnionFind uf = UnionFindUtil.make(sids);
    double fixed = calcfixedpart(model, ids, sim);
    outer: for(DBIDIter iiter = sids.iter(); iiter.valid(); iiter.advance()) {
      NumberVector ivec = relation.get(iiter);
      // skip if start is already outside the sphere
      if(!accept(ivec, model, fixed, ids, sim, r_square)) {
        continue;
      }
      double[] start = ivec.toArray();
      // check connection to other points
      for(DBIDIter jiter = sids.iter(); jiter.valid(); jiter.advance()) {
        // skip if already connected
        if(uf.isConnected(iiter, jiter)) {
          // go to next outer if equal (lower left triangle)
          if(DBIDUtil.equal(iiter, jiter)) {
            if(prog != null) {
              prog.incrementProcessed(LOG);
            }
            // row is done, go to next
            continue outer;
          }
          continue;
        }

        // union the two points if they are connected
        // (line stays inside circle in kernel space)
        if(checkConnectivity(relation, start, jiter, model, fixed, ids, sim, r_square)) {
          uf.union(iiter, jiter);
        }
      }
      if(prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if(prog != null) {
      prog.setProcessed(relation.size(), LOG);
    }

    if(LOG.isVerbose()) {
      LOG.verbose("Building clusters...");
    }

    // collect dbids
    ArrayList<ArrayModifiableDBIDs> groups = new ArrayList<>();

    // sort DBIDs into clusters
    idloop: for(DBIDIter it = sids.iter(); it.valid(); it.advance()) {
      for(ModifiableDBIDs modifiableDBIDs : groups) {
        if(uf.isConnected(modifiableDBIDs.iter(), it)) {
          modifiableDBIDs.add(it);
          continue idloop;
        }
      }
      ArrayModifiableDBIDs temp = DBIDUtil.newArray();
      temp.add(it);
      groups.add(temp);
    }
    // create clustering
    ArrayList<Cluster<Model>> clusters = new ArrayList<>();
    for(ModifiableDBIDs modifiableDBIDs : groups) {
      clusters.add(new Cluster<Model>(modifiableDBIDs));
    }
    Clustering<Model> result = new Clustering<Model>(clusters);
    return result;
  }

  /**
   * 
   * Checks if the connecting line between start and dest lies inside the kernel
   * space sphere.
   * 
   * @param relation database
   * @param start start vector as array
   * @param destRef dest vector as DBIDRef
   * @param model model containing sphere
   * @param fixed fixed part of evaluation
   * @param ids ArrayDBIDs used to train model
   * @param sim Similary Query used to train model
   * @param r_squared squared radius of trained model
   * @return true if connected, false if not
   */
  private boolean checkConnectivity(Relation<NumberVector> relation, double[] start, DBIDRef destRef, RegressionModel model, double fixed, ArrayDBIDs ids, SimilarityQuery<NumberVector> sim, double r_squared) {
    NumberVector jvec = relation.get(destRef);
    double[] dest = jvec.toArray();
    double[] step = VMath.timesEquals(VMath.minus(dest, start), 1.0 / lsz);
    NumberVector cur = new DoubleVector(dest);
    if(!accept(cur, model, fixed, ids, sim, r_squared)) {
      return false;
    }
    // checking "backwards"
    for(int k = 0; k < lsz; k++) {
      // currently checking 19 points between + start + dest
      cur = new DoubleVector(VMath.minusEquals(dest, step));
      // this might be numerically more stable (might need k adjustment)
      // cur = new DoubleVector(VMath.minusTimes(dest, step,k));
      if(!accept(cur, model, fixed, ids, sim, r_squared/*r_square*/)) {
        return false;
      }
    }
    return true;
  }

  /**
   * evaluate if a point cur is inside the sphere in kernel space.
   * 
   * @param cur point to evaluate
   * @param model Model to check the point in
   * @param fixed fixed part of calculation
   * @param ids IDs used for access
   * @param sim kernel similarity query
   * @param r_square squared radius
   * @return true iff point is inside sphere
   */
  private boolean accept(NumberVector cur, RegressionModel model, double fixed, ArrayDBIDs ids, SimilarityQuery<NumberVector> sim, double r_square) {
    double eval = sim.similarity(cur, cur);

    double sum = 0;
    DBIDArrayIter iter = ids.iter();
    for(int i = 0; i < model.sv_indices.length; i++) {
      iter.seek(model.sv_indices[i]);
      sum += model.sv_coef[0][i] * sim.similarity(cur, iter);
    }
    eval -= 2 * sum;
    eval += fixed;
    return eval <= r_square;
  }

  /**
   * calculate fixed part of model evaluation
   * 
   * @param model model to calculate the fixed part for
   * @param ids IDs for access
   * @param sim kernel similarity query
   * @return fixed part of evaluation
   */
  private double calcfixedpart(RegressionModel model, ArrayDBIDs ids, SimilarityQuery<NumberVector> sim) {
    double eval = 0;
    DBIDArrayIter iter = ids.iter(), iter2 = ids.iter();
    for(int i = 0; i < model.sv_indices.length; i++) {
      iter.seek(model.sv_indices[i]);
      for(int j = 0; j < model.sv_indices.length; j++) {
        iter.seek(model.sv_indices[j]);
        eval += model.sv_coef[0][i] * model.sv_coef[0][j] * sim.similarity(iter, iter2);
      }
    }
    return eval;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  public void setRSquare(double r_square) {
    this.r_square = r_square;
  }

  /**
   * Parameterization class.
   * 
   * @author Robert Gehde
   * 
   * @hidden
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter for kernel function.
     */
    public static final OptionID KERNEL_ID = new OptionID("svm.kernel", "Kernel to use with SVM.");

    /**
     * SVM C parameter
     */
    public static final OptionID C_ID = new OptionID("svm.C", "SVM C parameter.");

    /**
     * Kernel in use.
     */
    protected PrimitiveSimilarity<? super NumberVector> kernel;

    /**
     * C parameter.
     */
    protected double C;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<PrimitiveSimilarity<? super NumberVector>>(KERNEL_ID, PrimitiveSimilarity.class, RadialBasisFunctionKernel.class) //
          .grab(config, x -> kernel = x);
      new DoubleParameter(C_ID, 0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> C = x);
    }

    @Override
    public SVC make() {
      return new SVC(kernel, C);
    }
  }
}
