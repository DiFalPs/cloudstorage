package nio;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class NioTelnetServer {
	private final ByteBuffer buffer = ByteBuffer.allocate(512);

	public static final String LS_COMMAND = "\tls          view all files from current directory\n\r";
	public static final String MKDIR_COMMAND = "\tmkdir       create directory\n\r";
	public static final String TOUCH_COMMAND = "\ttouch       create file\n\r";
	public static final String CD_COMMAND = "\tcd          change path\n\r";
	public static final String RM_COMMAND = "\trm          remove file or directory\n\r";
	public static final String COPY_COMMAND = "\tcopy        copy file\n\r";
	public static final String CAT_COMMAND = "\tcat         read from file\n\r";
	public static String consoleName = "D:\\Java_projects\\cloudstorage(nio)\\server";   //это для отображния пути (на этом основная завязка для методов сделана)
	public static String pathToDirServer = "D:\\Java_projects\\cloudstorage(nio)\\server";  //поменять на корневую папку (в случае необходимости)

	public NioTelnetServer() throws IOException {
		ServerSocketChannel server = ServerSocketChannel.open(); // открыли
		server.bind(new InetSocketAddress(1234));
		server.configureBlocking(false); // ВАЖНО
		Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);
		System.out.println("Server started");
		while (server.isOpen()) {
			selector.select();
			var selectionKeys = selector.selectedKeys();
			var iterator = selectionKeys.iterator();
			while (iterator.hasNext()) {
				var key = iterator.next();
				if (key.isAcceptable()) {
					handleAccept(key, selector);
				} else if (key.isReadable()) {
					handleRead(key, selector);
				}
				iterator.remove();
			}
		}
	}

	private void handleRead(SelectionKey key, Selector selector) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		int readBytes = channel.read(buffer);
		if (readBytes < 0) {
			channel.close();
			return;
		} else if (readBytes == 0) {
			return;
		}

		buffer.flip();
		StringBuilder sb = new StringBuilder();
		while (buffer.hasRemaining()) {
			sb.append((char) buffer.get());
		}
		buffer.clear();

		// TODO: 05.03.2021
		// touch (имя файла) - создание файла             :complete
		// mkdir (имя директории) - создание директории   :complete
		// cd (path) - перемещение по дереву папок        :complete
		// rm (имя файла или папки) - удаление объекта    :complete
		// copy (src, target) - копирование файла         :complete
		// cat (имя файла) - вывод в консоль содержимого  :complete

		if (key.isValid()) {
			String command = sb.toString()
					.replace("\n", "")
					.replace("\r", "");
			if ("--help".equals(command)) {
				sendMessage(LS_COMMAND, selector);
				sendMessage(TOUCH_COMMAND, selector);
				sendMessage(MKDIR_COMMAND, selector);
				sendMessage(CD_COMMAND, selector);
				sendMessage(RM_COMMAND, selector);
				sendMessage(COPY_COMMAND, selector);
				sendMessage(CAT_COMMAND, selector);
			} else if ("ls".equals(command)) {
				sendMessage(getFilesList(consoleName).concat("\n\r"), selector);
			} else if ("exit".equals(command)) {
				System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
				channel.close();
				return;
			} else if (command.startsWith("touch ")) {
				String fileName = command.substring(6);
				sendMessage(touch(fileName), selector);
			} else if (command.startsWith("mkdir ")) {
				String dirName = command.substring(6);
				sendMessage(mkdir(dirName), selector);
			}else if (command.startsWith("cd ")) {           //доп условие, для вызова метода addToCD
				String path = command.substring(3);
				if (command.startsWith("cd server")) {
					sendMessage(addToCD(path), selector);
				} else {
					sendMessage(cd(path), selector);
				}
			} else if (command.startsWith("rm ")) {
				String rmName = command.substring(3);
				sendMessage(removeFromServer(rmName), selector);
			} else if (command.startsWith("cat ")) {
				String fileName = command.substring(4);
				sendMessage(cat(fileName), selector);
			} else if (command.startsWith("copy ")) {
				String fileName = command.substring(5);
				sendMessage(copy(fileName), selector);
			}
		}
		if (checkConsoleName(consoleName) == true) {
			sendName(channel, "");
		} else sendName(channel, consoleName);        //если была команда cd, то будет путь вместо ip


	}

	private boolean checkConsoleName(String consoleName) {
		if(consoleName == pathToDirServer) {
			return true;
		} else return false;
	}

	private String checkFileDoesntExists (String fileOrDirectoryName) {
		if (consoleName.equals(pathToDirServer) && !Files.exists(Paths.get(pathToDirServer, fileOrDirectoryName))) {
			String rtn = "true1";
			return rtn;
		} else if (!Files.exists(Paths.get(consoleName, fileOrDirectoryName))) {
			String rtn = "true2";
			return rtn;
		}  else return "Wrong name\"" + fileOrDirectoryName + "\" , try again\n\r";
	}


	private String checkFileExist (String fileOrDirectoryName) {
		if (consoleName.equals(pathToDirServer) && Files.exists(Paths.get(pathToDirServer, fileOrDirectoryName))) {
			String rtn = "true1";
			return rtn;
		} else if (Files.exists(Paths.get(consoleName, fileOrDirectoryName))) {
			String rtn = "true2";
			return rtn;
		} else return "File or directory with name \"" + fileOrDirectoryName + "\" not found\n\r";
	}

	/** Метод copy принимает одной строке имя копируемого файла и путь
	 * Дальше идёт split, чтобы разделить имя файла и путь
	 * на 133 строке проверка пути, если ничего нет, то это корневая папка
	 * если есть, то передаём путь
	 */

	private String copy(String fileName) throws IOException {
		String fileSourceName = fileName.split(" ")[0];
		String pathTargetName = fileName.split(" ")[1];
		Path pathTarget = Path.of(pathTargetName, fileSourceName);
		if (Files.exists(Paths.get(pathToDirServer, fileSourceName))) {
			Path pathSource = Path.of("server", fileSourceName);
			Files.copy(pathSource,pathTarget, StandardCopyOption.REPLACE_EXISTING);
			String rtn = "File " + fileSourceName + " was copy to path [" + pathTargetName + "]\n\r";
			return rtn;
		} else if (checkFileExist(pathTargetName).equals("true2")){
			Path pathSource = Path.of(consoleName, fileSourceName);
			Files.copy(pathSource,pathTarget, StandardCopyOption.REPLACE_EXISTING);
			String rtn = "File " + fileSourceName + " was copy to path [" + pathTargetName + "]\n\r";
			return rtn;
		} else return "sda";
	}


	/** Метод fileReader не вызывается пользователем на прямую
	 * Это просто обычная читалка, которая возвращает тело файла
	 * Сам же метод нужен для команды cat
	 */


	private String fileReader (String path, String file) throws IOException {
		RandomAccessFile raf = new RandomAccessFile(path + File.separator + file,  "r");
		FileChannel fileChannel = raf.getChannel();
		int bytesRead = fileChannel.read(buffer);
		String str = "";
		while (bytesRead != -1) {
			buffer.flip();
			while (buffer.hasRemaining()) {
				str = StandardCharsets.UTF_8.decode(buffer).toString().replace("\n", "\n\r");
			}
			buffer.clear();
			bytesRead = fileChannel.read(buffer);
		}
		return str;
	}

	/** Метод cat проверяет путь и существует ли файл
	 * Вызывает fileReader, передаёт в него название файла и путь до него
	 * Возвращает в консоль тело файла
	 */

	private String cat(String fileName) throws IOException {
		if (consoleName == "" && Files.exists(Paths.get("server", fileName))) {              // 1
			return fileReader("server", fileName);
		} else if (Files.exists(Paths.get(consoleName, fileName))) {
			return fileReader(consoleName, fileName).concat("\n\r");
		} else if (!Files.exists(Paths.get(consoleName, fileName)) || !Files.exists(Paths.get("server", fileName))) {
			return "File not exists\n\r";
		} else return "Shkiper we got a problem!!!\n\r";
	}

	/** Метод removeFromServer получает имя файла
	 * Проверяет путь и подставляет имя
	 *
	 */

	private String removeFromServer(String rmName) throws IOException {
		if (checkFileExist(rmName) == "true1") {
			Path path = Path.of("server", rmName);
			Files.delete(path);
			String rtn = "Remove completed\n\r";
			return rtn;
		} else if (checkFileExist(rmName) == "true2") {
			Path path = Path.of(consoleName, rmName);
			Files.delete(path);
			String rtn = "Remove completed\n\r";
			return rtn;
		} else return checkFileExist(rmName);
	}

	/** Метод mkdir получает имя директории
	 * Проверяет путь и подставляет имя
	 */

	private String mkdir(String dirName) throws IOException {
		if (checkFileDoesntExists(dirName) == "true1") {
			Path path = Path.of(pathToDirServer, dirName);
			Files.createDirectories(path);
			String rtn = "Directory was created in directory - [server]\n\r";
			return rtn;
		} else if (checkFileDoesntExists(dirName) == "true2") {
			Path path = Path.of(consoleName, dirName);
			Files.createDirectories(path);
			String rtn = "Directory was created in - [" + consoleName + "]\n\r" ;
			return rtn;
		} else if (checkFileExist(dirName) == "true1") {
			return "Directory exist\n\r";
		} else if (checkFileExist(dirName) == "true2") {
			return "Directory exist\n\r";
		} else return checkFileDoesntExists(dirName);
	}

	/** Метод touch получает имя файла
	 * Проверяет путь и подставляет имя
	 */

	private String touch(String fileName) throws IOException {
		if (consoleName == "" && !Files.exists(Path.of("server" + File.separator + fileName))) { //4
			Path path = Path.of("server", fileName);
			Files.createFile(path);
			String rtn = "File was created in directory - [server]\n\r";
			return rtn;
		} else if (!Files.exists(Path.of(consoleName, fileName))){
			Path path = Path.of(consoleName, fileName);
			Files.createFile(path);
			String rtn = "File was created in directory - [" + consoleName + "]\n\r" ;
			return rtn;
		} else return "File exists\n\r";
	}

	/** old version: Метод addToCD получает имя пути
	 * Это вспомогательный метод для cd
	 * Если человек находится в директории server\dir
	 * А ему нужно попасть в server\dir\files
	 * То ему не нужно будет полностью прописывать путь
	 * А просто вызывать cd files и он попадёт в server\dir\files
	 *
	 * upd: Теперь метод регулируется при вызове команды,
	 * Метод полностью вариативный и не нужно прописывать абсолютный путь
	 * По сути он нужен если будет вызвано "cd server" или "cd server\path\and etc."
	 * Если же сначала вызовут "cd server", а потом вызовут "cd dir_name",
	 * То "cd dir_name" уже обработается в методе CD и "dir_name" будет добавлено к "server"
	 * В итоге мы увидим "server/dir"
	 */

	private String addToCD(String path) {
		if (Files.exists(Paths.get(path)) & Files.isDirectory(Paths.get(path))) {
			consoleName = path;
			String rtn = "Directory was changed\n\r";
			return rtn;
		} else if (!Files.isDirectory(Paths.get(path))) {
			String normalized = path.replace("/", File.separator);
			String[] array = normalized.split((File.separator + File.separator));
			ArrayUtils.reverse(array);
			path = array[0];
			path.replace(", ", "")
					.replace("[", "")
					.replace("]", "");
			String rtn = "\"" + path + "\" isn't a directory\n\r";
			return rtn;
		} else return "Path [" + path + "] is not exists\n\r";
	}


	/** old version: Метод cd получает имя пути
	 * Он просо задаёт consoleName
	 * Тем самым пользователь вмето своего ip
	 * Начинает видеть название пути,
	 * В котором он находится
	 * Собственно все команды начинают работать
	 * По заданному пути
	 * Если вызывать cd server\dir
	 * А после прописать ls/mkdir and etc.,
	 * То пользователь увидит все файлы в server\dir
	 *
	 * upd: Метод умеет работать как с абсолютным путём,
	 * Так и с относительным
	 */

	private String cd(String path) {
		String doublePoint = "..";
		String firstLatter = path.split(":")[0];
		if (path.startsWith(firstLatter + ":" + File.separator) & Files.exists((Paths.get(path))) ) {
			consoleName = path;
			String rtn = "Directory was changed\n\r";
			return rtn;
		} else if (path.startsWith(firstLatter + ":" + "/") & Files.exists((Paths.get(path))) )  {
			consoleName = path.replace("/", File.separator);
			String rtn = "Directory was changed\n\r";
			return rtn;
		} else if (path.equals(doublePoint))  {
			return cdBackOnOneStep();
		} else if (!Files.isDirectory(Paths.get(consoleName + File.separator + path)))  {
			String normalized = path.replace("/", File.separator);
			String[] array = normalized.split((File.separator + File.separator));
			ArrayUtils.reverse(array);
			path = array[0];
			path.replace(", ", "")
					.replace("[", "")
					.replace("]", "");
			String rtn = "\"" + path + "\" isn't a directory\n\r";
			return rtn;
		}  else if (!(path.startsWith(firstLatter + ":" + File.separator)) && Files.exists(Paths.get(consoleName + File.separator + path)))  {
			consoleName += File.separator + path;
			String rtn = "Directory was changed\n\r";
			return rtn;
		} else {
			String rtn = "Path [" + path + "] is not exists\n\r";
			return rtn;
		}
	}

	private String cdBackOnOneStep() {
		if (!(consoleName.equals(pathToDirServer)) & !(consoleName.equals("server"))) {
			String newConsoleName = "";
			String previousPath = consoleName;
			String[] array = previousPath.split("\\\\");
			for (int i = 0; i < array.length - 1; i++) {
				if (i == (array.length - 2)) {
					array = ArrayUtils.remove(array, i + 1);
				}
			}
			newConsoleName = Arrays.toString(array)
					.replace(", ", File.separator)
					.replace("[", "")
					.replace("]", "");
			consoleName = newConsoleName;
			String rtn = "Directory was changed\n\r";
			return rtn;
		} else return "You can't step back here cus u in root directory\n\r";
	}

	/** Метод sendName совсем не много изменился
	 * Если пользователь вызвал cd, то вместо ip
	 * Будет показываться путь в котором он находится
	 */

	private void sendName(SocketChannel channel, String consoleName) throws IOException {
		if (consoleName == "") {
			consoleName = channel.getRemoteAddress().toString();
		}
		channel.write(
				ByteBuffer.wrap(consoleName
						.concat(">: ")
						.getBytes(StandardCharsets.UTF_8)
				)
		);
	}

	/** Метод getFilesList тоже чуть-чуть поменял
	 * Собственно тут проверка пути
	 * Если не был вызван cd, то покажет все файлы папки "Server"
	 */

	private String getFilesList(String path) {
		if (path == "server" || path.equals(pathToDirServer)) {
			return String.join("\t", new File(pathToDirServer).list());
		} else return String.join("\t", new File(path).list());
	}

	private void sendMessage(String message, Selector selector) throws IOException {
		for (SelectionKey key : selector.keys()) {
			if (key.isValid() && key.channel() instanceof SocketChannel) {
				((SocketChannel) key.channel())
						.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
			}
		}
	}

	private void handleAccept(SelectionKey key, Selector selector) throws IOException {
		SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
		channel.configureBlocking(false);
		System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
		channel.register(selector, SelectionKey.OP_READ, "some attach");
		channel.write(ByteBuffer.wrap("Hello user!\n\r".getBytes(StandardCharsets.UTF_8)));
		channel.write(ByteBuffer.wrap("Enter --help for support info\n\r".getBytes(StandardCharsets.UTF_8)));
		sendName(channel, "");
	}

	public static void main(String[] args) throws IOException {
		new NioTelnetServer();
	}
}
