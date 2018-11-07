package TypingGameServer;

import java.net.MalformedURLException;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class TypingGameMain {
	public static void main(String[] args) throws MalformedURLException {
		//로딩화면 출력
		Loading ld = new Loading();
		ld.setLoading();
		ld.interrupt();
		
		//서버 띄우기
		TypingGameServer ts = new TypingGameServer();
		ts.start();
	}
}

class Loading extends Thread {
	Icon icon;
	JLabel label;
	JFrame f;
	
	public Loading() {
		//icon = new ImageIcon("C:\\loading.gif");
		label = new JLabel(icon);
		f = new JFrame();
	}
	
	void setLoading() {
		f.setUndecorated(true);
		f.getContentPane().add(label);
		f.pack();
		f.setLocationRelativeTo(null);
		f.setVisible(true);
		
		delay(8 * 1000);
		
		f.setVisible(false);
	}
	
	void delay(int millis) {
		try {
			Thread.sleep(millis);
		} catch(Exception e) { }
	}
}