import java.io.*;
import java.net.*;
import java.util.*;

public class GameComClient
{
    Socket sock;
    BufferedReader reader;
    PrintWriter writer;
    Battleship gameInstance;

    public GameComClient(String ip, Battleship game)
    {
        gameInstance = game;
        setUpNetworking(ip);
        Thread readerThread = new Thread(new IncomingReader());
        readerThread.start();
    }

    private void setUpNetworking(String ipAddr)
    {
        try
        {
            sock = new Socket(ipAddr, GameComConstants.SERVER_PORT);         
            InputStreamReader streamReader =
                new InputStreamReader(sock.getInputStream());
            reader = new BufferedReader(streamReader);
            writer = new PrintWriter(sock.getOutputStream());
            System.out.println("GameComClient.setUpNetworking(): " +
                               "networking established");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void sendMessage(String msg)
    {
        try
        {
            writer.println(msg);
            writer.flush();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public class IncomingReader implements Runnable
    {
        public void run()
        {
            String message;

            try
            {
                while ((message = reader.readLine()) != null)
                {
                    gameInstance.messageFromServer(message);
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
}
