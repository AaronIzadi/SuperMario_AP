package model.brick;

import java.awt.image.BufferedImage;

public class Border extends Brick{

    public Border(double x, double y, BufferedImage style) {
        super(x, y, style);
        setBreakable(false);
        setEmpty(true);
        setDimension(48, 48);
    }
}
