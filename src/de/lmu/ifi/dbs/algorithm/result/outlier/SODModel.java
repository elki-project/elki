package de.lmu.ifi.dbs.algorithm.result.outlier;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

/**
 * @author Arthur Zimek
 */
public class SODModel<O extends RealVector<O,Double>> extends AbstractResult<O> {

  private O center;
  
  private double[] variances;
  
  public SODModel(Database<O> database, O queryObject, List<Integer> neighborhood){
    super(database);
    center = queryObject.nullVector();
    double[] centerValues = new double[center.getDimensionality()];
    variances = new double[center.getDimensionality()];
    for(Iterator<Integer> iter = neighborhood.iterator(); iter.hasNext();){
      O databaseObject = database.get(iter.next());
      for(int d = 0; d < center.getDimensionality(); d++){
        centerValues[d] += databaseObject.getValue(d+1);
      }
    }
    for(Iterator<Integer> iter = neighborhood.iterator(); iter.hasNext();){
      O databaseObject = database.get(iter.next());
      for(int d = 0; d < center.getDimensionality(); d++){
        // distance
        double distance = centerValues[d] - databaseObject.getValue(d+1);
        // variance
        variances[d] += distance * distance; 
      }
    }
  }
  
  
  
  /**
   * 
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    // TODO Auto-generated method stub

  }

}
