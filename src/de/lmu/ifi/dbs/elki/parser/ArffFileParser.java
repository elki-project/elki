package de.lmu.ifi.dbs.elki.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.converter.Arff2Txt;
import de.lmu.ifi.dbs.elki.converter.WekaAttribute;
import de.lmu.ifi.dbs.elki.converter.WekaObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Parser to translate an arff formated file to the whitespace separated format
 * used by most parsers in the framework and to pass the translation to a specified
 * base parser.
 * 
 * @author Arthur Zimek
 * @param <O> object type
 * @param <W> attribute type
 */
public class ArffFileParser<O extends DatabaseObject & WekaObject<W>, W extends WekaAttribute> extends AbstractParser<O>
{
    /**
     * The default base parser ({@link DoubleVectorLabelParser}).
     */
    public static final String DEFAULT_PARSER = DoubleVectorLabelParser.class.getCanonicalName();
    
    /**
     * OptionID for {@link #BASE_PARSER_PARAM}
     */
    public static final OptionID BASE_PARSER_ID = OptionID.getOrCreateOptionID("arff.baseparser",
        "parser getting the input translated from arff format to whitespace separated format");
    
    /**
     * Parameter for the base parser.
     * Default base parser is set to {@link #DEFAULT_PARSER}.
     */
    public final ClassParameter<Parser<O>> BASE_PARSER_PARAM =
      new ClassParameter<Parser<O>>(BASE_PARSER_ID, Parser.class, DEFAULT_PARSER);
    
    /**
     * Keeps the currently set base parser.
     */
    private Parser<O> parser;

    /**
     * Creates an arff file parser.
     */
    public ArffFileParser()
    {
        addOption(BASE_PARSER_PARAM);
    }

    /**
     * Translates the input stream from arff to whitespace separated format
     * and passes the translation to the currently set base parser.
     * 
     */
    public ParsingResult<O> parse(InputStream in)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try
        {
            Arff2Txt.translate(in, out);
        }
        catch(IOException e)
        {
            throw new IllegalArgumentException("Error in translation from Arff to whitespace-separated", e);
        }
        return parser.parse(new ByteArrayInputStream(out.toByteArray()));
    }

    /**
     * Additionally to the settings performed in
     * the super method,
     * sets the base parser to the specified class or else to the default base parser
     * {@link #DEFAULT_PARSER}.
     * 
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);
        parser = BASE_PARSER_PARAM.instantiateClass();
        remainingParameters = parser.setParameters(remainingParameters);
        addParameterizable(parser);

        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    
}
