import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
    private static final String HOST = "netprog1.csit.rmit.edu.au"; // Server address
    private static final int PORT = 61197;// server port number
    private Socket connection;
    private String name = "player"; // default game name
    private MulticastSocket socket = null;
    private DatagramPacket packet = null;
    private Boolean active = false; // session active variable

    // getter
    public MulticastSocket getSocket() {
        return this.socket;
    }

    public boolean getActive() {
        return this.active;
    }

    public MulticastSocket setSocket(MulticastSocket socket) {
        return this.socket = socket;
    }

    // constructor for client
    public Client() throws ClassNotFoundException {
        int request = 0;// creating a int variable for request.
        Boolean win = false;
        String response = ""; // to store response string
        try {
            connection = new Socket(HOST, PORT); // Creates a socket connection
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); // for user input

            // DataInputStream and DataOutputStream for sending and receiving request and
            // response
            DataInputStream input = new DataInputStream(connection.getInputStream());
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());

            register(input, output); // calling user registeration method

            response = input.readUTF();
            System.out.println(response);

            this.socket = new MulticastSocket(61642); // creating a multicastsocket for receiving message from server
            this.setSocket(socket); // calling setter function for setting multicastsocket value.
            InetAddress group = InetAddress.getByName("224.1.1.1"); // group inetaddress for every client to join and
                                                                    // listen.
            this.socket.joinGroup(group); // joining the multicastsocket to the group inetaddress

            String check = receiveMulticastMessageToClient(this.socket); // storing udp multicast message on string
                                                                         // variable
            System.out.println(check); // print the message on clientside.
            timer.schedule(keepAliveTask, 30000, 30000); // timer for keep alive functionality

            while (true) {
                this.active = input.readBoolean(); // storing the session value changed from server.
                // if session is true then game starts for client.
                if (this.active) {
                    connection.setSoTimeout(90000); // 90 second socket timeout
                    request = guessInput(reader); // storing client guess input by calling method guessInput.
                    output.writeInt(request); // sending request data to server.
                    output.flush(); // flush
                    win = input.readBoolean(); // reading object send by server.
                    response = input.readUTF();
                    System.out.println(response);
                    if (response.equals("\nYou have forfeited the game")) {
                        break; // if client forfeits the game then break the loop to close the game for client
                    }
                    boolean flag = exitCondition(win, reader, output, input); // storing the boolean flag value
                                                                              // by calling exitCondition
                                                                              // function
                    if (!flag) {
                        break; // if flag false then break the loop and exit the game.
                    }
                }
            }
            timer.cancel(); // closing timer.
            reader.close();
            input.close();
            output.close();
        } catch (SocketTimeoutException e) {
            System.out.println("Server Timeout!");
        } catch (EOFException e) {
        } catch (SocketException e) {
            System.out.println("Server is crush or closed!");
        } catch (IOException e) {
            System.out.println("Error stream.");
        } finally {
            try {
                System.out.println("Thanks for playing!");
                if (socket != null)
                    socket.close();
                if (connection != null)
                    connection.close();
            } catch (IOException e) {
                System.out.println("Error, with a stream");
            }
        }
    }

    // creating timer object for receiving keep alive message 
    Timer timer = new Timer();
    TimerTask keepAliveTask = new TimerTask() { // timer task object

        @Override
        public void run() {
            MulticastSocket ms = null;
            try {
                ms = getSocket();
                byte[] keepAliveMessage = new byte[1024];
                // initializing a datagram packet
                DatagramPacket datapack = new DatagramPacket(keepAliveMessage, keepAliveMessage.length);
                ms.receive(datapack); // receiving the message from the server for client.
                String received = new String(datapack.getData(), 0, datapack.getLength()); // converting data packet
                                                                                           // message to string
                boolean flag = getActive();
                // if flag is true and message is keep alive message then print it.
                if (flag && received.contains("\nPlease, enter a value")) {
                    System.out.println(received);
                }
            } catch (IOException e) {
                System.err.println("Server might be closed or crashed!");
                System.exit(0);
            }
        }
    };

    /**
     * This function is used for registering the detail of client to server
     * 
     * @param input  for reading server response
     * @param output for sending client name.
     */
    public void register(DataInputStream input, DataOutputStream output) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter your name: ");
        try {
            String name = reader.readLine();
            if (name.equals("")) { // if name is blank the default name is give that is player.
                output.writeUTF(this.name);
                this.name = input.readUTF();
            } else {
                output.writeUTF(name);
                this.name = input.readUTF();
            }
            System.out.println("Your name given by server is: " + this.name); // printing server given name
        } catch (IOException e) {
            System.out.println("Error, with a stream");
        }
    }

    /**
     * This function is used for receiving all the multicast message from the server
     * to clients
     * 
     * @param socket // for receiving multicast message for the clients
     * @return
     */
    public String receiveMulticastMessageToClient(MulticastSocket socket) {
        String received = "";
        try {
            byte[] sendBuf = new byte[256];
            // create a DatgramPacket to receive the data.
            this.packet = new DatagramPacket(sendBuf, sendBuf.length);
            socket.receive(packet);
            sendBuf = new byte[1024];
            received = new String(this.packet.getData(), 0, this.packet.getLength());
        } catch (IOException e) {
            System.out.println("Error, with a stream");
        }
        return received;
    }

    /**
     * This function make sure that the client should only enter a number between 0
     * to 12 or e for forfeit the game.
     * 
     * @param reader Bufferedreader is used for user input
     * @return
     */
    public int guessInput(BufferedReader reader) {
        boolean inputFlag = true;
        int request = 0;
        while (inputFlag) {
            try {
                String regex = "\\d+";
                System.out.print("Please enter a number between 0 to 12 or 'e' for forfeit:");
                String guess;
                guess = reader.readLine();
                if (guess.equalsIgnoreCase("e")) {
                    request = -1;
                    inputFlag = false;
                } else if (guess.matches(regex)) {
                    request = Integer.parseInt(guess);
                    if (request >= 0 && request < 13) {
                        inputFlag = false;
                    } else {
                        System.out.print("Number should be between 0 to 12.");
                    }
                } else {
                    System.out.print("Please! Enter a number only.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return request;
    }

    /**
     * This function is used for asking a client whether the client wants to play
     * more or want to quit.
     * 
     * @param win    a boolean value for checking whether the client is either win
     *               the game or couldn't find an answer in 4 guess.
     * @param reader for user input for either play again press p or quit then press
     *               q
     * @param output for writing client response to server
     * @param input  for reading the server response to client response
     * @return a boolean value for either continue playing or quit playing
     */
    public Boolean exitCondition(Boolean win, BufferedReader reader, DataOutputStream output, DataInputStream input) {
        if (win) {
            try {
                String response = "";
                response = input.readUTF();
                System.out.println(response);
                Boolean inputFlag = true;
                while (inputFlag) {
                    response = reader.readLine();
                    if (response.equalsIgnoreCase("q") || response.equalsIgnoreCase("p")) {
                        inputFlag = false;
                    } else {
                        System.out.println("Please enter only p or q");
                    }
                }
                // if Client wins the game then end the connection.
                if (response.equalsIgnoreCase("q")) {
                    output.writeUTF(response); // sending request data to server.
                    output.flush();
                    while (true) {
                        String check = receiveMulticastMessageToClient(this.getSocket()); // receiving result of the
                                                                                          // round
                        if ((check.contains(this.name + " and  Number guesses: "))) { // for print only results that
                                                                                      // contains current client
                            System.out.println(check); // print the result sent from server
                            break;
                        }
                    }
                    return false; // to close the game
                } else {
                    output.writeUTF(response); // sending request data to server.
                    output.flush();
                    while (true) {
                        String check = receiveMulticastMessageToClient(this.getSocket()); // receiving result of the
                                                                                          // round
                        if ((check.contains(this.name + " and  Number guesses: "))) { // for print only results that
                                                                                      // contains current client
                            System.out.println(check); // print the result sent from server
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true; // to continue the game
    }

    public static void main(String[] args) throws ClassNotFoundException, InterruptedException {
        new Client();
    }
}
