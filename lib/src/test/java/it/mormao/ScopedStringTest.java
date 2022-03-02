package it.mormao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
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
		assertSorted("a", null, SortingResult.GT); // "a" > null
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
		assertSorted(
				  low  == null ? null : low.toCharArray(),
				  high == null ? null : high.toCharArray(),
				  expected
		);
	}

	static void assertSorted(final char[] low, final char[] high, SortingResult expected){
		final ScopedString
				  a = (low  == null) ? null : ScopedString.from(low).build(),
				  b = (high == null) ? null : ScopedString.from(high).build();
		assertSorted(a, b, expected);
	}

	static void assertSorted(final ScopedString a, final ScopedString b){
		assertSorted(a, b, SortingResult.LT);
	}

	static void assertSorted(ScopedString a, ScopedString b, SortingResult expected){
		switch (expected) {
			case LT -> Assertions.assertTrue(a.compareTo(b) < 0);
			case EQ -> Assertions.assertEquals(0, a.compareTo(b));
			case GT -> Assertions.assertTrue(a.compareTo(b) > 0);
		}
	}

	enum SortingResult {
		LT, EQ, GT
	}

	@Test
	void testSubSequence(){
		final char[] testPassword = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd'};
		final CharSequence sub;
		try(ScopedString s = ScopedString.from(testPassword).build()){
			//noinspection deprecation
			sub = s.subSequence(0, 4);
		}
		final char[] expected = {'t', 'e', 's', 't'};
		Assertions.assertArrayEquals(expected, toCharArray(sub));

		final ScopedString
				  scopedSub = (ScopedString) sub,
				  expectedScopedStr = ScopedString.from(expected).build();

		Assertions.assertEquals(expectedScopedStr, scopedSub);

		// Just to give a good example
		scopedSub.close();
		expectedScopedStr.close();
	}

	static char[] toCharArray(CharSequence cs){
		if(cs == null)
			return null;
		if(cs.isEmpty())
			return new char[0];
		if(cs instanceof String str)
			return str.toCharArray();
		if(cs instanceof ScopedString sc)
			return sc.getContent();

		final char[] out = new char[cs.length()];
		for(int i = cs.length(); --i > 0;)
			out[i] = cs.charAt(i);
		return out;
	}

	@Test
	void doNotSerializeContent() throws IOException {
		final char[] testPassword = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd'};
		final ScopedString s1 = ScopedString.from(testPassword).build();

		var outBytes = new ByteArrayOutputStream();
		try(var out = new ObjectOutputStream(outBytes)){
			Assertions.assertThrows(NotSerializableException.class, () -> out.writeObject(s1));
		}
	}

	@Test
	void testEquality(){
		Assertions.assertTrue(ScopedString.areEquals(null, null));

		final char[]
				  testPassword = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd'},
				  testPasswordCopy = Arrays.copyOf(testPassword, testPassword.length),
				  wrongCopy = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd', ' ', 'n','o'},
				  wrongCopy2 = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd', ' ', 'o','n'};

		try(ScopedString s1 = ScopedString.from(testPassword).build();
		    ScopedString s2 = ScopedString.from(testPasswordCopy).build();
		    ScopedString n1 = ScopedString.from(wrongCopy).build();
		    ScopedString n2 = ScopedString.from(wrongCopy2).build())
		{
			Assertions.assertFalse(ScopedString.areEquals(s1, null));
			Assertions.assertFalse(ScopedString.areEquals(null, s2));

			Assertions.assertFalse(ScopedString.areEquals(s1, n1));
			Assertions.assertFalse(ScopedString.areEquals(s2, n1));
			Assertions.assertTrue(ScopedString.areEquals(s1, s2));
			Assertions.assertFalse(ScopedString.areEquals(n1, n2));
		}
	}

	@SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
	@Test
	void accessInvalidIndexes(){
		final char[] testPassword = {'t', 'e', 's', 't', ' ', 'P', 'a', 's', 's', 'w', 'o', 'r', 'd'};
		try(ScopedString s = ScopedString.from(testPassword).build()){
			// less than zero index
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> s.charAt(-1));

			// over the end index
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> s.charAt(s.length()));

			// substring with upper limit too high
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> s.subSequence(0, s.length() + 1));

			// substring with inverted (valid) limits
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> s.subSequence(s.length() - 1, 0));

			// substring with invalid lower limit (valid upper limit)
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> s.subSequence(-1, s.length() - 1));

			// substring with invalid lower limit (and invalid upper limit)
			Assertions.assertThrows(IndexOutOfBoundsException.class, () -> s.subSequence(-1, s.length() + 1));
		}
	}
}
