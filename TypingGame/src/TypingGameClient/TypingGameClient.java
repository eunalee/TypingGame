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
	
	//본 게임 클래스
	static TypingGameLobby tyGame;
	
	//플레이어의 점수를 담을 HashMap 객체
	static HashMap playerPoint = null;
	
	//채팅 메세지 변수
	static String chatMsg = "";
	
	//메세지 보낼 
	static boolean isEntered = false;
	static boolean isVirus = false;
	static boolean isReady = false;
	
	public TypingGameClient(TypingGameLobby tyGame) {
		//연결된 메인 클래스
		this.tyGame = tyGame;
		
		//플레이어 포인트를 저장하는 HashMap
		playerPoint = new HashMap();
		
		//채팅창 엔터
		tyGame.tfNetwork.addActionListener(new ActionListener() {
			//엔터 쳤을 때, 반응
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
			//소켓을 생성하여 연결 요청
			Socket socket = new Socket(serverIp, 7777);
			System.out.println("서버에 연결되었습니다.");
			
			//쓰레드 정의
			Thread chatSender = new Thread(new chatSender(socket, id));
			Thread receiver = new Thread(new ClientReceiver(socket));
			Thread pointSender = new Thread(new PointSender(socket, id));
			
			//쓰레드 시작
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
					//포인트 서버에게 갱신
					out.writeUTF("PT:" + id + ":" + Integer.toString(_score));
					
					//바이러스 서버에게 갱신
					if(isVirus) {
						out.writeUTF("VS:" + id + ":" + "바이러스를 발생시켰습니다.");
						isVirus = false;
					}
					
					//레디 여부 갱신
					if(isReady) {
						out.writeUTF("RD:" + id + ":" + "님이 준비를 완료했습니다.");
						isReady = false;
					}
					
					//1초에 한 번씩 갱신
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
					
					//채팅 메세지 처리
					if(msgArr[0].equals("CH"))
						MessageAdd("[" + msgArr[1] + "] : " + msgArr[2]);
					
					//공격 메세지 처리
					if(msgArr[0].equals("VS")) {
						MessageAdd("바이러스가 발생했습니다.");
						tyGame.game.newVirus();
					}
					
					//접속 메세지 처리
					if(msgArr[0].equals("#")) {
						String name = msgArr[1];
						String contrentValue = msgArr[2];
						
						switch(contrentValue){
							case "Join" :
								MessageAdd(msgArr[1] + "님이 입장했습니다.");
								playerPoint.put(name, 0); //점수 초기화
								break;
							case "Exit" :
								MessageAdd(msgArr[1] + "님이 나갔습니다.");
								playerPoint.remove(name);
								break;
							default:
								break;
						}
					}
					
					//점수 메세지 처리
					if(msgArr[0].equals("PT")) {
						playerPoint.put(msgArr[1], msgArr[2]);
						
						tyGame.listJoin.removeAll();
						
						//점수 전달 부분
						Iterator it = playerPoint.keySet().iterator();
						
						//점수가 갱신될 때마다 list 업데이트
						while(it.hasNext()) {
							String tempIT = (String)it.next();
							String tempGET = (String)playerPoint.get(tempIT);
							tyGame.listJoin.add(tempIT + ":" + tempGET);
						}
					}
					
					//게임 시작
					if(msgArr[0].equals("OK"))
						tyGame.gameStart();
				} catch(IOException e) { }
			}
		}
		
		//연결된 TextArea에 메세지 추가
		void MessageAdd(String msg) {
			TextArea ta = tyGame.taNetwork;
			
			//채팅창에 아무 메세지도 없을 때
			if(ta.getText().equals(""))
				ta.setText(msg);
			
			//채팅창에 메세지가 이미 존재할 때
			else
				ta.setText(ta.getText() + "\n" + msg);
		}
	}
	
	//이 메서드가 발동되면
	void sendVirus() {
		isVirus = true;  //VS flag가 풀림 -> PointSender에서 flag가 true이면 VS 메세지가 날라감
	}
	
	//이 메서드가 발동되면
	void sendReady() {
		isReady = true;  //레디가 됐다는 메세지를 서버에게 보냄
	}
}