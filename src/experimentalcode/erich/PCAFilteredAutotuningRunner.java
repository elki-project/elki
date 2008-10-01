package experimentalcode.erich;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.varianceanalysis.FilteredEigenPairs;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.varianceanalysis.PCAFilteredRunner;

/**
 * Performs a self-tuning local PCA based on the covariance matrices of given
 * objects. At most the closest 'k' points are used in the calculation and a
 * weight function is applied.
 * 
 * The number of points actually used depends on when the strong eigenvectors
 * exhibit the clearest correlation.
 * 
 * @author Erich Schubert
 */
public class PCAFilteredAutotuningRunner<V extends RealVector<V, ?>> extends PCAFilteredRunner<V> {
  public PCAFilteredAutotuningRunner() {
    super();
  }

  /**
   * Run PCA on a collection of database IDs
   * 
   * @param ids a collection of ids
   * @param database the database used
   * @return PCA result
   */
  @Override
  public PCAFilteredResult processIds(Collection<Integer> ids, Database<V> database) {
    // FIXME: We're only supporting QueryResults for now, add compatibility
    // wrapper?
    return null;
  }

  /**
   * Run PCA on a QueryResult Collection
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @return PCA result
   */
  @Override
  public PCAFilteredResult processQueryResult(Collection<QueryResult<DoubleDistance>> results, Database<V> database) {
    assertSortedByDistance(results);
    int dim = database.dimensionality();

    List<Matrix> best = new LinkedList<Matrix>();
    for(int i = 0; i < dim; i++)
      best.add(null);
    double[] beststrength = new double[dim];
    for(int i = 0; i < dim; i++)
      beststrength[i] = -1;
    int[] bestk = new int[dim];
    // 'history'
    LinkedList<Matrix> prevM = new LinkedList<Matrix>();
    LinkedList<Double> prevS = new LinkedList<Double>();
    LinkedList<Integer> prevD = new LinkedList<Integer>();
    // TODO: starting parameter shouldn't be hardcoded...
    int smooth = 3;
    int startk = 4;
    if(startk > results.size() - 1)
      startk = results.size() - 1;
    // TODO: add smoothing options, handle border cases better.
    for(int k = startk; k < results.size(); k++) {
      // sorted eigenpairs, eigenvectors, eigenvalues
      Matrix covMat = covarianceMatrixBuilder.processQueryResults(results, database);
      EigenvalueDecomposition evd = covMat.eig();
      SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, false);
      FilteredEigenPairs filteredEigenPairs = getEigenPairFilter().filter(eigenPairs);
      
      // correlationDimension = #strong EV
      int thisdim = filteredEigenPairs.countStrongEigenPairs();
      
      // FIXME: handle the case of no strong EVs.
      assert ((thisdim > 0) && (thisdim <= dim));
      double thisexplain = computeExplainedVariance(filteredEigenPairs);

      prevM.add(covMat);
      prevS.add(thisexplain);
      prevD.add(thisdim);
      assert (prevS.size() == prevM.size());
      assert (prevS.size() == prevD.size());

      if(prevS.size() >= 2 * smooth + 1) {
        // all the same dimension?
        boolean samedim = true;
        for(Iterator<Integer> it = prevD.iterator(); it.hasNext();)
          if(it.next().intValue() != thisdim)
            samedim = false;
        if(samedim) {
          // average their explain values
          double avgexplain = 0.0;
          for(Iterator<Double> it = prevS.iterator(); it.hasNext();)
            avgexplain += it.next().doubleValue();
          avgexplain /= prevS.size();

          if(avgexplain > beststrength[thisdim - 1]) {
            beststrength[thisdim - 1] = avgexplain;
            best.set(thisdim - 1, prevM.get(smooth));
            bestk[thisdim - 1] = k - smooth;
          }
        }
        prevM.removeFirst();
        prevS.removeFirst();
        prevD.removeFirst();
        assert (prevS.size() == prevM.size());
        assert (prevS.size() == prevD.size());
      }
    }
    // Try all dimensions, lowest first.
    for(int i = 0; i < dim; i++)
      if(beststrength[i] > 0.0) {
        // If the best was the lowest or the biggest k, skip it!
        if(bestk[i] == startk + smooth)
          continue;
        if(bestk[i] == results.size() - smooth - 1)
          continue;
        Matrix covMat = best.get(i);
        
        // We stop at the lowest dimension that did the job for us.
        // System.err.println("Auto-k: "+bestk[i]+" dim: "+(i+1));
        return processCovarMatrix(covMat);
      }
    // NOTE: if we didn't get a 'maximum' anywhere, we end up with the data from
    // the last
    // run of the loop above. I.e. PCA on the full data set. That is intended.
    
    return processCovarMatrix(covarianceMatrixBuilder.processQueryResults(results, database));
  }

  /**
   * Compute the explained variance for a FilteredEigenPairs
   * @param filteredEigenPairs
   * @return explained variance by the strong eigenvectors.
   */
  private double computeExplainedVariance(FilteredEigenPairs filteredEigenPairs) {
    double strongsum = 0.0;
    double weaksum = 0.0;
    for (EigenPair ep : filteredEigenPairs.getStrongEigenPairs())
      strongsum += ep.getEigenvalue();
    for (EigenPair ep : filteredEigenPairs.getWeakEigenPairs())
      weaksum += ep.getEigenvalue();
    return strongsum / (strongsum / weaksum);
  }

  /**
   * Ensure that the results are sorted by distance.
   * 
   * @param results
   */
  private void assertSortedByDistance(Collection<QueryResult<DoubleDistance>> results) {
    // TODO: sort results instead?
    double dist = -1.0;
    for(Iterator<QueryResult<DoubleDistance>> it = results.iterator(); it.hasNext();) {
      QueryResult<DoubleDistance> qr = it.next();
      if(qr.getDistance().getValue() < dist) {
        System.err.println("WARNING: results not sorted by distance!");
      }
      dist = qr.getDistance().getValue();
    }
  }
}
