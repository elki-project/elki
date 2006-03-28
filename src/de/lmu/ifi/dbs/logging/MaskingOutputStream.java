package de.lmu.ifi.dbs.logging;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An OutputStream to mask the underlying OutputStream.
 * MaskingOutputStream behaves exactly like a FilterOutputStream
 * except in closing. A call of the close() method will not close the underlying
 * OutputStream, but free the reference to it. Thus setting System.err or System.out as
 * underlying OutputStream does not put those SystemOutputStreams at risk
 * of being closed.
 * 
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class MaskingOutputStream extends FilterOutputStream
{

    /**
     * Keeps the given OutputStream as underlying OutputStream,
     * passing calls to the underlying OutputStream,
     * but preventing it from being closed.
     * 
     * @param out an OutputStream to pass methodcalls to
     */
    public MaskingOutputStream(OutputStream out)
    {
        super(out);
    }
    
    /**
     * Closes this output stream and releases any system resources 
     * associated with the stream. 
     * <p>
     * The <code>close</code> method of <code>MaskingOutputStream</code> 
     * calls the <code>flush</code> method of
     * <code>FilterOutputStream</code>, and then sets 
     * its underlying output stream to <code>null</code>.
     * Thus any subsequent calls of methods to this OutputStream
     * will result in a NullPointerException. 
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#flush()
     */
    @Override
    public void close() throws IOException
    {
        flush();
        out = null;
    }
    
    

}
