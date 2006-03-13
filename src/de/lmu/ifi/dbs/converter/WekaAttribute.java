package de.lmu.ifi.dbs.converter;


import java.util.Arrays;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface WekaAttribute<W extends WekaAttribute<W>> extends Comparable<W>
{
    
    public static final String NOMINAL = "nominal";
    
    public static final String NUMERIC = "numeric";
    
    public static final String STRING = "string";
    
    public static final String[] TYPES = {NOMINAL,NUMERIC,STRING};
    
    public static final int NOMINAL_INDEX = Arrays.binarySearch(TYPES,NOMINAL);
    
    public static final int NUMERIC_INDEX = Arrays.binarySearch(TYPES,NUMERIC);
    
    public static final int STRING_INDEX = Arrays.binarySearch(TYPES,STRING);
    
    public String getType();
    
    public String getValue();
    
    public boolean isNominal();
    
    public boolean isString();
    
    public boolean isNumeric();
    
    public boolean equals(Object o);
    
}
