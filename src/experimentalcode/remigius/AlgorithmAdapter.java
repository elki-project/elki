package experimentalcode.remigius;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

public abstract class AlgorithmAdapter<O extends DatabaseObject, T> {
	
	protected Result result;
	protected Database<O> database;
	
	protected AssociationID<T> asID;
	
	public AlgorithmAdapter(Database<O> database, Result result){
		this.database = database;
		this.result = result;
	}
	
	public Database<O> getDatabase(){
		return database;
	}
	
	public Result getResult(){
		return this.result;
	}
	
	public AssociationID<T> getAlgorithmID(){
		return asID;
	}
	
	protected T getScore(DatabaseObject dbo){
		return getOutlierAnnotationResult().getValueFor(dbo.getID());
	}
	
	private AnnotationResult<T> getOutlierAnnotationResult(){
		if (result instanceof MultiResult){
			return ResultUtil.findAnnotationResult((MultiResult)result, asID);
		} else {
			throw new IllegalArgumentException("No MultiResult.");
		}
	}
	
	public abstract Double getUnnormalized(DatabaseObject dbo);
	public abstract Double getNormalized(DatabaseObject dbo);

}
