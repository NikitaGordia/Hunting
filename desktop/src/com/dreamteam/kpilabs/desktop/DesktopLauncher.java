package com.dreamteam.kpilabs.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.dreamteam.kpilabs.HuntingGame;

public class DesktopLauncher {

	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1450;
		config.height = 900;
		new LwjglApplication(new HuntingGame(), config);
	}
}
