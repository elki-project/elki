/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
import elki.data.type.CombinedTypeInformation;
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
 * SupportVectorClustering then checks if the line between two data points stay
 * inside the sphere in
 * kernel space. Clusters are those points connected by enclosed lines.
 * <p>
 * References:
 * <p>
 * A. Ben-Hur, D. Horn, H. T. Siegelmann, V. Vapnik<br>
 * A Support Vector Method for Clustering<br>
 * Neural Information Processing Systems
 * <p>
 * A. Ben-Hur, H. T. Siegelmann, D. Horn, V. Vapnik<br>
 * A Support Vector Clustering Method<br>
 * International Conference on Pattern Recognition (ICPR)
 * <p>
 * A. Ben-Hur, D. Horn, H. T. Siegelmann, V. Vapnik<br>
 * Support Vector Clustering<br>
 * Journal of Machine Learning Research 2 (2001)
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
@Reference(authors = "A. Ben-Hur, D. Horn, H. T. Siegelmann, V. Vapnik", //
    title = "A Support Vector Method for Clustering", //
    booktitle = "Neural Information Processing Systems", //
    url = "https://proceedings.neurips.cc/paper/2000/hash/14cfdb59b5bda1fc245aadae15b1984a-Abstract.html", //
    bibkey = "DBLP:conf/nips/Ben-HurHSV00")
@Reference(authors = "A. Ben-Hur, H. T. Siegelmann, D. Horn, V. Vapnik", //
    title = "A Support Vector Clustering Method", //
    booktitle = "International Conference on Pattern Recognition (ICPR)", //
    url = "https://doi.org/10.1109/ICPR.2000.906177", //
    bibkey = "DBLP:conf/icpr/Ben-HurSHV00")
@Reference(authors = "A. Ben-Hur, D. Horn, H. T. Siegelmann, V. Vapnik", //
    title = "Support Vector Clustering", //
    booktitle = "Journal of Machine Learning Research", //
    url = "http://jmlr.org/papers/v2/horn01a.html", //
    bibkey = "DBLP:journals/jmlr/Ben-HurHSV01")
public class SupportVectorClustering implements ClusteringAlgorithm<Clustering<? extends Model>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SupportVectorClustering.class);

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
   * 
   * Constructor.
   *
   * @param kernel kernel to use
   * @param C C parameter
   */
  public SupportVectorClustering(PrimitiveSimilarity<? super NumberVector> kernel, double C) {
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
    final StaticDBIDs sids = DBIDUtil.makeUnmodifiable(relation.getDBIDs());

    SimilarityQuery<NumberVector> sim = new QueryBuilder<>(relation, kernel).similarityQuery();

    if(LOG.isVerbose()) {
      LOG.verbose("Training one-class SVM...");
    }
    SimilarityQueryAdapter adapter = new SimilarityQueryAdapter(sim, ids);
    SVDD svm = new SVDD(1e-4, true, 1000 /* MB */, C > 0 ? C : 20. / ids.size());
    RegressionModel model = svm.train(adapter);
    LOG.statistics(new LongStatistic(getClass().getCanonicalName() + ".numsv", model.l));

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Connectivity checks", sids.size(), LOG) : null;
    final double r_square = model.r_square;
    assert !Double.isNaN(r_square) : "Model not trained correctly!";

    UnionFind uf = UnionFindUtil.make(sids);
    final double fixed = calcfixedpart(model, ids, sim);
    for(DBIDIter iiter = sids.iter(); iiter.valid(); iiter.advance()) {
      NumberVector ivec = relation.get(iiter);
      // skip if start is already outside the sphere
      if(accept(ivec, model, fixed, ids, sim, r_square)) {
        double[] start = ivec.toArray();
        // check connection to other points
        for(DBIDIter jiter = sids.iter(); jiter.valid(); jiter.advance()) {
          // skip if already connected
          if(uf.isConnected(iiter, jiter)) {
            // go to next outer if equal (lower left triangle)
            if(DBIDUtil.equal(iiter, jiter)) {
              break; // half matrix only
            }
            continue;
          }

          // union the two points if they are connected
          // (line stays inside circle in kernel space)
          if(checkConnectivity(relation, start, jiter, model, fixed, ids, sim, r_square)) {
            uf.union(iiter, jiter);
          }
        }
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);

    ArrayList<ArrayModifiableDBIDs> groups = collectClusters(sids, uf);
    // create clustering
    ArrayList<Cluster<Model>> clusters = new ArrayList<>(groups.size());
    ArrayModifiableDBIDs noise = null;
    for(ArrayModifiableDBIDs modifiableDBIDs : groups) {
      if(modifiableDBIDs.size() > 1) {
        clusters.add(new Cluster<Model>(modifiableDBIDs));
      }
      else if(noise == null) {
        noise = modifiableDBIDs;
      }
      else {
        noise.addDBIDs(modifiableDBIDs);
      }
    }
    if(noise != null) {
      clusters.add(new Cluster<Model>(noise, true));
    }
    return new Clustering<Model>(clusters);
  }

  /**
   * Checks if the connecting line between start and dest lies inside the kernel
   * space sphere.
   * 
   * @param relation database
   * @param start start vector as array
   * @param destRef dest vector as DBIDRef
   * @param model model containing sphere
   * @param fixed fixed part of evaluation
   * @param ids ArrayDBIDs used to train model
   * @param sim Similarity Query used to train model
   * @param r_squared squared radius of trained model
   * @return true if connected, false if not
   */
  private boolean checkConnectivity(Relation<NumberVector> relation, double[] start, DBIDRef destRef, RegressionModel model, double fixed, ArrayDBIDs ids, SimilarityQuery<NumberVector> sim, double r_squared) {
    NumberVector jvec = relation.get(destRef);
    double[] dest = jvec.toArray();
    double[] step = VMath.timesEquals(VMath.minus(dest, start), 1.0 / lsz);
    if(!accept(jvec, model, fixed, ids, sim, r_squared)) {
      return false;
    }
    // checking "backwards"
    for(int k = 0; k < lsz; k++) {
      // currently checking 19 points between + start + dest
      // this might be numerically more stable (might need k adjustment)
      // cur = new DoubleVector(VMath.minusTimes(dest, step,k));
      if(!accept(DoubleVector.wrap(VMath.minusEquals(dest, step)), //
          model, fixed, ids, sim, r_squared)) {
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
    final double l = sim.similarity(cur, cur);
    double sum = 0;
    DBIDArrayIter iter = ids.iter();
    for(int i = 0; i < model.sv_indices.length; i++) {
      sum += model.sv_coef[0][i] * sim.similarity(cur, iter.seek(model.sv_indices[i]));
    }
    return l - 2 * sum + fixed <= r_square;
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
        eval += model.sv_coef[0][i] * model.sv_coef[0][j] * sim.similarity(iter, iter2.seek(model.sv_indices[j]));
      }
    }
    return eval;
  }

  private ArrayList<ArrayModifiableDBIDs> collectClusters(final StaticDBIDs sids, UnionFind uf) {
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
    return groups;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(new CombinedTypeInformation(kernel.getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD));
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
    public SupportVectorClustering make() {
      return new SupportVectorClustering(kernel, C);
    }
  }
}
