package it.mormao;

import java.util.Arrays;

public record StaticCharWiper(char filler) implements CharWiper {
	@Override
	public void wipe(char[] original) {
		Arrays.fill(original, filler);
	}
}
