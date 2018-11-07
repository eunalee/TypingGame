package TypingGameClient;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TypingGameLobby extends Frame {
	TypingGameCore game = null;
	TypingGameClient tc = null;
	
	//타이핑 게임 프레임 가로 넓이, 세로 넓이
	final int FRAME_WIDTH = 350;
	final int FRAME_HEIGHT = 450;
	
	//게임 준비
	static boolean isReady = false;
	static boolean isAllReady = false;
	
	//네트워크 정보 출력 패널
	Panel nPanel = new Panel(new GridLayout(3,1));
	Panel underNPanel = new Panel(new GridLayout(1,2));
	Panel buttonNPanel = new Panel(new GridLayout(4,1));
	
	//네트워크 처리 관련 텍스트바
	TextArea taNetwork = new TextArea();  //네트워크 채팅, 상태 출력
	TextField tfNetwork = new TextField();  //채팅 메세지 입력 텍스트 필드
	TextField tfConnect = new TextField();  //IP 입력 텍스트 필드
	TextField tfNick = new TextField();  //닉네임 입력 텍스트 필드
	
	//네트워크 처리 관련 버튼
	Button btnConnect = new Button("연결");
	Button btnReady = new Button("준비");
	
	//접속자 리스트
	List listJoin = new List();
	
	TypingGameLobby() {
		this("Typing game ver 1.0");
	}
	
	TypingGameLobby(String name) {
		super(name);
		
		TypingGameLobby mine = this;
		
		//접속 버튼 이벤트
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tc = new TypingGameClient(mine);
				tc.start(tfNick.getText());
			}
		});
		
		//준비 버튼 이벤트
		btnReady.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!isReady) {
					btnReady.setBackground(Color.YELLOW);
					isReady = true;
					tc.sendReady();
				}
			}
		});
		
		buttonNPanel.add(tfConnect);
		buttonNPanel.add(tfNick);
		buttonNPanel.add(btnConnect);
		buttonNPanel.add(btnReady);
		
		underNPanel.add(listJoin);
		underNPanel.add(buttonNPanel);
		
		//네트워크 정보
		nPanel.add(underNPanel);
		nPanel.add(taNetwork);
		nPanel.add(tfNetwork);
		
		add(nPanel);
		
		setBounds(700, 200, FRAME_WIDTH, FRAME_HEIGHT);
		setResizable(false);
		setVisible(true);
	}
	
	void gameStart() {
		game = new TypingGameCore();
		game.start(tc);
	}
	
	public static void main(String[] args) {
		TypingGameLobby tl = new TypingGameLobby();
	}
}