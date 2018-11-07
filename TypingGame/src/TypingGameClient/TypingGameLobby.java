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
	
	//Ÿ���� ���� ������ ���� ����, ���� ����
	final int FRAME_WIDTH = 350;
	final int FRAME_HEIGHT = 450;
	
	//���� �غ�
	static boolean isReady = false;
	static boolean isAllReady = false;
	
	//��Ʈ��ũ ���� ��� �г�
	Panel nPanel = new Panel(new GridLayout(3,1));
	Panel underNPanel = new Panel(new GridLayout(1,2));
	Panel buttonNPanel = new Panel(new GridLayout(4,1));
	
	//��Ʈ��ũ ó�� ���� �ؽ�Ʈ��
	TextArea taNetwork = new TextArea();  //��Ʈ��ũ ä��, ���� ���
	TextField tfNetwork = new TextField();  //ä�� �޼��� �Է� �ؽ�Ʈ �ʵ�
	TextField tfConnect = new TextField();  //IP �Է� �ؽ�Ʈ �ʵ�
	TextField tfNick = new TextField();  //�г��� �Է� �ؽ�Ʈ �ʵ�
	
	//��Ʈ��ũ ó�� ���� ��ư
	Button btnConnect = new Button("����");
	Button btnReady = new Button("�غ�");
	
	//������ ����Ʈ
	List listJoin = new List();
	
	TypingGameLobby() {
		this("Typing game ver 1.0");
	}
	
	TypingGameLobby(String name) {
		super(name);
		
		TypingGameLobby mine = this;
		
		//���� ��ư �̺�Ʈ
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tc = new TypingGameClient(mine);
				tc.start(tfNick.getText());
			}
		});
		
		//�غ� ��ư �̺�Ʈ
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
		
		//��Ʈ��ũ ����
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