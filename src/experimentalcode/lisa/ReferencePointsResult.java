package experimentalcode.lisa;


import java.util.Collection;

import de.lmu.ifi.dbs.elki.result.CollectionResult;

/**
 * Simple 'collection' type of result.
 * For example, a list of NumberVectors.
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