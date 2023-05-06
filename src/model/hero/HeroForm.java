package model.hero;

import graphic.view.Animation;
import graphic.view.ImageLoader;
import model.prize.Fireball;

import java.awt.image.BufferedImage;

public class HeroForm {

    public static final int SMALL = 0;
    public static final int SUPER = 1;
    public static final int FIRE = 2;
    private int type;
    private Animation animation;
    private boolean isSuper;
    private boolean CanShootFire;
    private BufferedImage fireballStyle;


    public HeroForm(Animation animation, boolean isSuper, boolean CanShootFire, int type) {
        this.type = type;
        this.animation = animation;
        this.isSuper = isSuper;
        this.CanShootFire = CanShootFire;

        fireballStyle = new ImageLoader(type).getFireballImage(type);
    }

    public BufferedImage getCurrentStyle(boolean toRight, boolean movingInX, boolean movingInY) {

        BufferedImage style;

        if (movingInY && toRight) {
            style = animation.getRightFrames()[0];
        } else if (movingInY) {
            style = animation.getLeftFrames()[0];
        } else if (movingInX) {
            style = animation.animate(5, toRight);
        } else {
            if (toRight) {
                style = animation.getRightFrames()[1];
            } else
                style = animation.getLeftFrames()[1];
        }

        return style;
    }

    public HeroForm onTouchEnemy(ImageLoader imageLoader) {
        BufferedImage[] leftFrames = imageLoader.getLeftFrames(0);
        BufferedImage[] rightFrames = imageLoader.getRightFrames(0);

        Animation newAnimation = new Animation(leftFrames, rightFrames);

        return new HeroForm(newAnimation, false, false, type);
    }

    public Fireball fire(boolean toRight, double x, double y) {
        if (CanShootFire) {
            return new Fireball(x, y + 48, fireballStyle, toRight);
        }
        return null;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public boolean isCanShootFire() {
        return CanShootFire;
    }

    public void setCanShootFire(boolean canShootFire) {
        CanShootFire = canShootFire;
    }

    public void setFireballStyle(BufferedImage fireballStyle) {
        this.fireballStyle = fireballStyle;
    }

    public BufferedImage getFireballStyle() {
        return fireballStyle;
    }

    public boolean isSuper() {
        return isSuper;
    }

    public void setSuper(boolean aSuper) {
        isSuper = aSuper;
    }

    public boolean ifCanShootFire() {
        return CanShootFire;
    }
}
