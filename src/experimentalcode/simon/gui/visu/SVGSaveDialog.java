/**
 * 
 */
package experimentalcode.simon.gui.visu;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.batik.transcoder.TranscoderException;

import experimentalcode.erich.visualization.SVGPlot;

/** A "generic" save dialog to save a SVG to disk.
 * Supported formats:
 * <ul>
 *   <li>JPEG (broken)</li>
 *   <li>PNG</li>
 *   <li>PDF</li>
 *   <li>PS</li>
 *   <li>EPS</li>
 *   <li>SVG</li>
 * </ul> 
 * @author simon
 *
 */
public class SVGSaveDialog extends JDialog implements ActionListener, ItemListener {

	public final static String DEFAULT_TITLE = "Save as ..."; 
	

	/** The plot to be exported. */
	private SVGPlot svgPlot;
	/** The width of the exported image (if exported to JPEG or PNG). Default is 1024. */
	private int width = 1024;
	/** The height of the exported image (if exported to JPEG or PNG). Default is 768. */
	private int height = 768;
	
	private JPanel mainPanel;
	private JPanel rasterPanel;
	/** If saving as JPEG/PNG show width/height infos here. */
	private JPanel sizePanel;

	//private JCheckBox cbAutoWidth;
	//private JCheckBox cbAutoHeight;
	private JButton cancelButton;
	private JButton saveButton;

	private JTextField widthField;
	private JTextField heightField;

	private JComboBox cbFormat;

	//private JLabel logLabel;

	String[] formats = { "png", "svg", "jpeg", "pdf", "ps", "eps" };

	
	private SVGSaveDialog(Frame owner, String title) {
		super(owner, title);
	}
	
	private SVGSaveDialog(Frame owner) {
		super(owner, DEFAULT_TITLE);
	}
	
	private SVGSaveDialog(Frame owner, boolean modal) {
		super(owner, DEFAULT_TITLE, modal);
	}
	
	private SVGSaveDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
	}
	
	/** A constructor to specify width and height hints for export to JPEG/PNG.
	 * @param The plot to be exported. 
	 * @param width The width of the exported image (when export to JPEG/PNG).
	 * @param height The height of the exported image (when export to JPEG/PNG). 
	 * @see {@link SVGSaveDialog(SVGPlot)}
	 * */
	public SVGSaveDialog(SVGPlot plot, int width, int height) {
		this(plot);
		this.width = width;
		this.height = height;
		widthField.setText("" + width); 
		heightField.setText("" + height); 
	}
	
	public SVGSaveDialog(SVGPlot plot) {
		super((Frame)null, DEFAULT_TITLE);
		init(plot);
		try {
			System.out.println("WIDTH = " + plot.getRoot().getAttributes().getNamedItem("width").getNodeValue());
			System.out.println("HEIGHT = " + plot.getRoot().getAttributes().getNamedItem("height").getNodeValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void init(SVGPlot plot) {
		
		setSize(200, 250);
		this.svgPlot = plot;

		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		sizePanel = new JPanel();
		sizePanel.setLayout(new BoxLayout(sizePanel, BoxLayout.Y_AXIS));
		sizePanel.setBorder(BorderFactory.createTitledBorder("Size options"));
		rasterPanel = new JPanel();
		rasterPanel.setLayout(new BoxLayout(rasterPanel, BoxLayout.Y_AXIS));

		// FORMATS:
		JLabel l = new JLabel("Choose format:");
		mainPanel.add(l);

		cbFormat = new JComboBox(formats);
		cbFormat.setSelectedIndex(0);
		cbFormat.addItemListener(this);
		mainPanel.add(cbFormat);

		// SIZE:
//		JLabel sizeLabel = new JLabel("Width depends on height.");
//		sizePanel.add(sizeLabel);
		JPanel widthPanel = new JPanel();
		JLabel widthLabel = new JLabel("Width:");
		widthField = new JTextField(8);
		//widthField.setEditable(true);
		//widthField.addKeyListener(this);
		widthField.setText("" + width); //visu.getCanvas().getWidth() + "");
		widthPanel.add(widthLabel);
		widthPanel.add(widthField);
		sizePanel.add(widthPanel);

		JPanel heightPanel = new JPanel();
		JLabel heightLabel = new JLabel("Height:");
		heightField = new JTextField(8);
		heightField.setText("" + height); //visu.getCanvas().getHeight() + "");
		//heightField.addKeyListener(this);
		//heightField.setEditable(false);
		heightPanel.add(heightLabel);
		heightPanel.add(heightField);
		sizePanel.add(heightPanel);

		/*cbAutoWidth = new JCheckBox("Auto width");
		cbAutoWidth.setActionCommand("lockWidth");
		cbAutoWidth.setSelected(true);
		cbAutoWidth.addActionListener(this);
		cbAutoHeight = new JCheckBox("Auto height");
		cbAutoHeight.setActionCommand("lockHeight");
		cbAutoHeight.addActionListener(this);
		cbAutoHeight.setSelected(true);
		sizePanel.add(cbAutoWidth);
		sizePanel.add(cbAutoHeight);
		*/
		mainPanel.add(sizePanel);

		// SAVE:
		JPanel buttonPanel = new JPanel();
		saveButton = new JButton("Save");
		saveButton.addActionListener(this);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		buttonPanel.add(saveButton);

		// logLabel = new JLabel("");
		// mainPanel.add(logLabel);

		mainPanel.add(buttonPanel);

		getContentPane().add(mainPanel);
		pack();
	}

	public void itemStateChanged(ItemEvent evt) {
		if (evt.getItem() != null) {
			String format = (String) cbFormat.getSelectedItem();
			if (format != null) {
				if (format.equals("jpeg")) {
					sizePanel.setVisible(true);
					pack();
					// Show size
				} else if (format.equals("png")) {
					// Show size
					sizePanel.setVisible(true);
					pack();
				} else if (format.equals("pdf")) {
					// hideSize
					sizePanel.setVisible(false);
					mainPanel.validate();
					pack();
				} else if (format.equals("ps")) {
					sizePanel.setVisible(false);
					mainPanel.validate();
					pack();
				} else if (format.equals("svg")) {
					sizePanel.setVisible(false);
					mainPanel.validate();
					pack();
				} else {

				}
			}
		} else {

		}
	}
//
//	public void keyTyped(KeyEvent e) {	}
//
//	/** Handle the key-pressed event from the text field. */
//	public void keyPressed(KeyEvent e) {	}
//
//	/** Handle the key-released event from the text field. */
//	public void keyReleased(KeyEvent e) {
//		/*if (cbAutoWidth.isSelected() && cbAutoHeight.isSelected()) {
//			int height = svgPlot.getCanvas().getHeight();
//			int width = visu.getCanvas().getWidth();
//			widthField.setText("" + width);
//			heightField.setText("" + height);
//		} else if (cbAutoWidth.isSelected()) {
//			try {
//				int height = Integer.parseInt(heightField.getText());
//				int width = (int) Math.round(visu.getPatternPlot().ratio * height);
//				widthField.setText("" + width);
//			} catch (NumberFormatException nfe) {
//				nfe.printStackTrace();
//			}
//		} else if (cbAutoHeight.isSelected()) {
//			try {
//				int width = Integer.parseInt(widthField.getText());
//				int height = (int) Math.round(width / visu.getPatternPlot().ratio);
//				heightField.setText("" + height);
//			} catch (NumberFormatException nfe) {
//				nfe.printStackTrace();
//			}
//		} 
//		*/
//	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == cancelButton) {
			dispose();
//		} else if (evt.getSource() == cbAutoHeight) {
//			heightField.setEditable(! cbAutoHeight.isSelected());
//			if (cbAutoWidth.isSelected() && cbAutoHeight.isSelected()) {
//				int height = visu.getCanvas().getHeight();
//				int width = visu.getCanvas().getWidth();
//				widthField.setText("" + width);
//				heightField.setText("" + height);
//			} else if (cbAutoWidth.isSelected()) {
//				try {
//					int height = Integer.parseInt(heightField.getText());
//					int width = (int) Math.round(visu.getPatternPlot().ratio * height);
//					widthField.setText("" + width);
//				} catch (NumberFormatException nfe) {
//					nfe.printStackTrace();
//				}
//			} else if (cbAutoHeight.isSelected()) {
//				try {
//					int width = Integer.parseInt(widthField.getText());
//					int height = (int) Math.round(width / visu.getPatternPlot().ratio);
//					heightField.setText("" + height);
//				} catch (NumberFormatException nfe) {
//					nfe.printStackTrace();
//				}
//			} 
//		} else if (evt.getSource() == cbAutoWidth) {
//			widthField.setEditable(! cbAutoWidth.isSelected());
//			if (cbAutoWidth.isSelected() && cbAutoHeight.isSelected()) {
//				int height = visu.getCanvas().getHeight();
//				int width = visu.getCanvas().getWidth();
//				widthField.setText("" + width);
//				heightField.setText("" + height);
//			} else if (cbAutoWidth.isSelected()) {
//				try {
//					int height = Integer.parseInt(heightField.getText());
//					int width = (int) Math.round(visu.getPatternPlot().ratio * height);
//					widthField.setText("" + width);
//				} catch (NumberFormatException nfe) {
//					nfe.printStackTrace();
//				}
//			} else if (cbAutoHeight.isSelected()) {
//				try {
//					int width = Integer.parseInt(widthField.getText());
//					int height = (int) Math.round(width / visu.getPatternPlot().ratio);
//					heightField.setText("" + height);
//				} catch (NumberFormatException nfe) {
//					nfe.printStackTrace();
//				}
//			} 
		} else if (evt.getSource() == saveButton) {
			
			if (cbFormat.getSelectedItem() == null) {
				//logLabel.setText("Not saved: Select a format.");
				showError(this, "No format selected.");
				pack();
				return;
			}
			String format = (String) cbFormat.getSelectedItem();

			try {
				JFileChooser fc = new JFileChooser();
				int ret = fc.showSaveDialog(this);
				if (ret == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					// String fileExt = file.getName().substring(file.getName().lastIndexOf(".")+1).toLowerCase();
					
					//System.out.println("SAVE: ending:" + format);
					if ("jpeg".equals(format) || "jpg".equals(format)) {
						String widthTemp = widthField.getText();
						String heightTemp = heightField.getText();
						try {
							width = Integer.parseInt(widthTemp);
							height = Integer.parseInt(heightTemp);
						} catch (NumberFormatException nfe) {
							showError(this, "Please specify valid numbers for width and height.");
							// TODO: show error???
							//logLabel.setText("Not saved: " + heightTemp + " is not a valid number.");
							nfe.printStackTrace();
							pack();
							return;
						}
						svgPlot.saveAsJPEG(file, width, height);
					} else if ("png".equals(format)) {
						String widthTemp = widthField.getText();
						String heightTemp = heightField.getText();
						try {
							width = Integer.parseInt(widthTemp);
							height = Integer.parseInt(heightTemp);
						} catch (NumberFormatException nfe) {
							showError(this, "Please specify valid numbers for width and height.");
							// TODO: show error???
							//logLabel.setText("Not saved: " + heightTemp + " is not a valid number.");
							nfe.printStackTrace();
							pack();
							return;
						}
						svgPlot.saveAsPNG(file, width, height);
					} else if ("svg".equals(format)) {
						try {
							svgPlot.saveAsSVG(file);
						} catch (TransformerFactoryConfigurationError e) {
							showError(getOwner(), "Unable to save as SVG.\n" 
									+ "(Exception: " + e + ")");
							e.printStackTrace();
							pack();
							return;
						} catch (TransformerException e) {
							showError(getOwner(), "Unable to save as SVG.\n" 
									+ "(Exception: " + e + ")");
							e.printStackTrace();
							pack();
							return;
						}
					} else if ("pdf".equals(format)) {
						svgPlot.saveAsPDF(file);
					} else if ("ps".equals(format)) {
						svgPlot.saveAsPS(file);
					} else if ("eps".equals(format)) {
						svgPlot.saveAsEPS(file);
					} else {
						JOptionPane.showMessageDialog(fc, "Unable to save.\n" +
								"Unsupported ending: \"" + format + "\".");
						System.out.println("SAVE: Unknown ending.");
						//logLabel.setText("Not saved: Format " + format + " not supported.");
						pack();
						return;
					}
				} else {
					System.out.println("SAVE: ret=" + ret);
					//logLabel.setText("Not saved: Cancel clicked.");
					pack();
					return;
				}
				dispose();
			} catch (IOException ioe) {
				showError(getOwner(), "Not saved: Exception: " + ioe);
				ioe.printStackTrace();
				//logLabel.setText("Not saved: Exception: " + ioe);
				pack();
			} catch (TranscoderException te) {
				showError(getOwner(), "Not saved: Exception: " + te);
				te.printStackTrace();
				//logLabel.setText("Not saved: Exception: " + ioe);
				pack();
			}
		}
	}
	
	/** Helper method to show call {@link JOptionPane#showMessageDialog(java.awt.Component, Object)}. 
	 * @param parent The parent component for the popup.
	 * @param msg The message to be displayed.
	 * */
	private void showError(Component parent, String  msg) {
		JOptionPane.showMessageDialog(parent, msg);
	}
}
