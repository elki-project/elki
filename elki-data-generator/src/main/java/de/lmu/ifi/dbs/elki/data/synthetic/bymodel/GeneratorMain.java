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
package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Generate a data set according to a given model.
 *
 * Key idea of this generator is to re-generate points if they are more likely
 * to belong to a different cluster than the one they were generated for. The
 * benefit is that we should end up with a data set that follows closely the
 * model that we specified.
 *
 * The drawbacks are that on one hand, specifications might be unsatisfiable.
 * For this a retry count is kept and an {@link AbortException} is
 * thrown when the maximum number of retries is exceeded.
 *
 * On the other hand, the model might not be exactly as specified. When the
 * generator reports an "Density correction factor estimation" that differs from
 * 1.0 this is an indication that the result is not exact.
 *
 * On the third hand, rejecting points introduces effects where one generator
 * can influence others, so random generator results will not be stable with
 * respect to the addition of new dimensions and similar if there are any
 * rejects involved. So this generator is not entirely optimal for generating
 * data sets for scalability tests on the number of dimensions, although if
 * clusters overlap little enough (so that no rejects happen) the results should
 * be as expected.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @has - - - GeneratorInterface
 * @has - - - Model
 * @composed - - - AssignPoint
 */
public class GeneratorMain {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GeneratorMain.class);

  /**
   * List of clusters to generate.
   */
  protected ArrayList<GeneratorInterface> generators = new ArrayList<>();

  /**
   * Controls whether points are tested against the model during generation.
   */
  protected boolean testAgainstModel = true;

  /**
   * Pattern, which clusters (e.g. "Noise") to relabel by the second best
   * cluster.
   */
  protected Pattern relabelClusters = null;

  /**
   * Relabel objects by distance.
   */
  protected boolean relabelDistance = false;

  /**
   * Add a cluster to the cluster list.
   *
   * @param c cluster to add
   */
  public void addCluster(GeneratorInterface c) {
    generators.add(c);
  }

  /**
   * Main loop to generate data set.
   *
   * @return Generated data set
   */
  public MultipleObjectsBundle generate() {
    // we actually need some clusters.
    if(generators.isEmpty()) {
      throw new AbortException("No clusters specified.");
    }
    // Assert that cluster dimensions agree.
    final int dim = generators.get(0).getDim();
    for(GeneratorInterface c : generators) {
      if(c.getDim() != dim) {
        throw new AbortException("Cluster dimensions do not agree.");
      }
    }
    // Prepare result bundle
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    VectorFieldTypeInformation<DoubleVector> type = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    bundle.appendColumn(type, new ArrayList<>());
    bundle.appendColumn(TypeUtil.CLASSLABEL, new ArrayList<>());
    bundle.appendColumn(Model.TYPE, new ArrayList<Model>());

    // generate clusters
    ClassLabel[] labels = new ClassLabel[generators.size()];
    Model[] models = new Model[generators.size()];
    initLabelsAndModels(generators, labels, models, relabelClusters);
    final AssignPoint assignment;
    if(!testAgainstModel) {
      assignment = new AssignPoint();
    }
    else if(relabelClusters == null) {
      assignment = new TestModel();
    }
    else if(!relabelDistance) {
      assignment = new AssignLabelsByDensity(labels);
    }
    else {
      assignment = new AssignLabelsByDistance(labels);
    }
    for(int i = 0; i < labels.length; i++) {
      final GeneratorInterface curclus = generators.get(i);
      assignment.newCluster(i, curclus);
      // Only dynamic generators allow rejection / model testing:
      GeneratorInterfaceDynamic cursclus = (curclus instanceof GeneratorInterfaceDynamic) ? (GeneratorInterfaceDynamic) curclus : null;
      int kept = 0;
      while(kept < curclus.getSize()) {
        // generate the "missing" number of points
        List<double[]> newp = curclus.generate(curclus.getSize() - kept);
        for(double[] p : newp) {
          int bestc = assignment.getAssignment(i, p);
          if(bestc < 0) {
            cursclus.incrementDiscarded();
            continue;
          }
          bundle.appendSimple(DoubleVector.wrap(p), labels[bestc], models[bestc]);
          ++kept;
        }
      }
    }
    return bundle;
  }

  /**
   * Point (re-)assignment strategy. Default: no change.
   *
   * @author Erich Schubert
   * @hidden
   */
  private static class AssignPoint {
    /**
     * Assign a point to a cluster.
     *
     * @param i Cluster number
     * @param p Point
     * @return New cluster number.
     */
    public int getAssignment(int i, double[] p) {
      return i;
    }

    /**
     * Set the current cluster.
     *
     * @param i Current cluster.
     * @param curclus Generator
     */
    public void newCluster(int i, GeneratorInterface curclus) {
      // Ignore by default.
    }
  }

  /**
   * Reject objects with a higher density in another generator.
   *
   * @author Erich Schubert
   */
  private class TestModel extends AssignPoint {
    @Override
    public int getAssignment(int i, double[] p) {
      int bestc = i;
      GeneratorInterface curclus = generators.get(i);
      double is = curclus.getDensity(p) * curclus.getSize();
      for(int j = 0; j < generators.size(); j++) {
        if(j == i) {
          continue;
        }
        // Compute density by each (non-reassigned) generator:
        final GeneratorInterface other = generators.get(j);
        final double dens = other.getDensity(p) * other.getSize();
        // Model testing:
        if(dens > is) {
          return -1;
        }
      }
      return bestc;
    }
  }

  /**
   * Reassign objects in certain labels; but also always test against the model.
   *
   * @author Erich Schubert
   */
  private class AssignLabelsByDensity extends AssignPoint {
    /**
     * Cluster labels.
     */
    private ClassLabel[] labels;

    /**
     * Current cluster generator.
     */
    private GeneratorInterface curclus;

    /**
     * Constructor.
     *
     * @param labels Cluster labels
     */
    public AssignLabelsByDensity(ClassLabel[] labels) {
      this.labels = labels;
    }

    @Override
    public void newCluster(int i, GeneratorInterface curclus) {
      this.curclus = curclus;
    }

    @Override
    public int getAssignment(int i, double[] p) {
      double is = curclus.getDensity(p) * curclus.getSize();
      int bestc = i;
      boolean reassign = labels[i] == null;
      double bestd = reassign ? Double.NEGATIVE_INFINITY : is;
      for(int j = 0; j < generators.size(); j++) {
        if(j == i) {
          continue;
        }
        // Compute density by each (non-reassigned) generator:
        final GeneratorInterface other = generators.get(j);
        final double dens = other.getDensity(p) * other.getSize();
        // Model testing:
        if(dens > is) {
          return -1;
        }
        // Reassignment logic:
        if(reassign && labels[j] != null && dens > bestd) {
          bestd = dens;
          bestc = j;
        }
      }
      return bestc;
    }
  }

  /**
   * Reassign objects in certain labels; but also always test against the model.
   *
   * @author Erich Schubert
   */
  private class AssignLabelsByDistance extends AssignPoint {
    /**
     * Cluster centers.
     */
    private double[][] centers;

    /**
     * Constructor.
     *
     * @param labels Cluster labels
     */
    public AssignLabelsByDistance(ClassLabel[] labels) {
      // Compute cluster centers *except* for those to be reassigned.
      this.centers = clusterCenters(generators, labels);
    }

    /**
     * Compute the cluster centers for each cluster.
     *
     * @param generators Generators
     * @param labels Labels ({@code null} if not needed)
     * @return Cluster centers
     */
    private double[][] clusterCenters(ArrayList<GeneratorInterface> generators, ClassLabel[] labels) {
      final int l = generators.size();
      double[][] vs = new double[l][];
      for(int i = 0; i < l; i++) {
        if(labels[i] == null) {
          continue; // Will be reassigned
        }
        vs[i] = generators.get(i).computeMean();
      }
      return vs;
    }

    @Override
    public int getAssignment(int i, double[] p) {
      int bestc = i;
      boolean reassign = (centers[i] == null);
      double is = reassign ? 0. : SquaredEuclideanDistanceFunction.STATIC.distance(centers[i], p);
      double bestd = reassign ? Double.POSITIVE_INFINITY : is;
      for(int j = 0; j < generators.size(); j++) {
        if(centers[j] == null) {
          continue;
        }
        // Compute distance by each (non-reassigned) generator:
        final double dist = SquaredEuclideanDistanceFunction.STATIC.distance(centers[j], p);
        // Model testing:
        if(dist < bestd) {
          if(!reassign) {
            return -1;
          }
          bestd = dist;
          bestc = j;
        }
      }
      return bestc;
    }
  }

  /**
   * Initialize cluster labels and models.
   *
   * Clusters that are set to "reassign" will have their labels set to null, or
   * if there is only one possible reassignment, to this target label.
   *
   * @param generators Cluster generators
   * @param labels Labels (output)
   * @param models Models (output)
   * @param reassign Pattern for clusters to reassign.
   */
  private void initLabelsAndModels(ArrayList<GeneratorInterface> generators, ClassLabel[] labels, Model[] models, Pattern reassign) {
    int existingclusters = 0;
    if(reassign != null) {
      for(int i = 0; i < labels.length; i++) {
        final GeneratorInterface curclus = generators.get(i);
        if(!reassign.matcher(curclus.getName()).find()) {
          labels[i] = new SimpleClassLabel(curclus.getName());
          models[i] = curclus.makeModel();
          ++existingclusters;
        }
      }
      if(existingclusters == 0) {
        LOG.warning("All clusters matched the 'reassign' pattern. Ignoring.");
      }
      if(existingclusters == 1) {
        // No need to test - only one possible answer.
        for(int i = 0; i < labels.length; i++) {
          if(labels[i] != null) {
            Arrays.fill(labels, labels[i]);
            Arrays.fill(models, models[i]);
            break;
          }
        }
      }
      if(existingclusters == labels.length) {
        LOG.warning("No clusters matched the 'reassign' pattern.");
      }
    }
    // Default case, every cluster has a label and model.
    if(existingclusters == 0) {
      for(int i = 0; i < labels.length; i++) {
        final GeneratorInterface curclus = generators.get(i);
        labels[i] = new SimpleClassLabel(curclus.getName());
        models[i] = curclus.makeModel();
      }
    }
  }

  /**
   * Return value of the {@link #testAgainstModel} flag.
   *
   * @return value of testAgainstModel
   */
  public boolean isTestAgainstModel() {
    return testAgainstModel;
  }

  /**
   * Set the value of the {@link #testAgainstModel} flag.
   *
   * @param testAgainstModel New value
   */
  public void setTestAgainstModel(boolean testAgainstModel) {
    this.testAgainstModel = testAgainstModel;
  }

  /**
   * Access the generators.
   *
   * @return generators
   */
  public List<GeneratorInterface> getGenerators() {
    return Collections.unmodifiableList(generators);
  }

  /**
   * Set the reassignment pattern.
   *
   * @param reassign Reassignment pattern.
   */
  public void setReassignPattern(Pattern reassign) {
    this.relabelClusters = reassign;
  }

  /**
   * Relabel objects by distance, instead of by density.
   *
   * @param bydistance Boolean when to use distances.
   */
  public void setReassignByDistance(boolean bydistance) {
    this.relabelDistance = bydistance;
  }
}