package me.incognitojam.kahootj.actionprovider;

import me.incognitojam.kahootj.Game;

import java.util.Random;

public class RandomActionProvider implements IActionProvider {

    @Override
    public int getChoice(Game game) {
        int range = 2;
        if (game.optionFourValid) range = 4;
        else if (game.optionThreeValid) range = 3;
        return new Random().nextInt(range);
    }

}
