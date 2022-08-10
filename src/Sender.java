import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
 
public class Sender {
	public static int numOfPicsPerFrame = 6;
	public static int numOfBytesPerQRCode = 64;
	
	public static void updateFrame(ArrayList<BufferedImage> qrImages, int start, int end, JFrame frame) {
		frame.getContentPane().removeAll();
		JPanel panel = new JPanel();
		for (int i = start; i <= end; i++) {
			panel.add(new JLabel(new ImageIcon(qrImages.get(i))));
		}
		frame.getContentPane().add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
	}
 
    public static void main(String[] args) throws IOException {
    	ArrayList<BufferedImage> qrImages = new ArrayList<BufferedImage>();
    	
    	char[] readBuffer = new char[numOfBytesPerQRCode];
    	int bytesRead = 0;
    	int totalSize = 0;
    	BufferedReader in = new BufferedReader(new FileReader("in.txt"));
    	while ((bytesRead = in.read(readBuffer, 0, numOfBytesPerQRCode)) != -1) {
    	    qrImages.add(QRHelper.generateQRImage(QRHelper.generateQRByteMatrix(new String(readBuffer))));
    	    totalSize += bytesRead;
    	}
    	System.out.println("File size: " + totalSize);
    	in.close();
    	
    	JFrame frame = new JFrame("Sender");
    	updateFrame(qrImages, 0, 5, frame);
    }       
}