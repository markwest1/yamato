import java.io.*;
import java.net.*;
import java.util.*;

// Class purpose: server for GameCom
public class GameComServer implements Runnable
{
    ArrayList<PrintWriter> clientOutputStreams;

    // GameCom client handler
    public class GameComClientHandler implements Runnable
    {
        BufferedReader reader;
        Socket sock;

        public GameComClientHandler(Socket clientSock)
        {
            try
            {
                sock = clientSock;
                InputStreamReader isReader =
                    new InputStreamReader(sock.getInputStream());

                reader = new BufferedReader(isReader);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }

        public void run()
        {
            String message;

            try
            {
                while ((message = reader.readLine()) != null)
                {
                    System.out.println("read: \"" + message + "\"");
                    tellEveryone(message);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
   
    }

    public void run()
    {
        clientOutputStreams = new ArrayList<PrintWriter>();

        try
        {
            ServerSocket serverSock =
                new ServerSocket(GameComConstants.SERVER_PORT);

            while (true)
            {
                Socket clientSocket = serverSock.accept();

                PrintWriter writer =
                    new PrintWriter(clientSocket.getOutputStream());
                clientOutputStreams.add(writer);

                Thread t = new Thread(new GameComClientHandler(clientSocket));
                t.start();
                System.out.println("GameComServer.run(): got a connection");
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void tellEveryone(String message)
    {
        Iterator it = clientOutputStreams.iterator();

        // DEBUG
        //System.out.println("In GameComServer.tellEveryone()...");

        while (it.hasNext())
        {
            try
            {
                PrintWriter writer = (PrintWriter) it.next();
                writer.println(message);
                writer.flush();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
}
