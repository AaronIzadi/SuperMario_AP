package SuperMario.logic;

import SuperMario.graphic.manager.Camera;
import SuperMario.graphic.manager.InputManager;
import SuperMario.graphic.view.animation.Animation;
import SuperMario.graphic.view.states.*;
import SuperMario.input.ImageLoader;
import SuperMario.input.SoundManager;

import SuperMario.graphic.view.animation.UIManager;
import SuperMario.model.map.Map;
import SuperMario.model.hero.Hero;
import SuperMario.model.hero.HeroForm;


import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class GameEngine implements Runnable {

    private static final GameEngine instance = new GameEngine();

    private final static int WIDTH = 1268, HEIGHT = 708;
    private boolean isMute = false;
    private UserData userData;
    private MapManager mapManager;
    private UIManager uiManager;
    private SoundManager soundManager;
    private GameState gameState;
    private boolean isRunning;
    private Camera camera;
    private ImageLoader imageLoader;
    private Thread thread;
    private StartScreenSelection startScreenSelection = StartScreenSelection.LOAD_SCREEN;
    private LoadGameScreenSelection loadGameScreenSelection = LoadGameScreenSelection.NEW_GAME;
    private PauseScreenSelection pauseScreenSelection = PauseScreenSelection.GO_TO_MAIN_MENU;
    private StoreScreenSelection storeScreenSelection = StoreScreenSelection.MARIO;
    private int selectedMap = 0;

    private GameEngine() {
        initial();
    }

    public static GameEngine getInstance() {
        return instance;
    }

    private void initial() {
        imageLoader = ImageLoader.getInstance();
        InputManager inputManager = InputManager.getInstance();
        gameState = GameState.START_SCREEN;
        camera = new Camera();
        uiManager = new UIManager(this, WIDTH, HEIGHT);
        soundManager = new SoundManager();
        mapManager = MapManager.getInstance();
        userData = UserData.getInstance();

        JFrame frame = new JFrame("Super Mario Bros.");
        frame.setIconImage(imageLoader.getIcon());
        frame.add(uiManager);
        frame.addKeyListener(inputManager);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        start();
    }

    private synchronized void start() {
        if (isRunning)
            return;

        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    private void reset() {
        resetCamera();
        setGameState(GameState.START_SCREEN);
        pauseBackGround();
    }

    public void resetCamera() {
        camera = new Camera();
        soundManager.restartBackground();
    }

    public void selectMapViaKeyboard() {
        String path = uiManager.selectMapViaKeyboard(selectedMap);
        if (path != null) {
            createMap(path);
        }
    }

    public void changeSelectedMap(boolean up) {
        selectedMap = uiManager.changeSelectedMap(selectedMap, up);
    }

    private void createMap(String path) {
        boolean loaded = mapManager.createMap(path);
        userData.setHero(mapManager.getHero());
        userData.setMap(mapManager.getMap());
        if (loaded) {
            setGameState(GameState.RUNNING);
            soundManager.restartBackground();
        } else {
            setGameState(GameState.START_SCREEN);
        }
    }

    private Map createMap(String path, Hero hero) {
        boolean loaded = mapManager.createMap(path, hero);
        userData.setHero(hero);
        userData.setMap(mapManager.getMap());
        if (loaded) {
            setGameState(GameState.RUNNING);
            soundManager.restartBackground();
            return mapManager.getMap();
        } else {
            setGameState(GameState.START_SCREEN);
        }
        return null;
    }

    @Override
    public void run() {
        renderLoop();
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();

        while (isRunning && !thread.isInterrupted()) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            while (delta >= 1) {
                if (gameState == GameState.RUNNING) {
                    gameLoop();
                }
                delta--;
            }

            if (gameState != GameState.RUNNING) {
                timer = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                mapManager.updateTime();
            }
        }
    }

    private void renderLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(12);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    updateCamera();
                    if (userData.getHero().getX() <= this.getCameraLocation().getX() && userData.getHero().getVelX() < 0) {
                        userData.getHero().setVelX(0);
                        userData.getHero().setX(this.getCameraLocation().getX());
                    }

                } catch (Exception ignored) {

                }

                render();
            }
        }).start();
    }

    private void render() {
        uiManager.repaint();
    }

    private void gameLoop() {
        updateLocations();
        checkCollisions();

        if (isGameOver()) {
            setGameState(GameState.GAME_OVER);
            soundManager.pauseBackground();
        }

        int missionPassed = passMission();
        if (missionPassed > -1) {
            soundManager.pauseBackground();
            mapManager.acquirePoints(missionPassed);
        } else if (mapManager.endLevel()) {
            soundManager.pauseBackground();
            playStageClear();
            setGameState(GameState.MISSION_PASSED);
        }
    }

    private void updateCamera() {
        Hero hero = mapManager.getHero();
        double heroVelocityX = hero.getVelX();
        double shiftAmount = 0;

        if (heroVelocityX > 0 && hero.getX() - 600 > camera.getX()) {
            shiftAmount = heroVelocityX;
        }

        camera.moveCam(shiftAmount, 0);
    }

    private void updateLocations() {
        mapManager.updateLocations();
    }

    private void checkCollisions() {
        mapManager.checkCollisions(this);
    }

    private void checkAndThenLoadFile(int fileId) throws IOException {
        if (2 < fileId || fileId < 0) {
            startGame();
            return;
        }
        if (!userData.getLoadGameRepository().isFileEmpty(fileId)) {
            loadGame(fileId);
            setGameState(GameState.STORE_SCREEN);
        } else {
            startGame();
        }
    }

    private void checkIfThenBuyHero(StoreScreenSelection selection) {
        int heroId = selection.getColumnNumber();
        Hero hero = userData.getHero();

        if (userData.getTypesOwned()[heroId]) {
            loadNewHero(heroId);
        } else if (hero.getCoins() >= selection.getHeroPrice()) {
            hero.setCoins(hero.getCoins() - selection.getHeroPrice());
            buyAndLoadNewHero(heroId);
        }
        setGameState(GameState.RUNNING);
    }

    public void receiveInput() throws IOException {

        InputManager inputMgr = InputManager.getInstance();

        if (gameState == GameState.START_SCREEN) {

            if (inputMgr.isEnter()) {
                switch (startScreenSelection) {
                    case LOAD_SCREEN:
                        setGameState(GameState.LOAD_GAME);
                        break;
                    case VIEW_ABOUT:
                        setGameState(GameState.ABOUT_SCREEN);
                        break;
                    case VIEW_HELP:
                        setGameState(GameState.HELP_SCREEN);
                        break;
                }
            } else if (inputMgr.isUp()) {
                selectOption(true);
            } else if (inputMgr.isDown()) {
                selectOption(false);
            }

        } else if (gameState == GameState.LOAD_GAME) {

            if (inputMgr.isEnter()) {
                switch (loadGameScreenSelection) {
                    case NEW_GAME:
                        startGame();
                        break;
                    case LOAD_GAME_1:
                        checkAndThenLoadFile(0);
                        break;
                    case LOAD_GAME_2:
                        checkAndThenLoadFile(1);
                        break;
                    case LOAD_GAME_3:
                        checkAndThenLoadFile(2);
                        break;
                }
            } else if (inputMgr.isUp()) {
                selectToStartOrLoad(true);
            } else if (inputMgr.isDown()) {
                selectToStartOrLoad(false);
            }

        } else if (gameState == GameState.STORE_SCREEN) {

            if (inputMgr.isEnter()) {
                checkIfThenBuyHero(storeScreenSelection);
            } else if (inputMgr.isLeft()) {
                selectHero(true);
            } else if (inputMgr.isRight()) {
                selectHero(false);
            }

        } else if (gameState == GameState.MAP_SELECTION) {

            if (inputMgr.isEnter()) {
                selectMapViaKeyboard();
            } else if (inputMgr.isUp()) {
                changeSelectedMap(true);
            } else if (inputMgr.isDown()) {
                changeSelectedMap(false);
            }

        } else if (gameState == GameState.RUNNING) {

            if (inputMgr.isUp()) {
                userData.getHero().jump();
            } else if (inputMgr.isRight()) {
                userData.getHero().move(true, camera);
            } else if (inputMgr.isLeft()) {
                userData.getHero().move(false, camera);
            } else if (inputMgr.isEmpty()) {
                userData.getHero().setVelX(0);
            } else if (inputMgr.isSpace()) {
                mapManager.fire(this);
            } else if (inputMgr.isUpAndDownSelected()) {
                //TODO
            } else if (inputMgr.isEscape()) {
                pauseGame();
            }

        } else if (gameState == GameState.PAUSED) {

            if (inputMgr.isEnter()) {
                switch (pauseScreenSelection) {
                    case GO_TO_MAIN_MENU:
                        saveGame(0);
                        gameState = GameState.START_SCREEN;
                        break;
                    case SAVE_ON_FILE_1:
                        saveGame(0);
                        break;
                    case SAVE_ON_FILE_2:
                        saveGame(1);
                        break;
                    case SAVE_ON_FILE_3:
                        saveGame(2);
                        break;
                    case MUTE_BACKGROUND_SOUND: {
                        if (isMute) {
                            resumeBackground();
                        } else {
                            pauseBackGround();
                        }
                        gameState = GameState.RUNNING;
                        break;
                    }
                }
            } else if (inputMgr.isUp()) {
                selectToResume(true);
            } else if (inputMgr.isDown()) {
                selectToResume(false);
            } else if (inputMgr.isEscape()) {
                pauseGame();
            }

        } else if (gameState == GameState.GAME_OVER && inputMgr.isEscape()) {
            reset();
        } else if (gameState == GameState.MISSION_PASSED && inputMgr.isEscape()) {
            pauseBackGround();
            reset();
        } else {
            if (inputMgr.isEscape()) {
                setGameState(GameState.START_SCREEN);
            }
        }
    }

    private void buyAndLoadNewHero(int type) {
        userData.getTypesOwned()[type] = true;
        loadNewHero(type);
    }

    private void loadNewHero(int type) {
        mapManager.getHero().setType(type);
        mapManager.getHero().getHeroForm().setHeroType(type);
        imageLoader.setHeroType(type);
        userData.getHero().setType(type);
        int heroFormId = userData.getHero().isSuper() ? 1 : 0;
        userData.getHero().setHeroForm(
                new HeroForm(
                        new Animation(
                                imageLoader.getLeftFrames(heroFormId),
                                imageLoader.getRightFrames(heroFormId)),
                        userData.getHero().isSuper(),
                        userData.getHero().getHeroForm().ifCanShootFire(),
                        type));
    }

    private void selectToStartOrLoad(boolean selectUp) {
        loadGameScreenSelection = loadGameScreenSelection.select(selectUp);
    }

    private void loadGame(int fileNumber) throws IOException {
        userData = userData.getLoadGameRepository().getUserData(fileNumber);
        userData.setHero(userData.getHero());
        userData.setTypesOwned(userData.getTypesOwned());
        mapManager.setMap(createMap(userData.getMapPath(), userData.getHero()));
        mapManager.setHero(userData.getHero());
    }

    private void saveGame(int fileNumber) {
        userData.setMapPath(mapManager.getMap().getPath());
        userData.getSaveGameRepository().addUserData(userData, fileNumber);
    }

    private void selectOption(boolean selectUp) {
        startScreenSelection = startScreenSelection.select(selectUp);
    }

    private void selectToResume(boolean selectUp) {
        pauseScreenSelection = pauseScreenSelection.select(selectUp);
    }

    private void selectHero(boolean selectLeft) {
        storeScreenSelection = storeScreenSelection.select(selectLeft);
    }

    private void startGame() {
        if (gameState != GameState.GAME_OVER) {
            setGameState(GameState.MAP_SELECTION);
        }
    }

    private void pauseGame() {
        if (gameState == GameState.RUNNING) {
            setGameState(GameState.PAUSED);
            soundManager.pauseBackground();
        } else if (gameState == GameState.PAUSED) {
            setGameState(GameState.RUNNING);
            soundManager.resumeBackground();
        }
    }

    public void shakeCamera() {
        camera.shakeCamera();
    }

    private boolean isGameOver() {
        if (gameState == GameState.RUNNING) {
            return mapManager.isGameOver();
        }
        return false;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public GameState getGameState() {
        return gameState;
    }

    public StartScreenSelection getStartScreenSelection() {
        return startScreenSelection;
    }

    public LoadGameScreenSelection getLoadGameScreenSelection() {
        return loadGameScreenSelection;
    }

    public PauseScreenSelection getPauseScreenSelection() {
        return pauseScreenSelection;
    }

    public StoreScreenSelection getStoreScreenSelection() {
        return storeScreenSelection;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public int getScore() {
        return mapManager.getScore();
    }

    public int getRemainingLives() {
        return mapManager.getRemainingLives();
    }

    public int getCoins() {
        return mapManager.getCoins();
    }

    public int getSelectedMap() {
        return selectedMap;
    }

    public void drawMap(Graphics2D g2) {
        mapManager.drawMap(g2);
    }

    public Point getCameraLocation() {
        return new Point((int) camera.getX(), (int) camera.getY());
    }

    private int passMission() {
        return mapManager.passMission();
    }

    public void playCoin() {
        soundManager.playCoin();
    }

    public void playOneUp() {
        soundManager.playOneUp();
    }

    public void playPowerUp() {
        soundManager.playPowerUp();
    }

    public void playHeroDies() {
        soundManager.playHeroDies();
    }

    public void playGameOver() {
        soundManager.playGameOver();
    }

    public void playJump() {
        soundManager.playJump();
    }

    public void playFireball() {
        soundManager.playFireball();
    }

    public void playHeroFalls() {
        soundManager.playHeroFalls();
    }

    public void playKickEnemy() {
        soundManager.playKickEnemy();
    }

    public void playBreakBrick() {
        soundManager.playBreakBrick();
    }

    public void playStageClear() {
        soundManager.playStageClear();
    }

    public void playFlagPole() {
        soundManager.playFlagPole();
    }

    public void playSuperStar() {
        soundManager.playSuperStar();
    }

    public void pauseBackGround() {
        soundManager.pauseBackground();
        isMute = true;
    }

    public void resumeBackground() {
        soundManager.resumeBackground();
        isMute = false;
    }

    public void playStomp() {
        soundManager.playStomp();
    }


    public int getRemainingTime() {
        return mapManager.getRemainingTime();
    }

    public UserData getUserData() {
        return userData;
    }

    public boolean isMute() {
        return isMute;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public static void main(String... args) {

    }

}