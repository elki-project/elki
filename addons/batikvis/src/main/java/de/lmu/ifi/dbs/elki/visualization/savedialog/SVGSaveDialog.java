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
package de.lmu.ifi.dbs.elki.visualization.savedialog;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.ext.awt.image.spi.ImageWriterRegistry;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.visualization.batikutil.BatikUtil;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * A save dialog to save/export a SVG image to a file.
 *
 * Supported formats:
 * <ul>
 * <li>SVG</li>
 * <li>PNG (with width and height options)</li>
 * <li>JPEG (with width and height options, and if an ImageWrite is available)
 * </li>
 * <li>PDF (if FOP transcoders are available)</li>
 * <li>PS (if FOP transcoders are available)</li>
 * <li>EPS (if FOP transcoders are available)</li>
 * </ul>
 *
 * @author Simon Mittermüller
 * @since 0.2
 *
 * @composed - - - SaveOptionsPanel
 */
public final class SVGSaveDialog {
  /**
   * Private constructor. Static methods only.
   */
  private SVGSaveDialog() {
    // Do not use.
  }

  /** The default title. "Save as ...". */
  public static final String DEFAULT_TITLE = "Save as ...";

  /** Static logger reference */
  private static final Logging LOG = Logging.getLogger(SVGSaveDialog.class);

  /** Automagic file format */
  final static String AUTOMAGIC_FORMAT = "automatic";

  /** Supported file format (extensions) */
  final static String[] FORMATS;

  /** Visible file formats */
  final static String[] VISIBLE_FORMATS;

  static {
    ArrayList<String> fmt = new ArrayList<>();
    fmt.add(AUTOMAGIC_FORMAT);
    fmt.add("svg");

    // Check that the image writers are available:
    ImageWriterRegistry reg = ImageWriterRegistry.getInstance();
    // Detect broken Batik unable to use inlined base64 images:
    ImageTagRegistry tagreg = ImageTagRegistry.getRegistry();
    // These will only work if we can decode pngs (thumbnails in overview)!
    if(tagreg.getRegisteredMimeTypes().contains("image/png")) {
      if(reg.getWriterFor("image/png") != null) {
        fmt.add("png");
      }
      if(reg.getWriterFor("image/jpeg") != null) {
        fmt.add("jpeg");
      }
    }

    // FOP installed?
    if(BatikUtil.hasFOPInstalled()) {
      fmt.add("pdf");
      fmt.add("ps");
      fmt.add("eps");
    }
    FORMATS = fmt.toArray(new String[fmt.size()]);
    fmt.remove(0);
    VISIBLE_FORMATS = fmt.toArray(new String[fmt.size()]);
  }

  /**
   * Show a "Save as" dialog.
   *
   * @param plot The plot to be exported.
   * @param width The width of the exported image (when export to JPEG/PNG).
   * @param height The height of the exported image (when export to JPEG/PNG).
   * @return Result from {@link JFileChooser#showSaveDialog}
   */
  public static int showSaveDialog(SVGPlot plot, int width, int height) {
    JFileChooser fc = new JFileChooser(new File("."));
    fc.setDialogTitle(DEFAULT_TITLE);
    // fc.setFileFilter(new ImageFilter());
    SaveOptionsPanel optionsPanel = new SaveOptionsPanel(fc, width, height);
    fc.setAccessory(optionsPanel);

    int ret = fc.showSaveDialog(null);
    if(ret == JFileChooser.APPROVE_OPTION) {
      fc.setDialogTitle("Saving... Please wait.");
      File file = fc.getSelectedFile();
      String format = optionsPanel.getSelectedFormat();
      width = optionsPanel.getSelectedWidth();
      height = optionsPanel.getSelectedHeight();
      if(format == null || AUTOMAGIC_FORMAT.equals(format)) {
        format = guessFormat(file.getName());
      }
      try {
        if(format == null) {
          showError(fc, "Error saving image.", "File format not recognized.");
        }
        else if("jpeg".equals(format) || "jpg".equals(format)) {
          float quality = optionsPanel.getJPEGQuality();
          plot.saveAsJPEG(file, width, height, quality);
        }
        else if("png".equals(format)) {
          plot.saveAsPNG(file, width, height);
        }
        else if("ps".equals(format)) {
          plot.saveAsPS(file);
        }
        else if("eps".equals(format)) {
          plot.saveAsEPS(file);
        }
        else if("pdf".equals(format)) {
          plot.saveAsPDF(file);
        }
        else if("svg".equals(format)) {
          plot.saveAsSVG(file);
        }
        else {
          showError(fc, "Error saving image.", "Unsupported format: " + format);
        }
      }
      catch(java.lang.IncompatibleClassChangeError e) {
        showError(fc, "Error saving image.", "It seems that your Java version is incompatible with this version of Batik and Jpeg writing. Sorry.");
      }
      catch(ClassNotFoundException e) {
        showError(fc, "Error saving image.", "A class was not found when saving this image. Maybe installing Apache FOP will help (for PDF, PS and EPS output).\n" + e.toString());
      }
      catch(TransformerFactoryConfigurationError | Exception e) {
        LOG.exception(e);
        showError(fc, "Error saving image.", e.toString());
      }
    }
    else if(ret == JFileChooser.ERROR_OPTION) {
      showError(fc, "Error in file dialog.", "Unknown Error.");
    }
    else if(ret == JFileChooser.CANCEL_OPTION) {
      // do nothing - except return result
    }
    return ret;
  }

  /**
   * Guess a supported format from the file name. For "auto" format handling.
   *
   * @param name File name
   * @return format or "null"
   */
  public static String guessFormat(String name) {
    String ext = FileUtil.getFilenameExtension(name);
    for(String format : FORMATS) {
      if(format.equalsIgnoreCase(ext)) {
        return ext;
      }
    }
    return null;
  }

  /**
   * @return the formats
   */
  public static String[] getFormats() {
    return FORMATS;
  }

  /**
   * @return the visibleformats
   */
  public static String[] getVisibleFormats() {
    return VISIBLE_FORMATS;
  }

  /**
   * Helper method to show a error message as "popup". Calls
   * {@link JOptionPane#showMessageDialog(java.awt.Component, Object)}.
   *
   * @param parent The parent component for the popup.
   * @param msg The message to be displayed.
   */
  private static void showError(Component parent, String title, String msg) {
    JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
  }
}
