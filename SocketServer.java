import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

public class SocketServer {
    ServerSocket server;
    ServerSocket server2;
    Socket sk;
    InetAddress addr;
    ArrayList<ServerThread> list = new ArrayList<ServerThread>();

    public SocketServer() {
        try {
        	addr = InetAddress.getByName("127.0.0.1");
        	server = new ServerSocket(1234,50,addr);
            System.out.println("\n Waiting for Client connection");
            SocketClient.main(null);
            while(true) {
                sk = server.accept();
                System.out.println(sk.getInetAddress() + " connect");

                //Thread connected clients to ArrayList
                ServerThread st = new ServerThread(this);
                addThread(st);
                st.start();
            }
        } catch(IOException e) {
            System.out.println(e + "-> ServerSocket failed");
        }
    }

    public void addThread(ServerThread st) {
        list.add(st);
    }

    public void removeThread(ServerThread st){
        list.remove(st); //remove
    }

    public void broadCast(String message){
        for(ServerThread st : list){
            st.pw.println(message);
        }
    }

    public static void main(String[] args) {
        new SocketServer();
    }
}

class ServerThread extends Thread {
    SocketServer server;
    PrintWriter pw;
    String name;
    JFileChooser jFileChooser = new JFileChooser();
    DataInputStream dis;
    File file;
    Image image;
    static HashSet<String> userList = new HashSet<>();


    public ServerThread(SocketServer server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // read
            BufferedReader br = new BufferedReader(new InputStreamReader(server.sk.getInputStream()));

            // writing
            pw = new PrintWriter(server.sk.getOutputStream(), true);
            dis = new DataInputStream(server.sk.getInputStream());
            name = br.readLine();
            userList.add(name);
            server.broadCast("Users: " + userList.toString());
            server.broadCast("**["+name+"] Entered**");

            String data;
            while((data = br.readLine()) != null ){
                if(data.startsWith("FILE:")){
                    handleFile();
                } else {
                    server.broadCast("["+name+"] "+ data);

                }
            }
        } catch (Exception e) {
            //Remove the current thread from the ArrayList.
            server.removeThread(this);
            server.broadCast("**["+name+"] Left**");
            System.out.println(server.sk.getInetAddress()+" - ["+name+"] Exit");
            System.out.println(e + "---->");
        }
    }


    private void handleFile() {
        try {
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            File receivedFile = new File("received_" + fileName);

            try (FileOutputStream fos = new FileOutputStream(receivedFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }
                bos.flush();
                System.out.println("File received: " + receivedFile.getAbsolutePath());
                server.broadCast("[" + name + "] sent an image: " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}