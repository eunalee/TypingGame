package TypingGameClient;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TypingGameCore extends JFrame {
	//Ÿ���� ���� ������ ���� ����, ���� ����
	final int FRAME_WIDTH = 639;
	final int FRAME_HEIGHT = 358;
	
	final int SCREEN_WIDTH;
	final int SCREEN_HEIGHT;
	
	int speed = 500; //�ܾ ���� ���� �ӵ� - ���� ���� ����
	int interval = 3 * 1000; //���ο� �ܾ ������ ����
	
	//����, ����, ���� ����, �ְ� ���� �ʱ�ȭ
	int score = 0;
	int life = 3;
	int curLevel = 0;
	final int MAX_LEVEL;
	
	boolean isPlaying = false;
	static boolean isConnect = false;
	
	WordGenerator wg = null; //�ܾ �����ϴ� ������
	WordDropper wm = null; //�ܾ ����߸��� ������
	
	TypingGameClient tc = null;
	
	FontMetrics fm; //ȭ�鿡���� ���� ���̸� ���ϴµ� ���
	ThreadGroup virusGrp = new ThreadGroup("virus"); //���̷��� ��������� �׷�
	
	String[][] data = {
			{ "���", "����Ŭ", "B����", "��Ÿũ", "ũ����", "�ִϾ�", "��Ѹ�", "ä����", "�÷�", "�ý�", "��", "����", "�丣" },
			{ "���̾��", "ĸƾ�Ƹ޸�ī", "����", "��Ƽ��", "����", "Ÿ�뽺", "�δ���Ʈ��", "�����", "�ں�", "���̸�" },
			{ "�ܽ�����", "����������", "ī������", "����Ʈ", "��ƼƼ��", "�׷�Ʈ", "�����", "��ġ��" },
			{ "���׵�Ʈ", "�Ĺ���ġ", "�ܽ�����", "���Ҽ�", "���̾��", "��Į�����ѽ�", "�����ں���", "����Ÿ�Ͽ�", "������", "�繫���轼", "�����̴���", "���̺�׷�Ʈ", "��Ŭ����Ƽ����" }
	};
	
	//���� ���̵��� ������ ��� �迭
	//* �������� ��µǴ� �迭�� ���� ������ ������ �������� ������ ������ �ٲ� �� �ֽ��ϴ�.
	final Level[] LEVEL = {
			new Level(500, 2000, 2000, data[0]),
			new Level(400, 1500, 4000, data[1]),
			new Level(300, 1000, 6000, data[2]),
			new Level(200, 500, 8000, data[3])
	};
	
	//���� ������ ���ڵ��� ��� ����
	Vector words = new Vector();
	
	//���ڸ� ġ�� �ؽ�Ʈ��
	TextField tf = new TextField();
	
	//���� ������ ����� �г� : ����� ����� ��
	Panel pScore = new Panel(new GridLayout(1,3));
	
	//����, ���ھ�, ���� ����
	Label lbLevel = new Label("Level:" + curLevel, Label.CENTER);
	Label lbScore = new Label("Score:" + score, Label.CENTER);
	Label lbLife = new Label("Life:" + life, Label.CENTER);
	
	//�߾ӿ� ���ڵ��� �������� ȭ�� �г�
	MyCanvas screen = new MyCanvas();
	
	//�̹���
	Image img = Toolkit.getDefaultToolkit().getImage("C:\\image\\backgound.jpg");
	
	//���̷��� ��ü ����
	Virus v = new Virus(words);
	
	TypingGameCore(){
		this("Typing game ver 1.0");
	}
	
	//ȭ�� ����
	TypingGameCore(String title){
		super(title);
		
		//����â�� ������
		pScore.setBackground(Color.MAGENTA);
		
		//����â�� �� ���� �߰�
		pScore.add(lbLevel);
		pScore.add(lbScore);
		pScore.add(lbLife);
		
		//������ â�� �� ����â, ���ڰ� ������ ��ũ��, �ؽ�Ʈ�� �߰�
		add(pScore, "North");
		add(screen, "Center");
		add(tf, "South");
		
		//�����尣�� ����� ���� ȭ�鿡 ��������� ����ϴ� �ڵ鷯
		MyEventHandler handler = new MyEventHandler();
		addWindowListener(handler);
		tf.addActionListener(handler);
		
		//���� ���� ������ ȭ���� ����
		setBounds(500, 300, FRAME_WIDTH, FRAME_HEIGHT);
		setResizable(false);
		setVisible(true);
		setSize(639, 358);
		
		SCREEN_WIDTH = screen.getWidth();
		SCREEN_HEIGHT = screen.getHeight();
		MAX_LEVEL = LEVEL.length - 1;
		
		fm = getFontMetrics(getFont());
	}
	
	//���� ȭ�� ����
	public void repaint() {
		super.repaint();
		screen.repaint();
	}
	
	//���� ������ ������ ��Ű��
	static public void delay(int millis) {
		try {
			Thread.sleep(millis);
		} catch(Exception e) { }
	}
	
	//������ ����
	public void start(TypingGameClient tc) {
		showLevel(0);
		isPlaying = true;
		
		File file = new File("C:\\sound\\Music.wav");
		
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(file);
			Clip clip = AudioSystem.getClip();
			clip.open(stream);
			clip.start();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		wg = new WordGenerator();
		wg.start();
		
		wm = new WordDropper();
		wm.start();
		
		this.tc = tc;
	}
	
	//���� �����ϱ�
	public Level getLevel(int level) {
		if(level > MAX_LEVEL)
			level = MAX_LEVEL;
		if(level < 0)
			level = 0;
		
		return LEVEL[level];
	}
	
	//�������� �ص� �Ǵ��� �ȵǴ��� üũ
	public boolean levelUpCheck() {
		Level lvl = getLevel(curLevel);
		
		return score >= lvl.levelUpScore;
	}
	
	//���� ���� ���
	public synchronized int getCurLevel() {
		return curLevel;
	}
	
	//������ ó��
	public synchronized void levelUp() {
		virusGrp.interrupt();
		
		Level lvl = getLevel(++curLevel);
		
		lbLevel.setText("Level:" + curLevel);
		words.clear();
		screen.clear();
		showLevel(curLevel);
		
		speed = lvl.speed;
		interval = lvl.interval;
	}
	
	public void showLevel(int level) {
		showTitle("Level " + level);
	}
	
	//ȭ�鿡 ���� ��� : ���̵� ���� ��¿� ���
	public void showTitle(String title, int time, Color color) {
		Graphics g = screen.getGraphics();
		
		//��Ʈ ����
		Font titleFont = new Font("Serif", Font.BOLD, 70);
		g.setFont(titleFont);
		g.setColor(color);
		
		FontMetrics fm = getFontMetrics(titleFont);
		int width = fm.stringWidth(title);
		
		g.drawString(title, SCREEN_WIDTH/2 - 170, SCREEN_HEIGHT/2);
		delay(time);
	}
	
	public void showTitle(String title) {
		showTitle(title, 1*1000, Color.BLACK);
	}
	
	//�۾� ����߸��� ������
	class WordDropper extends Thread {
		public void run() {
			outer:
				while(isPlaying) {
					delay(speed);
					
					for(int i=0; i<words.size(); i++) {
						Word tmp = (Word)words.get(i);
						
						tmp.y += tmp.step;
						
						if(tmp.y >= SCREEN_HEIGHT) {
							tmp.y = SCREEN_HEIGHT;
							words.remove(tmp);
							life--;
							lbLife.setText("Life:" + life);
							break;
						}
						
						if(life <= 0) {
							isPlaying = false;
							showTitle("Game Over", 0, Color.WHITE);
							
							break outer;
						}
					}
					repaint();
				}
		}
	}
	
	//�۾� �����ϴ� ������
	class WordGenerator extends Thread {
		public void run() {
			while(isPlaying) {
				String[] data = LEVEL[getCurLevel()].data;
				
				int rand = (int)(Math.random() * data.length);
				
				//�� 10���� �� �� �÷� ���̷��� ����
				boolean isVirus = ((int)(Math.random() * 10) + 1) / 10 != 0;
				
				Word word = new Word(data[rand], isVirus);
				words.add(word);
				delay(interval);
			}
		}
	}
	
	//ȭ�鿡 ����ִ� �޼������ ���� Ŭ����
	class MyCanvas extends JPanel {
		//ȭ���� ���� ���� �����
		public void clear() {
			Graphics g = getGraphics();
			g.clearRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
		}
		
		//ȭ���� ���� ���� ����ϱ�
		public void paint(Graphics g) {
			clear();
			
			//���ȭ�� ����
			g.drawImage(img, 0, 0, this);
			
			for(int i=0; i<words.size(); i++) {
				Word tmp = (Word)words.get(i);
				g.setColor(tmp.color);
				g.drawString(tmp.word, tmp.x, tmp.y);
			}
		}
		
		@Override
		public void update(Graphics g) {
			paint(g);
		}
	}
	
	//���̷��� ��� ����
	class VirusThread extends Thread {
		public VirusThread(ThreadGroup group, String name) {
			super(group, name);
		}
		
		public void run() {
			int rand = (int)(Math.random() * 9);
			
			int oldValue = 0;
			int virusTime = 10 * 1000; //���̷��� ���۽ð� 10��
			
			switch(rand) {
				case 0:
					speed = speed / 2;
					break;
				case 1:
					interval = interval / 2;
					break;
				case 2:
					speed = speed * 2;
					break;
				case 3:
					interval = interval * 2;
					break;
				case 4:
					words.clear();
					break;
				case 5:
					v.ReverseName();
					break;
				case 6:
					v.LongName();
					break;
				case 7:
					v.RandomName();
					break;
				case 8:
					v.QueName();
					break;
			}
			
			delay(virusTime);
			
			int curLevel = getCurLevel();
			speed = LEVEL[curLevel].speed;
			interval = LEVEL[curLevel].interval;
		}
	}
	
	//������ ���̷��� Ŭ����
	class Virus {
		Vector<Word> words;
		String[] temp;
		
		Virus(Vector words){
			this.words = words;
		}
		
		//���� ���
		void ReverseName() {
			for(int i=0; i<words.size(); i++) {
				Word w = words.get(i);
				String word = w.word;
				
				temp = new String[word.length()];
				
				for(int j=word.length(); j>0; j--)
					temp[j-1] = Character.toString(word.charAt(word.length()-j));
				
				String strTemp = "";
				
				for(int k=0; k<temp.length; k++)
					strTemp += temp[k];
				
				w.word = strTemp;
				words.set(i, w);
			}
		}
		
		//�ܾ� ���� �ø���
		void LongName() {
			for(int i=0; i<words.size(); i++) {
				String tmp = "";
				
				Word w = words.get(i);
				String word = w.word;
				
				for(int j=0; j<word.length(); j++)
					tmp += word.charAt(j) + word.charAt(j);
				
				w.word = tmp;
				words.set(i, w);
			}
		}
		
		//���� ���
		void RandomName() {
			for(int i=0; i<words.size(); i++) {
				Word w = words.get(i);
				String word = w.word;
				
				temp = new String[word.length()];
				
				for(int j=0; j<temp.length; j++)
					temp[j] = Character.toString(word.charAt(j));
				
				int mix = (int)(Math.random() * word.length());
				
				for(int k=0; k<temp.length; k++) {
					String tmp = temp[mix];
					temp[mix] = temp[k];
					temp[k] = tmp;
				}
				
				String strTemp = "";
				for(int q=0; q<temp.length; q++)
					strTemp += temp[q];
				
				w.word = strTemp;
				words.set(i, w);
			}
		}
		
		// $
		void QueName() {
			for(int i=0; i<words.size(); i++) {
				Word w = words.get(i);
				String word = w.word;
				
				temp = new String[word.length()];
				
				for(int j=0; j<temp.length; j++)
					temp[j] = Character.toString(word.charAt(j));
				
				int mix = (int)(Math.random() * word.length());
				temp[mix] = "$";
				
				String strTemp = "";
				
				for(int k=0; k<word.length(); k++)
					strTemp += temp[k];
				
				w.word = strTemp;
				words.set(i, w);
			}
		}
	}
	
	//���̵��� ������ ��� Ŭ����
	class Level {
		int speed;
		int interval;
		int levelUpScore;
		String[] data;
		
		Level(int speed, int interval, int levelUpScore, String[] data) {
			this.speed = speed;
			this.interval = interval;
			this.levelUpScore = levelUpScore;
			this.data = data;
		}
	}
	
	//���� ������ �����ϴ� Ŭ����
	class Word {
		String word = "";
		int x = 0;
		int y = 0;
		int step = 5;
		
		Color color = Color.WHITE;
		boolean isVirus = false;
		
		//������ : ���ڸ�, �������� ���� ��ġ(10�� �ְ���), ���̷��� X
		Word(String word) {
			this(word, 10, false);
		}
		
		//���̷����� ���, �� �����ڿ� (���ڸ�, true)�� �����ڸ� ����
		Word(String word, boolean isVirus) {
			this(word, 10, isVirus);
		}
		
		Word(String word, int step, boolean isVirus) {
			this.word = word;
			this.step = step;
			this.isVirus = isVirus;
			
			//���̷����� ���, ���ڸ� ����������
			if(isVirus)
				color = Color.RED;
			
			int strWidth = fm.stringWidth(word);
			
			x = (int)(Math.random() * SCREEN_WIDTH);
			
			if(x + strWidth >= SCREEN_WIDTH)
				x = SCREEN_WIDTH - strWidth;
		}
		
		public String toString() {
			return word;
		}
	}
	
	class MyEventHandler extends WindowAdapter implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			String input = tf.getText().trim();
			tf.setText("");
			
			System.out.println(input);
			
			if(!isPlaying)
				return;
			
			for(int i=0; i<words.size(); i++) {
				Word tmp = (Word)words.get(i);
				
				if(input.equals(tmp.word)) {
					words.remove(i);
					score += input.length() * 50;
					lbScore.setText("Score:" + score);
					
					if(curLevel != MAX_LEVEL && levelUpCheck()) {
						levelUp();
					} else {
						if(tmp.isVirus) {
							tc.sendVirus();
						}
					}
					break;
				}
			}
			repaint();  //��ȭ�� ���� �� ���� ȣ���ؾ� �ٷ� �ٷ� �����
		}
		
		//���� ����
		public void windowClosing(WindowEvent e) {
			e.getWindow().setVisible(false);
			e.getWindow().dispose();
		}
	}
	
	//���̷��� ������ �߻���Ŵ
	void newVirus() {
		new VirusThread(virusGrp, "virus").start();
	}
}