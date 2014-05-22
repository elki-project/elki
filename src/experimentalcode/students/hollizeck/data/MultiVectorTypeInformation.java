package experimentalcode.students.hollizeck.data;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;

/**
 * 
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
  
  /**
   * default constructor
   * <br>
   * @deprecated
   * @param cls
   */
  public MultiVectorTypeInformation(Class<? super V> cls) {
    this(cls, -1, Integer.MAX_VALUE, -1);
  }


  /**
   * Constructor for an actual type.
   *
   * @param cls base class
   * @param mindim Minimum dimensionality
   * @param maxdim Maximum dimensionality
   * @param multiplicity
   */
  public MultiVectorTypeInformation(Class<? super V> cls, int mindim, int maxdim, int multiplicity) {
    super(cls, mindim, maxdim);
    this.multiplicity = multiplicity;
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
  public MultiVectorTypeInformation(FeatureVector.Factory<V, ?> factory, ByteBufferSerializer<? super V> serializer, int mindim, int maxdim, int multiplicity) {
    super(factory, serializer, mindim, maxdim);
    this.multiplicity = multiplicity;
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
   return new MultiVectorTypeInformation<>(cls,-1, Integer.MAX_VALUE, -1);
 }
}
