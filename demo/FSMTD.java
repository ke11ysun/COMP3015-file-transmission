package demo;

import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;

//multithread downloading for one client
//actually it's treating each FILE as one different CLIENT in the ChatRoom program
class ServerThread extends Thread {

	Socket cSocket;
	ArrayList<DataOutputStream> clients;
	DataInputStream in;
	DataOutputStream out;
	int used = 0;
	byte[] data;
	int size, len;
	public static String passServer;
	public static boolean set = false;
	boolean checked = false;
	String client;

	public ServerThread(Socket cSocket, ArrayList<DataOutputStream> clients) {
		this.cSocket = cSocket;
		this.clients = clients;
	}

	void checkPass() throws IOException {
		// 1st get: passClient
		size = in.readInt();
		data = new byte[size];
		do {
			len = in.read(data, data.length - size, size);
			size -= len;
		} while (size > 0);
		String passClient = new String(data);
		if (passClient.equals(passServer)) {
			checked = true;
		} else
			checked = false;
	}

	public void run() {
		try {
			in = new DataInputStream(cSocket.getInputStream());
			out = new DataOutputStream(cSocket.getOutputStream());
			clients.add(out);

			size = in.readInt();
			data = new byte[size];
			do {
				len = in.read(data, data.length - size, size);
				size -= len;
			} while (size > 0);

			String fileName = new String(data);
			//String src = "/Users/sunjingxuan/Desktop/project/share";
			String src = "D:/share";
			sendFile(out, src, fileName);

		} catch (IOException e1) {

		}
		clients.remove(out);
		System.out.println("still " + clients.size() + " clients.");
	}

	void sendFile(DataOutputStream out, String src, String fileName) throws IOException {
		File source = new File(src);
		File[] files = source.listFiles();
		int size = 0;
		for (int i = 0; i < files.length; i++) {
			if (fileName.equals(files[i].getName())) {
				size = (int) files[i].length();
			}
		}
		byte[] data = new byte[size];
		out.writeInt(size);

		File got = new File(src + "/" + fileName);
		System.out.println(got.getAbsolutePath());
		DataInputStream inFromFile = new DataInputStream(new FileInputStream(got));

		inFromFile.read(data, 0, size);
		inFromFile.close();
		System.out.println("file name: " + fileName);
		System.out.println("size: " + size);
		out.write(data);
		out.flush();
		System.out.println("Send to Client " + client + " :");
		System.out.println("File " + fileName + " sent.");

	}

}

public class FSMTD {

	public static String passServer;
	public static boolean set = false;
	boolean checked = false;
	DataOutputStream outToClient;
	DataInputStream inFromClient;
	ArrayList<DataOutputStream> clients = new ArrayList<DataOutputStream>();

	public FSMTD(int port) throws IOException {
		ServerSocket sSocket = new ServerSocket(port);
		System.out.printf("Listening to TCP port# %d...\n", sSocket.getLocalPort());
		Socket cSocket;
		int task = 1;
		while (true) {
			cSocket = sSocket.accept();
			ServerThread t = new ServerThread(cSocket, clients);
			t.start();
			task--;
			System.out.println("task: " + task);

			// if one round of command is over, this multithread server stops
			// automatically
			// need to start again manually if the client want to use this
			// function again
			if (task < 0) {
				System.out.println("Tasks done. Bye bye...");
				System.out.println("======================");
				sSocket.close();
				break;
			}
		}
	}

	static String setPass() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please input new password: ");
		passServer = scanner.nextLine();
		System.out.println("New password saved!");
		set = true;
		return passServer;
	}

	public static void main(String[] args) throws IOException {
		Scanner scanner = new Scanner(System.in);
		int choice = 4;
		System.out.println("Welcome to FSMTD!");
		while (choice != 0) {
			System.out.println("Press \"1\" to change password.");
			System.out.println("Press \"0\" to quit.");
			System.out.println("Press any other number to access share folder.");
			choice = scanner.nextInt();
			if (choice == 1) {
				passServer = setPass();
			} else if (choice == 0) {
				System.out.println("Bye bye!");
				break;
			} else {
				new FSMTD(7999);
			}
		}
	}

}
