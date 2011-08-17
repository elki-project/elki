package de.lmu.ifi.dbs.elki.datasource.filter;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.util.ArrayList;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * A filter to sort the data set by some label.
 * 
 * @author Erich Schubert
 */
public class FilterByLabelFilter implements ObjectFilter {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(FilterByLabelFilter.class);

  /**
   * The filter pattern
   */
  private final Pattern pattern;
  
  /**
   * Inversion flag
   */
  private final boolean inverted;

  /**
   * Constructor.
   * 
   * @param pattern Filter pattern
   * @param inverted Inversion flag
   */
  public FilterByLabelFilter(Pattern pattern, boolean inverted) {
    super();
    this.pattern = pattern;
    this.inverted = inverted;
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if(logger.isDebugging()) {
      logger.debug("Filtering the data set");
    }

    // Identify a label column
    final int lblcol;
    {
      int lblc = -1;
      for(int i = 0; i < objects.metaLength(); i++) {
        if(TypeUtil.GUESSED_LABEL.isAssignableFromType(objects.meta(i))) {
          lblc = i;
          break;
        }
      }
      lblcol = lblc; // make static
    }

    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      bundle.appendColumn(objects.meta(j), new ArrayList<Object>());
    }
    for(int i = 0; i < objects.dataLength(); i++) {
      Object l = objects.data(i, lblcol);
      if(l instanceof LabelList) {
        boolean good = false;
        for(String label : (LabelList) l) {
          if(pattern.matcher(label).matches()) {
            good = true;
            break;
          }
        }
        if(good == inverted) {
          continue;
        }
      }
      else {
        if(!pattern.matcher(l.toString()).matches()) {
          continue;
        }
      }
      bundle.appendSimple(objects.getRow(i));
    }
    return bundle;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter that specifies the filter pattern (regular expression).
     * <p>
     * Key: {@code -patternfilter.pattern}
     * </p>
     */
    public static final OptionID LABELFILTER_PATTERN_ID = OptionID.getOrCreateOptionID("patternfilter.pattern", "The filter pattern to use.");

    /**
     * Flag to use the pattern in inverted mode
     * <p>
     * Key: {@code -patternfilter.invert}
     * </p>
     */
    public static final OptionID LABELFILTER_PATTERN_INVERT_ID = OptionID.getOrCreateOptionID("patternfilter.invert", "Flag to invert pattern.");

    /**
     * The pattern configured.
     */
    Pattern pattern = null;

    /**
     * Inversion flag
     */
    private boolean inverted = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final PatternParameter patternP = new PatternParameter(LABELFILTER_PATTERN_ID);
      if(config.grab(patternP)) {
        pattern = patternP.getValue();
      }
      final Flag invertedF = new Flag(LABELFILTER_PATTERN_INVERT_ID);
      if(config.grab(invertedF)) {
        inverted = invertedF.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new FilterByLabelFilter(pattern, inverted);
    }
  }
}