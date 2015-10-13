class Ship
{
    private int size;
    private int heading;
    private boolean isMobile = false;
    private int[][] coordinates = new int[Fleet.LARGESTSHIPSIZE][2];
    private boolean[] hits = new boolean[Fleet.LARGESTSHIPSIZE];
    private float opacityFactor = 1.0f;

    // Constructor
    public Ship(int s)
    {
        setSize(s);
        resetCoordinates();
        repairDamage();
    }

    // Set methods
    public void setSize(int s)
    {
        size = s;
    }

    public void repairDamage()
    {
        for (int i = 0; i < getSize(); i++)
        {
            hits[i] = false;
        }
    }

    public void setHeading(int h)
    {
        heading = h;
    }

    public void setCoordinates(int section, int row, int col)
    {
        // If the first section...
        if (section == 0)
        {
            // ... reset the ship's coordinates
            resetCoordinates();
        }

        // Set the coordinate
        coordinates[section][0] = row;
        coordinates[section][1] = col;
    }

    public void setMobility(boolean v)
    {
        isMobile = v;
    }

    // Get methods
    public int getSize()
    {
        return size;
    }

    public int getHeading()
    {
        return heading;
    }

    public int[] getCoordinates(int index)
    {
        return coordinates[index];
    }

    public boolean isMobile()
    {
        return isMobile;
    }

    // Other methods
    private void resetCoordinates()
    {
        for (int i = 0; i < getSize(); i++)
        {
            coordinates[i][0] = -1;
            coordinates[i][1] = -1;
        }
    }

    public boolean isLocation(int row, int col)
    {
        for (int i = 0; i < getSize(); i++)
        {
            if ((coordinates[i][0] == row) &&
                (coordinates[i][1] == col))
            {
                return true;
            }
        }

        return false;
    }

    private int section(int row, int col)
    {
        for (int i = 0; i < getSize(); i++)
        {
            if ((coordinates[i][0] == row) &&
                (coordinates[i][1] == col))
            {
                return i;
            }
        }

        return -1;
    }

    public void printHeading()
    {
        if (heading == Fleet.EAST)
        {
            System.out.println("HEADING: EAST");
        }
        else if (heading == Fleet.SOUTH)
        {
            System.out.println("HEADING: SOUTH");
        }
        else if (heading == Fleet.WEST)
        {
            System.out.println("HEADING: WEST");
        }
        else if (heading == Fleet.NORTH)
        {
            System.out.println("HEADING: NORTH");
        }
    }

    public void setHitSection(int row, int col)
    {
        if (isLocation(row, col))
        {
            hits[section(row, col)] = true;
        }
    }

    public void unsetHitSection(int row, int col)
    {
        if (isLocation(row, col))
        {
            hits[section(row, col)] = false;
        }
    }

    public boolean isHitCoordinates(int row, int col)
    {
        if (true == isLocation(row, col))
        {
            return hits[section(row, col)];
        }

        return false;
    }

    public boolean isSunk()
    {
        for (int i = 0; i < getSize(); i++)
        {
            if (hits[i] == false)
            {
                return false;
            }
        }

        return true;
    }

    public void setOpacityFactor(float o)
    {
        opacityFactor = o;
    }

    public float getOpacityFactor()
    {
        return opacityFactor;
    }
}

