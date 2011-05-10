package experimentalcode.frankenb.model.ifaces;

import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;


/**
 * Projects a given {@link IDataSet} to another {@link IDataSet}
 * 
 * @author Florian Frankenberger
 */
public interface IProjection {

  public IDataSet project(IDataSet dataSet) throws UnableToComplyException;
  
}
