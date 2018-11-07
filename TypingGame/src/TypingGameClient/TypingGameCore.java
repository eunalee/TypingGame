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
	//타이핑 게임 프레임 가로 넓이, 세로 넓이
	final int FRAME_WIDTH = 639;
	final int FRAME_HEIGHT = 358;
	
	final int SCREEN_WIDTH;
	final int SCREEN_HEIGHT;
	
	int speed = 500; //단어가 떨어 지는 속도 - 높을 수록 느림
	int interval = 3 * 1000; //새로운 단어가 나오는 간격
	
	//점수, 생명, 현재 레벨, 최고 레벨 초기화
	int score = 0;
	int life = 3;
	int curLevel = 0;
	final int MAX_LEVEL;
	
	boolean isPlaying = false;
	static boolean isConnect = false;
	
	WordGenerator wg = null; //단어를 생성하는 쓰레드
	WordDropper wm = null; //단어를 떨어뜨리는 쓰레드
	
	TypingGameClient tc = null;
	
	FontMetrics fm; //화면에서의 글자 길이를 구하는데 사용
	ThreadGroup virusGrp = new ThreadGroup("virus"); //바이러스 쓰레드들의 그룹
	
	String[][] data = {
			{ "토니", "마이클", "B조던", "스타크", "크리스", "주니어", "드롤링", "채드윅", "플랫", "올슨", "퀼", "피터", "토르" },
			{ "아이언맨", "캡틴아메리카", "비젼", "맨티스", "로켓", "타노스", "인더스트리", "어벤져스", "자비스", "묠늬르" },
			{ "햄스워스", "마르지엘라", "카렌딜런", "라이트", "레티티아", "그루트", "빈디젤", "돈치들" },
			{ "베네딕트", "컴버비치", "햄스워스", "블랙팬서", "아이언맨", "스칼렛요한슨", "엘리자베스", "폴벳타니엘", "조셀디나", "사무엘잭슨", "스파이더맨", "베이비그루트", "폼클레멘티에프" }
	};
	
	//게임 난이도의 정보가 담긴 배열
	//* 레벨별로 출력되는 배열을 수정 가능한 구조로 레벨마다 글자의 패턴을 바꿀 수 있습니다.
	final Level[] LEVEL = {
			new Level(500, 2000, 2000, data[0]),
			new Level(400, 1500, 4000, data[1]),
			new Level(300, 1000, 6000, data[2]),
			new Level(200, 500, 8000, data[3])
	};
	
	//현재 생성된 글자들을 담는 벡터
	Vector words = new Vector();
	
	//글자를 치는 텍스트바
	TextField tf = new TextField();
	
	//각종 정보를 출력할 패널 : 상단의 노란색 바
	Panel pScore = new Panel(new GridLayout(1,3));
	
	//레벨, 스코어, 생명 정보
	Label lbLevel = new Label("Level:" + curLevel, Label.CENTER);
	Label lbScore = new Label("Score:" + score, Label.CENTER);
	Label lbLife = new Label("Life:" + life, Label.CENTER);
	
	//중앙에 글자들이 떨어지는 화면 패널
	MyCanvas screen = new MyCanvas();
	
	//이미지
	Image img = Toolkit.getDefaultToolkit().getImage("C:\\image\\backgound.jpg");
	
	//바이러스 객체 생성
	Virus v = new Virus(words);
	
	TypingGameCore(){
		this("Typing game ver 1.0");
	}
	
	//화면 정보
	TypingGameCore(String title){
		super(title);
		
		//정보창의 배경색깔
		pScore.setBackground(Color.MAGENTA);
		
		//정보창의 각 정보 추가
		pScore.add(lbLevel);
		pScore.add(lbScore);
		pScore.add(lbLife);
		
		//윈도우 창에 각 정보창, 글자가 떨어질 스크린, 텍스트바 추가
		add(pScore, "North");
		add(screen, "Center");
		add(tf, "South");
		
		//쓰레드간의 통신을 도와 화면에 변경사항을 출력하는 핸들러
		MyEventHandler handler = new MyEventHandler();
		addWindowListener(handler);
		tf.addActionListener(handler);
		
		//제공 받은 정보로 화면을 생성
		setBounds(500, 300, FRAME_WIDTH, FRAME_HEIGHT);
		setResizable(false);
		setVisible(true);
		setSize(639, 358);
		
		SCREEN_WIDTH = screen.getWidth();
		SCREEN_HEIGHT = screen.getHeight();
		MAX_LEVEL = LEVEL.length - 1;
		
		fm = getFontMetrics(getFont());
	}
	
	//현재 화면 비우기
	public void repaint() {
		super.repaint();
		screen.repaint();
	}
	
	//현재 쓰레드 딜레이 시키기
	static public void delay(int millis) {
		try {
			Thread.sleep(millis);
		} catch(Exception e) { }
	}
	
	//쓰레드 시작
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
	
	//레벨 설정하기
	public Level getLevel(int level) {
		if(level > MAX_LEVEL)
			level = MAX_LEVEL;
		if(level < 0)
			level = 0;
		
		return LEVEL[level];
	}
	
	//레벨업을 해도 되는지 안되는지 체크
	public boolean levelUpCheck() {
		Level lvl = getLevel(curLevel);
		
		return score >= lvl.levelUpScore;
	}
	
	//현재 레벨 얻기
	public synchronized int getCurLevel() {
		return curLevel;
	}
	
	//레벨업 처리
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
	
	//화면에 글자 출력 : 난이도 레벨 출력에 사용
	public void showTitle(String title, int time, Color color) {
		Graphics g = screen.getGraphics();
		
		//폰트 정보
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
	
	//글씨 떨어뜨리는 쓰레드
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
	
	//글씨 생성하는 쓰레드
	class WordGenerator extends Thread {
		public void run() {
			while(isPlaying) {
				String[] data = LEVEL[getCurLevel()].data;
				
				int rand = (int)(Math.random() * data.length);
				
				//약 10번에 한 번 꼴로 바이러스 생성
				boolean isVirus = ((int)(Math.random() * 10) + 1) / 10 != 0;
				
				Word word = new Word(data[rand], isVirus);
				words.add(word);
				delay(interval);
			}
		}
	}
	
	//화면에 찍어주는 메서드들의 집합 클래스
	class MyCanvas extends JPanel {
		//화면의 글자 내용 지우기
		public void clear() {
			Graphics g = getGraphics();
			g.clearRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
		}
		
		//화면의 글자 내용 출력하기
		public void paint(Graphics g) {
			clear();
			
			//배경화면 설정
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
	
	//바이러스 기능 구현
	class VirusThread extends Thread {
		public VirusThread(ThreadGroup group, String name) {
			super(group, name);
		}
		
		public void run() {
			int rand = (int)(Math.random() * 9);
			
			int oldValue = 0;
			int virusTime = 10 * 1000; //바이러스 동작시간 10초
			
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
	
	//종류별 바이러스 클래스
	class Virus {
		Vector<Word> words;
		String[] temp;
		
		Virus(Vector words){
			this.words = words;
		}
		
		//역순 출력
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
		
		//단어 길이 늘리기
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
		
		//랜덤 출력
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
	
	//난이도의 정보를 담는 클래스
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
	
	//글자 정보를 정의하는 클래스
	class Word {
		String word = "";
		int x = 0;
		int y = 0;
		int step = 5;
		
		Color color = Color.WHITE;
		boolean isVirus = false;
		
		//생성자 : 글자명, 떨어지는 현재 위치(10이 최고점), 바이러스 X
		Word(String word) {
			this(word, 10, false);
		}
		
		//바이러스일 경우, 이 생성자에 (글자명, true)로 생성자를 지정
		Word(String word, boolean isVirus) {
			this(word, 10, isVirus);
		}
		
		Word(String word, int step, boolean isVirus) {
			this.word = word;
			this.step = step;
			this.isVirus = isVirus;
			
			//바이러스일 경우, 글자를 빨간색으로
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
			repaint();  //변화가 있을 때 마다 호출해야 바로 바로 적용됨
		}
		
		//게임 종료
		public void windowClosing(WindowEvent e) {
			e.getWindow().setVisible(false);
			e.getWindow().dispose();
		}
	}
	
	//바이러스 쓰레드 발생시킴
	void newVirus() {
		new VirusThread(virusGrp, "virus").start();
	}
}