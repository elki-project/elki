/**
 * 
 */
package experimentalcode.frankenb.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class FileLogWriter implements ILogWriter {

  private PrintWriter writer = null;
  
  public FileLogWriter(File targetFile) throws IOException {
    writer = new PrintWriter(
        new OutputStreamWriter(
            new BufferedOutputStream(
                new FileOutputStream(targetFile)
                )
            , Charset.forName("UTF-8")
            )
        );
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.log.ILogWriter#putLogLine(experimentalcode.frankenb.log.LogLevel, java.lang.String)
   */
  @Override
  public void putLogLine(LogLevel level, String formattedMessage) {
    writer.println(formattedMessage);
  }
  
  public void close() throws IOException {
    writer.flush();
    writer.close();
  }

}
