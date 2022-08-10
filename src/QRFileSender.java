import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class QRFileSender extends JFrame implements Runnable, ThreadFactory {
	private static final long serialVersionUID = 6441489157408381878L;
	private Executor executor = Executors.newSingleThreadExecutor(this);
	private Webcam webcam = null;
	private WebcamPanel panel = null;
	private JTextArea textarea = null;
	
	private BufferedReader in = new BufferedReader(new FileReader("in.txt"));
	private int minNumOfBytesPerQRCode = 32;
	private int maxNumOfBytesPerQRCode = 1024;
	private int numOfBytesPerQRCode = maxNumOfBytesPerQRCode;
	private int currentBatch = 1;
	private int thisTimeBytesRead = 0;
	private int currentTimeShown = 0;
	private int maxTimeShown = 25;
	private JFrame jframe = new JFrame("QR Code Display Interface");
	private JLabel jlabel = new JLabel();

	public QRFileSender() throws IOException {
		super();
		
	    jlabel.setHorizontalAlignment(JLabel.CENTER);
	    jlabel.setVerticalAlignment(JLabel.CENTER);
		jframe.getContentPane().add(jlabel);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jframe.pack();
        jframe.setExtendedState(JFrame.MAXIMIZED_BOTH);
        jframe.setVisible(true);
		
		in.mark(maxNumOfBytesPerQRCode);
		updateFrame();
		
		setLayout(new FlowLayout());
		setTitle("Capturing Feedback from Receiver");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Dimension size = WebcamResolution.VGA.getSize();
		
		webcam = Webcam.getWebcams().get(0);
		webcam.setViewSize(size);

		panel = new WebcamPanel(webcam);
		panel.setPreferredSize(size);

		textarea = new JTextArea();
		textarea.setEditable(false);
		textarea.setPreferredSize(size);

		add(panel);
		add(textarea);

		pack();
		setVisible(true);
		executor.execute(this);
	}

	@Override
	public void run() {
		do {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			currentTimeShown++;
			if (currentTimeShown >= maxTimeShown) {
				currentTimeShown = 0;
				numOfBytesPerQRCode = (numOfBytesPerQRCode == minNumOfBytesPerQRCode) ? minNumOfBytesPerQRCode : numOfBytesPerQRCode / 2;
				try {
					updateFrame();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			Result result = null;
			BufferedImage image = null;

			if (webcam.isOpen()) {
				if ((image = webcam.getImage()) == null) {
					continue;
				}
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

				try {
					result = new MultiFormatReader().decode(bitmap);
				} catch (NotFoundException e) {
					// No QR code found
				}
			}
			
			if (result != null) {
				String[] tokens = result.getText().split(" ");
				if (Integer.parseInt(tokens[0]) == currentBatch && Integer.parseInt(tokens[1]) == thisTimeBytesRead) {
					currentBatch++;
					try {
						in.mark(maxNumOfBytesPerQRCode);
						numOfBytesPerQRCode = (numOfBytesPerQRCode == maxNumOfBytesPerQRCode) ? maxNumOfBytesPerQRCode : numOfBytesPerQRCode * 2;
						updateFrame();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} while (true);
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "example-runner");
		t.setDaemon(true);
		return t;
	}
	
	public void updateFrame() throws IOException {
		in.reset();
		char[] readBuffer = new char[numOfBytesPerQRCode];
		thisTimeBytesRead = in.read(readBuffer, 0, numOfBytesPerQRCode);
		
		if (thisTimeBytesRead == -1) {
			jlabel.setIcon(null);
			jframe.setTitle("QR Code Display Interface. Finished");
			return;
		}
		
		jlabel.setIcon(new ImageIcon(QRHelper.generateQRImage(QRHelper.generateQRByteMatrix(currentBatch + " " + new String(readBuffer)))));
		jframe.setTitle("QR Code Display Interface. " + "Current Sending Batch: " + currentBatch + ". Data Size In This Batch: " + numOfBytesPerQRCode + ".");
	}
	
	public static void main(String[] args) throws IOException {
		new QRFileSender();
	}
}
