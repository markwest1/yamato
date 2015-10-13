import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.Vector;
import java.net.URL;

class BattleshipPanel extends JPanel
{
    // To make this class serializable
    private static final long serialVersionUID = 1L;
    private static final int ovalWidth  = 10;
    private static final int ovalHeight = 10;

    // Static variables
    private static BufferedImage[][] bi = new BufferedImage[2][Fleet.NUMSHIPS];
    private static int w = 26;
    private static int h = 26;
    private static int REG_IMG = 0;
    private static int HIT_IMG = 1;

    // Warning! order dependent
    private static String[] imageFilenames = {
        "images/patrol_boat.jpg",
        "images/submarine.jpg",
        "images/destroyer.jpg",
        "images/battleship.jpg",
        "images/carrier.jpg"
    };

    private static String[] hitImgFilenames = {
        "images/hits_patrol_boat.jpg",
        "images/hits_submarine.jpg",
        "images/hits_destroyer.jpg",
        "images/hits_battleship.jpg",
        "images/hits_carrier.jpg"
    };

    private static int[][][] imgOffset = {
                                   // PTBOAT
                {{4, 3,  0, -2},   // sx1, sy1, sx2, sy2 // Section0
                 {0, 3,  0, -2}},  // sx1, sy1, sx2, sy2 // Section1
                                   // UBOAT
                {{3, 6,  0, -4},   // sx1, sy1, sx2, sy2 // Section0
                 {0, 6,  0, -4},   // sx1, sy1, sx2, sy2 // Section1
                 {0, 6, -3, -4}},  // sx1, sy1, sx2, sy2 // Section2
                                   // DESTROYER
                {{3, 4,  0, -4},   // sx1, sy1, sx2, sy2 // Section0
                 {0, 4,  0, -4},   // sx1, sy1, sx2, sy2 // Section1
                 {0, 4, -3, -4}},  // sx1, sy1, sx2, sy2 // Section2
                                   // BATTLESHIP
                {{0, 4,  0, -4},   // sx1, sy1, sx2, sy2 // Section0
                 {0, 4,  0, -4},   // sx1, sy1, sx2, sy2 // Section1
                 {0, 4,  0, -4},   // sx1, sy1, sx2, sy2 // Section2
                 {0, 4,  0, -4}},  // sx1, sy1, sx2, sy2 // Section3
                                   // CARRIER
                {{0, 6,  0, -4},   // sx1, sy1, sx2, sy2 // Section0
                 {0, 6,  0, -4},   // sx1, sy1, sx2, sy2 // Section1
                 {0, 6,  0, -4},   // sx1, sy1, sx2, sy2 // Section2
                 {0, 6,  0, -4},   // sx1, sy1, sx2, sy2 // Section3
                 {0, 6,  0, -4}}}; // sx1, sy1, sx2, sy2 // Section3

    // Border straddle correction is only relevant when manipulating the ship
    // heading in a linear fashion -- for example when using a button or key
    // input to change the ship's heading one direction after another. If the
    // changes to ship direction are non-linear (e.g. via drag-and-drop) these
    // corrections may confuse the user
    private static boolean borderStraddleCorrection = false;

    // Non-Static
    private int[] panelCoords = new int[2];
    private Fleet fleet;
    private Color originalBackgroundColor;
    private JLabel text;
    private boolean isSelected = false;

    // Constructor
    public BattleshipPanel(int row, int col, Fleet f)
    {
        // Set the coordinates
        panelCoords[0] = row;
        panelCoords[1] = col;

        // Set the fleet
        fleet = f;

        // Save the original background color
        originalBackgroundColor = getBackground();

        // Create and add a label
        text = new JLabel(""); 
        add(text);

        // For each ship
        for (int i = 0; i < Fleet.NUMSHIPS; i++)
        {
            // If the image has not been loaded
            if ((bi[REG_IMG][i] == null) || (bi[HIT_IMG][i] == null))
            {
                // Load the regular and hit images
                try
                {
                    URL imgURL = getClass().getResource(imageFilenames[i]);
                    BufferedImage img = ImageIO.read(imgURL);

                    int w = img.getWidth(null);
                    int h = img.getHeight(null);

                    bi[REG_IMG][i] =
                        new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

                    Graphics g1 = bi[REG_IMG][i].getGraphics();
                    g1.drawImage(img, 0, 0, null);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                try
                {

                    URL imgURL = getClass().getResource(hitImgFilenames[i]);
                    BufferedImage hitImg = ImageIO.read(imgURL);

                    int w = hitImg.getWidth(null);
                    int h = hitImg.getHeight(null);

                    bi[HIT_IMG][i] =
                        new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

                    Graphics g2 = bi[HIT_IMG][i].getGraphics();
                    g2.drawImage(hitImg, 0, 0, null);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(w, h);
    }

    public static void setBorderStraddleCorrection(boolean b)
    {
        borderStraddleCorrection = b;
    }

    public int[] getCoordinates()
    {
        return panelCoords;
    }

    public void setSelected(boolean s)
    {
        isSelected = s;
    }

    public boolean isSelected()
    {
        return isSelected;
    }

    // The all-important paintComponent method
    public void paintComponent(Graphics g)
    {
        // Use a flag to remember if the Graphic should be drawn
        boolean drawGraphic = true;

        for (int shipIndex = 0; shipIndex < Fleet.NUMSHIPS; shipIndex++)
        {
            int[][] shipCoords = fleet.getShipLocation(shipIndex);

            for (int i = 0; i < fleet.getShipSize(shipIndex); i++)
            {
                if ((panelCoords[0] == shipCoords[i][0]) &&
                    (panelCoords[1] == shipCoords[i][1]))
                {
                    // Display no text
                    text.setText("");

                    // Draw a ship
                    drawShipSection(shipIndex, i,
                                    shipCoords[i][0], shipCoords[i][1], g);

                    // Don't draw a graphic
                    drawGraphic = false;
                }
            }
        }

        if (drawGraphic)
        {
            boolean drawCircle = true;

            int w = this.getWidth();
            int h = this.getHeight();

            // Wipe the panel clean
            g.setColor(getBackground());
            g.fillRect(0, 0, w, h);

            // Draw the border rectangle
            g.setColor(Color.black);

            if ((panelCoords[0] != 9) && (panelCoords[1] != 9))
            {
                g.drawRect(0, 0, w, h);
            }
            else if ((panelCoords[0] != 9) && (panelCoords[1] == 9))
            {
                g.drawRect(0, 0, w-1, h);
            }
            else if ((panelCoords[0] == 9) && (panelCoords[1] != 9))
            {
                g.drawRect(0, 0, w, h-1);
            }
            else if ((panelCoords[0] == 9) && (panelCoords[1] == 9))
            {
                g.drawRect(0, 0, w-1, h-1);
            }

            // Set the background color, the text, and
            // decide whether to draw a circle
            if (fleet.isHitLocation(panelCoords[0], panelCoords[1]))
            {
                setBackground(Color.RED);
                drawCircle = false;
                text.setText("H");
                text.setForeground(Color.WHITE);
            }
            else if (fleet.isMissLocation(panelCoords[0], panelCoords[1]))
            {
                setBackground(Color.WHITE);
                drawCircle = false;
                text.setText("M");
                text.setForeground(Color.BLACK);
            }
            else
            {
                setBackground(originalBackgroundColor);
                text.setText("");
                text.setForeground(Color.BLACK);
            }

            // If a cirle should be drawn
            if (drawCircle)
            {
                // Put the circle in the center of the panel
                int x = (w - ovalWidth)/2;
                int y = (h - ovalHeight)/2;

                // Draw circle
                g.fillOval(x, y, ovalWidth, ovalHeight);
            }
        }
    }

    // Paint helper method
    private void drawShipSection(int shipIndex, int secIndex,
                                 int row, int col, Graphics g)
    {
        // Has this section of the ship been hit?
        int imgIndex;

        if (false == fleet.isShipHit(shipIndex, row, col))
        {
            imgIndex = REG_IMG;

            // DEVEL
            //System.out.println("REG_IMG");
        }
        else
        {
            imgIndex = HIT_IMG;

            // DEVEL
            //System.out.println("HIT_IMG");
        }

        // Convert the Graphics object to a Graphics2D object
        Graphics2D g2d = (Graphics2D)g;

        // Compute the angle
        double angle = (((fleet.getShipHeading(shipIndex) + 3) % 4) * 90);

        // Rotate the image by the angle
        g2d.rotate(Math.toRadians(angle), w/2, h/2);

        // Get the ship image dimensions
        int siw = bi[imgIndex][shipIndex].getWidth();
        int sih = bi[imgIndex][shipIndex].getHeight();

        // Get the offsets for each coordinate
        int sx1Offset = imgOffset[shipIndex][secIndex][0];
        int sy1Offset = imgOffset[shipIndex][secIndex][1];
        int sx2Offset = imgOffset[shipIndex][secIndex][2];
        int sy2Offset = imgOffset[shipIndex][secIndex][3];

        // Calculate sx1, sy1, sx2, sy2
        int sx1 = ((secIndex)*siw)/fleet.getShipSize(shipIndex) + sx1Offset;
        int sy1 = 0 + sy1Offset;
        int sx2 = ((secIndex+1)*siw)/fleet.getShipSize(shipIndex) + sx2Offset;
        int sy2 = sih + sy2Offset;

        // Set the composite (make less or more opaque)
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                              fleet.getShipOpacity(shipIndex)));

        // Draw the ship image
        g2d.drawImage(bi[imgIndex][shipIndex],
                      0,   //dx1
                      0,   //dy1
                      w,   //dx2
                      h,   //dy2
                      sx1, //sx1
                      sy1, //sy1
                      sx2, //sx2
                      sy2, //sy2
                      Color.white,
                      null); //ImageObserver
    }
}
