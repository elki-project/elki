package de.lmu.ifi.dbs.utilities;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * Provides a file filter based on a pattern to define acceptable pathnames.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PatternBasedFileFilter implements FileFilter
{
    /**
     * Keeps the pattern to match a pathname against.
     */
    private Pattern pattern;
    
    /**
     * Provides a file filter based on a pattern to define acceptable pathnames.
     * 
     * @param pattern a pattern to define acceptable pathnames
     */
    public PatternBasedFileFilter(Pattern pattern)
    {
        this.pattern = pattern;
    }
    
    /**
     * Returns true if
     * the given pathname matches exactly against the pattern,
     * that is iff
     * @code{this.pattern.matcher(pathname.getName()).matches()},
     * false otherwise.
     * 
     * @see java.io.FileFilter#accept(java.io.File)
     */
    public boolean accept(File pathname)
    {
        return this.pattern.matcher(pathname.getName()).matches();
    }
}
