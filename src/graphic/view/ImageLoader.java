package graphic.view;

import model.hero.HeroType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ImageLoader {
    private int heroType;
    private BufferedImage heroForms;
    private final BufferedImage brickAnimation;
    private static final ImageLoader instance = new ImageLoader();

    private ImageLoader() {
        brickAnimation = loadImage("/brick-animation.png");
    }

    public static ImageLoader getInstance() {
        return instance;
    }

    public BufferedImage loadImage(String path) {
        BufferedImage imageToReturn = null;

        try {
            imageToReturn = ImageIO.read(Objects.requireNonNull(getClass().getResource("/graphic/media" + path)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageToReturn;
    }

    public BufferedImage loadImage(File file) {
        BufferedImage imageToReturn = null;

        try {
            imageToReturn = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageToReturn;
    }

    public BufferedImage getSubImage(BufferedImage image, int col, int row, int w, int h) {
        if ((col == 1 || col == 4) && row == 3) {
            return image.getSubimage((col - 1) * 48, 128, w, h);
        }
        return image.getSubimage((col - 1) * 48, (row - 1) * 48, w, h);
    }

    public BufferedImage[] getLeftFrames(int heroForm) {
        BufferedImage[] leftFrames = new BufferedImage[5];
        int col = 1;
        int width = 52, height = 48;

        if (heroForm == 1) {
            col = 4;
            width = 48;
            height = 96;
        } else if (heroForm == 2) {
            col = 7;
            width = 48;
            height = 96;
        }

        for (int i = 0; i < 5; i++) {
            leftFrames[i] = heroForms.getSubimage((col - 1) * width, (i) * height, width, height);
        }
        return leftFrames;
    }

    public BufferedImage[] getRightFrames(int heroForm) {
        BufferedImage[] rightFrames = new BufferedImage[5];
        int col = 2;
        int width = 52, height = 48;

        if (heroForm == 1) {
            col = 5;
            width = 48;
            height = 96;
        } else if (heroForm == 2) {
            col = 8;
            width = 48;
            height = 96;
        }

        for (int i = 0; i < 5; i++) {
            rightFrames[i] = heroForms.getSubimage((col - 1) * width, (i) * height, width, height);
        }
        return rightFrames;
    }

    public BufferedImage[] getBrickFrames() {
        BufferedImage[] frames = new BufferedImage[4];
        for (int i = 0; i < 4; i++) {
            frames[i] = brickAnimation.getSubimage(i * 105, 0, 105, 105);
        }
        return frames;
    }

    public BufferedImage getFireballImage(int type) {
            return getSubImage(loadImage("/sprite.png"), 3, 4, 24, 24);
    }


    public void setHeroType(int heroType) {
        this.heroType = heroType;
        setHeroForms(heroType);
    }

    private void setHeroForms(int heroType) {
        switch (heroType) {
            case HeroType.LUIGI:
                heroForms = loadImage("/luigi-forms.png");
                break;
            case HeroType.ROSS:
                heroForms = loadImage("/ross-forms.png");
                break;
            case HeroType.TOAD:
                heroForms = loadImage("/toad-forms.png");
                break;
            case HeroType.PRINCE_PEACH:
                heroForms = loadImage("/prince peach-forms.png");
                break;
            default:
                heroForms = loadImage("/mario-forms.png");
                break;
        }
    }
}
