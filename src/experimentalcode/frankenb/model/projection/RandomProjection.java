/**
 * 
 */
package experimentalcode.frankenb.model.projection;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.model.DataSet;
import experimentalcode.frankenb.model.ifaces.IDataSet;
import experimentalcode.frankenb.model.ifaces.IProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjection implements IProjection {

  public static final OptionID SPARSITY_ID = OptionID.getOrCreateOptionID("sparsity", "frequency of zeros in the projection matrix");
  private final IntParameter SPARSITY_PARAM = new IntParameter(SPARSITY_ID, 3);
  
  public static final OptionID NEW_DIMENSIONALITY_ID = OptionID.getOrCreateOptionID("newdimensionality", "amount of dimensions to project down to");
  private final IntParameter NEW_DIMENSIONALITY_PARAM = new IntParameter(NEW_DIMENSIONALITY_ID, false);    
  
  private int sparsity;
  private int newDimensionality;
  
  public RandomProjection(Parameterization config) {
    if (config.grab(SPARSITY_PARAM)) {
      this.sparsity = SPARSITY_PARAM.getValue();
    }
    
    if (config.grab(NEW_DIMENSIONALITY_PARAM)) {
      this.newDimensionality = NEW_DIMENSIONALITY_PARAM.getValue();
    }
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IProjection#project(experimentalcode.frankenb.model.ifaces.IDataSet)
   */
  @Override
  public IDataSet project(IDataSet dataSet) throws UnableToComplyException {
    if (dataSet.getDimensionality() <= this.newDimensionality) {
      throw new UnableToComplyException("New dimension is higher or equal to the old one!");
    }
    DataSet projectedDataSet = new DataSet(dataSet.getOriginal(), this.newDimensionality);
    
    Matrix projectionMatrix = new Matrix(this.newDimensionality, dataSet.getDimensionality());

    double possibilityOne = 1.0 / (double) sparsity;
    double baseValuePart = Math.sqrt(this.sparsity);
    
    Random random = new Random(System.currentTimeMillis());
    for (int i = 0; i < this.newDimensionality; ++i) {
      for (int j = 0; j < dataSet.getDimensionality(); ++j) {
        double rnd = random.nextDouble();
        double value = baseValuePart;
        
        //possibility 1/s => * +1
        if (rnd < possibilityOne) {
        } else
          //possibility 1/s => * -1
          if (rnd < 2.0 * possibilityOne) {
            value *= -1;
          } else {
            //else * 0!
            value = 0;
          }
        
        projectionMatrix.set(i, j, value);
      }
    }
    
    for (int id : dataSet.getIDs()) {
      DoubleVector result = new DoubleVector(projectionMatrix.times(dataSet.get(id).getColumnVector()));
      projectedDataSet.add(id, result);
    }
    
    System.out.println(projectionMatrix);
    
    return projectedDataSet;
  }

}
