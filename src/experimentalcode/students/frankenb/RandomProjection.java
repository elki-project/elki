package experimentalcode.students.frankenb;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjection<V extends NumberVector<V, ?>> {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(RandomProjection.class);

  public static final OptionID SPARSITY_ID = OptionID.getOrCreateOptionID("sparsity", "frequency of zeros in the projection matrix");

  private final IntParameter SPARSITY_PARAM = new IntParameter(SPARSITY_ID, 3);

  public static final OptionID NEW_DIMENSIONALITY_ID = OptionID.getOrCreateOptionID("newdimensionality", "amount of dimensions to project down to");

  private final IntParameter NEW_DIMENSIONALITY_PARAM = new IntParameter(NEW_DIMENSIONALITY_ID, false);

  private int sparsity;

  private int newDimensionality;

  public RandomProjection(Parameterization config) {
    if(config.grab(SPARSITY_PARAM)) {
      this.sparsity = SPARSITY_PARAM.getValue();
    }

    if(config.grab(NEW_DIMENSIONALITY_PARAM)) {
      this.newDimensionality = NEW_DIMENSIONALITY_PARAM.getValue();
    }
  }

  public Relation<V> project(Relation<V> dataSet) throws UnableToComplyException {
    if(DatabaseUtil.dimensionality(dataSet) <= this.newDimensionality) {
      throw new UnableToComplyException("New dimension is higher or equal to the old one!");
    }
    SimpleTypeInformation<V> type = new VectorFieldTypeInformation<V>(dataSet.getDataTypeInformation().getRestrictionClass(), this.newDimensionality);
    Relation<V> projectedDataSet = new MaterializedRelation<V>(dataSet.getDatabase(), type, dataSet.getDBIDs());

    Matrix projectionMatrix = new Matrix(this.newDimensionality, DatabaseUtil.dimensionality(dataSet));

    double possibilityOne = 1.0 / (double) sparsity;
    double baseValuePart = Math.sqrt(this.sparsity);

    Random random = new Random(System.currentTimeMillis());
    for(int i = 0; i < this.newDimensionality; ++i) {
      for(int j = 0; j < DatabaseUtil.dimensionality(dataSet); ++j) {
        double rnd = random.nextDouble();
        double value = baseValuePart;

        // possibility 1/s => * +1
        if(rnd < possibilityOne) {
        }
        else
        // possibility 1/s => * -1
        if(rnd < 2.0 * possibilityOne) {
          value *= -1;
        }
        else {
          // else * 0!
          value = 0;
        }

        projectionMatrix.set(i, j, value);
      }
    }

    V factory = DatabaseUtil.assumeVectorField(dataSet).getFactory();
    for(DBID id : dataSet.iterDBIDs()) {
      V result = factory.newNumberVector(projectionMatrix.times(dataSet.get(id).getColumnVector()).getArrayRef());
      projectedDataSet.set(id, result);
    }

    if(logger.isDebugging()) {
      logger.debug("Projection Matrix:\n" + projectionMatrix.toString());
    }

    return projectedDataSet;
  }
}