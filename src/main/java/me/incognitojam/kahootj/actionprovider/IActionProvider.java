package me.incognitojam.kahootj.actionprovider;

import me.incognitojam.kahootj.Game;

public interface IActionProvider {

    /**
     * Get a choice for the current Kahoot game
     * @param game Game context
     * @return integer option between 0 and 3
     */
    int getChoice(Game game);

}
