package experimentalcode.lisa;


import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.IterableResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Simple 'collection' type of result.
 * For example, a list of RealVectors.
 * 
 * @author Erich Schubert
 *
 * @param <O> data type
 */
public class ReferencePointsResult<O> extends CollectionResult<O> {

  public ReferencePointsResult(Collection<O> col) {
    super(col);
    // TODO Auto-generated constructor stub
  }

  public ReferencePointsResult(Collection<O> col, Collection<String> header) {
    super(col, header);
    // TODO Auto-generated constructor stub
  }
  

}