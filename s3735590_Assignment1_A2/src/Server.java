import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
    // class private variables
    private Socket connection;
    private static Queue<ClientHandler> players = new LinkedList<>();
    private MulticastSocket ds = null;
    private DatagramPacket DpReceive = null;
    private DataInputStream input = null;
    private DataOutputStream output = null;

    // getter for queue
    public static Queue<ClientHandler> getPlayers() {
        return players;
    }

    /**
     * Constructor for the server In this server create a server socket at port
     * 61197 and host its self on netprog1.csit.rmit.edu.au. Firstly add at max 6
     * client in a queue then start the game for them untill all player exit from
     * the game.
     * 
     * @throws ClassNotFoundException
     */
    public Server() throws ClassNotFoundException {
        System.out.println("Server running");
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(61197); // Binds to the server port
            serverSocket.setSoTimeout(20000);

            addPlayers(serverSocket);// adding new players to queue

        } catch (SocketTimeoutException e) {
            System.out.println("Time for accepting new players is now over.");
        } catch (EOFException e) {
            e.printStackTrace();
        } catch (SocketException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
        }

        // print the total number of play currently playing the game
        System.out.println("There are total " + Server.players.size() + " players for playing the guessing game.");

        timer.schedule(keepAliveTask, 30000, 30000); // timer is now schedule for keep sending keep alive message
        lobbyQueue(); // calling lobby Queue function to start and handle the queue.
        timer.cancel(); // cancelling the timer task

        // Server is exiting with a message
        System.out.println("Game Over! Thanks for playing. Now game server is going to close.");
        try {
            Thread.sleep(1000);
            if (this.input != null) {
                this.input.close();
            }
            if (this.output != null) {
                this.output.close();
            }
            if (ds != null)
                ds.close();
            if (connection != null)
                connection.close();
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * This function is used for adding new client in the queue of clienthandler so
     * that they could play the game later.
     * 
     * @param serverSocket need to store the connection formed with the client
     * @throws IOException
     */
    public void addPlayers(ServerSocket serverSocket) throws IOException {
        int count = 0; // to count number of client
        String name = ""; // to store name of client
        while (count < 6) { // run until 6 clients added
            this.connection = serverSocket.accept(); // Create a connection between server and client

            // DataInputStream and DataOutputStream for sending and receiving request and
            // response
            this.input = new DataInputStream(this.connection.getInputStream());
            this.output = new DataOutputStream(this.connection.getOutputStream());

            name = this.input.readUTF(); // storing the client name
            count++; // incrementing the count
            name = "Player " + count + ": " + name;
            System.out.println(name + " has connected to the game.");
            this.output.writeUTF(name); // sending the modified name set by server to client

            // adding client in the queue
            Server.players.add(new ClientHandler(this.connection, name));

            this.output.writeUTF("Please wait until the game start " + name); // sending the client a message of wait
            this.output.flush(); // flushing the ObjectOutputStream object
        }
    }

    /**
     * This function will send a udp multicast message to all the client
     * 
     * @param group    a inetaddress of all the client common address
     * @param response a message to send to all the clients connected to the given
     *                 client-server common inetaddress.
     */
    public void sendMulticastMessageToClient(InetAddress group, String response) {
        try {
            // Creating the socket object for server for carrying the data.
            ds = new MulticastSocket();
            byte[] sendBuf = response.getBytes();
            // Creating a DatgramPacket to send the data.
            DpReceive = new DatagramPacket(sendBuf, sendBuf.length, group, 61642);
            sendBuf = new byte[1024];
            // invoke the send call to send the message to all clients.
            ds.send(DpReceive);
        } catch (UnknownHostException e) {
        } catch (IOException e1) {
        }

    }

    /**
     * This function will generate result of a game's round in a string
     * 
     * @param arraylist playing clients arraylist helps to set the players and ther
     *                  guess count into string
     * @return returns the result string from lowest number of guess count to
     *         highest.
     */
    public String resultOfRound(ArrayList<ClientHandler> arraylist) {
        String result = "Following are the result for this round:\n";
        Collections.sort(arraylist, ClientHandler.clientGuess);
        for (int j = 0; j < arraylist.size(); j++) {
            if (arraylist.get(j).getForfeit() == true) {
                result = result + arraylist.get(j).getName() + " has forfeited the game \n";
            } else {
                result = result + arraylist.get(j).getName() + " and  Number guesses: "
                        + arraylist.get(j).getGuessCount() + " \n";
            }
        }
        String emptyPlayerString = "";
        ClientHandler.writeGameLog(emptyPlayerString, result);
        return result;
    }

    // creating timer object for sending keep alive message
    Timer timer = new Timer();
    TimerTask keepAliveTask = new TimerTask() { // timer task object

        @Override
        public void run() {
            MulticastSocket ms = null;
            try {
                ms = new MulticastSocket(); // creating a object of multicastsocket to send a message
                byte[] keepAliveMessage = "\nPlease, enter a value".getBytes(); // keep alive message

                // Datagram packet with keep alive message, all client-server common inetaddress
                // and
                // port
                DatagramPacket datapack = new DatagramPacket(keepAliveMessage, keepAliveMessage.length,
                        InetAddress.getByName("224.1.1.1"), 61642);

                // sending datagram packet to all the client.
                ms.send(datapack);
            } catch (IOException e) {
                System.err.println("Error!");
            } finally {
                if (ms != null) {
                    ms.close();
                }
            }

        }
    };

    /**
     * This function is created for maintaining a game lobby where playing lobby
     * arraylist contains players who are going to play and waiting lobby contain
     * player who are waiting for their turn Once all players completes the game
     * they will announced the result and then allow other waiting players to enter
     * playing lobby.
     */
    public void lobbyQueue() {
        Random rand = new Random(); // creating random object
        while (Server.players.peek() != null) {// while player queue is not empty
            int randomGuess = rand.nextInt(13); // generating random number from 0 to 12 using random object
            int i = 0;
            String startString = ""; // to store starting game announcement message
            ArrayList<ClientHandler> playingLobby = new ArrayList<>(); // playing lobby

            try {
                InetAddress group = InetAddress.getByName("224.1.1.1"); // common client-server inetaddress

                while (i++ < 3 && Server.players.peek() != null) { // while 1 or 3 Server.players in a player queue
                    ClientHandler player = Server.players.poll(); // retrieve the clienthander thread at head position
                                                                  // of queue
                    playingLobby.add(player); // adding into playing lobby
                    startString = startString + "Game starts now " + player.getName() + "\n";
                }

                ArrayList<ClientHandler> waitingLobby = new ArrayList<>(); // arraylist for waiting lobby for players
                for (ClientHandler player : Server.players) {
                    waitingLobby.add(player); // adding remaining player from queue to waiting lobby.
                    startString = startString + player.getName() + " are in waiting queue\n";
                }

                sendMulticastMessageToClient(group, startString); // calling multicast udp message for sending to all
                                                                  // clients

                // Starting game for players in playing lobby
                for (ClientHandler gamer : playingLobby) {
                    gamer.setGuess(randomGuess); // setting the random number generated by server for client to guess
                    gamer.setGuessCount(0); // setting guess count to 0
                    gamer.setActive(true); // setting the game session active variable to true
                    gamer.start(); // starting the clienthandler thread to start the game
                    System.out.println("\nGame started for " + gamer.getName());
                }

                // wait untill all the player of current round is over.
                for (ClientHandler gamer : playingLobby) {
                    gamer.join();
                }

                String result = resultOfRound(playingLobby); // storing the result
                sendMulticastMessageToClient(group, result); // sending the result

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        new Server();
    }
}
