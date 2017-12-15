package demo;

import java.net.*;
import java.util.*;
import java.io.*;

public class FileClient extends Thread {

	DataOutputStream outToServer;
	DataInputStream inFromServer;
	//String defaultsrc = "/Users/sunjingxuan/Desktop/project/share";
	//String defaultdest = "/Users/sunjingxuan/Desktop/default";
	String defaultsrc = "D:/share";
	String defaultdest = "D:/default";
	Scanner scanner = new Scanner(System.in);
	String command;
	boolean exist;
	byte[] data;
	int size, len;

	void sendToServer(String msg) throws IOException {
		outToServer.writeInt(msg.length());
		outToServer.write(msg.getBytes());
		outToServer.flush();
	}

	public FileClient(String ServerIP, int port) throws IOException {

		Socket cSocket = new Socket(InetAddress.getByName(ServerIP), port);
		outToServer = new DataOutputStream(cSocket.getOutputStream());
		inFromServer = new DataInputStream(cSocket.getInputStream());
		System.out.println("Connected to server port# " + port + " using local port: " + cSocket.getLocalPort());

		String passClient = "";
		boolean isVerified = false;
		int funcNum = 5;

		ohh: while (funcNum != 0) {

			while (!isVerified) {
				System.out.println("Please input password: ");
				passClient = scanner.nextLine();
				// 1st send: passClient
				sendToServer(passClient);
				// 1st get: pass verify msg
				isVerified = inFromServer.readBoolean();
				if (!isVerified) {
					System.out.println("Wrong password!");
				} else {
					System.out.println("Password verified!");
					System.out.println("Please input your name: ");
					String user = scanner.nextLine();
					// 2nd send: username
					sendToServer(user);
				}
			}

			String funcs = "What do you want to do?\nType \"0\" to quit.\n";
			funcs += "1: Browse file name and file size\n";
			funcs += "2: Download a file\n";
			funcs += "3: Download all files from directory\n";
			funcs += "4: Download several files";
			System.out.println(funcs);
			funcNum = scanner.nextInt();

			String fileName, change, newsrc, write;
			// 3rd send: command (some sent in function call)
			// quit
			if (funcNum == 0) {
				command = "0,";
				sendToServer(command);
				System.out.println("Bye bye!");
				break;
			}

			// browse info of input dir
			if (funcNum == 1) {
				System.out.println("Default source: /Users/sunjingxuan/Desktop/project/share");
				System.out.println(
						"Press C to change source directory to a sub folder. Press N to use default source directory.");
				scanner = new Scanner(System.in);
				change = scanner.nextLine();

				// change dir or use default dir
				if (change.equalsIgnoreCase("c")) {
					System.out.println("Please input SUB FOLDER NAME as new source directory: ");
					scanner = new Scanner(System.in);
					//newsrc = "/Users/sunjingxuan/Desktop/project/share/" + scanner.nextLine();
					newsrc = "D:/share/" + scanner.nextLine();
				} else {
					newsrc = defaultsrc;
				}

				// 2.5 send: check exist
				exist = false;
				sendToServer(newsrc);
				exist = inFromServer.readBoolean();
				while (!exist) {
					System.out.println("Input DIRECTORY does not exist!");
					System.out.println("Please input DIRECTORY you want to list files: ");
					scanner = new Scanner(System.in);
					newsrc = scanner.nextLine();
					sendToServer(newsrc);
					exist = inFromServer.readBoolean();
				}

				readInfo(newsrc);
			}

			// download file with the input name
			if (funcNum == 2) {
				System.out.println("Default source: /Users/sunjingxuan/Desktop/project/share");
				System.out.println(
						"Press C to change source directory to a sub folder. Press N to use default source directory.");
				scanner = new Scanner(System.in);
				change = scanner.nextLine();

				// change dir or use default dir
				if (change.equalsIgnoreCase("c")) {
					System.out.println("Please input SUB FOLDER NAME as new source directory: ");
					scanner = new Scanner(System.in);
					//newsrc = "/Users/sunjingxuan/Desktop/project/share/" + scanner.nextLine();
					newsrc = "D:/share/" + scanner.nextLine();

					// (this check should also be sent to server though, but let
					// it be for now)
					while (new File(newsrc).exists()) {
						System.out.println("Input source folder does not exist!");
						System.out.println("Please input new source directory: ");
						scanner = new Scanner(System.in);
						//newsrc = "/Users/sunjingxuan/Desktop/project/share/" + scanner.nextLine();
						newsrc = "D:/share/" + scanner.nextLine();
					}
				} else {
					newsrc = defaultsrc;
				}

				// type in file name within the chosen dir
				System.out.println("Please input FILE NAME you want to download: ");
				scanner = new Scanner(System.in);
				fileName = scanner.nextLine();

				// check file type, this function can only download a file, not
				// a dir
				File abfile = new File(newsrc + "/" + fileName);
				System.out.println(abfile.getAbsolutePath());
				while (abfile.isDirectory()) {
					System.out.println(
							"\nWrong type! Cannot download DIRECTORY!\nPlease input FILE NAME you want to download: ");
					scanner = new Scanner(System.in);
					fileName = scanner.nextLine();
					abfile = new File(newsrc + "/" + fileName);
				}

				// 2.5 send: check exist
				exist = false;
				sendToServer(newsrc + "/" + fileName);
				exist = inFromServer.readBoolean();
				while (!exist) {
					System.out.println("Input file does not exist!");
					System.out.println("Please input FILE NAME you want to download: ");
					scanner = new Scanner(System.in);
					fileName = scanner.nextLine();
					sendToServer(newsrc + "/" + fileName);
					exist = inFromServer.readBoolean();
				}

				// check if file is already downloaded in the destination dir
				if (new File(defaultdest + "/" + fileName).exists()) {
					System.out.println(
							"File already exists in the default download folder!\nDo you want to overwrite it?");
					System.out.println("Press Y to overwrite. Press N to quit.");
					scanner = new Scanner(System.in);
					write = scanner.nextLine();

					// back to input command number
					if (write.equalsIgnoreCase("n")) {
						command = "4,";
						outToServer.writeInt(command.length());
						outToServer.write(command.getBytes());
						continue ohh;
					}
				}

				downloadFile(newsrc, fileName, defaultdest);
			}

			// download the whole dir as configured on the server side with
			// client input dir name
			if (funcNum == 3) {
				// input drc dir (should be absolute path)
				scanner = new Scanner(System.in);
				System.out.println("Please input DIRECTORY you want to download files FROM: ");
				String srcdir = scanner.nextLine();

				// 2.5 send: check exist
				exist = false;
				sendToServer(srcdir);
				exist = inFromServer.readBoolean();
				while (!exist) {
					System.out.println("Input DIRECTORY does not exist!");
					System.out.println("Please input DIRECTORY you want to download files FROM: ");
					scanner = new Scanner(System.in);
					srcdir = scanner.nextLine();
					sendToServer(srcdir);
					exist = inFromServer.readBoolean();
				}

				File src = new File(srcdir);

				// input dest dir (should be absolute path)
				System.out.println("Please input DIRECTORY you want to save files TO: ");
				scanner = new Scanner(System.in);
				String destdir = scanner.nextLine();

				// check if the dest is already downloaded
				if (new File(destdir).exists()) {
					System.out.println("Folder already exists!\nDo you want to overwrite it?");
					System.out.println("Press Y to overwrite. Press N to quit.");
					scanner = new Scanner(System.in);
					write = scanner.nextLine();

					// back to input command number
					if (write.equalsIgnoreCase("n")) {
						command = "4,";
						outToServer.writeInt(command.length());
						outToServer.write(command.getBytes());
						continue ohh;
					}
				}

				File dest = new File(destdir);

				command = "3," + src;
				sendToServer(command);
				downloadDir(src, dest);
			}

			// use multithread to download several input files, can only
			// download files, not dir
			if (funcNum == 4) {
				Scanner scan = new Scanner(System.in);
				System.out.println("Welcome to FCMTD!");
				System.out.println("Please input FILE NAMES you want to download, separate with \",\" : ");
				String input = scan.nextLine();
				String[] fileNames = input.split(",");

				// check type of input files
				int notFile = 0;
				do {
					System.out.println("start check type.");
					for (int i = 0; i < fileNames.length; i++) {
						System.out.println(fileNames[i]);
						System.out.println(new File(defaultsrc + "/" + fileNames[i]).isFile());
						if (new File(defaultsrc + "/" + fileNames[i]).isDirectory())
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
					// create new client for each file
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
	}

	// get file info of certain dir from stream
	void readInfo(String src) throws IOException {
		command = "1," + src;
		sendToServer(command);

		// 2nd get: file info
		size = inFromServer.readInt();
		data = new byte[size];
		do {
			len = inFromServer.read(data, data.length - size, size);
			size -= len;
		} while (size > 0);

		String[] tokens = new String(data).split(",");
		for (int i = 0; i < tokens.length; i++) {
			String[] info = tokens[i].split(" ");
			String fileName = info[0];
			String fileSize = info[1];
			String type = info[2];

			// check file type and print corresponding info
			if (type.equals("file"))
				System.out.println(String.format("File name: \"%s\"\tFile size: \"%s\"", fileName, fileSize));
			if (type.equals("dir"))
				System.out.println(String.format("Directory path: \"%s\"\tDirectory size: \"%s\"", fileName, fileSize));
			System.out.println();
		}
	}

	// get one file from in stream
	void downloadFile(String src, String fileName, String dest) throws IOException {
		command = "2," + src + "," + fileName;
		sendToServer(command);

		// 3rd get: file
		size = inFromServer.readInt();
		data = new byte[size];
		new File(defaultdest).mkdirs();
		DataOutputStream outToFile = new DataOutputStream(new FileOutputStream(dest + "/" + fileName));
		do {
			len = inFromServer.read(data, data.length - size, size);
			size -= len;
			outToFile.write(data);
		} while (size > 0);
		System.out.println("File " + fileName + " downloaded!\n");
	}

	// get all underlying files of a dir from stream
	void downloadDir(File src, File dest) throws IOException {
		// command = "3," + src;
		// sendToServer(command);

		if (!dest.exists())
			dest.mkdir();
		System.out.println("Begin copy from " + src + " to " + dest);

		// get file names of the dir first
		size = inFromServer.readInt();
		data = new byte[size];
		do {
			len = inFromServer.read(data, data.length - size, size);
			size -= len;
		} while (size > 0);

		String[] tokens = new String(data).split(",");
		String[] fileNames = new String[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			String[] info = tokens[i].split(" ");
			fileNames[i] = info[0];
			System.out.println(fileNames[i]);
		}

		// download each file or dir in the given dir
		File srcfile;
		for (String file : fileNames) {
			srcfile = new File(src.getAbsolutePath() + "/" + file);
			if (srcfile.getAbsolutePath().contains(".")) {
				// download directly if is a file
				String srcname = srcfile.getAbsolutePath();
				srcname = new File(srcname).getName();

				size = inFromServer.readInt();
				data = new byte[size];
				new File(defaultdest).mkdirs();
				DataOutputStream outToFile = new DataOutputStream(new FileOutputStream(dest + "/" + file));
				do {
					len = inFromServer.read(data, data.length - size, size);
					size -= len;
					outToFile.write(data);
				} while (size > 0);
				System.out.println("File " + file + " downloaded!\n");

			} else if (!srcfile.getAbsolutePath().contains(".")) {

				// recursively download underlying files and dirs if is a dir
				File destfile = new File(dest.getAbsolutePath() + "/" + file);
				System.out.println("Download sub folder " + dest.getAbsolutePath() + "/" + file);	
				downloadDir(srcfile, destfile);
			}
		}
		System.out.println("Dir downloaded!\n");
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Welcome to CLIENT!");
		new FileClient("158.182.6.86", 8999);
	}
}
