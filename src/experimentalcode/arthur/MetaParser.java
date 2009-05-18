/**
 * 
 */
package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.io.InputStream;

/**
 * 
 * @author Arthur Zimek
 *
 * @param <O>
 */
public abstract class MetaParser<O extends DatabaseObject> extends AbstractParameterizable implements Parser<O> {

  public static final OptionID BASEPARSER_ID = OptionID.getOrCreateOptionID("metaparser.baseparser", "Parser to use as base parser");
  
  private final ClassParameter<? extends Parser<O>> BASEPARSER_PARAM = new ClassParameter<Parser<O>>(BASEPARSER_ID,Parser.class,DoubleVectorLabelParser.class.getCanonicalName());
  
  private Parser<O> baseparser;
  
  protected MetaParser(){
    addOption(BASEPARSER_PARAM);
  }
  
  protected ParsingResult<O> retrieveBaseParsingresult(InputStream in){
    return baseparser.parse(in);
  }


  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(java.lang.String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    baseparser = BASEPARSER_PARAM.instantiateClass();
    return baseparser.setParameters(remainingParameters);
  }



  /**
   * Returns a usage string based on the usage of optionHandler.
   *
   * @param message a message string to be included in the usage string
   * @return a usage string based on the usage of optionHandler
   */
  protected String usage(String message) {
    return optionHandler.usage(message, false);
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }
}
