package de.lmu.ifi.dbs.elki.data.type;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;

/**
 * Utility package containing various common types
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has TypeInformation oneway - -
 * @apiviz.landmark
 */
public final class TypeUtil {
  /**
   * Input type for algorithms that accept anything
   */
  public static final SimpleTypeInformation<Object> ANY = new SimpleTypeInformation<Object>(Object.class);

  /**
   * Database IDs
   */
  public static final SimpleTypeInformation<DBID> DBID = new SimpleTypeInformation<DBID>(DBID.class);

  /**
   * A string
   */
  public static final SimpleTypeInformation<String> STRING = new SimpleTypeInformation<String>(String.class);

  /**
   * A class label
   */
  public static final SimpleTypeInformation<ClassLabel> CLASSLABEL = new SimpleTypeInformation<ClassLabel>(ClassLabel.class);

  /**
   * A list of labels.
   */
  public static final SimpleTypeInformation<LabelList> LABELLIST = new SimpleTypeInformation<LabelList>(LabelList.class);

  /**
   * Either class label, object labels or a string - anything that will be
   * accepted by
   * {@link de.lmu.ifi.dbs.elki.utilities.DatabaseUtil#guessObjectLabelRepresentation}
   */
  public static final TypeInformation GUESSED_LABEL = new AlternativeTypeInformation(LABELLIST, CLASSLABEL, STRING);

  /**
   * Input type for algorithms that require number vector fields.
   */
  public static final VectorFieldTypeInformation<NumberVector<?, ?>> NUMBER_VECTOR_FIELD = new VectorFieldTypeInformation<NumberVector<?, ?>>(NumberVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   * 
   * If possible, please use {@link #NUMBER_VECTOR_FIELD}!
   */
  public static final VectorFieldTypeInformation<DoubleVector> DOUBLE_VECTOR_FIELD = new VectorFieldTypeInformation<DoubleVector>(DoubleVector.class);

  /**
   * Input type for algorithms that require number vector fields.
   */
  public static final VectorFieldTypeInformation<BitVector> BIT_VECTOR_FIELD = new VectorFieldTypeInformation<BitVector>(BitVector.class);

  /**
   * Sparse float vector field.
   */
  public static final VectorFieldTypeInformation<SparseFloatVector> SPARSE_FLOAT_FIELD = new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class);

  /**
   * External ID type
   * 
   * FIXME: Don't use String here!
   */
  public static final SimpleTypeInformation<String> EXTERNALID = SimpleTypeInformation.get(String.class);

  /**
   * Make a type array easily.
   * 
   * @param ts Types
   * @return array
   */
  public static final TypeInformation[] array(TypeInformation... ts) {
    return ts;
  }
}