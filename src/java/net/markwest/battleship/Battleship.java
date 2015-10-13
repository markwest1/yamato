import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.imageio.*;

public class Battleship
{
    // Integer defines
    static final int numShips = Fleet.NUMSHIPS;
    static final int numCols  = Fleet.NUMCOLS;
    static final int numRows  = Fleet.NUMROWS;

    // String defines
    final String startServerMenuItemString = "Start Server";
    final String startClientMenuItemString = "Start Client";
    final String resetBoardMenuItemString  = "Reset Board";
    final String startGameMenuItemString   = "Start Game";
    final String showChatBoxMenuItemString = "Show Chat Box";

    // Networking
    InetAddress myIp;
    InetAddress opIp;
    GameComClient myGameClient;

    // Network states
    final int NS_INIT = 0;
    final int NS_SERVER = 1;
    final int NS_CLIENT = 2;

    // Game states
    final int GS_INVALID                       = -1;
    final int GS_PLACE_SHIPS                   = 0;
    final int GS_AWAITING_INVITATION           = 1;
    final int GS_INVITATION_SENT               = 2;
    final int GS_GAME_IN_PROGRESS_MY_TURN      = 3;
    final int GS_GAME_IN_PROGRESS_NOT_MY_TURN  = 4;
    final int GS_GAME_OVER                     = 5;

    // State variables
    int networkState = NS_INIT;
    int gameState = GS_INVALID;

    // Protocol type definitions
    final String TYPE_REQ           = "REQ";
    final String TYPE_RSP           = "RSP";
    final String TYPE_SHOT          = "SHOT";
    final String TYPE_SHOT_ACK      = "HIT";
    final String TYPE_SHOT_ACK_SUNK = "SUNK";
    final String TYPE_SHOT_ACK_DEST = "FLEET_DESTROYED";
    final String TYPE_SHOT_NACK     = "MISS";
    final String TYPE_CHAT          = "CHAT";

    // Opponent identifier
    String opponent = "";

    // Highest-level GUI componentes
    JFrame    theFrame;
    JPanel    theOutermostPane;
    JPanel    opPaneOut;
    JPanel    opPaneIn;
    JPanel    myPaneOut;
    JPanel    myPaneIn;
    StatusBar statusBar;

    // Chat window GUI components
    // TODO:
    // 1. Change JTextArea to JTextPane to allow colored text --
    //    my messages colored blue, opponent messages red
    // 2. If the <RETURN> key is pressed in the chatWindowTextField,
    //    automagically press the "SEND" button
    // 3. Add a score display to the chatWindow to show how many games
    //    each player has won/lost.
    JDialog     chatWindow;
    JTextArea   chatWindowTextArea;
    JTextField  chatWindowTextField;

    String myName = null;
    String opName;
    String clientIpAddr = null;
    TitledBorder myShipsTitledBorder;
    TitledBorder myWaterTitledBorder;
    TitledBorder opShipsTitledBorder;
    TitledBorder opWaterTitledBorder;

    // Menu-related components
    JMenuBar theMenuBar;
    JMenu networkMenu;
    JMenu gameMenu;
    JMenu helpMenu;
    JMenu chatMenu;
    JMenuItem startServerMenuItem;
    JMenuItem startClientMenuItem;
    JMenuItem resetBoardMenuItem;
    JMenuItem startGameMenuItem;
    JCheckBoxMenuItem showChatBoxMenuItem;
    MenuListener menuListener;

    // The glassPanel
    ImageMovePanel glassPane;

    // Water components
    JPanel owBorderPane;
    JPanel mwBorderPane;
    JPanel owGridPane;
    JPanel mwGridPane;
    JLabel owEmpty     = new JLabel(" ");
    JLabel mwEmpty     = new JLabel(" ");
    JLabel owClabels[] = new JLabel[numCols];
    JLabel mwClabels[] = new JLabel[numCols];
    JLabel owRlabels[] = new JLabel[numRows];
    JLabel mwRlabels[] = new JLabel[numRows];
    JPanel owCpanels[] = new JPanel[numCols];
    JPanel mwCpanels[] = new JPanel[numCols];
    JPanel owRpanels[] = new JPanel[numRows];
    JPanel mwRpanels[] = new JPanel[numRows];

    // Column labels
    String cindex[] = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};

    // Row labels
    String rindex[] = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    // Ship components
    JPanel osBorderPane;
    JPanel msBorderPane;
    JPanel osBoxPane;
    JPanel msBoxPane;
    String imageFilenames[] = {
       "images/patrol_boat.jpg",
       "images/submarine.jpg",
       "images/destroyer.jpg",
       "images/battleship.jpg",
       "images/carrier.jpg"
    };
 
    String sunkImageFilenames[] = {
        "images/sunk_patrol_boat.jpg",
        "images/sunk_submarine.jpg",
        "images/sunk_destroyer.jpg",
        "images/sunk_battleship.jpg",
        "images/sunk_carrier.jpg"
    };

    // Ship labels
    String shipNames[] = {"Patrol Boat", "Submarine", "Destroyer", "Battleship",
                           "Aircraft Carrier",};

    // Heading labels, must be in same order as BattleshipPanel constants
    String headingNames[] = {"East", "South", "West", "North"};

    // Ship buttons
    BufferedImage[] shipImages = new BufferedImage[numShips];
    BufferedImage[] sunkImages = new BufferedImage[numShips];
    ImageIcon[] shipIcons = new ImageIcon[numShips];
    ImageIcon[] sunkIcons = new ImageIcon[numShips];
    JToggleButton[] myShipButtons = new JToggleButton[numShips];
    JToggleButton[] opShipButtons = new JToggleButton[numShips];
    ButtonGroup shipButtonGroup = new ButtonGroup();

    // BattleshipPanel arrays
    BattleshipPanel[][] myBsPanels = new BattleshipPanel[numRows][numCols];
    BattleshipPanel[][] opBsPanels = new BattleshipPanel[numRows][numCols];

    // Fleet
    Fleet myFleet;
    Fleet opFleet;

    // Ship placing and dragging info shared between mouse and key listeners
    boolean shipBeingDragged[] = {false, false, false, false, false};
    boolean shipPlaced[] = {false, false, false, false, false};
    int rotAngle[] = {-90, -90, -90, -90, -90};
    Point p;
    
    // Column and row indexes
    int c, r;

    public static void main (String[] args)
    {
        // Set the look and feel of the GUI
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            //UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Build the GUI
        new Battleship().buildGui();
    }

    // GUI main
    private void buildGui()
    {
        // Set the game state
        setGameState(GS_PLACE_SHIPS);

        // Set the look and feel
        JFrame.setDefaultLookAndFeelDecorated(false);

        // Instantiate the listeners
        ShipPlacementMouseListener mouseListener = new ShipPlacementMouseListener();
        ShipPlacementKeyListener keyListener = new ShipPlacementKeyListener();
        GamePlayMouseListener gpMouseListener = new GamePlayMouseListener();
        
        // Instantiate the Fleets
        myFleet = new Fleet();
        opFleet = new Fleet();

        // Setup the view GUI components
        for (c = 0; c < numCols; c++)
        {
            // Create column label
            owClabels[c] = new JLabel(cindex[c]);
            mwClabels[c] = new JLabel(cindex[c]);

            // Create column panel
            owCpanels[c] = new JPanel();
            mwCpanels[c] = new JPanel();

            // Add the new label to its panel
            owCpanels[c].add(owClabels[c]);
            mwCpanels[c].add(mwClabels[c]);

            // Loop through each row
            for (r = 0; r < numRows; r++)
            {
                // Create row label
                owRlabels[r] = new JLabel(rindex[r]);
                mwRlabels[r] = new JLabel(rindex[r]);

                // Create row panel
                owRpanels[r] = new JPanel();
                mwRpanels[r] = new JPanel();

                // Add the new label to its panel
                owRpanels[r].add(owRlabels[r]);
                mwRpanels[r].add(mwRlabels[r]);
            }
        }

        // Create the top-level border panels and add a border
        owBorderPane = new JPanel();
        mwBorderPane = new JPanel();

        // Create the grid panels and add to the top-level border panels
        owGridPane = new JPanel(new GridLayout(11,11));
        owBorderPane.add(owGridPane);
        mwGridPane = new JPanel(new GridLayout(11,11));
        mwBorderPane.add(mwGridPane);
        
        // Add the empty labels to the top left-most grid position
        owGridPane.add(owEmpty);
        mwGridPane.add(mwEmpty);

        // Add the column labels to the grid
        for (c = 0; c < numCols; c++)
        {
            owGridPane.add(owCpanels[c]);
            mwGridPane.add(mwCpanels[c]);
        }

        // Add the rest of the labels and buttons to the grid
        for (r = 0; r < numRows; r++)
        {
            owGridPane.add(owRpanels[r]);
            mwGridPane.add(mwRpanels[r]);

            for(c = 0; c < numCols; c++)
            {
                // My water panel creation, configuration and placement
                myBsPanels[r][c] = new BattleshipPanel(r, c, myFleet);

                // Add mouse listeners to each BattleshipPanel
                myBsPanels[r][c].addMouseListener(mouseListener);
                myBsPanels[r][c].addMouseMotionListener(mouseListener);
                //mbp.addMouseWheelListener(mouseListener);

                mwGridPane.add(myBsPanels[r][c]);

                // Opponent water panel creation, configuration and placement
                opBsPanels[r][c] = new BattleshipPanel(r, c, opFleet);

                // Add mouse listeners to each BattleshipPanel
                opBsPanels[r][c].addMouseListener(gpMouseListener);
                opBsPanels[r][c].addMouseMotionListener(gpMouseListener);
                //obp.addMouseWheelListener(mouseListener);

                owGridPane.add(opBsPanels[r][c]);
            }
        }

        // Add borders to each of the water panels
        opWaterTitledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Oponent Water", TitledBorder.LEFT, TitledBorder.TOP);
        owBorderPane.setBorder(opWaterTitledBorder);

        myWaterTitledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "My Water", TitledBorder.LEFT, TitledBorder.TOP);
        mwBorderPane.setBorder(myWaterTitledBorder);

        // Setup ship components
        osBoxPane = new JPanel();
        osBoxPane.setLayout(new BoxLayout(osBoxPane, BoxLayout.Y_AXIS));
        msBoxPane = new JPanel();
        msBoxPane.setLayout(new BoxLayout(msBoxPane, BoxLayout.Y_AXIS));
        osBorderPane = new JPanel();
        msBorderPane = new JPanel();

        // Instantiate the ship images
        for (int shipIndex = 0; shipIndex < numShips; ++shipIndex)
        {
            // Try to instantiate the buffered images
            try
            {
                URL imgURL = getClass().getResource(imageFilenames[shipIndex]);
                shipImages[shipIndex] = ImageIO.read(imgURL);

                imgURL = getClass().getResource(sunkImageFilenames[shipIndex]);
                sunkImages[shipIndex] = ImageIO.read(imgURL);
            }
            catch (IOException e)
            {
                System.out.println(imageFilenames[shipIndex] +
                                   "file could not be read");
                e.printStackTrace();
            }


            // Instantiate the button icons
            shipIcons[shipIndex] = new ImageIcon(shipImages[shipIndex]);
            sunkIcons[shipIndex] = new ImageIcon(sunkImages[shipIndex]);

            // Instantiate opponent ship buttons and remove all the borders
            opShipButtons[shipIndex] =
                new JToggleButton(shipIcons[shipIndex], false);
            opShipButtons[shipIndex].setMargin(new Insets(0, 0, 0, 0));
            opShipButtons[shipIndex].
                setBorder(BorderFactory.createEmptyBorder());
            opShipButtons[shipIndex].setEnabled(false);

            // Instantiate my ship buttons and remove all borders
            myShipButtons[shipIndex] =
                new JToggleButton(shipIcons[shipIndex], false);
            myShipButtons[shipIndex].setMargin(new Insets(0, 0, 0, 0));
            myShipButtons[shipIndex].
                setBorder(BorderFactory.createEmptyBorder());
            myShipButtons[shipIndex].setName(shipNames[shipIndex]);

            // Add action listener to my ship buttons
            myShipButtons[shipIndex].addMouseListener(mouseListener);
            myShipButtons[shipIndex].addMouseMotionListener(mouseListener);

            // Add the button to the button group to enforce mutual exclusion
            shipButtonGroup.add(myShipButtons[shipIndex]);

            // Add oponent ship buttons to the GUI
            JPanel p = new JPanel();
            Font f = new Font(Font.SERIF, Font.PLAIN, 12);
            JLabel l = new JLabel(shipNames[shipIndex]);
            l.setFont(f);
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.add(opShipButtons[shipIndex]);
            p.add(l);
            p.setBorder(BorderFactory.createEmptyBorder(3,1,3,1));
            osBoxPane.add(p);

            // Add my ship buttons to the GUI
            JPanel q = new JPanel();
            Font g = new Font(Font.SERIF, Font.PLAIN, 12);
            JLabel k = new JLabel(shipNames[shipIndex]);
            k.setFont(g);
            q.setLayout(new BoxLayout(q, BoxLayout.Y_AXIS));
            q.add(myShipButtons[shipIndex]);
            q.add(k);
            q.setBorder(BorderFactory.createEmptyBorder(3,1,3,1));
            msBoxPane.add(q);
        }

        // Add the grid panes to the border panes
        osBorderPane.add(osBoxPane);
        opShipsTitledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Opponent Ships", TitledBorder.LEFT, TitledBorder.TOP);
        osBorderPane.setBorder(opShipsTitledBorder);

        msBorderPane.add(msBoxPane);
        myShipsTitledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "My Ships", TitledBorder.LEFT, TitledBorder.TOP);
        msBorderPane.setBorder(myShipsTitledBorder);

        // Create and configure the frame
        theFrame = new JFrame("Yamato");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set the frame icon
        URL imgURL = getClass().getResource("images/imperial_crest.jpg");
        Image yamatoImg = Toolkit.getDefaultToolkit().getImage(imgURL);
        theFrame.setIconImage(yamatoImg);

        // Setup the glassPane
        glassPane = new ImageMovePanel();
        glassPane.addKeyListener(keyListener);
        theFrame.setGlassPane(glassPane);

        // Create the outermost panel and set it as the frame's content panel
        theOutermostPane = new JPanel(new BorderLayout());
        theFrame.setContentPane(theOutermostPane);
        
        // Create the second-layer panes and add ship and water panes to them
        FlowLayout opfl = new FlowLayout();
        opfl.setAlignOnBaseline(true);
        opPaneIn = new JPanel(opfl);
        opPaneIn.add(osBorderPane);
        opPaneIn.add(owBorderPane);
        opPaneIn.setBorder(BorderFactory.createLineBorder(Color.red, 2));
        opPaneOut = new JPanel();
        opPaneOut.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        opPaneOut.add(opPaneIn);
  
        FlowLayout fl = new FlowLayout();
        fl.setAlignOnBaseline(true);
        myPaneIn = new JPanel();
        myPaneIn.setLayout(fl);
        myPaneIn.add(msBorderPane);
        myPaneIn.add(mwBorderPane);
        myPaneIn.setBorder(BorderFactory.createLineBorder(Color.blue, 2));
        myPaneOut = new JPanel();
        myPaneOut.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        myPaneOut.add(myPaneIn);

        // Add mouse listeners to the "my" panel
        myPaneIn.addMouseListener(mouseListener);
        myPaneIn.addMouseMotionListener(mouseListener);
        //myPaneIn.addMouseWheelListener(mouseListener);

        // Add the second-layer panels to the outermost pane
        theOutermostPane.add(opPaneOut, java.awt.BorderLayout.EAST);
        theOutermostPane.add(myPaneOut, java.awt.BorderLayout.WEST);

        // Add a status bar
        statusBar = new StatusBar();
        theOutermostPane.add(statusBar, java.awt.BorderLayout.SOUTH);

        // Add the menu bar
        theMenuBar = new JMenuBar();

        networkMenu = new JMenu("Network");
        networkMenu.setMnemonic(KeyEvent.VK_N);

        gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);

        chatMenu = new JMenu("Chat");
        chatMenu.setMnemonic(KeyEvent.VK_C);

        helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        menuListener = new MenuListener();

        startServerMenuItem = new JMenuItem(startServerMenuItemString);
        startServerMenuItem.setMnemonic(KeyEvent.VK_S);
        startServerMenuItem.addActionListener(menuListener);

        startClientMenuItem = new JMenuItem(startClientMenuItemString);
        startClientMenuItem.setMnemonic(KeyEvent.VK_C);
        startClientMenuItem.addActionListener(menuListener);

        networkMenu.add(startServerMenuItem);
        networkMenu.add(startClientMenuItem);

        startGameMenuItem = new JMenuItem(startGameMenuItemString);
        startGameMenuItem.setMnemonic(KeyEvent.VK_S);
        startGameMenuItem.addActionListener(menuListener);
        startGameMenuItem.setEnabled(false);

        resetBoardMenuItem = new JMenuItem(resetBoardMenuItemString);
        resetBoardMenuItem.setMnemonic(KeyEvent.VK_R);
        resetBoardMenuItem.addActionListener(menuListener);
        resetBoardMenuItem.setEnabled(false);

        gameMenu.add(resetBoardMenuItem);
        gameMenu.add(startGameMenuItem);

        showChatBoxMenuItem =
            new JCheckBoxMenuItem(showChatBoxMenuItemString, false);
        showChatBoxMenuItem.addActionListener(menuListener);

        chatMenu.add(showChatBoxMenuItem);

        theMenuBar.add(networkMenu);
        theMenuBar.add(gameMenu);
        theMenuBar.add(chatMenu);
        theMenuBar.add(helpMenu);
        theFrame.setJMenuBar(theMenuBar);

        // Pack the frame and make it visible
        theFrame.pack();
        theFrame.setVisible(true);
    }

    // Are all my ships placed in the water?
    private String allMyShipsPlaced()
    {
        // All ships must be placed before starting the game
        boolean allShipsPlaced = true;
        String notPlaced = "";

        // Cycle through the list of my ships
        for (int shipIndex = 0; shipIndex < numShips; ++shipIndex)
        {
            // If one or more of the ships has not been placed
            if (false == shipPlaced[shipIndex])
            {
                // Remember that not all ships have been placed
                allShipsPlaced = false;

                // Save the ship name
                notPlaced += "\n" + "   - " + shipNames[shipIndex];
            }
        }

        return notPlaced;
    }

    // Sets and Gets
    private void setMyWaterBorderTitle(String title)
    {
        myWaterTitledBorder.setTitle(title);
    }

    private void setMyShipsBorderTitle(String title)
    {
        myShipsTitledBorder.setTitle(title);
    }

    private void setOpponentWaterBorderTitle(String title)
    {
        opWaterTitledBorder.setTitle(title);
    }

    private void setOpponentShipsBorderTitle(String title)
    {
        opShipsTitledBorder.setTitle(title);
    }

    private void enableTargetingButtons(boolean b)
    {
    }

    private boolean setMyName()
    {
        myName = JOptionPane.showInputDialog(theFrame,
                                             "Enter Your Battleship Name:",
                                             myName);

        if (null == myName)
        {
            // User selected JOptionPane.CANCEL_OPTION
            setMyShipsBorderTitle("My Ships");
            setMyWaterBorderTitle("My Water");
            theFrame.repaint();
            return false;
        }

        myName.trim();

        while ((myName.matches("")) || (myName.matches(".*\\s.*")))
        {
            myName = JOptionPane.showInputDialog(theFrame,
                              "Enter Your Battleship Name:",
                              myName);

            myName.trim();

            if (null == myName)
            {
                // User selected JOptionPane.CANCEL_OPTION
                setMyShipsBorderTitle("My Ships");
                setMyWaterBorderTitle("My Water");
                theFrame.repaint();
                return false;
            }

            if ((myName.matches("")) || (true == myName.matches(".*\\s.*")))
            {
                JOptionPane.showMessageDialog(theFrame,
                                              "No Spaces Allowed in Your Name",
                                              "ERROR: Spaces in Name",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }

        setMyShipsBorderTitle(myName + "'s Ships");
        setMyWaterBorderTitle(myName + "'s Water");
        theFrame.repaint();

        return true;
    }

    private void setOpponentName(String name)
    {
        opName = name;
        setOpponentShipsBorderTitle(name + "'s Ships");
        setOpponentWaterBorderTitle(name + "'s Ships");
        theFrame.repaint();
    }

    // Message received from network
    public void messageFromServer(String msg)
    {
        // Assume each message is a String of the form:
        // "FROM=<SENDER_ID>;TYPE=<MSG_TYPE>;VALUE=<MSG_VALUE>"
        // If not, the message will be ignored
        String protocol[] = msg.split(";");
        String sender[]   = protocol[0].split("=");
        String type[]     = protocol[1].split("=");
        String value[]    = protocol[2].split("=");

        // Chat message
        if (type[1].compareTo(TYPE_CHAT) == 0)
        {
            if (chatWindowTextArea != null)
            {
                String chatMsg = sender[1] + ": " + value[1] + "\n";
                chatWindowTextArea.append(chatMsg);
            }
        }

        // Invitation to start game received in wrong state
        if ((gameState != GS_AWAITING_INVITATION) &&
            (sender[1].compareTo(myName) != 0) &&
            (type[1].compareTo(TYPE_REQ) == 0) &&
            (value[1].compareTo("startGame") == 0))
        {
            // Send a negative response
            String networkMsg = new String("FROM=" + myName + ";");
            networkMsg += "TYPE=" + TYPE_RSP + ";";
            networkMsg += "VALUE=NO";
            myGameClient.sendMessage(networkMsg);
        }

        // Invitation to start game received in correct state
        if ((gameState == GS_AWAITING_INVITATION) &&
            (sender[1].compareTo(myName) != 0) &&
            (type[1].compareTo(TYPE_REQ) == 0) &&
            (value[1].compareTo("startGame") == 0))
        {
            String dialog = "Would you like to play a game\n" +
                            "of Battleship with " + sender[1] + "?";

            int dAnswer = JOptionPane.showConfirmDialog(theFrame, dialog,
                                                        "Play Battleship?",
                                                        JOptionPane.
                                                        YES_NO_OPTION);

            String networkMsg = new String("FROM=" + myName + ";");

            if (JOptionPane.YES_OPTION == dAnswer)
            {
                // Update the game state
                setGameState(GS_GAME_IN_PROGRESS_NOT_MY_TURN);

                // Set the opponent name
                setOpponentName(sender[1]);

                // Enable the opponent buttons
                enableTargetingButtons(true);

                // Update the status bar
                statusBar.setMessage("Game started, " + opName + "'s turn");

                // Disable the start game menu item
                startGameMenuItem.setEnabled(false);

                // Enable all of the opponent ship buttons
                for (int si = Fleet.PTBOAT; si <= Fleet.CARRIER; si++)
                {
                    opShipButtons[si].setEnabled(true);
                }

                networkMsg += "TYPE=" + TYPE_RSP + ";";
                networkMsg += "VALUE=YES";
            }
            else if (JOptionPane.NO_OPTION == dAnswer)
            {
                networkMsg += "TYPE=" + TYPE_RSP + ";";
                networkMsg += "VALUE=NO";
            }

            myGameClient.sendMessage(networkMsg);
        }

        // Invitation to startGame accepted
        if ((gameState == GS_INVITATION_SENT) &&
            (type[1].compareTo(TYPE_RSP) == 0) &&
            (value[1].compareTo("YES") == 0))
        {
            // Update the game state
            setGameState(GS_GAME_IN_PROGRESS_MY_TURN);

            // Set the opponent name
            setOpponentName(sender[1]);

            // Enable the opponent buttons
            enableTargetingButtons(true);

            // Update the status bar
            statusBar.setMessage("Game started, " + myName + "'s turn");

            // Notify the user
            String message = sender[1] + " has accepted your invitation\n" +
                             "to play Battleship. You go first.";                          
            JOptionPane.showMessageDialog(theFrame, message);

            // Enable all of the opponent ship buttons
            for (int si = Fleet.PTBOAT; si <= Fleet.CARRIER; si++)
            {
                opShipButtons[si].setEnabled(true);
            }
        }
        
        // Invitation to startGame refused
        if ((gameState == GS_INVITATION_SENT) &&
            (type[1].compareTo(TYPE_RSP) == 0) &&
            (value[1].compareTo("NO") == 0))
        {
            // Update the game state
            setGameState(GS_AWAITING_INVITATION);

            // Enable the start game menu item
            startGameMenuItem.setEnabled(true);

            // Notify the user
            String message = sender[1] + " is not ready to play\n" +
                             "Battleship right now. Sorry.";                          
            JOptionPane.showMessageDialog(theFrame, message);

            // TODO: Destroy the game client?
        }

        // The opponent has taken a shot
        if ((gameState == GS_GAME_IN_PROGRESS_NOT_MY_TURN) &&
            (sender[1].compareTo(opName) == 0) &&
            (type[1].compareTo(TYPE_SHOT) == 0))
        {
            // Update the status messge
            String statusMsg = opName + " shoots: \"" + value[1] + "\"";
            statusBar.setForeground(Color.RED);
            statusBar.setBold();
            statusBar.setMessage(statusMsg);

            // Convert strings to numbers
            String row = value[1].substring(0, 1);
            String col = value[1].substring(1);
            int r = (int)row.charAt(0) - (int)'A';
            int c = Integer.parseInt(col) - 1;

            boolean isShipHit = false;
            boolean isShipSunk = false;
            boolean isMyFleetDestroyed = false;
            int si = 0;

            // Loop through all ships
            for (si = Fleet.PTBOAT; si <= Fleet.CARRIER; si++)
            {
                if (true == myFleet.isShipLocation(si, r, c))
                {
                    isShipHit = true;
                    myFleet.setShipIsHit(si, r, c);
                    
                    // See if the ship is sunk
                    isShipSunk = myFleet.isShipSunk(si);

                    // See if the fleet is destroyed
                    isMyFleetDestroyed = myFleet.isFleetDestroyed(); 

                    // Preserve the ship index for use below
                    break;
                }
            }

            String networkMsg = "FROM=" + myName + ";";

            if (true == isShipSunk)
            {
                int[][] coords = myFleet.getShipLocation(si);

                networkMsg += "TYPE=" + TYPE_SHOT_ACK_SUNK + ";";
                networkMsg += "VALUE=" + "SHIP:" + si + "," + "R:" +
                              coords[0][0]  + "," + "C:" + coords[0][1] + "," +
                              "HEADING:" + myFleet.getShipHeading(si);

                // Update game state
                setGameState(GS_GAME_IN_PROGRESS_MY_TURN);

                // Set the button icon
                myShipButtons[si].setIcon(sunkIcons[si]);

                if (true == isMyFleetDestroyed)
                {
                    // Change the background and foreground colors
                    statusBar.setBackground(Color.RED);
                    statusBar.setForeground(Color.WHITE);
                    statusBar.setBold();

                    // Update the status bar message
                    statusBar.setMessage(opName + " WINS!");

                    // Update game state
                    setGameState(GS_GAME_OVER);

                    // Enable the reset board menu item
                    resetBoardMenuItem.setEnabled(true);
                }
            }
            else if (true == isShipHit)
            {
                networkMsg += "TYPE=" + TYPE_SHOT_ACK + ";";
                networkMsg += "VALUE=" + value[1];

                // Update game state
                setGameState(GS_GAME_IN_PROGRESS_MY_TURN);
            }
            else
            {
                myFleet.setMissLocation(r, c);
                networkMsg += "TYPE=" + TYPE_SHOT_NACK + ";";
                networkMsg += "VALUE=" + value[1];

                // Update game state
                setGameState(GS_GAME_IN_PROGRESS_MY_TURN);
            }

            // Repaint
            myBsPanels[r][c].repaint();

            // Send the reply
            myGameClient.sendMessage(networkMsg);
        }

        // The shot was a hit, ship not sunk
        if ((gameState == GS_GAME_IN_PROGRESS_MY_TURN) &&
            (sender[1].compareTo(opName) == 0) &&
            (type[1].compareTo(TYPE_SHOT_ACK) == 0))
        {
            // Convert shot strings to numbers
            String row = value[1].substring(0, 1);
            String col = value[1].substring(1);
            int r = (int)row.charAt(0) - (int)'A';
            int c = Integer.parseInt(col) - 1;

            // Mark a hit on the opponent water
            opFleet.setHitLocation(r, c);
            opBsPanels[r][c].repaint();

            // Update game state
            setGameState(GS_GAME_IN_PROGRESS_NOT_MY_TURN);
        }

        // The shot was a hit, ship is sunk
        if ((gameState == GS_GAME_IN_PROGRESS_MY_TURN) &&
            (sender[1].compareTo(opName) == 0) &&
            (type[1].compareTo(TYPE_SHOT_ACK_SUNK) == 0))
        {
            // Break value[1] into
            // 1. SHIP:<SHIP_INDEX>
            // 2. R:<ROW>
            // 3. C:<COL>
            // 4. HEADING:<HEADING>
            String values[] = value[1].split(",");
            String shipIndex[] = values[0].split(":");  
            String row[]       = values[1].split(":");
            String col[]       = values[2].split(":");
            String heading[]   = values[3].split(":");

            int si = Integer.parseInt(shipIndex[1]);
            int r  = Integer.parseInt(row[1]);
            int c  = Integer.parseInt(col[1]);
            int h  = Integer.parseInt(heading[1]);

            // Set ship heading
            opFleet.setShipHeading(si, h);

            // Set ship in oponent water at <ROW>, <COL>
            opFleet.setShipLocation(si, r, c);
            
            // Set ship hit in each section
            opFleet.setShipIsSunk(si);

            // Set the button icon
            opShipButtons[si].setIcon(sunkIcons[si]);

            // Redraw each battleship panel
            int loc[][] = opFleet.getShipLocation(si);

            for (int i = 0; i < opFleet.getShipSize(si); i++)
            {
                opFleet.unsetHitLocation(loc[i][0], loc[i][1]);
                opBsPanels[loc[i][0]][loc[i][1]].repaint();
            }

            if (opFleet.isFleetDestroyed())
            {
                // Change the background and foreground colors
                statusBar.setBackground(Color.BLUE);
                statusBar.setForeground(Color.WHITE);
                statusBar.setBold();

                // Update the status bar message
                statusBar.setMessage(myName + " WINS!");

                // Update game state
                setGameState(GS_GAME_OVER);

                // Enable the reset board menu item
                resetBoardMenuItem.setEnabled(true);
            }
            else
            {
                // Update game state
                setGameState(GS_GAME_IN_PROGRESS_NOT_MY_TURN);
            }
        }

        // The shot was a miss
        if ((gameState == GS_GAME_IN_PROGRESS_MY_TURN) &&
            (sender[1].compareTo(opName) == 0) &&
            (type[1].compareTo(TYPE_SHOT_NACK) == 0))
        {
            // Convert shot strings to numbers
            String row = value[1].substring(0, 1);
            String col = value[1].substring(1);
            int r = (int)row.charAt(0) - (int)'A';
            int c = Integer.parseInt(col) - 1;

            // Mark a miss on the opponent water
            opFleet.setMissLocation(r, c);
            opBsPanels[r][c].repaint();

            // Update game state
            setGameState(GS_GAME_IN_PROGRESS_NOT_MY_TURN);
        }
    }

    // Inner classes
    class GamePlayMouseListener extends MouseInputAdapter
    {
        int row = -1;
        int col = -1;

        public void mouseEntered(MouseEvent me)
        {
            if (me.getComponent() instanceof BattleshipPanel)
            {
                BattleshipPanel bp = (BattleshipPanel)me.getComponent();

                int[] coords = new int[2];
                coords = bp.getCoordinates();

                row = coords[0];
                col = coords[1];
            }
        }

        public void mouseExited(MouseEvent me)
        {
            row = -1;
            col = -1;
        }

        public void mousePressed(MouseEvent me)
        {
            BattleshipPanel bp;

            if (me.getComponent() instanceof BattleshipPanel)
            {
                bp = (BattleshipPanel)me.getComponent();
            }
            else
            {
                // Stop
                return;
            }

            if (gameState == GS_GAME_IN_PROGRESS_MY_TURN)
            {
                if (false == bp.isSelected())
                {
                    String networkMsg = "FROM=" + myName + ";" +
                                        "TYPE=" + TYPE_SHOT + ";" +
                                        "VALUE=" + rindex[row] + cindex[col];

                    myGameClient.sendMessage(networkMsg);

                    // Set the button selected
                    bp.setSelected(true);

                    // Update the status messge
                    String statusMsg = myName + " shoots: " + "\"" +
                                       rindex[row] + cindex[col] + "\"";
                    statusBar.setForeground(Color.BLUE);
                    statusBar.setPlain();
                    statusBar.setMessage(statusMsg);
                }
                else
                {
                    String dialogMsg = "You've already tried that one.";

                    JOptionPane.showMessageDialog(opPaneIn, dialogMsg);
                }
            }
            else if (gameState == GS_GAME_IN_PROGRESS_NOT_MY_TURN)
            {
                String dialogMsg = "It's " + opName + "'s turn.";

                JOptionPane.showMessageDialog(opPaneIn, dialogMsg);
            }
        }
    }

    class ShipPlacementMouseListener extends MouseInputAdapter
    {
        int row = -1;
        int col = -1;

        public void mouseDragged(MouseEvent me)
        {
            for (int shipIndex = 0; shipIndex < numShips; ++shipIndex)
            {
                if (true == shipBeingDragged[shipIndex])
                {
                    glassPane.requestFocusInWindow();

                    p = SwingUtilities.convertPoint(me.getComponent(),
                                                    me.getPoint(),
                                                    glassPane);

                    int nX = p.x - shipImages[shipIndex].getWidth()/2;
                    int nY = p.y - shipImages[shipIndex].getHeight()/2;

                    int newX = nX;
                    int newY = nY;

                    if (90 == rotAngle[shipIndex])
                    {
                        newX =  nY;
                        newY = -nX;
                    }
                    else if (180 == rotAngle[shipIndex])
                    {
                        newX = -nX;
                        newY = -nY;
                    }
                    else if (270 == rotAngle[shipIndex])
                    {
                        newX = -nY;
                        newY =  nX;
                    }

                    glassPane.setSelectionBounds(
                        new Rectangle(newX, newY,
                                      shipImages[shipIndex].getWidth(),
                                      shipImages[shipIndex].getHeight()));

                    // Only one ship at a time can be dragged
                    break;
                }
            }
        }
        
        public void mouseEntered(MouseEvent me)
        {
            boolean draggingSomeShip = false;
            int shipIndex = 0;

            if (me.getComponent() instanceof BattleshipPanel)
            {
                for(shipIndex = 0; shipIndex < numShips; ++shipIndex)
                {
                    if (true == shipBeingDragged[shipIndex])
                    {
                        draggingSomeShip = true;
                        break;
                    }
                }

                if (true == draggingSomeShip)
                {
                    if (true == shipPlaced[shipIndex])
                    {
                        shipPlaced[shipIndex] = false;
                        myFleet.setShipLocation(shipIndex, -1, -1);
                    }

                    BattleshipPanel bp = (BattleshipPanel)me.getComponent();

                    int[] coords = new int[2];
                    coords = bp.getCoordinates();

                    row = coords[0];
                    col = coords[1];
                }
            }
        }
        
        public void mouseExited(MouseEvent me)
        {
            boolean draggingShip = false;

            if (me.getComponent() instanceof BattleshipPanel)
            {
                for(int shipIndex = 0; shipIndex < numShips; ++shipIndex)
                {
                    if (true == shipBeingDragged[shipIndex])
                    {
                        draggingShip = true;
                        break;
                    }
                }

                if (draggingShip)
                {
                    row = -1;
                    col = -1;
                }
            }
        }
        
        public void mousePressed(MouseEvent me)
        {
            int shipIndex = 0;
            boolean draggingSomeShip = false;

            if (me.getComponent() instanceof JToggleButton)
            {
                if (gameState == GS_GAME_OVER)
                {
                    // Create the messge
                    String msg = "Select \"" + resetBoardMenuItemString +
                                 "\" from the\nGame menu to reset the board.";

                    // Notify the user that ships cannot be moved
                    JOptionPane.showMessageDialog(myPaneOut, msg);
    
                    // Don't allow ships to be moved
                    return;
                }

                if ((gameState == GS_GAME_IN_PROGRESS_MY_TURN) ||
                    (gameState == GS_GAME_IN_PROGRESS_NOT_MY_TURN))
                {
                    // Notify the user that ships cannot be moved
                    JOptionPane.showMessageDialog(myPaneOut,
                                                  "Ships cannot be moved.");
    
                    // Don't allow ships to be moved
                    return;
                }

                JToggleButton button = (JToggleButton)me.getComponent();
               
                for (shipIndex = 0; shipIndex < numShips; ++shipIndex)
                {
                    if ((button.getName() == shipNames[shipIndex]) &&
                        (false == shipBeingDragged[shipIndex]))
                    {
                        // A ship is being dragged
                        draggingSomeShip = true;

                        // Initialize the row and column
                        row = -1;
                        col = -1;

                        rotAngle[shipIndex] = (rotAngle[shipIndex] + 90) % 360;

                        // Only one ship at a time can be selected
                        break;
                    }
                }
            }
            else if (me.getComponent() instanceof BattleshipPanel)
            {
                if (gameState == GS_GAME_OVER)
                {
                    // Create the messge
                    String msg = "Select \"" + resetBoardMenuItemString +
                                 "\" from the\nGame menu to reset the board.";

                    // Notify the user that ships cannot be moved
                    JOptionPane.showMessageDialog(myPaneOut, msg);

                    // Don't allow ships to be moved
                    return;
                }

                for (shipIndex = 0; shipIndex < numShips; ++shipIndex)
                {
                    BattleshipPanel bp = (BattleshipPanel)me.getComponent();
                    int[] coords = new int[2];
                    coords = bp.getCoordinates();

                    row = coords[0];
                    col = coords[1];

                    if (true == myFleet.isShipLocation(shipIndex, row, col))
                    {
                        if (gameState == GS_GAME_OVER)
                        {
                            // Create the messge
                            String msg = "Select \"" + resetBoardMenuItemString
                                         + "\" from the Game\nmenu to " +
                                           "reset the board.";

                            // Notify the user that ships cannot be moved
                            JOptionPane.showMessageDialog(myPaneOut, msg);

                            // Don't allow ships to be moved
                            return;
                        }

                        if ((gameState == GS_GAME_IN_PROGRESS_MY_TURN) ||
                            (gameState == GS_GAME_IN_PROGRESS_NOT_MY_TURN))
                        {
                            // Notify the user that ships cannot be moved
                            JOptionPane.showMessageDialog(myPaneOut,
                                                      "Ships cannot be moved.");
    
                            // Don't allow ships to be moved
                            return;
                        }

                        // A ship is being dragged
                        draggingSomeShip = true;

                        // Remove the ship from the water
                        shipPlaced[shipIndex] = false;
                        myFleet.setShipLocation(shipIndex, -1, -1);

                        // Set the rotation angle
                        if (Fleet.EAST ==
                                myFleet.getShipHeading(shipIndex))
                        {
                            rotAngle[shipIndex] = 0;
                        }
                        else if (Fleet.SOUTH ==
                                     myFleet.getShipHeading(shipIndex))
                        {
                            rotAngle[shipIndex] = 90;
                        }
                        else if (Fleet.WEST ==
                                     myFleet.getShipHeading(shipIndex))
                        {
                            rotAngle[shipIndex] = 180;
                        }
                        else if (Fleet.NORTH ==
                                     myFleet.getShipHeading(shipIndex))
                        {
                            rotAngle[shipIndex] = 270;
                        }

                        // Only one ship at a time can be selected
                        break;
                    }
                }
            }

            if (true == draggingSomeShip)
            {
                glassPane.requestFocusInWindow();

                shipBeingDragged[shipIndex] = true;

                p = SwingUtilities.convertPoint(me.getComponent(),
                                                me.getPoint(),
                                                glassPane);

                p.translate(-shipImages[shipIndex].getWidth()/2,
                            -shipImages[shipIndex].getHeight()/2);

                if (90 == rotAngle[shipIndex])
                {
                    p.move(p.y, -p.x);
                }
                else if (180 == rotAngle[shipIndex])
                {
                    p.move(-p.x, -p.y);
                }
                else if (270 == rotAngle[shipIndex])
                {
                    p.move(-p.y, p.x);
                }

                glassPane.setRotationAngle(rotAngle[shipIndex]);
                glassPane.setVisible(true);
                glassPane.setImage(p, shipImages[shipIndex]);

                // Get the index
                int index = rotAngle[shipIndex] / 90;

                // Update the status bar
                statusBar.setMessage("Placing the " + shipNames[shipIndex] +
                                     ", heading " + headingNames[index]);

                // Don't accept game invitation while dragging a ship
                if (gameState == GS_AWAITING_INVITATION)
                {
                    setGameState(GS_PLACE_SHIPS);
                }
            }
        }
        
        public void mouseReleased(MouseEvent me)
        {
            boolean draggingSomeShip = false;
            int shipIndex = 0;

            if ((me.getComponent() instanceof JToggleButton) ||
                (me.getComponent() instanceof BattleshipPanel))
            {
                for (shipIndex = 0; shipIndex < numShips; ++shipIndex)
                {
                    if (true == shipBeingDragged[shipIndex])
                    {
                        draggingSomeShip = true;
                        break;
                    }
                }
            }

            if ((true == draggingSomeShip) && (row == -1) && (col == -1))
            {
                // Stop dragging the ship
                shipBeingDragged[shipIndex] = false;
                glassPane.setVisible(false);
                glassPane.setImage(new Point(0,0), null);
            }

            if ((true == draggingSomeShip)    &&
                (row >= 0) && (row < numRows) &&
                (col >= 0) && (col < numCols))
            {
                // Stop dragging the ship
                shipBeingDragged[shipIndex] = false;
                glassPane.setVisible(false);
                glassPane.setImage(new Point(0,0), null);

                // Save the old heading in case of a collision
                int oldHeading = myFleet.getShipHeading(shipIndex);

                // Get the heading
                int heading = 0;

                if (0 == rotAngle[shipIndex])
                {
                    // Set the heading
                    heading = Fleet.EAST;

                    // The column/row must be adjusted according
                    // to the functionality of the Battleship
                    // Panel.setShipLocation() method -- so that
                    // the ship gets placed in the water where
                    // the user expects it to be placed
                    if ((Fleet.PTBOAT == shipIndex) ||
                        (Fleet.UBOAT == shipIndex) ||
                        (Fleet.DESTROYER == shipIndex))
                    {
                        col -= 1;
                    }
                    else if ((Fleet.BATTLESHIP == shipIndex) ||
                             (Fleet.CARRIER == shipIndex))
                    {
                        col -= 2;
                    }

                    if (col < 0)
                    {
                        col = 0;
                    }
                    else if ((col + myFleet.getShipSize(shipIndex)) > 9)
                    {
                        col = 9 - myFleet.getShipSize(shipIndex) + 1;
                    }
                }
                else if (90 == rotAngle[shipIndex])
                {
                    // Set the heading
                    heading = Fleet.SOUTH;

                    // The column/row must be adjusted according
                    // to the functionality of the Battleship
                    // Panel.setShipLocation() method -- so that
                    // the ship gets placed in the water where
                    // the user expects it to be placed
                    if ((Fleet.PTBOAT == shipIndex) ||
                        (Fleet.UBOAT == shipIndex) ||
                        (Fleet.DESTROYER == shipIndex))
                    {
                        row -= 1;
                    }
                    else if ((Fleet.BATTLESHIP == shipIndex) ||
                             (Fleet.CARRIER == shipIndex))
                    {
                        row -= 2;
                    }

                    if (row < 0)
                    {
                        row = 0;
                    }
                    else if ((row + myFleet.getShipSize(shipIndex)) > 9)
                    {
                        row = 9 - myFleet.getShipSize(shipIndex) + 1;
                    }
                }
                else if (180 == rotAngle[shipIndex])
                {
                    // Set the heading
                    heading = Fleet.WEST;

                    // The column/row must be adjusted according
                    // to the functionality of the Battleship
                    // Panel.setShipLocation() method -- so that
                    // the ship gets placed in the water where
                    // the user expects it to be placed
                    if ((Fleet.UBOAT == shipIndex) ||
                        (Fleet.DESTROYER == shipIndex) ||
                        (Fleet.BATTLESHIP == shipIndex))
                    {
                        col += 1;
                    }
                    else if (Fleet.CARRIER == shipIndex)
                    {
                        col += 2;
                    }

                    if (col > 9)
                    {
                        col = 9;
                    }
                    else if ((col - myFleet.getShipSize(shipIndex)) < 0)
                    {
                        col = myFleet.getShipSize(shipIndex) - 1;
                    }
                }
                else if (270 == rotAngle[shipIndex])
                {
                    // Set the heading
                    heading = Fleet.NORTH;

                    // The column/row must be adjusted according
                    // to the functionality of the Battleship
                    // Panel.setShipLocation() method -- so that
                    // the ship gets placed in the water where
                    // the user expects it to be placed
                    if ((Fleet.UBOAT == shipIndex) ||
                        (Fleet.DESTROYER == shipIndex) ||
                        (Fleet.BATTLESHIP == shipIndex))
                    {
                        row += 1;
                    }
                    else if (Fleet.CARRIER == shipIndex)
                    {
                        row += 2;
                    }

                    if (row > 9)
                    {
                        row = 9;
                    }
                    else if ((row - myFleet.getShipSize(shipIndex)) < 0)
                    {
                        row = myFleet.getShipSize(shipIndex) - 1;
                    }
                }

                // Set the ship in the water
                myFleet.setShipHeading(shipIndex, heading);

                // If the ship was placed in a already occupied location
                if (false == myFleet.setShipLocation(shipIndex, row, col))
                {
                    // DEVELOPMENT
                    System.out.println("COLLISION!");

                    // Restore the old heading
                    myFleet.setShipHeading(shipIndex, oldHeading);

                    // Tell the user not to overlap ships
                    JOptionPane.showMessageDialog(myPaneIn,
                        "Two Ships Can't Occupy the Same Location!");
                }
                else
                {
                    shipPlaced[shipIndex] = true;
                }
            }

            // Update the status bar
            if ((draggingSomeShip == true) &&
                (gameState == GS_PLACE_SHIPS))
            {
                if (true == allMyShipsPlaced().isEmpty())
                {
                    // If network communication has not been initiated
                    if (myGameClient == null)
                    {
                        // Update the status bar
                        statusBar.setMessage("Start the server or client");
                    }
                    else
                    {
                        // Update the game state
                        setGameState(GS_AWAITING_INVITATION);

                        // Update the status bar
                        statusBar.setMessage("Select \"Start Game\" " +
                                             "from the Game menu");

                        // Enable the start game menu item
                        startGameMenuItem.setEnabled(true);
                    }
                }
                else
                {
                    // Update the status bar
                    statusBar.setMessage("Place ships");

                    // Disable the start game menu item
                    startGameMenuItem.setEnabled(false);
                }
            }
            else if ((draggingSomeShip == true) &&
                     (gameState == GS_AWAITING_INVITATION))
            {
                // If all ships are not placed
                if (false == allMyShipsPlaced().isEmpty())
                {
                    // Change the game state
                    setGameState(GS_PLACE_SHIPS);

                    // Disable the start game menu item
                    startGameMenuItem.setEnabled(false);

                    // Update the status bar
                    statusBar.setMessage("Place ships");
                }
            }
        }
    }

    class ShipPlacementKeyListener extends KeyAdapter
    {
        public void keyPressed(KeyEvent ke)
        {
            int keyCode = ke.getKeyCode();
            int shipIndex = 0;
            boolean modifyRotationAngle = false;

            if ((KeyEvent.VK_TAB     == keyCode) ||
                (KeyEvent.VK_SHIFT   == keyCode) ||
                (KeyEvent.VK_CONTROL == keyCode) ||
                (KeyEvent.VK_ALT     == keyCode))
            {
                for (shipIndex = 0; shipIndex < numShips; ++shipIndex)
                {
                    if (true == shipBeingDragged[shipIndex])
                    {
                        modifyRotationAngle = true;
                        break;
                    }
                }

                if ((true == modifyRotationAngle) && (null != p))
                {
                    rotAngle[shipIndex] = (rotAngle[shipIndex] + 90) % 360;
                    glassPane.setRotationAngle(rotAngle[shipIndex]);

                    int nX = p.x - shipImages[shipIndex].getWidth()/2;
                    int nY = p.y - shipImages[shipIndex].getHeight()/2;

                    int newX = nX;
                    int newY = nY;

                    if (90 == rotAngle[shipIndex])
                    {
                        newX =  nY;
                        newY = -nX;
                    }
                    else if (180 == rotAngle[shipIndex])
                    {
                        newX = -nX;
                        newY = -nY;
                    }
                    else if (270 == rotAngle[shipIndex])
                    {
                        newX = -nY;
                        newY =  nX;
                    }

                    glassPane.setSelectionBounds(
                        new Rectangle(newX, newY,
                                      shipImages[shipIndex].getWidth(),
                                      shipImages[shipIndex].getHeight()));

                    // Get the index
                    int index = rotAngle[shipIndex] / 90;

                    // Update the status bar
                    statusBar.setMessage("Placing the " + shipNames[shipIndex] +
                                         ", heading " + headingNames[index]);
                }
            }
        }
    }

    class MenuListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            // Chat window menu item handler
            if (e.getActionCommand() == showChatBoxMenuItemString)
            {
                // If a network connection does not exist
                if ((networkState != NS_SERVER) && (networkState != NS_CLIENT))
                {
                    // Inform the user there is no network connection
                    JOptionPane.showMessageDialog(theFrame,
                                                  "Can't chat without a\n" +
                                                  "network connection.");

                    // Unset the checkbox
                    showChatBoxMenuItem.setState(false);
                }

                // If the chat window is not yet instantiated
                if (chatWindow == null)
                {
                    // Create the chat window and text components
                    chatWindow          = new JDialog(theFrame, "Chat Window");
                    chatWindowTextField = new JTextField(30);
                    JButton sendButton  = new JButton("Send");
                    chatWindowTextArea  = new JTextArea(20, 35);
                    chatWindowTextArea.setLineWrap(true);
                    chatWindowTextArea.setWrapStyleWord(true);
                    chatWindowTextArea.setEditable(false);
                    sendButton.addActionListener(
                            new ChatButtonActionListener());

                    // Add a window listener
                    chatWindow.addWindowListener(new chatWindowListener());

                    // Create, populate and configure text component containers
                    JPanel topPanel = new JPanel(new BorderLayout());
                    JScrollPane scrollPane = new JScrollPane(
                            chatWindowTextArea,
                            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

                    scrollPane.setBorder(
                            BorderFactory.createEmptyBorder(4,10,4,10));

                    JPanel sendPanel = new JPanel();
                    sendPanel.setLayout(new FlowLayout());
                    sendPanel.add(chatWindowTextField);
                    sendPanel.add(sendButton);

                    topPanel.add(scrollPane, BorderLayout.CENTER);
                    topPanel.add(sendPanel, BorderLayout.SOUTH);
                    
                    // Add the top container to the chat window
                    chatWindow.add(topPanel);
                }

                // If the checkbox is checked
                if (true == showChatBoxMenuItem.getState())
                {
                    // Make the chat window visible
                    chatWindow.setVisible(true);

                    // Resize the chat window
                    chatWindow.pack();

                    // Request focus for the text field
                    chatWindowTextField.requestFocus();

                    // Set the location relative to the main frame
                    chatWindow.setLocationRelativeTo(theFrame);
                }
                else
                {
                    // Make the chat window invisible
                    chatWindow.setVisible(false);
                }

                // Stop
                return;
            }

            ////////////////////////////////////////////////////////////////////
            // TODO:
            // In a future version, the process of starting a game and choosing
            // an opponent will be simplified -- the user will not have to
            // enter the opponent computer's IP address.
            // 
            // A drop-down list of possible opponents will be presented to the
            // user, and by selecting one entry in the list, the selected
            // opponent will receive an invitation to start a game. This will
            // require some networking development: automatic game instance
            // discovery.
            //
            // I need to do some research to figure out how it can be done, but
            // I'm pretty sure IP multicast makes automatic discovery possible.
            ////////////////////////////////////////////////////////////////////

            // Ships not placed string
            String shipsNotPlaced = allMyShipsPlaced();

            // If all my ships are not placed in the water
            if (false == shipsNotPlaced.isEmpty())
            {
                JOptionPane.showMessageDialog(theFrame,
                            "Game cannot start until all ships " +
                            "are in the water,\nShips not placed:" +
                            shipsNotPlaced,
                            "ERROR: All Ships Not in Water",
                            JOptionPane.ERROR_MESSAGE);

                // Don't allow a client or server to be started
                return;
            }

            // My name must be set before starting the game
            if ((null == myName) && (false == setMyName()))
            {
                JOptionPane.showMessageDialog(theFrame,
                                            "The game cannot start until\n" +
                                            "you have entered your name.",
                                            "No Name, No Game",
                                            JOptionPane.ERROR_MESSAGE);

                // Stop
                return;
            }

            // Start server menu item handler
            if (0 == e.getActionCommand().compareTo(startServerMenuItemString))
            {
                // If the network state is init
                if (networkState == NS_INIT)
                {

                    // Start a GameComServer thread
                    Thread serverThread = new Thread(new GameComServer());
                    serverThread.start();

                    // Start a client for this instance
                    myGameClient = new GameComClient("127.0.0.1", Battleship.this);

                    // Update the network state
                    networkState = NS_SERVER;

                    // Update the game state
                    setGameState(GS_AWAITING_INVITATION);

                    // Disable the "Start Server" and "Start Client"
                    startServerMenuItem.setEnabled(false);
                    startClientMenuItem.setEnabled(false);

                    // Update the status bar
                    statusBar.setMessage(myName + " has started the server");
                }
            }
            // Start client menu item server
            else if (0 == e.getActionCommand().
                            compareTo(startClientMenuItemString))
            {
                // If the network state is init
                if (networkState == NS_INIT)
                {
                    // Get the client IP address
                    clientIpAddr = JOptionPane.showInputDialog(theFrame,
                                   "Enter the server's IP address", "");

                    // Start a client
                    myGameClient = new GameComClient(clientIpAddr, Battleship.this);

                    // Update the network state
                    networkState = NS_CLIENT;

                    // Send a message
                    myGameClient.sendMessage("FROM="+myName+
                                             ";TYPE=REQ;VALUE=startGame");

                    // Update the game state
                    setGameState(GS_INVITATION_SENT);

                    // Disable the "Start Server" and "Start Client"
                    startServerMenuItem.setEnabled(false);
                    startClientMenuItem.setEnabled(false);

                    // Update the status bar
                    statusBar.setMessage(myName + " has started the client");
                }
            }
            // Start game menu item handler
            else if (0 == e.getActionCommand().
                            compareTo(startGameMenuItemString))
            {
                System.out.println("START GAME MENU ACTION...");

                // Send a message
                myGameClient.sendMessage("FROM="+myName+
                                         ";TYPE=REQ;VALUE=startGame");

                // Update the game state
                setGameState(GS_INVITATION_SENT);

                // Disable the start game menu item
                startGameMenuItem.setEnabled(false);
            }
            // Reset board menu item handler
            else if (0 == e.getActionCommand().
                            compareTo(resetBoardMenuItemString))
            {
                // Remove my fleet from the water and repair
                myFleet.repairFleet();

                // Remove opponent's fleet from the water and repair
                opFleet.repairFleet();

                // Unselect each battleship panel
                for (int i = 0; i < numRows; i++)
                {
                    for (int j = 0; j < numCols; j++)
                    {
                        myBsPanels[i][j].setSelected(false);
                        opBsPanels[i][j].setSelected(false);
                    }
                }

                // Reset the rotation angles and shipPlaced
                for (int i = 0; i < numShips; i++)
                {
                    rotAngle[i] = -90;
                    shipPlaced[i] = false;
                    opShipButtons[i].setIcon(shipIcons[i]);
                    myShipButtons[i].setIcon(shipIcons[i]);
                    opShipButtons[i].setEnabled(false);
                }

                // Disable the reset board menu item
                resetBoardMenuItem.setEnabled(false);

                // Set the default foreground and background colors
                statusBar.setForeground(Color.BLACK);
                statusBar.setDefaultBackground();
                statusBar.setPlain();

                // Update the status bar
                statusBar.setMessage("Place ships");

                // Repaint the entire frame
                theFrame.repaint();

                // Let the user know how to start a new game
                String msg = "After placing ships, select\n" +
                             "\"Start Game\" from the Game\nmenu to " +
                             "start a new game.";

                JOptionPane.showMessageDialog(theFrame, msg);

                // Update the game state
                setGameState(GS_PLACE_SHIPS);
            }
        }
    }

    // Chat window send button listener
    class ChatButtonActionListener implements ActionListener
    {
        // Action handler
        public void actionPerformed(ActionEvent e)
        {
            // Get the text from the chat window text field
            String chatMsg = chatWindowTextField.getText().trim();

            // If the message is empty
            if (chatMsg.isEmpty())
            {
                // Do nothing
                return;
            }

            // Create a chat message
            String networkMsg = "FROM=" + myName + ";" +
                                "TYPE=" + TYPE_CHAT + ";" +
                                "VALUE=" + chatMsg;

            // Send the chat message
            myGameClient.sendMessage(networkMsg);

            // Empty the chat window text field
            chatWindowTextField.setText("");

            // Request focus on the chat window text field
            chatWindowTextField.requestFocus();
        }
    }

    class StatusBar extends JPanel
    {
        // To make this class serializable
        public static final long serialVersionUID = 1L;

        // Declare local vars
        private JLabel label;

        // Default background color
        Color defaultBgColor = getBackground();

        // Creates a new instance of StatusBar
        public StatusBar()
        {
            // Create and set a flow layout
            setLayout(new FlowLayout(FlowLayout.LEFT));

            // Set a bevel border
            setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

            // Create and add the label
            label = new JLabel();
            label.setFont(new Font("Dialog", Font.PLAIN, 10));
            add(label);

            // Set starting text
            setMessage("Place ships");
        }
    
        public void setMessage(String message)
        {
            label.setText(" "+message);        
        }

        public void setForeground(Color c)
        {
            if (label != null)
            {
                label.setForeground(c);
            }
        }

        public void setBold ()
        {
            if (label != null)
            {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
        }

        public void setPlain()
        {
            if (label != null)
            {
                label.setFont(label.getFont().deriveFont(Font.PLAIN));
            }
        }

        public void setDefaultBackground()
        {
            setBackground(defaultBgColor);
        }
    }

    class chatWindowListener extends WindowAdapter
    {
        public void windowClosing(WindowEvent we)
        {
            // Unset the show chatbox checkbox
            showChatBoxMenuItem.setState(false);
        }
    }

    // DEVEL
    private void setGameState(int newState)
    {
        String msg;

        if (myName != null)
        {
            msg = "[" + myName + "] ";
        }
        else
        {
            msg = "[] ";
        }

        msg += "GAME STATE CHANGE: old = ";

        switch(gameState)
        {
            case GS_INVALID: msg += "GS_INVALID"; break;
            case GS_PLACE_SHIPS: msg += "GS_PLACE_SHIPS"; break;
            case GS_AWAITING_INVITATION: msg += "GS_AWAITING_INVITATION"; break;
            case GS_INVITATION_SENT: msg += "GS_INVITATION_SENT"; break;
            case GS_GAME_IN_PROGRESS_MY_TURN: msg += "GS_GAME_IN_PROGRESS_MY_TURN"; break;
            case GS_GAME_IN_PROGRESS_NOT_MY_TURN: msg += "GS_GAME_IN_PROGRESS_NOT_MY_TURN"; break;
            case GS_GAME_OVER: msg += "GS_GAME_OVER"; break;
        }

        msg += ", new = ";

        switch(newState)
        {
            case GS_INVALID: msg += "GS_INVALID"; break;
            case GS_PLACE_SHIPS: msg += "GS_PLACE_SHIPS"; break;
            case GS_AWAITING_INVITATION: msg += "GS_AWAITING_INVITATION"; break;
            case GS_INVITATION_SENT: msg += "GS_INVITATION_SENT"; break;
            case GS_GAME_IN_PROGRESS_MY_TURN: msg += "GS_GAME_IN_PROGRESS_MY_TURN"; break;
            case GS_GAME_IN_PROGRESS_NOT_MY_TURN: msg += "GS_GAME_IN_PROGRESS_NOT_MY_TURN"; break;
            case GS_GAME_OVER: msg += "GS_GAME_OVER"; break;
        }

        System.out.println(msg);

        gameState = newState;
    }
}

