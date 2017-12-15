package demo;

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class FileServer {

	public static String passServer;
	public static boolean set = false;
	boolean checked = false;
	DataOutputStream outToClient;
	DataInputStream inFromClient;
	byte[] data;
	int size, len;
	ServerSocket sSocket;

	public FileServer(int port) throws IOException {
		sSocket = new ServerSocket(port);
		System.out.printf("Listening to TCP port# %d...\n", sSocket.getLocalPort());
		Socket cSocket = sSocket.accept();
		System.out.println(String.format("Established connection to client %s:%d",
				cSocket.getInetAddress().getHostAddress(), cSocket.getPort()));
		serve(cSocket);
		cSocket.close();
	}

	void serve(Socket cSocket) throws IOException {
		outToClient = new DataOutputStream(cSocket.getOutputStream());
		inFromClient = new DataInputStream(cSocket.getInputStream());

		// server user set system password or use default password
		if (!set)
			passServer = "z";

		// 1st send: password verify msg
		checkPass();
		while (!checked) {
			String msg = "Wrong password.";
			System.out.println(msg);
			outToClient.writeBoolean(false);
			outToClient.flush();
			checkPass();
		}
		if (checked) {
			String msg = "Password verified.";
			System.out.println(msg);
			outToClient.writeBoolean(true);
			outToClient.flush();

			// 2nd get: username
			size = inFromClient.readInt();
			data = new byte[size];
			do {
				len = inFromClient.read(data, data.length - size, size);
				size -= len;
			} while (size > 0);
			String client = new String(data);

			int funcNum = 5;
			boolean zero = false;
			boolean exist;
			while (funcNum != 0) {
				// 2.5 get: check exist
				exist = false;
				while (exist == false && zero == false) {
					size = inFromClient.readInt();
					data = new byte[size];
					do {
						len = inFromClient.read(data, data.length - size, size);
						size -= len;
					} while (size > 0);
					if (new String(data).equals("0,")) {
						funcNum = 0;
						System.out.println("Client " + client + " quitted program.");
						zero = true;
						break;
					}

					File fileToCheck = new File(new String(data));
					exist = fileToCheck.exists();
					// exist = true;

					outToClient.writeBoolean(exist);
					System.out.println(fileToCheck.getAbsolutePath() + ": " + exist);
					System.out.println("check result sent.");
				}

				// if client quit program, stop server too
				if (new String(data).equals("0,")) {
					sSocket.close();
					continue;
				}

				// 3rd get: command
				else {
					size = inFromClient.readInt();
					data = new byte[size];
					do {
						len = inFromClient.read(data, data.length - size, size);
						size -= len;
					} while (size > 0);
				}

				String command = new String(data);
				String[] tokens = command.split(",");
				funcNum = Integer.parseInt(tokens[0]);
				// if (funcNum == 0) {
				// System.out.println("Client " + client + " quitted program.");
				// break;
				// }
				if (funcNum == 1) {
					// 2nd send: file info
					System.out.println("Send to Client " + client + " info: ");
					sendInfo(tokens[1]);
				}
				if (funcNum == 2) {
					// 3rd send: file
					System.out.println("Send to Client " + client + " file: ");
					sendFile(tokens[1], tokens[2]);
				}
				if (funcNum == 3) {
					sendDir(tokens[1]);
				}

				// if client does not want to overwrite, start new round
				if (funcNum == 4) {
					continue;
				}
			}
			inFromClient.close();
			outToClient.close();
			cSocket.close();
		}
	}

	// send file/dir name and size through stream
	void sendInfo(String src) throws IOException {
		System.out.println(src);
		File folder = new File(src);
		File[] files = folder.listFiles();
		String[] info = new String[files.length];
		String data = "";
		String[] type = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			System.out.println(files[i].getName());
			if (!files[i].isFile())
				type[i] = "dir";
			else
				type[i] = "file";
			info[i] = files[i].getName() + " " + files[i].length() + " " + type[i] + ",";
			data += info[i];
		}
		outToClient.writeInt(data.length());
		outToClient.write(data.getBytes());
		outToClient.flush();
		System.out.println("Info sent.");
	}

	void sendFile(String src, String fileName) throws IOException {
		File source = new File(src);
		System.out.println(src);
		File[] files = source.listFiles();
		int size = 0;

		// get file size
		for (int i = 0; i < files.length; i++) {
			if (fileName.equals(files[i].getName())) {
				size = (int) files[i].length();
				System.out.println(files[i].getName());
			}
		}

		byte[] data = new byte[size];
		outToClient.writeInt(size);

		// write file to stream
		File got = new File(src + "/" + fileName);
		DataInputStream inFromFile = new DataInputStream(new FileInputStream(got));
		inFromFile.read(data, 0, size);
		inFromFile.close();
		System.out.println("size: " + size);
		outToClient.write(data);
		outToClient.flush();
		System.out.println("File " + fileName + " sent.");
	}

	void sendDir(String src) throws IOException {
		System.out.println("Send to Client info: ");
		sendInfo(src);
		File folder = new File(src);
		File[] files = folder.listFiles();

		// directly send file, recursively send dir
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				System.out.println("Send to Client file: ");
				sendFile(src, files[i].getName());
			} else {
				System.out.println(": it contains directory.");
				sendDir(files[i].getAbsolutePath());
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

	void checkPass() throws IOException {
		// 1st get: passClient
		size = inFromClient.readInt();
		data = new byte[size];
		do {
			len = inFromClient.read(data, data.length - size, size);
			size -= len;
		} while (size > 0);
		String passClient = new String(data);
		if (passClient.equals(passServer)) {
			checked = true;
		} else
			checked = false;
	}

	public static void main(String[] args) throws IOException {
		Scanner scanner = new Scanner(System.in);
		int choice = 4;
		System.out.println("Welcome to SERVER!");
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
				new FileServer(8999);
			}
		}
	}

}
