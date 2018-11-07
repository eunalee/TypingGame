package TypingGameClient;

import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

public class TypingGameClient {
	static int _score = 0;
	static String ID;
	
	//�� ���� Ŭ����
	static TypingGameLobby tyGame;
	
	//�÷��̾��� ������ ���� HashMap ��ü
	static HashMap playerPoint = null;
	
	//ä�� �޼��� ����
	static String chatMsg = "";
	
	//�޼��� ���� 
	static boolean isEntered = false;
	static boolean isVirus = false;
	static boolean isReady = false;
	
	public TypingGameClient(TypingGameLobby tyGame) {
		//����� ���� Ŭ����
		this.tyGame = tyGame;
		
		//�÷��̾� ����Ʈ�� �����ϴ� HashMap
		playerPoint = new HashMap();
		
		//ä��â ����
		tyGame.tfNetwork.addActionListener(new ActionListener() {
			//���� ���� ��, ����
			@Override
			public void actionPerformed(ActionEvent e) {
				chatMsg = tyGame.tfNetwork.getText();
				isEntered = true;
				tyGame.tfNetwork.setText("");
			}
		});
	}
	
	void start(String id) {
		try {
			String serverIp = "127.0.0.1";
			//������ �����Ͽ� ���� ��û
			Socket socket = new Socket(serverIp, 7777);
			System.out.println("������ ����Ǿ����ϴ�.");
			
			//������ ����
			Thread chatSender = new Thread(new chatSender(socket, id));
			Thread receiver = new Thread(new ClientReceiver(socket));
			Thread pointSender = new Thread(new PointSender(socket, id));
			
			//������ ����
			chatSender.start();
			pointSender.start();
			receiver.start();
		} catch(ConnectException ce) {
			ce.printStackTrace();
		} catch(Exception e) { }
	}
	
	static class Sender extends Thread {
		Socket socket;
		DataOutputStream out;
		String id;
		
		Sender(Socket socket, String id){
			this.socket = socket;
			
			try {
				out = new DataOutputStream(socket.getOutputStream());
				this.id = id;
			} catch(Exception e) { }
		}
	}
	
	static class chatSender extends Sender {
		chatSender(Socket socket, String id) {
			super(socket, id);
		}
		
		public void run() {
			try {
				if(out != null)
					out.writeUTF(id);
				
				while(out != null) {
					System.out.print("");
					
					if(isEntered) {
						out.writeUTF("CH:" + id + ":" + chatMsg);
						isEntered = false;
					}
				}
			} catch(IOException e) { }
		}
	}
	
	static class PointSender extends Sender {
		PointSender(Socket socket, String id) {
			super(socket, id);
		}
		
		public void run() {
			try {
				if(out != null)
					out.writeUTF(id);
				
				while(true) {
					//����Ʈ �������� ����
					out.writeUTF("PT:" + id + ":" + Integer.toString(_score));
					
					//���̷��� �������� ����
					if(isVirus) {
						out.writeUTF("VS:" + id + ":" + "���̷����� �߻����׽��ϴ�.");
						isVirus = false;
					}
					
					//���� ���� ����
					if(isReady) {
						out.writeUTF("RD:" + id + ":" + "���� �غ� �Ϸ��߽��ϴ�.");
						isReady = false;
					}
					
					//1�ʿ� �� ���� ����
					TypingGameCore.delay(1 * 1000);
				}
			} catch(IOException e) { }
		}
	}
	
	static class ClientReceiver extends Thread {
		Socket socket;
		DataInputStream in;
		
		ClientReceiver(Socket socket){
			this.socket = socket;
			
			try {
				in = new DataInputStream(socket.getInputStream());
			} catch(IOException e) { }
		}
		
		public void run() {
			while(in != null) {
				try {
					String msg = in.readUTF();
					System.out.println(msg);
					String[] msgArr = msg.split(":");
					
					//ä�� �޼��� ó��
					if(msgArr[0].equals("CH"))
						MessageAdd("[" + msgArr[1] + "] : " + msgArr[2]);
					
					//���� �޼��� ó��
					if(msgArr[0].equals("VS")) {
						MessageAdd("���̷����� �߻��߽��ϴ�.");
						tyGame.game.newVirus();
					}
					
					//���� �޼��� ó��
					if(msgArr[0].equals("#")) {
						String name = msgArr[1];
						String contrentValue = msgArr[2];
						
						switch(contrentValue){
							case "Join" :
								MessageAdd(msgArr[1] + "���� �����߽��ϴ�.");
								playerPoint.put(name, 0); //���� �ʱ�ȭ
								break;
							case "Exit" :
								MessageAdd(msgArr[1] + "���� �������ϴ�.");
								playerPoint.remove(name);
								break;
							default:
								break;
						}
					}
					
					//���� �޼��� ó��
					if(msgArr[0].equals("PT")) {
						playerPoint.put(msgArr[1], msgArr[2]);
						
						tyGame.listJoin.removeAll();
						
						//���� ���� �κ�
						Iterator it = playerPoint.keySet().iterator();
						
						//������ ���ŵ� ������ list ������Ʈ
						while(it.hasNext()) {
							String tempIT = (String)it.next();
							String tempGET = (String)playerPoint.get(tempIT);
							tyGame.listJoin.add(tempIT + ":" + tempGET);
						}
					}
					
					//���� ����
					if(msgArr[0].equals("OK"))
						tyGame.gameStart();
				} catch(IOException e) { }
			}
		}
		
		//����� TextArea�� �޼��� �߰�
		void MessageAdd(String msg) {
			TextArea ta = tyGame.taNetwork;
			
			//ä��â�� �ƹ� �޼����� ���� ��
			if(ta.getText().equals(""))
				ta.setText(msg);
			
			//ä��â�� �޼����� �̹� ������ ��
			else
				ta.setText(ta.getText() + "\n" + msg);
		}
	}
	
	//�� �޼��尡 �ߵ��Ǹ�
	void sendVirus() {
		isVirus = true;  //VS flag�� Ǯ�� -> PointSender���� flag�� true�̸� VS �޼����� ����
	}
	
	//�� �޼��尡 �ߵ��Ǹ�
	void sendReady() {
		isReady = true;  //���� �ƴٴ� �޼����� �������� ����
	}
}