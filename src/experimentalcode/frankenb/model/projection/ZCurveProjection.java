/**
 * 
 */
package experimentalcode.frankenb.model.projection;

import java.math.BigInteger;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.DataSet;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IProjection;
import experimentalcode.frankenb.utils.ZCurve;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ZCurveProjection implements IProjection {

  public ZCurveProjection(Parameterization config) {
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IProjection#project(experimentalcode.frankenb.model.ifaces.IDataSet)
   */
  @Override
  public IDataSet project(IDataSet dataSet) throws UnableToComplyException {
    try {
      DataSet resultDataSet = new DataSet(dataSet.getOriginal(), 1);

      List<Pair<Integer, BigInteger>> projection = ZCurve.projectToZCurve(dataSet);
      
      for (Pair<Integer, BigInteger> pair : projection) {
        int id = pair.first;
        double doubleProjection = pair.second.doubleValue();
        
        resultDataSet.add(id, new DoubleVector(new double[] { doubleProjection }));
      }
      
      return resultDataSet;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }

}
