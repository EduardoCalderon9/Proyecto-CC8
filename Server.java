import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class Server {
    static BufferedReader reader, ipreader;
    static Scanner read, ipread;
    static int[][] initial = new int[30][30];
    static int[][] cost_matrix = new int[30][30];
    static int[] distance_vector = new int[30];
    static String[] dv = new String[30];
    static int lines = 0;
    static String letter_id;
    static HashMap<Integer, String> idmap = new HashMap<Integer, String>();
    static int nodeid = 0;
    static int run = 0;
    static boolean changed = true;
    static PrintWriter serverwriter, clientwriter;
    static LinkedList<String> nodelist = new LinkedList<String>();
    static FileReader file, ipfilereader;
    static File filename, ipfile;
    static int num_id;

    public static void main(String[] args) throws IOException {
        String clientip;
        int clientport;
        int clientnum = 0;
        String clientid;
        String str = "";
        letter_id = args[0];
        int timeout = Integer.parseInt(args[1]);
        int notif_freq = Integer.parseInt(args[2]);

        ipfilereader = new FileReader("ips.txt");
        ipfile = new File("ips.txt");
        ipreader = new BufferedReader(ipfilereader);
        ipread = new Scanner(ipfile).useDelimiter(":| ");
        while (ipreader.readLine() != null)
            clientnum++;

        file = new FileReader("matrix.txt");
        filename = new File("matrix.txt");
        Scanner s = new Scanner(str).useDelimiter(":| ");
        reader = new BufferedReader(file);
        read = new Scanner(filename);
        Scanner mapscanner = new Scanner(filename);

        while (reader.readLine() != null)
            lines++;
        nodelist.add(letter_id);
        String t;

        for (int i = 0; i < lines; i++) {
            String idn = mapscanner.nextLine();
            idmap.put(nodeid++, idn.substring(0, 1));
        }
        num_id = getKeyByValue(idmap, letter_id);

        mapscanner.close();
        for (int i = 0; i < lines; i++) {
            read.next();

            for (int j = 0; j < lines; j++) {
                t = read.next();
                String tnum = t.replaceAll("[^0-9]", "");
                if (tnum.matches("[0-9.]+")) {
                    initial[i][j] = Integer.parseInt(tnum);
                    cost_matrix[i][j] = Integer.parseInt(tnum);

                    if (i == num_id) {
                        nodelist.add(t.substring(0, 1));
                        distance_vector[j] = j;

                    }
                }

            }

        }

        getDistanceVector();
        new Thread() {
            public void run() {
                ServerSocket server = null;

                try {
                    server = new ServerSocket(9080);
                    server.setReuseAddress(true);
                    while (true) {
                        Socket client = server.accept();
                        System.out.print("[93m[93m");

                        System.out.println("Nuevo Cliente " + client.getInetAddress().getHostAddress());
                        System.out.print("[0m[0m");
                        client.setSoTimeout(timeout * 1000);
                        Client clientSock = new Client(client);
                        new Thread(clientSock).start();
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
            clientid = ipread.next().trim();

            Clientside clientside = new Clientside(clientip, clientport, notif_freq, clientid);
            new Thread(clientside).start();
        }
        s.close();
    }

    static void getDistanceVector() throws IOException {

        for (int i = 0; i < lines; i++) {
            for (int j = 0; j < lines; j++) {
                for (int k = 0; k < lines; k++) {
                    // .out.println(" costo original : " + cost_matrix[i][k]);
                    // System.out.println(" costo nuevo : " + (initial[i][j] + cost_matrix[j][k]));
                    if (cost_matrix[i][k] > initial[i][j] + cost_matrix[j][k]) {
                        System.out.println("CAMBIO EN DV \n");

                        // System.out.println(cost_matrix[i][k]);
                        // System.out.println(initial[i][j] + cost_matrix[j][k]);
                        changed = true;
                        cost_matrix[i][k] = cost_matrix[i][j] + cost_matrix[j][k];
                        if (i == num_id) {
                            distance_vector[k] = j;
                        }

                    }
                }

            }
        }
        // System.out.print(idmap);

        FileWriter fileWriter = new FileWriter("DV.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        FileWriter distWriter = new FileWriter("Distance.txt");
        PrintWriter distW = new PrintWriter(distWriter);
        printWriter.print(idmap.get(num_id) + " ");
        distW.print(idmap.get(num_id) + " ");
        for (int i = 0; i < lines; i++) {
            System.out.print("[92m[92m");

            System.out.println("distancia de " + idmap.get(num_id) + " a " + idmap.get(i) + " : "
                    + cost_matrix[num_id][i] + " por " + idmap.get(distance_vector[i]));
            System.out.print("[0m[0m");

            if (i != num_id) {

                dv[i] = idmap.get(i) + ":" + cost_matrix[num_id][i];
                distW.print(dv[i] + " ");

                printWriter.print(idmap.get(i) + ":" + idmap.get(distance_vector[i]) + " ");
            }
        }
        System.out.println();

        distW.close();
        printWriter.close();

    }

    private static class Clientside implements Runnable {

        private int port;
        private String ip;
        private String id;
        ScheduledExecutorService executor;
        private int freq;

        public Clientside(String ip, int port, int freq, String id) {
            this.port = port;
            this.ip = ip;
            this.freq = freq;
            this.id = id;
        }

        public void run() {
            /*
             * try { reader = new BufferedReader(file); read = new Scanner(filename);
             * 
             * int lin = 0; while (reader.readLine() != null) lin++;
             * 
             * for (int i = 0; i < lin; i++) { String idn = read.next(); for (int j = 0; j <
             * lin; j++) { String t = read.next(); String tnum = t.replaceAll("[^0-9]", "");
             * if (tnum.matches("[0-9.]+")) { initial[i][j] = Integer.parseInt(tnum);
             * cost_matrix[i][j] = Integer.parseInt(tnum); if (i == num_id) {
             * nodelist.add(t.substring(0, 1)); distance_vector[j] = j;
             * 
             * } } if (letter_id.equals(idn)) { nodelist.add(t.substring(0, 1)); } }
             * 
             * } } catch (Exception e) { e.printStackTrace(); }
             */
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try (Socket socket = new Socket(ip, port)) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String line = null;
                    out.println("From:" + letter_id);
                    out.println("Type:HELLO");
                    System.out.print("[92m[92m");

                    System.out.println("HELLO a " + id + "\n");
                    System.out.print("[0m[0m");

                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /*
                     * out.println("From:" + letter_id); out.println("Type:DV"); out.println("Len:"
                     * + (lines - 1)); System.out.println("From:" + letter_id);
                     * System.out.println("Type:DV"); System.out.println("Len:" + (lines - 1));
                     * 
                     * for (int i = 0; i < lines; i++) { if (i != num_id) { out.println(dv[i]);
                     * System.out.print(num_id); System.out.println(dv[i]);
                     * 
                     * } }
                     */

                    Runnable helloRunnable = new Runnable() {
                        public void run() {
                            if (changed) {
                                out.println("From:" + letter_id);
                                out.println("Type:DV");
                                out.println("Len:" + (lines - 1));
                                System.out.println("[92m[92m");
                                System.out.println("------------------------------");

                                System.out.println("From:" + letter_id);
                                System.out.println("Type:DV");
                                System.out.println("Len:" + (lines - 1));

                                System.out.print("[0m[0m");
                                for (int i = 0; i < lines; i++) {
                                    if (i != num_id) {

                                        out.println(dv[i]);
                                        System.out.print("[92m[92m");
                                        System.out.println(dv[i]);
                                        System.out.print("[0m[0m");

                                    }
                                }
                                System.out.print("[92m[92m");

                                System.out.println("------------------------------");
                                System.out.print("[0m[0m");
                                System.out.print("[92m[92m");

                                System.out.println("DV a " + id + " \n");
                                System.out.print("[0m[0m");

                                changed = false;
                            } else {
                                out.println("From:" + letter_id);
                                out.println("Type:KeepAlive");
                                System.out.print("[92m[92m");

                                System.out.println("KeepAlive a " + id);
                                System.out.print("[0m[0m");

                            }
                        }
                    };

                    executor = Executors.newScheduledThreadPool(1);
                    executor.scheduleAtFixedRate(helloRunnable, 0, freq, TimeUnit.SECONDS);
                    while (true) {
                        if ((line = in.readLine()) != null) {
                            System.out.print("[94m[94m");

                            System.out.println(line);
                            System.out.print("[92m[92m");

                        }
                    }
                } catch (Exception e) {
                    if (executor != null) {
                        executor.shutdown();
                    }
                    System.out.print("[91m[91m");

                    System.out.println("Se desconecto " + id + " \n");
                    System.out.print("[0m[0m");

                    cost_matrix[num_id][getKeyByValue(idmap, id)] = 99;
                    initial[num_id][getKeyByValue(idmap, id)] = 99;
                    cost_matrix[getKeyByValue(idmap, id)][num_id] = 99;
                    initial[getKeyByValue(idmap, id)][num_id] = 99;
                    try {
                        getDistanceVector();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    private static class Client implements Runnable {
        private final Socket clientSocket;

        public Client(Socket socket) {
            this.clientSocket = socket;

        }

        public void run() {
            PrintWriter out = null;
            BufferedReader in = null;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String id = "";
                String line;
                String len = "";
                String node = "";
                while (true) {
                    try {
                        if ((line = in.readLine()) != null) {
                            System.out.print("[94m[94m");

                            System.out.println(line);
                            System.out.print("[0m[0m");

                            if (line.toLowerCase().contains("from")) {
                                id = line.substring(5).trim();
                            }
                            if (line.contains("HELLO")) {
                            System.out.print("[94m[94m");

                                System.out.println("WELCOME a " + id + "\n");
                                System.out.print("[0m[0m");

                                out.println("From:" + letter_id);
                                out.println("Type:WELCOME");
                            }

                            if (line.contains("DV")) {

                                len = in.readLine();
                                System.out.print("[94m[94m");

                                System.out.println(len);
                                System.out.print("[0m[0m");
                                len = len.substring(4);
                                for (int i = 0; i < Integer.valueOf(len.trim()); i++) {
                                    node = in.readLine();
                                    System.out.print("[94m[94m");

                                    System.out.println(node);
                                    System.out.print("[0m[0m");
                                    if (!nodelist.contains(node.substring(0, 1))) {
                                        nodelist.add(node.substring(0, 1));
                                        idmap.put(nodeid++, node.substring(0, 1));
                                        distance_vector[lines] = getKeyByValue(idmap, id);
                                        for (int j = 0; j < lines + 1; j++) {

                                            cost_matrix[j][j] = 0;
                                            initial[j][j] = 0;
                                            if (idmap.get(j).equals(id)) {
                                                cost_matrix[getKeyByValue(idmap, node.substring(0, 1))][j] = Integer
                                                        .valueOf(node.replaceAll("[^0-9]", "").trim());
                                                initial[getKeyByValue(idmap, node.substring(0, 1))][j] = Integer
                                                        .valueOf(node.replaceAll("[^0-9]", "").trim());
                                                cost_matrix[j][getKeyByValue(idmap, node.substring(0, 1))] = Integer
                                                        .valueOf(node.replaceAll("[^0-9]", "").trim());
                                                initial[j][getKeyByValue(idmap, node.substring(0, 1))] = Integer
                                                        .valueOf(node.replaceAll("[^0-9]", "").trim());
                                            } else {
                                                cost_matrix[getKeyByValue(idmap, node.substring(0, 1))][j] = 99;
                                                initial[getKeyByValue(idmap, node.substring(0, 1))][j] = 99;
                                                cost_matrix[j][getKeyByValue(idmap, node.substring(0, 1))] = 99;
                                                initial[j][getKeyByValue(idmap, node.substring(0, 1))] = 99;
                                            }

                                        }
                                        lines++;
                                    }
                                    cost_matrix[getKeyByValue(idmap, id)][getKeyByValue(idmap,
                                            (node.substring(0, 1)))] = Integer
                                                    .valueOf(node.replaceAll("[^0-9]", "").trim());

                                }
                                getDistanceVector();

                            }

                        }
                    } catch (SocketTimeoutException ex) {
                        System.out.print("[91m[91m");

                        System.out.println("TIMEOUT de " + id);
                        System.out.print("[0m[0m");

                        cost_matrix[num_id][getKeyByValue(idmap, id)] = 99;
                        initial[num_id][getKeyByValue(idmap, id)] = 99;
                        cost_matrix[getKeyByValue(idmap, id)][num_id] = 99;
                        initial[getKeyByValue(idmap, id)][num_id] = 99;
                        getDistanceVector();
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } catch (SocketException ex) {
                        System.out.print("[91m[91m");

                        System.out.println("No se pudo conectar a " + id);
                        System.out.print("[0m[0m");

                        cost_matrix[num_id][getKeyByValue(idmap, id)] = 99;
                        initial[num_id][getKeyByValue(idmap, id)] = 99;
                        cost_matrix[getKeyByValue(idmap, id)][num_id] = 99;
                        initial[getKeyByValue(idmap, id)][num_id] = 99;
                        getDistanceVector();
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();

            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static <T, E> T getKeyByValue(Map<T, E> idmap, E value) {
        for (Entry<T, E> entry : idmap.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

}
