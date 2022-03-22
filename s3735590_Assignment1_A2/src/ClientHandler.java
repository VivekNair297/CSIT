import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

public class ClientHandler extends Thread {
    // class private variables
    private final static int MAX_GUESSES = 4; // Max number of guess a client can give
    private Socket connection;
    private DataInputStream input;
    private DataOutputStream output;
    private int guess;
    private Boolean active = false;
    private int guessCount = 0;
    private Boolean forfeit = false;

    /**
     * Constructor for the ClientHandler
     * 
     * @param connection stores the client connection
     * @param name       client name to save as thread name
     * @throws IOException if any IO exception occur it throws the exception
     */
    public ClientHandler(Socket connection, String name) throws IOException {
        this.setName(name);
        this.connection = connection;
        this.input = new DataInputStream(this.connection.getInputStream());
        this.output = new DataOutputStream(this.connection.getOutputStream());
    }

    /**
     * Overiding the thread's run method This method will call the game function to
     * start the game for client.
     */
    @Override
    public void run() {
        game(); // calling game method
    }

    /**
     * This method is used for starting and handling the game of server it respond
     * the client request and send responds. And if the client want to play again it
     * allow the server to add the client to players queue.
     */
    public void game() {
        try {
            int guessedNumber; // creating a integer for client storing client guess number

            Boolean win = false;
            String response = "";

            while (guessCount != 4) {
                this.output.writeBoolean(this.active); // changing the session value of client by sending active value
                this.connection.setSoTimeout(90000); // 90 seconds timeout
                int request = this.input.readInt(); // reading the request from client

                // storing client guessed number
                guessedNumber = request;
                guessCount++;

                // Writing Gaming log
                String activity = "Guess :" + String.valueOf(guessedNumber);
                String playerName = this.getName();
                writeGameLog(playerName, activity);

                // Writing Communication Log
                InetAddress address = this.connection.getInetAddress();
                String hostIP = address.getHostAddress();
                writeCommunicationLog(hostIP, activity);

                // game logic
                if (guessedNumber == -1) { // for forfeit
                    response = "\nYou have forfeited the game";
                    this.forfeit = true;
                    this.output.writeBoolean(this.forfeit); // sending forfeit variable's value to client
                    this.output.writeUTF(response); // sending the forfeit message to client
                    this.output.flush();
                    break;
                } else if (this.guess == guessedNumber) {
                    // if client guessed right number
                    response = "\nHurray! You won. Congratulation!";
                    win = true;
                    this.output.writeBoolean(win);// sending win variable's value to client
                    this.output.writeUTF(response); // sending congrats to client for winning
                    this.output.flush();
                    break;
                } else if (MAX_GUESSES == guessCount) {
                    // if client din't guessed right number in 4 turns.
                    response = "\nGuessed number by server is " + this.guess;
                    win = true;
                    this.output.writeBoolean(win);// sending win variable's value to client
                    this.output.writeUTF(response);// sending server guessed number to client
                    this.output.flush();
                    break;
                } else if (guessedNumber < this.guess) {
                    // if client din't guessed right number and guessed number is less than guess
                    // number then server send clue.
                    response = "\nYour guessed number " + guessedNumber + " is smaller than the generated number";
                    this.output.writeBoolean(win);// sending win variable's value to client
                    this.output.writeUTF(response); // sending clue to client
                } else if (guessedNumber > this.guess) {
                    // if client din't guessed right number and guessed number is greater than guess
                    // number then server send clue.
                    response = "\nYour guessed number " + guessedNumber + " is bigger than the generated number";
                    this.output.writeBoolean(win);// sending win variable's value to client
                    this.output.writeUTF(response); // sending clue to client
                }
                this.output.flush(); // flushing the ObjectOutputStream object

            }
            // Condition for client to play again or not
            if (guessCount == 4 || win == true) {
                response = "Want's play again(p) or quit(q)?";
                this.output.writeUTF(response);
                String condition = this.input.readUTF();
                if (condition.equalsIgnoreCase("p")) { // if play again
                    Server.getPlayers().add(new ClientHandler(this.connection, this.getName())); // add the current
                                                                                                 // client connection as
                                                                                                 // new clientHandler
                                                                                                 // thread to the server
                                                                                                 // game queue
                    this.output.writeBoolean(false); // changing the session value of client to stop the game until the
                                                     // next turn the client game starts.
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Server Timeout!");
        } catch (SocketException e) {
            System.out.println("Client is crush or closed!");
        } catch (IOException e) {
        }
    }

    /**
     * This function will store all the logs of the client's game
     * 
     * @param playerName contains the players name
     * @param activity   and its activities(here guess values)
     */
    public static void writeGameLog(String playerName, String activity) {
        File file = new File("gamingLog.log"); // creating or opening gaminglog file
        FileOutputStream foutstr = null; // to store info of log, FileOutputStream is used
        try {
            if (!file.exists()) { // if file not exit
                file.createNewFile(); // create new file
            }
            foutstr = new FileOutputStream(file, true); // FileOutputStream object with file and true as parameters
            Date date = Calendar.getInstance().getTime(); // date
            DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss"); // time
            String currentTime = dateFormat.format(date); // converting into string format
            // storing date, time and player name as string
            String finalLogString;
            if (playerName.equals("")) {
                finalLogString = "\nTime :" + currentTime + " | Activity: Ranking\n" + activity;
            }else if(activity.contains("-1")){
                finalLogString = "\nTime :" + currentTime + " | " + playerName + " | Activity: Forfeited";
            }
            else {
                finalLogString = "\nTime :" + currentTime + " | " + playerName + " | Activity: " + activity;
            }

            byte[] contentInBytes = finalLogString.getBytes(); // converting the string to bytes array
            foutstr.write(contentInBytes); // writing the byte array in the file
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function will store all the communication log in a communicationlog
     * file. It contains all the communication logs between the server and client
     * 
     * @param clientAddress it contains the client address
     * @param activity      all client activity in string
     */
    public void writeCommunicationLog(String clientAddress, String activity) {
        File file = new File("communicationLog.log"); // creating or opening communicationLog file
        FileOutputStream foutstr = null; // to store info of log, FileOutputStream is used
        try {
            if (!file.exists()) { // if file not exit
                file.createNewFile(); // create new file
            }
            foutstr = new FileOutputStream(file, true); // FileOutputStream object with file and true as parameters
            Date date = Calendar.getInstance().getTime(); // date
            DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss"); // time
            String currentTime = dateFormat.format(date); // converting into string format
            // storing date, time and remote address as string
            String finalLogString = "\nTime :" + currentTime + " | PlayerIP: " + clientAddress + " | Activity: "
                    + activity;
            byte[] contentInBytes = finalLogString.getBytes(); // converting the string to bytes array
            foutstr.write(contentInBytes);// writing the byte array in the file
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Comparator<ClientHandler> clientGuess = new Comparator<ClientHandler>() {
        @Override
        public int compare(ClientHandler o1, ClientHandler o2) {
            int guessCount1 = o1.getGuessCount();
            int guessCount2 = o2.getGuessCount();
            return guessCount1 - guessCount2;
        }
    };

    // getters and setters
    public Socket getConnection() {
        return this.connection;
    }

    public Boolean getForfeit() {
        return this.forfeit;
    }

    public Boolean setForfeit(Boolean forfeit) {
        return this.forfeit = forfeit;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Boolean setActive(Boolean active) {
        return this.active = active;
    }

    public int setGuess(int guess) {
        return this.guess = guess;
    }

    public int getGuessCount() {
        return this.guessCount;
    }

    public int setGuessCount(int guessCount) {
        return this.guessCount = guessCount;
    }

    public DataInputStream getInput() {
        return this.input;
    }

    public DataOutputStream getOutput() {
        return this.output;
    }

}