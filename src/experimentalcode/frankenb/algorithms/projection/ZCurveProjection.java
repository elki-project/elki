package experimentalcode.frankenb.algorithms.projection;

import java.math.BigInteger;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ifaces.IProjection;
import experimentalcode.frankenb.utils.ZCurve;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ZCurveProjection<V extends NumberVector<V, ?>> implements IProjection<V> {

  public ZCurveProjection(Parameterization config) {
  }

  @Override
  public Relation<V> project(Relation<V> dataSet) throws UnableToComplyException {
    try {
      SimpleTypeInformation<V> type = new VectorFieldTypeInformation<V>(dataSet.getDataTypeInformation().getRestrictionClass(), 1);
      Relation<V> resultDataSet = new MaterializedRelation<V>(dataSet.getDatabase(), type, dataSet.getDBIDs());

      List<Pair<DBID, BigInteger>> projection = ZCurve.projectToZCurve(dataSet);

      V factory = DatabaseUtil.assumeVectorField(dataSet).getFactory();

      for(Pair<DBID, BigInteger> pair : projection) {
        DBID id = pair.first;
        double doubleProjection = pair.second.doubleValue();

        resultDataSet.set(id, factory.newInstance(new double[] { doubleProjection }));
      }

      return resultDataSet;
    }
    catch(RuntimeException e) {
      throw e;
    }
    catch(Exception e) {
      throw new UnableToComplyException(e);
    }
  }

}
