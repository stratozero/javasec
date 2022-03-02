package it.mormao;

import java.util.Arrays;

/* This class is a perfect candidate to be transformed into a record-class */
public class StaticCharWiper implements CharWiper{

	private final char filler;

	public StaticCharWiper(char filler) {
		this.filler = filler;
	}

	@Override
	public void wipe(char[] original) {
		Arrays.fill(original, filler);
	}
}
