package experimentalcode.remigius;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;

public abstract class AlgorithmAdapter<O extends DatabaseObject> {
	
	protected Result result;
	protected Database<O> database;
	
	public AlgorithmAdapter(Database<O> database, Result result){
		this.database = database;
		this.result = result;
	}
	
	public abstract Double getUnnormalized(DatabaseObject dbo);
	public abstract Double getNormalized(DatabaseObject dbo);

}
