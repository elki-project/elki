package de.lmu.ifi.dbs.elki.parser.meta;

import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * <p>
 * A ProjectionParser projects the {@link ParsingResult} of its base parser onto
 * a subspace specified by a BitSet.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector contained in both the ParsingResult<V> of
 *        the base parser and the projected ParsingResult<V> of this
 *        ProjectionParser
 */
public abstract class ProjectionParser<V extends NumberVector<V, ?>> extends MetaParser<V> {
  /**
   * <p>
   * Selected attributes parameter.
   * </p>
   * <p>
   * Key: <code>-projectionparser.selectedattributes</code>
   * </p>
   */
  public static final OptionID SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("projectionparser.selectedattributes", "a comma separated array of integer values d_i, where 1 <= d_i <= the " + "dimensionality of the feature space " + "specifying the dimensions to be considered " + "for projection. If this parameter is not set, " + "no dimensions will be considered, i.e. the projection is a zero-dimensional feature space");

  /**
   * Keeps the selection of the subspace to project onto.
   */
  private BitSet selectedAttributes;

  /**
   * Constructor.
   * 
   * @param baseparser
   * @param selectedAttributes
   */
  public ProjectionParser(Parser<V> baseparser, BitSet selectedAttributes) {
    super(baseparser);
    this.selectedAttributes = selectedAttributes;
  }

  /**
   * <p>
   * Sets the bits set to true in the given BitSet as selected attributes in
   * {@link #SELECTED_ATTRIBUTES_ID}.
   * </p>
   * 
   * The index in the BitSet is expected to be shifted to the left by one, i.e.,
   * index 0 in the BitSet relates to the first attribute.
   * 
   * @param selectedAttributes the new selected attributes
   */
  public void setSelectedAttributes(BitSet selectedAttributes) {
    this.selectedAttributes.or(selectedAttributes);
  }

  /**
   * <p>
   * Provides a BitSet with the bits set to true corresponding to the selected
   * attributes in {@link #SELECTED_ATTRIBUTES_ID}.
   * </p>
   * 
   * The index in the BitSet is shifted to the left by one, i.e., index 0 in the
   * BitSet relates to the first attribute.
   * 
   * @return the selected attributes
   */
  public BitSet getSelectedAttributes() {
    return selectedAttributes;
  }

  /**
   * Get the resulting dimensionality.
   * 
   * @return dimensionality
   */
  public int getDimensionality() {
    return selectedAttributes.cardinality();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<V extends NumberVector<V, ?>> extends MetaParser.Parameterizer<V> {
    protected BitSet selectedAttributes = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntListParameter selectedAttributesP = new IntListParameter(SELECTED_ATTRIBUTES_ID, new ListGreaterEqualConstraint<Integer>(1));
      if(config.grab(selectedAttributesP)) {
        selectedAttributes = new BitSet();
        List<Integer> dimensionList = selectedAttributesP.getValue();
        for(int d : dimensionList) {
          selectedAttributes.set(d - 1);
        }
      }
    }
  }
}