package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;


public class Client {
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;

	public Client() throws IOException {
		socket = new Socket("localhost", 1345);
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		runClient();
	}

	private void runClient() throws IOException {
		JFrame frame = new JFrame("Cloud Storage");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 300);

		JPanel panel = new JPanel();


		JTextArea ta = new JTextArea();
		// TODO: 02.03.2021
		// list file - JList
		DefaultListModel listModel = new DefaultListModel();
		updateListModel(listModel);
		JList listFile = new JList(listModel);



		JButton uploadButton = new JButton("Upload");
		JButton downloadButton = new JButton("Download"); // my
		JButton removeButton = new JButton("Remove"); //     code

		panel.add(uploadButton);
		panel.add(downloadButton);
		panel.add(removeButton);

		frame.getContentPane().add(BorderLayout.NORTH, ta);
		frame.getContentPane().add(BorderLayout.SOUTH, panel);
		frame.getContentPane().add(BorderLayout.CENTER, listFile);

		frame.setVisible(true);

		uploadButton.addActionListener(a -> {
			System.out.println(sendFile(ta.getText()));
			updateListModel(listModel);
		});

		downloadButton.addActionListener(b -> {
			System.out.println(downloadFile(ta.getText()));
		});

		removeButton.addActionListener(c -> {
			System.out.println(removeFile(ta.getText()));
			updateListModel(listModel);

		});

//		listFile.addListSelectionListener(d -> {
//			Object object = listFile.getSelectedValue();
//			ta.setText(object.toString());
//		});





	}

	private String sendFile(String filename) {
		try {
			File file = new File("client" + File.separator + filename);
			if (file.exists()) {
				out.writeUTF("upload");
				out.writeUTF(filename);
				long length = file.length();
				out.writeLong(length);
				FileInputStream fis = new FileInputStream(file);
				int read = 0;
				byte[] buffer = new byte[256];
				while ((read = fis.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				out.flush();
				String status = in.readUTF();
				return status;
			} else {
				return "File is not exists";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Something error";
	}

	private String downloadFile(String filename) {
		try {
			File file = new File("server" + File.separator + filename);
			if (file.exists()) {
				out.writeUTF("download");
				out.writeUTF(filename);
				long length = file.length();
				out.writeLong(length);
				FileInputStream fis = new FileInputStream(file);
				int read = 0;
				byte[] buffer = new byte[256];
				while ((read = fis.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				out.flush();
				String status = in.readUTF();
				return status;
			} else {
				return "File is not exists";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Something error";
	}

	private String removeFile(String filename) {
		try {
			File file = new File("server" + File.separator + filename);
			if (file.exists()) {
				out.writeUTF("remove");
				out.writeUTF(filename);
				out.flush();
				String status = in.readUTF();
				return status;
			} else {
				return "File is not exists";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Something error";
	}

	private DefaultListModel updateListModel(DefaultListModel model) {
		model.clear();
		File folderCortes = new File("server");
		File[] fileNames = folderCortes.listFiles();
		for (int i=0 ; i< fileNames.length ; i++){
			model.addElement(fileNames[i].getName());
		}
		return model;
	}






	public static void main(String[] args) throws IOException {
		new Client();
	}
}
