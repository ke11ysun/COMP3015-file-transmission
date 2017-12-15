package demo;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

//multithread downloading for one client
public class FCMTD extends Thread {
	Socket socket;
	DataInputStream in;
	DataOutputStream out;
	public ArrayList<String> theNames = new ArrayList<String>();
	int runtime = 0;
	boolean isVerified;

	void sendToServer(String msg) throws IOException {
		out.writeInt(msg.length());
		out.write(msg.getBytes());
		out.flush();
	}

	public void run() {
		byte[] data;
		int size, len;

		try {
			size = in.readInt();
			data = new byte[size];
			//String dest = "/Users/sunjingxuan/Desktop/MTD";
			String dest = "D:/MTD";
			new File(dest).mkdirs();
			String fileName = theNames.get(runtime);
			runtime++;
			DataOutputStream outToFile = new DataOutputStream(new FileOutputStream(dest + "/" + fileName));

			do {
				len = in.read(data, data.length - size, size);
				size -= len;
				outToFile.write(data);
			} while (size > 0);
			System.out.println("File " + fileName + " downloaded!");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void connect(String fileName, String serverIP, int port) throws UnknownHostException, IOException {

		socket = new Socket(InetAddress.getByName(serverIP), port);
		System.out.println("Connected to server port# " + port + " using local port: " + socket.getLocalPort());
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());

		out.writeInt(fileName.length());
		out.write(fileName.getBytes());
		out.flush();
		System.out.println("ask for file: " + fileName);

		theNames.add(fileName);

		Thread thread = new Thread(this);
		thread.start();
	}

	// won't be used
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		System.out.println("Welcome to FCMTD!");

		System.out.println("Please input FILE NAMES you want to download, separate with \",\" : ");
		String input = scan.nextLine();
		String[] fileNames = input.split(",");

		int notFile = 0;
		do {
			System.out.println("start check type.");
			for (int i = 0; i < fileNames.length; i++) {
				System.out.println(fileNames[i]);
				System.out.println(new File(new File(fileNames[i]).getAbsolutePath()).isFile());
				if (new File(fileNames[i]).isDirectory())
					notFile++;
			}
			if (notFile > 0) {
				System.out.println(
						"\nWrong type! Cannot download DIRECTORY!\nPlease input FILE NAMES you want to download, separate with \",\": ");
				input = scan.nextLine();
				fileNames = input.split(",");
			}
		} while (notFile > 0);

		for (int i = 0; i < fileNames.length; i++) {
			if (fileNames[i] != null) {
				FCMTD client = new FCMTD();
				try {
					client.connect(fileNames[i], "158.182.6.86", 7999);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}

	}
}
