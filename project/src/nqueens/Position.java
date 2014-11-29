package nqueens;


import java.io.Serializable;

/**
 * An object that describes an queen's position
 */
public class Position implements Serializable {
    private int mX;
    private int mY;

    /**
     * Constructor
     */
    public Position(int x, int y) {
        mX = x;
        mY = y;
    }
    
    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }
    
    @Override
    public String toString() {
        return "(" + mX + ", " + mY + ")";
    }
}
