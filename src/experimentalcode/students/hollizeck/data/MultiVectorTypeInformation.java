package experimentalcode.students.hollizeck.data;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

/**
 * Construct a type information for vector spaces with fixed dimensionality.
 *
 * @author Sebastian Hollizeck
 *
 * @apiviz.has FeatureVector
 *
 * @param <V> Vector type
 */
public class MultiVectorTypeInformation<V extends FeatureVector<?>> extends VectorTypeInformation<V> {

  protected final int multiplicity;

  /**
   * default constructor
   * <br>
   * @deprecated
   * @param cls
   */
  public MultiVectorTypeInformation(Class<? super V> cls) {
    super(cls);
    multiplicity= -1;
  }


  /**
   * Constructor for an actual type.
   *
   * @param cls base class
   * @param serializer Serializer
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @param multiplicity
   */
  public MultiVectorTypeInformation(Class<? super V> cls, ByteBufferSerializer<? super V> serializer, int mindim, int maxdim, int multiplicity) {
    super(cls, serializer, mindim, maxdim);
    this.multiplicity = multiplicity;
  }

  /**
  * Constructor for a type request.
  *
  * @param cls base class
  * @param mindim Minimum dimensionality
  * @param maxdim Maximum dimensionality
  */
 public MultiVectorTypeInformation(Class<? super V> cls, int mindim, int maxdim, int multiplicity) {
   this(cls, null, mindim, maxdim, multiplicity);
 }
}
