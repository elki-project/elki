package experimentalcode.students.kolbh;

  import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.AbstractNormalization;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

  /**
   * Class to perform a normalization on vectors to norm 1.
   *  
   * @author Heidi Kolb
   * @param <V> vector type
   */
  // TODO: extract superclass AbstractAttributeWiseNormalization
  public class NormalizationNorm1<V extends NumberVector<V, ?>> extends AbstractNormalization<V> {

    /**
     * Constructor 
     */
    public NormalizationNorm1() {
    }

    @Override
    protected V filterSingleObject(V featureVector) {

      // length of vector
      final double d = Math.sqrt(featureVector.scalarProduct(featureVector).doubleValue());  
      return featureVector.multiplicate(1/d);
    }

    @Override
    public V restore(V featureVector) throws NonNumericFeaturesException {
      return featureVector;
    }

    @Override
    public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
      return linearEquationSystem;
    }

    /**
     * Factory method for {@link Parameterizable}
     * 
     * @param config Parameterization
     * @return AngleNormalization
     */
    public static <V extends NumberVector<V, ?>> NormalizationNorm1<V> parameterize(Parameterization config) {
      return new NormalizationNorm1<V>();
    }

    @Override
    protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
      return TypeUtil.NUMBER_VECTOR_FIELD;
    }
  }
