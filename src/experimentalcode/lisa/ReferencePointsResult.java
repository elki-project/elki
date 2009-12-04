package experimentalcode.lisa;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.result.CollectionResult;

/**
 * Result used in passing the reference points to the visualizers.
 * 
 * @author Erich Schubert
 * 
 * @param <O> data type
 */
public class ReferencePointsResult<O> extends CollectionResult<O> {
  /**
   * Constructor with collection only.
   * 
   * @param col Reference Points
   */
  public ReferencePointsResult(Collection<O> col) {
    super(col);
  }

  /**
   * Full constructor.
   * 
   * @param col Reference Points
   * @param header Header
   */
  public ReferencePointsResult(Collection<O> col, Collection<String> header) {
    super(col, header);
  }

  @Override
  public String getName() {
    return "reference_points";
  }
}