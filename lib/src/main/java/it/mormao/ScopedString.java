package it.mormao;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is intended for use with try-with-resources, to wipe the content of a char array once it's scope
 * has been reached, not dealing with garbage collection.
 * An example of how to use it should be:
 * <blockquote><pre>
 *    char[] password = {'m','y',' ','p','w','d'};
 *    try (ScopedString scopedPassword = ScopedString.from(password).build()){
 *       doSomethingWith(scopedPassword.getContent());
 *    }
 *    // the compiler will be so nice to put here the code to wipe the password
 * </pre></blockquote>
 * <p>
 *    The default wiper replaces each char with '*', but if you need something different you also have the possibility
 *    to use a different character or write your own wiper like so:
 *    <blockquote><pre>
 *       char[] password = {'m','y',' ','g','r','a','t','e',' ','p','w','d'};
 *      try (ScopedString scopedPassword = ScopedString.from(password).wiper('-').build()){
 *         doSomethingWith(scopedPassword.getContent());
 *      }
 *    </pre></blockquote>
 *    or
 *    <blockquote><pre>
 *       char[] password = {'m','y',' ','g','r','a','t','e',' ','p','w','d'};
 *       try (ScopedString scopedPassword = ScopedString.from(password)
 *                                                      .wiper(chars -> wipeLikeABoss(chars))
 *                                                      .build())
 *       {
 *          doSomethingWith(scopedPassword.getContent());
 *       }
 *    </pre></blockquote>
 * </p>
 */
public final class ScopedString implements CharSequence, Closeable, Comparable<CharSequence> {

	private final transient char[] content;
	private CharWiper wiper = DEFAULT_WIPER;
	private final AtomicBoolean wiped = new AtomicBoolean();

	private static final CharWiper DEFAULT_WIPER = new StaticCharWiper('*');
	private static final char[] EMPTY_CHAR_ARRAY = new char[0];

	/**
	 * Build a String whose content is the given char array.
	 * Be aware that no new copy of the array are built
	 * @param original the original char array to wrap
	 */
	private ScopedString(char[] original){
		this.content = original == null ? EMPTY_CHAR_ARRAY : original;
	}

	private void setWiper(CharWiper wiper){
		this.wiper = Objects.requireNonNull(wiper, "Wiper can't be null");
	}

	private void setWiper(char wipeWith){
		this.wiper = new StaticCharWiper(wipeWith);
	}

	public static Builder from(char[] original){
		return new Builder(original);
	}
	
	public static ScopedString of(char[] original){
		return new ScopedString(original);
	}

	public static final class Builder {
		private final ScopedString s;

		private Builder(char[] content){
			s = new ScopedString(content);
		}

		public Builder withWiper(CharWiper wiper){
			s.setWiper(wiper);
			return this;
		}

		public Builder withWiper(char wipeWith){
			s.setWiper(wipeWith);
			return this;
		}

		public ScopedString build(){
			return s;
		}
	}

	@Override
	public void close() {
		if(wiped.compareAndSet(false, true))
			wiper.wipe(content);
	}

	@Override
	public int length() {
		return content.length;
	}

	@Override
	public char charAt(int index) {
		checkIndex(index, content.length);
		return content[index];
	}

	/**
	 * @deprecated This method create a new ScopedString and a consequent wipe would be performed on the copy,
	 * not on the original char array. Use with caution and be aware that it's up to you to wipe both, the original
	 * and the newly created
	 * @param   start   the beginning index, inclusive.
	 * @param   end     the ending index, exclusive.
	 * @return  a newly allocated ScopedString, cut as specified
	 */
	@Override
	@Deprecated
	public CharSequence subSequence(int start, int end) {
		checkBoundsBeginEnd(start, end, content.length);
		final char[] copy = Arrays.copyOfRange(content, start, end);
		return new ScopedString(copy);
	}

	private static void checkBoundsBeginEnd(int begin, int end, int length) {
		if (begin < 0 || begin > end || end > length) {
			throw new StringIndexOutOfBoundsException(
					  "begin " + begin + ", end " + end + ", length " + length);
		}
	}

	private static void checkIndex(int index, int len){
		if(index < 0 || index >= len)
			throw new StringIndexOutOfBoundsException("Invalid index " + index + ". Index must be in the range [0 - " + len + ']');
	}

	/* The string representation is at least 8 chars long (to prevent easy guessing of short password inadvertently logged) */
	private static final int TO_STRING_MIN_LEN = 8;

	/**
	 * Return a wiped representation of the content. If you want to read the content of a ScopedString use the {@link }
	 * This implementation deliberately violates the contract of CharSequence.toString()
	 * @return a wiped representation of the content
	 */
	@Override
	public String toString() {
		final char[] copy = Arrays.copyOf(content, content.length);
		wiper.wipe(copy);
		return new String(copy).intern();
	}

	/**
	 * This method returns the internal content, because this class is not intended to get ownership of it's value,
	 * but only to give a visual utility that can give a scope to a char array (with try-with-resources)
	 * @return the char array contained in
	 * @throws IllegalStateException in case that the content has yet been wiped out
	 */
	public char[] getContent(){
		if(isWiped())
			throw new IllegalStateException("The content has yet been wiped");
		return content;
	}

	@Override
	public int compareTo(CharSequence o) {
		if(isWiped())
			throw new IllegalStateException("The content of the current instance has yet been wiped");
		if(o == null)
			return 1; // modify this if you want nulls-first or nulls-last;
		if((o instanceof ScopedString) && ((ScopedString) o).isWiped())
			throw new IllegalStateException("The content of the other instance has yet been wiped");

		final int
			lenA = length(),
			lenB = o.length(),
			len = Math.min(lenA, lenB);
		
		for(int i = 0; i < len; i++){
			final char a = charAt(i);
			final char b = o.charAt(i);
			if(a != b)
				return a - b;
		}
		return lenA - lenB;
	}

	private boolean isWiped(){
		return wiped.get();
	}
}
