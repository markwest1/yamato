import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
 
public class ImageMovePanel extends JPanel
{
    // To make this class serializable
    private static final long serialVersionUID = 1L;

	private BufferedImage img;
	private Rectangle selection;
    private double rotationAngle;
 
    public ImageMovePanel()
    {
        setOpaque(false);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setRotationAngle(0);
    }

	public void setImage(Point p, BufferedImage i)
	{
		img = i;

        int w = 0;
        int h = 0;

        if (img != null)
        {
            w = img.getWidth();
            h = img.getHeight();
        }

		selection = new Rectangle(p.x, p.y, w, h);

		repaint();
	}
 
    public void setRotationAngle(double angle)
    {
        rotationAngle = angle;
    }

	public void setSelectionBounds(Rectangle rect)
	{
		selection = rect;
		repaint();
	}

	public void paintComponent(Graphics gr)
	{
        // Convert the Graphics object to a Graphics2D object
        Graphics2D g2d = (Graphics2D)gr;

        // Rotate the image
        g2d.rotate(Math.toRadians(rotationAngle),
                   img.getWidth()/2, img.getHeight()/2);

		if ((img != null) && (selection != null))
		{
            // Declare variables
            int dx1 = selection.x;
            int dy1 = selection.y;
            int dx2 = selection.x + selection.width;
            int dy2 = selection.y + selection.height;

            // Draw the image
			g2d.drawImage(img,
                          dx1,             //dx1
                          dy1,             //dy1
                          dx2,             //dx2
                          dy2,             //dy2
                          0,               //sx1
                          0,               //sy1
                          img.getWidth(),  //sx2
                          img.getHeight(), //sy2
                          this);
		}
	}
}

