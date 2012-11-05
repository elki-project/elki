package experimentalcode.shared.parallelcoord;

import java.util.ArrayList;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.visualization.projections.ProjectionParallel;

/**
 * Use the statistical tests as used by HiCS to arrange dimensions.
 * 
 * <p>
 * Based on:<br />
 * Fabian Keller, Emmanuel Müller, and Klemens Böhm.<br />
 * HiCS: High Contrast Subspaces for Density-Based Outlier Ranking. <br />
 * In ICDE, pages 1037–1048, 2012.
 * </p>
 * 
 * @author Erich Schubert
 * @author Robert Rödler
 */
public class HiCSDimensionSimilarity implements DimensionSimilarity<NumberVector<?>> {
  /**
   * Monte-Carlo iterations
   */
  private int m = 50;

  /**
   * Alpha threshold
   */
  private double alpha = 0.1;

  /**
   * Statistical test to use
   */
  private GoodnessOfFitTest statTest = new KolmogorovSmirnovTest();

  /**
   * Random generator
   */
  private RandomFactory rnd = RandomFactory.DEFAULT;

  @Override
  public double[][] computeDimensionSimilarites(Relation<? extends NumberVector<?>> relation, ProjectionParallel proj, DBIDs subset) {
    final Random random = rnd.getRandom();
    final int dim = RelationUtil.dimensionality(relation);

    ArrayList<ArrayDBIDs> subspaceIndex = buildOneDimIndexes(relation, subset);

    double[][] mat = new double[dim][dim];
    // compute two-element sets of subspaces
    for (int i = 0; i < dim; i++) {
      for (int j = i + 1; j < dim; j++) {
        double contrast = calculateContrast(relation, subspaceIndex, subset, i, j, random);
        mat[i][j] = contrast;
        mat[j][i] = contrast;
      }
    }
    return mat;
  }

  /**
   * Calculates "index structures" for every attribute, i.e. sorts a
   * ModifiableArray of every DBID in the database for every dimension and
   * stores them in a list
   * 
   * @param relation Relation to index
   * @param ids IDs to use
   * @return List of sorted objects
   */
  private ArrayList<ArrayDBIDs> buildOneDimIndexes(Relation<? extends NumberVector<?>> relation, DBIDs ids) {
    final int dim = RelationUtil.dimensionality(relation);
    ArrayList<ArrayDBIDs> subspaceIndex = new ArrayList<ArrayDBIDs>(dim);

    SortDBIDsBySingleDimension comp = new VectorUtil.SortDBIDsBySingleDimension(relation);
    for (int i = 0; i < dim; i++) {
      ArrayModifiableDBIDs amDBIDs = DBIDUtil.newArray(ids);
      comp.setDimension(i);
      amDBIDs.sort(comp);
      subspaceIndex.add(amDBIDs);
    }

    return subspaceIndex;
  }

  /**
   * Calculates the actual contrast of a given subspace
   * 
   * @param relation Data relation
   * @param subspaceIndex Subspace indexes
   * @param subset Subset to process
   * @param dim1 First dimension
   * @param dim2 Second dimension
   * @param random Random generator
   * @return Contrast
   */
  private double calculateContrast(Relation<? extends NumberVector<?>> relation, ArrayList<ArrayDBIDs> subspaceIndex, DBIDs subset, int dim1, int dim2, Random random) {
    final double alpha1 = Math.pow(alpha, .5);
    final int windowsize = (int) (relation.size() * alpha1);

    // TODO: speed up by keeping marginal distributions prepared.
    // Instead of doing the random switch, do half-half.
    double deviationSum = 0.0;
    for (int i = 0; i < m; i++) {
      // Randomly switch dimensions
      final int cdim1, cdim2;
      if (random.nextDouble() > .5) {
        cdim1 = dim1;
        cdim2 = dim2;
      } else {
        cdim1 = dim2;
        cdim2 = dim1;
      }
      // Build the sample
      DBIDArrayIter iter = subspaceIndex.get(cdim2).iter();
      HashSetModifiableDBIDs conditionalSample = DBIDUtil.newHashSet();
      iter.seek(random.nextInt(subset.size() - windowsize));
      for (int k = 0; k < windowsize && iter.valid(); k++, iter.advance()) {
        conditionalSample.add(iter);
      }
      // Project the data
      double[] fullValues = new double[subset.size()];
      double[] sampleValues = new double[conditionalSample.size()];
      {
        int l = 0, s = 0;
        // Note: we use the sorted index sets.
        for (DBIDIter id = subspaceIndex.get(cdim1).iter(); id.valid(); id.advance(), l++) {
          final double val = relation.get(id).doubleValue(cdim1);
          fullValues[l] = val;
          if (conditionalSample.contains(id)) {
            sampleValues[s] = val;
            s++;
          }
        }
        assert (s == conditionalSample.size());
      }
      double contrast = statTest.deviation(fullValues, sampleValues);
      if (Double.isNaN(contrast)) {
        i--;
        continue;
      }
      deviationSum += contrast;
    }
    return deviationSum / m;
  }
}
