package de.lmu.ifi.dbs.elki.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.converter.Arff2Txt;
import de.lmu.ifi.dbs.elki.converter.WekaAttribute;
import de.lmu.ifi.dbs.elki.converter.WekaObject;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Parser to translate an arff formated file to the whitespace separated format
 * used by most parsers in the framework and to pass the translation to a specified
 * base parser.
 * 
 * @author Arthur Zimek
 */
public class ArffFileParser<O extends DatabaseObject & WekaObject<W>, W extends WekaAttribute<W>> extends AbstractParser<O>
{
    /**
     * The default base parser ({@link RealVectorLabelParser}).
     */
    @SuppressWarnings("unchecked")
    public static final Parser<?> DEFAULT_PARSER = new RealVectorLabelParser();
    
    /**
     * Parameter name for the base parser.
     */
    public static final String BASE_PARSER_P = "baseparser";
    
    /**
     * Parameter description for the base parser.
     */
    public static final String BASE_PARSER_D = "parser getting the input translated from arff format to whitespace separated format";
    
    /**
     * Parameter for the base parser.
     * Default base parser is set to {@link #DEFAULT_PARSER}.
     */
    public static final ClassParameter<Parser<?>> BASE_PARSER_PARAM = new ClassParameter<Parser<?>>(BASE_PARSER_P, BASE_PARSER_P, Parser.class);
    
    static
    {
        BASE_PARSER_PARAM.setDefaultValue(DEFAULT_PARSER.getClass().getCanonicalName());
    }
    
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
    @SuppressWarnings("unchecked")
    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] params = super.setParameters(args);
        if (BASE_PARSER_PARAM.isSet())
        {
          this.parser = (Parser<O>) BASE_PARSER_PARAM.instantiateClass();
        }
        else
        {
            try
            {
                // todo
                this.parser = Util.instantiateGenerics(Parser.class,DEFAULT_PARSER.getClass().getCanonicalName());
            }
            catch(UnableToComplyException e)
            {
                exception(e.getMessage(),e);
            }
        }
        return this.parser.setParameters(params);
    }

    
}
