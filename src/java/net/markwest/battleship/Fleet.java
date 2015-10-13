class Fleet
{
    // Heading indexes
    public static final int NORTH = 0;  //   0 degrees
    public static final int EAST  = 1;  //  90 degrees
    public static final int SOUTH = 2;  // 180 degrees 
    public static final int WEST  = 3;  // 270 degrees

    // Ship type indexes
    public static final int PTBOAT     = 0;
    public static final int UBOAT      = 1;
    public static final int DESTROYER  = 2;
    public static final int BATTLESHIP = 3;
    public static final int CARRIER    = 4;

    // Static final variables
    public static final int NUMSHIPS        = 5;
    public static final int NUMROWS         = 10;
    public static final int NUMCOLS         = 10;
    public static final int LARGESTSHIPSIZE = 5;

    // Location tables
    private boolean[][] missLocations = new boolean[NUMROWS][NUMCOLS];
    private boolean[][] hitLocations = new boolean[NUMROWS][NUMCOLS];

    // A fleet is a group of ships
    private Ship[] ships = new Ship[NUMSHIPS];

    // Size of each ship corresponds to ship indexes above
    private int[] shipSize = {2, 3, 3, 4, 5};

    // Constructor
    public Fleet()
    {
        // Setup the fleet
        for (int shipType = PTBOAT; shipType <= CARRIER; shipType++)
        {
            // Create one of each type of ship
            ships[shipType] = new Ship(shipSize[shipType]);

            // Set the ship in dry dock
            setShipLocation(shipType, -1, -1);
        }

        resetLocationTables();
    }

    public int getShipHeading(int shipIndex)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return -1;
        }

        return ships[shipIndex].getHeading();
    }

    public int[][] getShipLocation(int shipIndex)
    {
        int[][] coords = new int[ships[shipIndex].getSize()][2];

        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);

            for (int i = 0; i < ships[shipIndex].getSize(); i++)
            {
                coords[i][0] = -1;
                coords[i][1] = -1;
            }
        }
        else
        {
            for (int i = 0; i < ships[shipIndex].getSize(); i++)
            {
                coords[i] = ships[shipIndex].getCoordinates(i);
            }
        }

        return coords;
    }

    public boolean isShipLocation(int shipIndex, int row, int col)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return false;
        }

        if ((row < 0) || (row >= NUMROWS) || (col < 0) || (col >= NUMCOLS))
        {
            return false;
        }

        return ships[shipIndex].isLocation(row, col);
    }

    public boolean setShipLocation(int shipIndex, int row, int col)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return false;
        }

        if ((row < 0) || (col < 0))
        {
            for (int i = 0; i < getShipSize(shipIndex); i++)
            {
                ships[shipIndex].setCoordinates(i, -1, -1);
            }

            return true;
        }

        // If set, the ship will collide with another ship
        boolean collision = false;

        // Get the ship size
        int size = getShipSize(shipIndex);

        // Set new coordinates based on ship heading
        if (getShipHeading(shipIndex) == EAST)
        {
            // Don't allow the ship to straddle the eastern border
            if ((col + (size-1)) > 9)
            {
                col = 0;
            }

            // Check each ship
            for (int si = 0; si < NUMSHIPS; si++)
            {
                // Except the ship being placed
                if (si != shipIndex)
                {
                    // Loop through each new point
                    for (int i = 0; i < size ; i++)
                    {
                        // If location is already occupied
                        if (true == isShipLocation(si, row, col+i))
                        {
                            collision = true;
                            System.out.println("Collision with a " +
                                               getShipName(si));
                            break;
                        }
                    }
                }

                if (true == collision)
                {
                    break;
                }
            }

            if (false == collision)
            {
                for (int i = 0; i < size ; i++)
                {
                    ships[shipIndex].setCoordinates(i, row, col+i);
                }
            }
        }
        else if (getShipHeading(shipIndex) == WEST)
        {
            // Don't allow ship to straddle the western border
            if ((col - (size-1)) < 0)
            {
                col = 9;
            }

            // Check each ship
            for (int si = 0; si < NUMSHIPS; si++)
            {
                // Except the ship being placed
                if (si != shipIndex)
                {
                    // Loop through each new point
                    for (int i = 0; i < size ; i++)
                    {
                        // If location is already occupied
                        if (true == isShipLocation(si, row, col-i))
                        {
                            collision = true;
                            System.out.println("Collision with a " +
                                               getShipName(si));
                            break;
                        }
                    }
                }

                if (true == collision)
                {
                    break;
                }
            }

            if (false == collision)
            {
                for (int i = 0; i < size; i++)
                {
                    ships[shipIndex].setCoordinates(i, row, col-i);
                }
            }
        }
        else if (getShipHeading(shipIndex) == NORTH)
        {
            // Don't allow the ship to straddle the northern border
            if ((row - (size-1)) < 0)
            {
                row = 9;
            }

            // Check each ship
            for (int si = 0; si < NUMSHIPS; si++)
            {
                // Except the ship being placed
                if (si != shipIndex)
                {
                    // Loop through each new point
                    for (int i = 0; i < size ; i++)
                    {
                        // If location is already occupied
                        if (true == isShipLocation(si, row-i, col))
                        {
                            collision = true;
                            System.out.println("Collision with a " +
                                               getShipName(si));
                            break;
                        }
                    }
                }

                if (true == collision)
                {
                    break;
                }
            }

            if (false == collision)
            {
                for (int i = 0; i < size; i++)
                {
                    ships[shipIndex].setCoordinates(i, row-i, col);
                }
            }
        }
        else if (getShipHeading(shipIndex) == SOUTH)
        {
            // Don't allow the ship to stradle the southern border
            if ((row + (size-1)) > 9)
            {
                row = 0;
            }

            // Check each ship
            for (int si = 0; si < NUMSHIPS; si++)
            {
                // Except the ship being placed
                if (si != shipIndex)
                {
                    // Loop through each new point
                    for (int i = 0; i < size ; i++)
                    {
                        // If location is already occupied
                        if (true == isShipLocation(si, row+i, col))
                        {
                            collision = true;
                            System.out.println("Collision with a " +
                                               getShipName(si));
                            break;
                        }
                    }
                }

                if (true == collision)
                {
                    break;
                }
            }

            if (false == collision)
            {
                for (int i = 0; i < size; i++)
                {
                    ships[shipIndex].setCoordinates(i, row+i, col);
                }
            }
        }

        if (collision == true)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public void setShipHeading(int shipIndex, int newHeading)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return;
        }

        int size = getShipSize(shipIndex);
        int[] sternCoords = new int[2];
        sternCoords = ships[shipIndex].getCoordinates(0);

        // If ship is off the visible grid or if border straddle correction is
        // turned off
        if ((sternCoords[0] < 0) && (sternCoords[1] < 0))
        {
            // Set the new heading
            ships[shipIndex].setHeading(newHeading);

            // ... and return
            return;
        }
        
        // If heading is changing by 180 degrees
        if ((getShipHeading(shipIndex) == EAST) && (newHeading == WEST))
        {
            // Switch direction
            sternCoords[1] = sternCoords[1] + (size-1);
        }
        else if ((getShipHeading(shipIndex) == WEST) && (newHeading == EAST))
        {
            // Switch direction
            sternCoords[1] = sternCoords[1] - (size-1);
        }
        else if ((getShipHeading(shipIndex) == NORTH) && (newHeading == SOUTH))
        {
            // Switch direction
            sternCoords[0] = sternCoords[0] - (size-1);
        }
        else if ((getShipHeading(shipIndex) == SOUTH) && (newHeading == NORTH))
        {
            // Switch direction
            sternCoords[0] = sternCoords[0] + (size-1);
        }
        // If heading is changing by 90 degrees
        else if ((getShipHeading(shipIndex) == EAST) && (newHeading == SOUTH))
        {
            // If when heading SOUTH the ship will straddle a border
            if ((sternCoords[0] + (size-1)) > 9)
            {
                // If when heading WEST the ship will straddle a border
                if ((sternCoords[1] - (size-1)) < 0)
                {
                    newHeading = NORTH;
                }
                else
                {
                    newHeading = WEST;
                }
            }
        }
        else if ((getShipHeading(shipIndex) == EAST) && (newHeading == NORTH))
        {
            // If when heading NORTH the ship will straddle a border
            if ((sternCoords[0] - (size-1)) < 0)
            {
                // If when heading WEST the ship will straddle a border
                if ((sternCoords[1] - (size-1)) < 0)
                {
                    newHeading = SOUTH;
                }
                else
                {
                    newHeading = WEST;
                }
            }
        }
        else if ((getShipHeading(shipIndex) == SOUTH) && (newHeading == EAST))
        {
            // If when heading EAST the ship will straddle a border
            if ((sternCoords[1] + (size-1)) > 9)
            {
                // If when heading NORTH the ship will straddle a border
                if ((sternCoords[0] - (size-1)) < 0)
                {
                    newHeading = WEST;
                }
                else
                {
                    newHeading = NORTH;
                }
            }
        }
        else if ((getShipHeading(shipIndex) == SOUTH) && (newHeading == WEST))
        {
            // If when heading WEST the ship will straddle a border
            if ((sternCoords[1] - (size-1)) < 0)
            {
                // If when heading NORTH the ship will straddle a border
                if ((sternCoords[0] - (size-1)) < 0)
                {
                    newHeading = EAST;
                }
                else
                {
                    newHeading = NORTH;
                }
            }
        }
        else if ((getShipHeading(shipIndex) == WEST) && (newHeading == NORTH))
        {
            // If when heading NORTH the ship will straddle a border
            if ((sternCoords[0] - (size-1)) < 0)
            {
                // If when heading EAST the ship will straddle a border
                if ((sternCoords[1] + (size-1)) > 9)
                {
                    newHeading = SOUTH;
                }
                else
                {
                    newHeading = EAST;
                }
            }
        }
        else if ((getShipHeading(shipIndex) == WEST) && (newHeading == SOUTH))
        {
            // If when heading SOUTH the ship will straddle a border
            if ((sternCoords[0] + (size-1)) > 9)
            {
                // If when heading EAST the ship will straddle a border
                if ((sternCoords[1] + (size-1)) > 9)
                {
                    newHeading = NORTH;
                }
                else
                {
                    newHeading = EAST;
                }
            }
        }
        else if ((getShipHeading(shipIndex) == NORTH) && (newHeading == WEST))
        {
            // If when heading WEST the ship will straddle a border
            if ((sternCoords[1] - (size-1)) < 0)
            {
                // If when heading SOUTH the ship will straddle a border
                if ((sternCoords[0] + (size-1)) > 9)
                {
                    newHeading = EAST;
                }
                else
                {
                    newHeading = SOUTH;
                }
            }
        }
        else if ((getShipHeading(shipIndex) == NORTH) && (newHeading == EAST))
        {
            // If when heading EAST the ship will straddle a border
            if ((sternCoords[1] + (size-1)) > 9)
            {
                // If when heading SOUTH the ship will straddle a border
                if ((sternCoords[0] + (size-1)) > 9)
                {
                    newHeading = WEST;
                }
                else
                {
                    newHeading = SOUTH;
                }
            }
        }
        
        // Set the new heading
        ships[shipIndex].setHeading(newHeading);

        // Set new coordinates
        setShipLocation(shipIndex, sternCoords[0], sternCoords[1]);
    }

    public int getShipSize(int shipIndex)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return -1;
        }

        return ships[shipIndex].getSize();
    }

    public void setShipIsMobile(int shipIndex, boolean m)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return;
        }

        int[] sternCoords = new int[2];
        sternCoords = ships[shipIndex].getCoordinates(0);

        // If this is the first time the ship is mobile
        if ((sternCoords[0] < 0) && (sternCoords[1] < 0))
        {
            // Locate the ship in the top left corner
            setShipLocation(shipIndex, 0, 0);
        }

        ships[shipIndex].setMobility(m);
    }

    // Set a ship section as hit
    public void setShipIsHit(int shipIndex, int row, int col)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return;
        }

        ships[shipIndex].setHitSection(row, col);
    }

    // Set an all ship sections as hit
    public void setShipIsSunk(int shipIndex)
    {
        int shipLocation[][] = getShipLocation(shipIndex);

        for (int i = 0; i < getShipSize(shipIndex); i++)
        {
            setShipIsHit(shipIndex, shipLocation[i][0], shipLocation[i][1]);
        }
    }

    // Set ship is repaired
    public void setShipIsRepaired(int shipIndex, int row, int col)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return;
        }

        ships[shipIndex].unsetHitSection(row, col);
    }

    // Find out if a ship is hit at a certain location
    public boolean isShipHit(int shipIndex, int row, int col)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return false;
        }

        return ships[shipIndex].isHitCoordinates(row, col);
    }

    public boolean isShipSunk(int shipIndex)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return false;
        }
        
        return ships[shipIndex].isSunk();
    }

    public boolean shipIsMobile(int shipIndex)
    {
        if ((shipIndex < 0) || (shipIndex >= NUMSHIPS))
        {
            System.out.println("ERROR: invalid shipIndex, " + shipIndex);
            return false;
        }

        return ships[shipIndex].isMobile();
    }

    public void resetLocationTables()
    {
        for (int i = 0; i < NUMROWS; i++)
        {
            for (int j = 0; j < NUMCOLS; j++)
            {
                hitLocations[i][j] = false;
                missLocations[i][j] = false;
            }
        }
    }

    public void setMissLocation(int row, int col)
    {
        unsetHitLocation(row, col);
        missLocations[row][col] = true;
    }

    public void setHitLocation(int row, int col)
    {
        unsetMissLocation(row, col);
        hitLocations[row][col] = true;
    }

    public boolean isHitLocation(int row, int col)
    {
        return hitLocations[row][col];
    }

    public boolean isMissLocation(int row, int col)
    {
        return missLocations[row][col];
    }

    public void unsetHitLocation(int row, int col)
    {
        hitLocations[row][col] = false;
    }

    public void unsetMissLocation(int row, int col)
    {
        missLocations[row][col] = false;
    }

    public boolean isFleetDestroyed()
    {
        boolean fleetDestroyed = true;

        for (int shipType = PTBOAT; shipType <= CARRIER; shipType++)
        {
            if (false == ships[shipType].isSunk())
            {
                fleetDestroyed = false;
                break;
            }
        }

        return fleetDestroyed;
    }

    public void setShipOpacity(int shipIndex, float op)
    {
        ships[shipIndex].setOpacityFactor(op);
    }

    public float getShipOpacity(int shipIndex)
    {
        return ships[shipIndex].getOpacityFactor();
    }

    public void repairFleet()
    {
        for (int shipType = PTBOAT; shipType <= CARRIER; shipType++)
        {
            // Set ship in dry dock
            setShipLocation(shipType, -1, -1);

            // Repair damage
            ships[shipType].repairDamage();

            // Set the ship heading
            setShipHeading(shipType, NORTH);
        }

        // Remove all hits and misses
        resetLocationTables();
    }

    // Miscellaneous methods
    public String getShipName(int s)
    {
        if (s == CARRIER)
        {
            return ("CARRIER");
        }
        else if (s == BATTLESHIP)
        {
            return ("BATTLESHIP");
        }
        else if (s == DESTROYER)
        {
            return ("DESTROYER");
        }
        else if (s == UBOAT)
        {
            return ("UBOAT");
        }
        else if (s == PTBOAT)
        {
            return ("PTBOAT");
        }

        return ("NOSHIP");
    }
}
