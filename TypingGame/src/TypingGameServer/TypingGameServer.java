package TypingGameServer;

import java.io.*;
import java.net.*;
import java.util.*;

public class TypingGameServer {
	HashMap clients;  //������ Ŭ���̾�Ʈ�� ������ ��� HashMap
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
			System.out.println("������ ���۵Ǿ����ϴ�.");
			
			while(true) {
				socket = serverSocket.accept();
				System.out.println("[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "���� �����Ͽ����ϴ�.");
				
				ServerReceiver thread = new ServerReceiver(socket);
				ServerSender sendThread = new ServerSender(socket, "����");
				
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
				String key = (String)it.next();  //�޾ƿ� ���� �̸�
				
				if(key.equals(id))
					continue;  //�Ѿ�� id�� ������ ��� ������� output�� ����
				
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
				
				playerPoint.put(name, 0);  //�÷��̾��� ����Ʈ�� ��� HashMap ��ü�� �÷��̾� �ʱ�ȭ
				playerIsReady.put(name, false);
				sendToAll("#:" + name + ":Join");
				System.out.println("#" + name + "���� �����̽��ϴ�.");
				
				clients.put(name, out);
				System.out.println("���� ���� ������ ���� " + clients.size() + "�Դϴ�.");
				
				while(in != null) {
					String msg = in.readUTF();  //�� �÷��̾�鿡�� ���� �޼���
					String[] msgArr = msg.split(":");  // : �����ڷ� �޼����� ����
					
					System.out.println(msg);
					
					//���� ó��
					if(msgArr[0].equals("PT"))  //���� �޼����� ���� ���� �޼����� ���
						playerPoint.put(msgArr[1], msgArr[2]);
					
					//�ٸ� �����鿡�Ե� ���� ���� ������ �ѷ���
					sendToAll(msg);
					
					//���̷��� ó��
					if(msgArr[0].equals("VS"))
						sendToWithout(msg, name);
					
					//�غ� ���� üũ
					if(msgArr[0].equals("RD")) {
						sendToAll(msg);
						playerIsReady.put(msgArr[1], true);
						
						int readyCnt = 0;
						
						Iterator it = playerIsReady.keySet().iterator();
						
						while(it.hasNext()) {
							System.out.println("�����");
							
							String playerName = (String)it.next();
							
							if(playerIsReady.get(playerName).equals(true))
								readyCnt++;
						}
						
						System.out.println(readyCnt);
						
						//���� ��ŭ�� ����� ���� ���� �Ϸ����� ���, ������ ����
						if(readyCnt == playerIsReady.size())
							sendToAll("OK:����:������ �����ϰڽ��ϴ�.");
					}
					
					//ä�� ó��
					if(msgArr[0].equals("CH"))
						sendToAll(msg);
				}
			} catch(IOException e) {
			} finally {
				sendToAll("#:" + name + ":Exit");
				playerIsReady.remove(name);
				clients.remove(name);
				playerPoint.remove(name);
				System.out.println("[" + socket.getInetAddress() + ":" + socket.getPort() + "]" + "���� ������ �����Ͽ����ϴ�.");
				System.out.println("���� ���� ������ ���� " + clients.size() + "�Դϴ�.");
		}
		}
	}
}