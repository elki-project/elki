package experimentalcode.students.hollizeck.data;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
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

  public static MultiVectorTypeInformation<NumberVector> MULTIVECTOR_TYPEINFORMATION = typeRequest(NumberVector.class);
  
  protected final int multiplicity;

  private final DoubleVector.Factory FACTORY;
  /**
   * default constructor
   * <br>
   * @deprecated
   * @param cls
   */
  public MultiVectorTypeInformation(Class<? super V> cls) {
    super(cls, null, -1, Integer.MAX_VALUE);
    multiplicity= -1;
    FACTORY = new DoubleVector.Factory();
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

    FACTORY = new DoubleVector.Factory();
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
 
 public int getMultiplicity(){
   return multiplicity;
 }
 
 @Override
 public String toString(){
   return super.toString()+",multiplicity="+multiplicity;
 }
 
 /**
  * Constructor for a type request without dimensionality constraints.
  * 
  * @param cls Class constraint
  * @param <V> vector type
  */
 public static <V extends FeatureVector<?>> MultiVectorTypeInformation<V> typeRequest(Class<? super V> cls) {
   return new MultiVectorTypeInformation<>(cls);
 }
}
