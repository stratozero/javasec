package it.mormao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

public class ScopedStringTest {
	@Test
	void testWiping(){
		final char[] testPassword = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd'};
		try(ScopedString s = ScopedString.from(testPassword).build()){
			// test that in the try-with-resources the string is yet valid and that the 'toString()' method returns and obfuscated string
			Assertions.assertNotEquals(String.valueOf(testPassword), s.toString());
			Assertions.assertArrayEquals(testPassword, s.getContent());
		}
		final char[] defaultObfuscated = new char[testPassword.length];
		Arrays.fill(defaultObfuscated, '*');
		final ScopedString closed = ScopedString.from(testPassword).build();
		closed.close();
		Assertions.assertThrows(IllegalStateException.class, closed::getContent);
		Assertions.assertEquals(String.valueOf(defaultObfuscated), closed.toString());
	}

	@Test
	void testCustomWiping1(){
		final char[] testPassword = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd'};
		try(ScopedString s = ScopedString.from(testPassword).withWiper('-').build()){
			// test that in the try-with-resources the string is yet valid and that the 'toString()' method returns and obfuscated string
			Assertions.assertNotEquals(String.valueOf(testPassword), s.toString());
			Assertions.assertArrayEquals(testPassword, s.getContent());
		}
		final char[] defaultObfuscated = new char[testPassword.length];
		Arrays.fill(defaultObfuscated, '*');
		final ScopedString closed = ScopedString.from(testPassword).build();
		closed.close();
		Assertions.assertThrows(IllegalStateException.class, closed::getContent);
		Assertions.assertEquals(String.valueOf(defaultObfuscated), closed.toString());
	}

	@Test
	void testCustomWiping2(){
		final char[] testPassword = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd'};
		final CharWiper wiper = chars -> {
			final Random random = new Random();
			for (int i = 0; i < chars.length; i++)
				chars[i] = (char) random.nextInt();
		};
		try(ScopedString s = ScopedString.from(testPassword).withWiper(wiper).build()){
			// test that in the try-with-resources the string is yet valid and that the 'toString()' method returns and obfuscated string
			Assertions.assertNotEquals(String.valueOf(testPassword), s.toString());
			Assertions.assertArrayEquals(testPassword, s.getContent());
		}
		final char[] defaultObfuscated = new char[testPassword.length];
		Arrays.fill(defaultObfuscated, '*');
		final ScopedString closed = ScopedString.from(testPassword).build();
		closed.close();
		Assertions.assertThrows(IllegalStateException.class, closed::getContent);
		Assertions.assertEquals(String.valueOf(defaultObfuscated), closed.toString());
	}

	@Test
	void compareScopedStrings(){
		assertSorted("a", "b");
		assertSorted("a", "aa");
		assertSorted("aaa", "ccc");
		assertSorted("abcdefg", "abcdefg", SortingResult.EQ);
		assertSorted("zyxwvutsrqponm", "", SortingResult.GT);
		assertSorted("zzzzzz", "zzz", SortingResult.GT);

		final ScopedString
				  a = ScopedString.from("test1".toCharArray()).build(),
				  b = ScopedString.from("test2".toCharArray()).build();
		assertSorted(a, a, SortingResult.EQ);
		assertSorted(a, b);

		a.close();

		Assertions.assertThrows(IllegalStateException.class, () -> assertSorted(a, b));
		Assertions.assertThrows(IllegalStateException.class, () -> assertSorted(b, a, SortingResult.GT));
	}

	static void assertSorted(final String low, final String high){
		assertSorted(low, high, SortingResult.LT);
	}

	static void assertSorted(final String low, final String high, SortingResult expected){
		assertSorted(low.toCharArray(), high.toCharArray(), expected);
	}

	static void assertSorted(final char[] low, final char[] high, SortingResult expected){
		final ScopedString
				  a = ScopedString.from(low).build(),
				  b = ScopedString.from(high).build();
		assertSorted(a, b, expected);
	}

	static void assertSorted(final ScopedString a, final ScopedString b){
		assertSorted(a, b, SortingResult.LT);
	}

	@SuppressWarnings("SimplifiableAssertion")
	static void assertSorted(ScopedString a, ScopedString b, SortingResult expected){
		switch (expected){
			case LT: Assertions.assertTrue(a.compareTo(b) < 0); return;
			case EQ: Assertions.assertTrue(a.compareTo(b) == 0); return;
			case GT: Assertions.assertTrue(a.compareTo(b) > 0);
		}
	}

	enum SortingResult {
		LT, EQ, GT
	}
}
