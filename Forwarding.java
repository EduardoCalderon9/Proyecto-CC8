import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class Forwarding {
    static FileReader ipfilereader, dvfilereader;
    static File ipfile, dvfile;
    static String ownid = "";
    static BufferedReader ipreader, dvreader;
    static Scanner ipread, dvread;
    static HashMap<String, ClientHandler> clientlist = new HashMap<String, ClientHandler>();
    static HashMap<String, Client> serverlist = new HashMap<String, Client>();
    static HashMap<String, String> map = new HashMap<String, String>();

    public static void main(String[] args) throws IOException {
        ownid = args[0];
        int clientnum = 0;
        String id;

        String clientip;
        int clientport;
        ipfilereader = new FileReader("ForwardingIPs.txt");
        ipfile = new File("ForwardingIPs.txt");
        ipreader = new BufferedReader(ipfilereader);

        ipread = new Scanner(ipfile).useDelimiter(":| ");

        dvfilereader = new FileReader("DV.txt");
        dvfile = new File("DV.txt");
        dvreader = new BufferedReader(dvfilereader);

        dvread = new Scanner(dvfile).useDelimiter(":| ");
        dvread.next();
        while (dvread.hasNext())
            map.put(dvread.next().trim(), dvread.next().trim());

        while (ipreader.readLine() != null)
            clientnum++;

        new Thread() {
            public void run() {
                ServerSocket server = null;
                try {
                    server = new ServerSocket(1981);
                    server.setReuseAddress(true);
                    while (true) {
                        Socket client = server.accept();
                        System.out.print("[93m[93m");

                        System.out.println("Nuevo Cliente " + client.getInetAddress().getHostAddress());
                        System.out.print("[0m[0m");

                        ClientHandler cli = new ClientHandler(client);
                        new Thread(cli).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (server != null) {
                        try {
                            server.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();
        for (int i = 0; i < clientnum; i++) {
            clientip = ipread.next().trim();
            clientport = Integer.valueOf(ipread.next());
            id = ipread.next().trim();

            serverlist.put(id, new Client(clientip, clientport, id));

            new Thread(serverlist.get(id)).start();

        }

    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        String filename;
        String size;
        String packets[] = new String[20000];
        String id;
        boolean filefound = false;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;

        }

        public void run() {
            String line = "";
            String from = "";
            String to = "";
            Path path;
            String data;
            int fragnum = 0, packet_num = 0;
            int fragln;
            try {

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                LinkedList<Integer> packetlist = new LinkedList<Integer>();

                while ((line = in.readLine()) != null) {

                    /*
                     * System.out.println("[35m[35m");
                     * 
                     * System.out.println("read"); System.out.println("[0m[0m");
                     */
                    if (line.contains("requestfile")) {
                        filename = in.readLine();
                        System.out.println("[92m[92m");
                        System.out.println("-----------------------------");

                        System.out.println("requested: ");
                        System.out.println("Name: " + filename);

                        size = in.readLine();
                        System.out.println("Size: " + size);

                        id = in.readLine();
                        System.out.println("To: " + id);
                        System.out.println("------------------------------");
                        System.out.println("[0m[0m");

                        serverlist.get(map.get(id)).requestFile(ownid, filename, size, id);

                    } else if (line.toLowerCase().contains("from")) {
                        System.out.print("[94m[94m");
                        System.out.println("-----------------------------");

                        System.out.println(line);

                        from = line.substring(5).trim();
                        id = from;

                        to = in.readLine();
                        System.out.println(to);
                        to = to.substring(3).trim();

                        line = in.readLine();
                        if (line.toLowerCase().contains("name")) {
                            filename = line.substring(5).trim();
                            line = in.readLine();

                        } else if (line.toLowerCase().contains("msg") && to.equals(ownid)) {
                            System.out.println(line);
                            System.out.println(in.readLine());
                            System.out.println("------------------------------");

                            System.out.println("[0m[0m");
                            continue;
                        }

                        if (line.toLowerCase().contains("size") && to.equals(ownid)) {

                            System.out.println(line);
                            System.out.println(in.readLine());
                            System.out.println("-----------------------------");

                            System.out.print("[0m[0m");
                            File fl = null;
                            size = line;
                            path = Paths.get("Files/" + filename);

                            if (Files.exists(path)) {

                                fl = new File("Files/" + filename);
                                filefound = true;

                            } else {
                                serverlist.get(map.get(id)).err(ownid, id, "Archivo no encontrado");
                                System.out.println("[92m[92m");
                                filefound = false;
                            }

                            if (filefound) {
                                byte[] fileContent = Files.readAllBytes(fl.toPath());

                                StringBuilder sb = new StringBuilder();
                                for (byte b : fileContent) {
                                    sb.append(String.format("%02x", b));
                                }
                                String hexdata = sb.toString();

                                packet_num = (hexdata.length() + 1459) / 1460;

                                int j2 = (hexdata.length() - 1460 * (packet_num - 1));

                                String packets[] = new String[packet_num];
                                int j = 0;

                                for (int i = 0; i < packet_num - 1; i++) {
                                    packets[i] = hexdata.substring(j, j + 1460);
                                    j += 1460;
                                }

                                if (packet_num == 1) {
                                    packets[0] = hexdata;
                                } else {
                                    packets[packet_num - 1] = hexdata.substring((packet_num - 1) * 1460,
                                            (packet_num - 1) * 1460 + j2);
                                }

                                int y = 1;

                                for (int i = 0; i < packets.length; i++, y++) {

                                    System.out.println("Enviado frag " + y);

                                    serverlist.get(map.get(id)).redirData(to, filename, size.substring(5).trim(), id,
                                            packets[i], y);
                                }
                            }

                        } else if (line.toLowerCase().contains("data") && to.equals(ownid)) {
                            // receive file data
                            // FileWriter fileWriter = new FileWriter("nuevo " + filename );
                            System.out.println(line.substring(0, 7) + "...");

                            PrintWriter printWriter = new PrintWriter("nuevo " + filename);
                            data = line.substring(5).trim();
                            line = in.readLine();
                            System.out.println(line);
                            fragnum = Integer.valueOf(line.substring(5).trim());
                            packetlist.add(fragnum);
                            line = in.readLine();
                            System.out.println(line);
                            System.out.println("-----------------------------");

                            System.out.print("[0m[0m");

                            int size = Integer.valueOf(line.substring(5).trim());
                            packet_num = (size + 729) / 730;
                            packets[fragnum - 1] = data;
                            String s = "";
                            String downloaded_file = "";
                            FileOutputStream stream = new FileOutputStream("Downloaded_Files/" + filename);
                            System.out.print("[92m[92m");

                            System.out.println("Recibido Paquete " + fragnum + " de " + from);
                            System.out.print("[0m[0m");

                            StringBuilder builder = new StringBuilder();
                            if (packetlist.size() == packet_num) {

                                for (int i = 0; i < packet_num; i++) {
                                    System.out.print("[32m[32m");

                                    System.out.println("escrito paquete " + i + " de " + from);
                                    System.out.print("[0m[0m");

                                    /*
                                     * builder.setLength(0); System.out.println(packets[i]); for (int j = 0; j <
                                     * packets[i].length(); j += 2) { s = packets[i].substring(j, j + 2); int n =
                                     * Integer.valueOf(s, 16); builder.append((char) n); }
                                     */

                                    downloaded_file += packets[i];

                                }
                                byte[] filebytes = new byte[downloaded_file.length() / 2];
                                for (int i = 0; i < downloaded_file.length() - 1; i += 2) {
                                    filebytes[i / 2] = (byte) ((Character.digit(downloaded_file.charAt(i), 16) << 4)
                                            + Character.digit(downloaded_file.charAt(i + 1), 16));
                                }
                                packetlist = new LinkedList<Integer>();

                                stream.write(filebytes, 0, size);
                                stream.close();
                                printWriter.close();

                            }
                            in.readLine();

                        } else if (!to.equals(ownid)) {
                            // redirect to
                            if (line.toLowerCase().contains("size")) {
                                size = line.substring(5).trim();

                                System.out.println(line);

                                System.out.println(in.readLine());
                                System.out.println("-----------------------------");

                                System.out.print("[0m[0m");

                                serverlist.get(map.get(to)).requestFile(from, filename, size, to);

                            } else if (line.toLowerCase().contains("data")) {

                                System.out.println(line.substring(0, 7) + "...");

                                String fg = in.readLine();

                                System.out.println(fg);

                                fragln = Integer.valueOf(fg.replaceAll("[^0-9]", ""));
                                size = in.readLine();
                                System.out.println(size);
                                size = size.substring(5).trim();
                                System.out.println(in.readLine());

                                System.out.println("-----------------------------");

                                System.out.print("[0m[0m");
                                serverlist.get(map.get(to)).redirData(from, filename, size, to, line.substring(5),
                                        fragln);

                            } else if (line.toLowerCase().contains("msg")) {
                                System.out.println(in.readLine());

                                System.out.println("-----------------------------");

                                System.out.print("[0m[0m");
                                serverlist.get(map.get(to)).err(from, id, line.substring(4));

                            }
                        }
                    }
                    line = "";

                }
            } catch (SocketException e) {

                System.out.print("[91m[91m");

                if (id != null) {
                    System.out.println("Se desconecto " + id);
                } else {
                    System.out.println("Se desconecto un vecino");

                }

                System.out.print("[0m[0m");

            }

            catch (Exception e) {

                e.printStackTrace();
            }

        }
    }

    private static class Client implements Runnable {

        private int port;
        private String ip;
        private String id;
        PrintWriter out;
        BufferedReader in;
        Socket socket;

        public Client(String ip, int port, String id) {
            this.port = port;
            this.ip = ip;
            this.id = id;
        }

        public void run() {

            String line = "";
            while (true) {
                try {
                    socket = new Socket(ip, port);
                    out = new PrintWriter(socket.getOutputStream(), true);

                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    System.out.print("[91m[91m");
                    System.out.println("No se pudo conectar con " + id);
                    System.out.print("[0m[0m");

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }

        }

        /*
         * public void print() { System.out.println(serverlist.get(id));
         * 
         * System.out.println("print: " + id); System.out.println(out);
         * 
         * }
         */

        public void requestFile(String from, String filename, String size, String id) {
            System.out.println("Enviando Peticion a " + id);
            out.println("From: " + from);
            out.println("To: " + id);
            out.println("Name: " + filename);
            out.println("Size: " + size);
            out.println("EOF");
            System.out.println("From: " + from);
            System.out.println("To: " + id);
            System.out.println("Name: " + filename);
            System.out.println("Size: " + size);
            System.out.println("EOF");
        }

        public void err(String from, String id, String err) {
            System.out.println("Enviando mensaje de error a " + this.id);

            out.println("From: " + from);
            out.println("To: " + id);
            out.println("Msg: " + err);
            out.println("EOF");
        }

        public void redirData(String from, String filename, String size, String id, String data, int frag) {
            System.out.println("Enviando datos a " + this.id);

            out.println("From: " + from);
            out.println("To: " + id);
            out.println("Name: " + filename);
            out.println("Data: " + data);
            out.println("Frag: " + frag);
            out.println("Size: " + size);
            out.println("EOF");
        }
    }

}
