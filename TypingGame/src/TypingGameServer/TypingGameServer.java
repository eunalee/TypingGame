package TypingGameServer;

import java.io.*;
import java.net.*;
import java.util.*;

public class TypingGameServer {
	HashMap clients;  //들어오는 클라이언트의 정보를 담는 HashMap
	HashMap playerPoint;
	HashMap playerIsReady;
	
	TypingGameServer() {
		clients = new HashMap();
		playerPoint = new HashMap();
		playerIsReady = new HashMap();
		
		Collections.synchronizedMap(clients);
		Collections.synchronizedMap(playerPoint);
		Collections.synchronizedMap(playerIsReady);
	}
	
	public void start() {
		ServerSocket serverSocket = null;
		Socket socket = null;
		
		try {
			serverSocket = new ServerSocket(7777);
			System.out.println("서버가 시작되었습니다.");
			
			while(true) {
				socket = serverSocket.accept();
				System.out.println("[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "에서 접속하였습니다.");
				
				ServerReceiver thread = new ServerReceiver(socket);
				ServerSender sendThread = new ServerSender(socket, "서버");
				
				thread.start();
				sendThread.start();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	void sendToAll(String msg) {
		Iterator it = clients.keySet().iterator();
		
		while(it.hasNext()) {
			try {
				DataOutputStream out = (DataOutputStream)clients.get(it.next());
				out.writeUTF(msg);
			} catch(IOException e) { }
		}
	}
	
	void sendToWithout(String msg, String id) {
		Iterator it = clients.keySet().iterator();
		
		while(it.hasNext()) {
			try {
				String key = (String)it.next();  //받아온 값의 이름
				
				if(key.equals(id))
					continue;  //넘어온 id를 제외한 모든 사람에게 output을 날림
				
				DataOutputStream out = (DataOutputStream)clients.get(key);
				out.writeUTF(msg);
			} catch(IOException e) { }
		}
	}
	
	class ServerSender extends Thread {
		Socket socket;
		DataOutputStream out;
		String name;
		
		ServerSender(Socket socket, String name){
			this.socket = socket;
			
			try {
				out = new DataOutputStream(socket.getOutputStream());
				this.name = name;
			} catch(Exception e) { }
		}
		
		public void run() {
			Scanner scanner = new Scanner(System.in);
			
			try {
				if(out != null)
					out.writeUTF(name);
				
				while(out != null) {
					String msg = scanner.nextLine();
					out.writeUTF("[" + name + "]" + msg);
					System.out.println("[" + name + "]" + msg);
				}
			} catch(IOException e) { }
		}
	}
	
	class ServerReceiver extends Thread {
		Socket socket;
		DataInputStream in;
		DataOutputStream out;
		
		ServerReceiver(Socket socket) {
			this.socket = socket;
			
			try {
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
			} catch(IOException e) { }
		}
		
		public void run() {
			String name = "";
			String nowPlayer = "";
			
			try {
				name = in.readUTF();
				
				playerPoint.put(name, 0);  //플레이어의 포인트를 담는 HashMap 객체에 플레이어 초기화
				playerIsReady.put(name, false);
				sendToAll("#:" + name + ":Join");
				System.out.println("#" + name + "님이 들어오셨습니다.");
				
				clients.put(name, out);
				System.out.println("현재 서버 접속자 수는 " + clients.size() + "입니다.");
				
				while(in != null) {
					String msg = in.readUTF();  //각 플레이어들에게 들어온 메세지
					String[] msgArr = msg.split(":");  // : 구분자로 메세지를 구별
					
					System.out.println(msg);
					
					//점수 처리
					if(msgArr[0].equals("PT"))  //들어온 메세지가 점수 관련 메세지일 경우
						playerPoint.put(msgArr[1], msgArr[2]);
					
					//다른 유저들에게도 점수 관련 정보를 뿌려줌
					sendToAll(msg);
					
					//바이러스 처리
					if(msgArr[0].equals("VS"))
						sendToWithout(msg, name);
					
					//준비 상태 체크
					if(msgArr[0].equals("RD")) {
						sendToAll(msg);
						playerIsReady.put(msgArr[1], true);
						
						int readyCnt = 0;
						
						Iterator it = playerIsReady.keySet().iterator();
						
						while(it.hasNext()) {
							System.out.println("실행됨");
							
							String playerName = (String)it.next();
							
							if(playerIsReady.get(playerName).equals(true))
								readyCnt++;
						}
						
						System.out.println(readyCnt);
						
						//정원 만큼의 사람의 수가 레디를 완료했을 경우, 게임을 시작
						if(readyCnt == playerIsReady.size())
							sendToAll("OK:서버:게임을 시작하겠습니다.");
					}
					
					//채팅 처리
					if(msgArr[0].equals("CH"))
						sendToAll(msg);
				}
			} catch(IOException e) {
			} finally {
				sendToAll("#:" + name + ":Exit");
				playerIsReady.remove(name);
				clients.remove(name);
				playerPoint.remove(name);
				System.out.println("[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "에서 접속을 종료하였습니다.");
				System.out.println("현재 서버 접속자 수는 " + clients.size() + "입니다.");
		}
		}
	}
}