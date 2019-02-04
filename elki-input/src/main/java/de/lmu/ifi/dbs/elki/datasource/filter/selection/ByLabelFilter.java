/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.filter.selection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;

/**
 * A filter to select data set by their label.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - reads - LabelList
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.ByLabelFilter", //
    "de.lmu.ifi.dbs.elki.datasource.filter.FilterByLabelFilter" })
public class ByLabelFilter extends AbstractStreamFilter {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ByLabelFilter.class);

  /**
   * The filter pattern matcher
   */
  private final Matcher matcher;

  /**
   * Inversion flag
   */
  private final boolean inverted;

  /**
   * Label column
   */
  private int lblcol = -1;

  /**
   * Constructor.
   * 
   * @param pattern Filter pattern
   * @param inverted Inversion flag
   */
  public ByLabelFilter(Pattern pattern, boolean inverted) {
    super();
    this.matcher = pattern.matcher("");
    this.inverted = inverted;
  }

  @Override
  public BundleMeta getMeta() {
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return source.data(rnum);
  }

  @Override
  public Event nextEvent() {
    while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        if(lblcol < 0) {
          LOG.warning("By label filter was used, but never saw a label relation!");
        }
        return Event.END_OF_STREAM;
      case META_CHANGED:
        // Search for the first label column
        if(lblcol < 0) {
          BundleMeta meta = source.getMeta();
          lblcol = FilterUtil.findLabelColumn(meta);
        }
        return Event.META_CHANGED;
      case NEXT_OBJECT:
        if(lblcol > 0) {
          Object l = source.data(lblcol);
          if(l instanceof LabelList) {
            boolean good = false;
            final LabelList ll = (LabelList) l;
            for(int i = 0; i < ll.size(); i++) {
              matcher.reset(ll.get(i));
              if(matcher.matches()) {
                good = true;
                break;
              }
            }
            if(good == inverted) {
              continue;
            }
          }
          else {
            matcher.reset(l.toString());
            if(!matcher.matches()) {
              continue;
            }
          }
        }
        else {
          // No labels known yet.
          if(!inverted) {
            continue;
          }
        }
        return Event.NEXT_OBJECT;
      default:
        LOG.warning("Unknown event: " + ev);
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter that specifies the filter pattern (regular expression).
     */
    public static final OptionID LABELFILTER_PATTERN_ID = new OptionID("patternfilter.pattern", "The filter pattern to use.");

    /**
     * Flag to use the pattern in inverted mode
     */
    public static final OptionID LABELFILTER_PATTERN_INVERT_ID = new OptionID("patternfilter.invert", "Flag to invert pattern.");

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
        inverted = invertedF.getValue().booleanValue();
      }
    }

    @Override
    protected ByLabelFilter makeInstance() {
      return new ByLabelFilter(pattern, inverted);
    }
  }
}
