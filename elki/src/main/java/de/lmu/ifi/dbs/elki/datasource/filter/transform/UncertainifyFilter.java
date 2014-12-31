package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.UOModel;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.gui.configurator.FlagParameterConfigurator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.workflow.AlgorithmStep;

/**
 * 
 * Filter class to transform a database containing vector fields
 * (TODO I need to express this more correctly) into a database
 * containing {@link UncertainObject} fields.
 * 
 * The purpose for that is to use those transformed databases
 * in experiments regarding uncertain data in some way.
 * 
 * @author Alexander Koos
 *
 * @param <V>
 * @param <UO>
 * @param <U>
 */
public class UncertainifyFilter<V extends NumberVector, UO extends UOModel<SpatialComparable>, U extends UncertainObject<UO>> extends AbstractConversionFilter<V, U> {

  private static final Logging LOG = Logging.getLogger(UncertainifyFilter.class);
  
  /**
   * The uncertainityModel specifies, how the values of
   * the database shall be uncertainified and how
   * the the sampling is made in the {@link AlgorithmStep}.
   */
  private UO uncertainityModel;
  
  /**
   * The flag blur specifies, if the boundaries of
   * the uncertain object shall be centered on a
   * randomized value or on the given data.
   */
  private boolean blur;
  
  /**
   * The flag groundtruth specifies, if the given
   * data shall be uncertainified or used directly
   * by some model dependent metric to build the
   * uncertain objects.
   */
  private boolean uncertainify;
  
  /**
   * If the flag uncertainify is set false, dims is
   * used to specify the dimensionality of the input
   * data.
   */
  private int dims;
  
  /**
   * 
   * Constructor.
   *
   * @param uoModel
   */
  public UncertainifyFilter(UO uoModel, boolean blur, boolean uncertainify, int dims) {
    this.uncertainityModel = uoModel;
    this.blur = blur;
    this.uncertainify = uncertainify;
    this.dims = dims;
  }
  
  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Transforms a single vector object into an
   * uncertainified version, wrapped into an
   * {@link UncertainObject}.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected U filterSingleObject(V obj) {
    return (U) uncertainityModel.uncertainify(obj, blur, uncertainify, dims);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<U> convertedType(SimpleTypeInformation<V> in) {
    // TODO: check if the API provides a neater way to do this
    final int dim = ((VectorFieldTypeInformation<V>) in).getDimensionality();
    return VectorFieldTypeInformation.typeRequest(UncertainObject.class, dim, dim);
  }
  
  /**
   * 
   * @author Alexander Koos
   *
   */
  public static class Parameterizer extends AbstractParameterizer {
    
    /**
     * Parameter to specify the uncertainityModel used for the
     * uncertainification.
     */
    public static final OptionID UNCERTAINITY_MODEL_ID = new OptionID("uncertainifyFilter.uoModel","To uncertainify a Database a Model for uncertainity is needed.");

    /**
     * Parameter to specify if the boundaries of the uncertain
     * object shall be centered on the genuine data or on
     * a sampled point.
     */
    public static final OptionID BLUR_DATA_ID = new OptionID("uncertainifyFilter.blurData","Shall the center for the uo be the genuine data? -- 'True' means 'no'.");
    
    /**
     * Parameter to specify if the data given is a groundtruth to 
     * be uncertainified or if it is to be wrapped into an uo
     * directly. 
     */
    public static final OptionID UNCERTAINIFY_ID = new OptionID("uncertainifyFilter.uncertainifyFlag","Shall the data be used directly to build an uncertain object.");
    
    /**
     * Parameter to specify the dimensionality of the data
     * to correctly set the dimensionality of the experiment.
     */
    public static final OptionID DIMENSIONAL_ID = new OptionID("uncertainifyFilter.dimensionalityConstraint","What dimensionaliy does the data have.");
    
    /**
     * Field to hold the uncertainityModel
     */
    protected UOModel<SpatialComparable> uncertainityModel;
    
    /**
     * Field to hold the blur flag.
     */
    protected boolean blur;
    
    /**
     * Field to hold the groundtruth flag.
     */
    protected boolean uncertainify;
    
    /**
     * Field to hold the dimensional constraint.
     */
    protected int dims;
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<UOModel<SpatialComparable>> uoModel = new ObjectParameter<>(UNCERTAINITY_MODEL_ID, UOModel.class);
      if(config.grab(uoModel)) {
        uncertainityModel = uoModel.instantiateClass(config);
      }
      final Flag pblur = new Flag(BLUR_DATA_ID);
      if(config.grab(pblur)) {
        blur = pblur.getValue();
      }
      final Flag pground = new Flag(UNCERTAINIFY_ID);
      if(config.grab(pground)) {
        uncertainify = pground.getValue();
      }
      final IntParameter pdims = new IntParameter(DIMENSIONAL_ID, 0);
      if(config.grab(pdims)) {
        dims = pdims.getValue();
      }
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected UncertainifyFilter makeInstance() {
      return new UncertainifyFilter(uncertainityModel, blur, uncertainify, dims);
    }
    
  }
}
