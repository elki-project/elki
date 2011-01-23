/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;


/**
 * Projects a given {@link IDataSet} to another {@link IDataSet}
 * 
 * @author Florian Frankenberger
 */
public interface IProjection {

  public IDataSet project(IDataSet dataSet);
  
}
