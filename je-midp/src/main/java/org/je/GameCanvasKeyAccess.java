package org.je;

/**
 *
 * @author Andres Navarro
 */

import javax.microedition.lcdui.game.GameCanvas;

public interface GameCanvasKeyAccess {
    
    boolean suppressedKeyEvents(GameCanvas canvas);
    
    void recordKeyPressed(GameCanvas canvas, int gameCode);
    
    void recordKeyReleased(GameCanvas canvas, int gameCode);
    
}
