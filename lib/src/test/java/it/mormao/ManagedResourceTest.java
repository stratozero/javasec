package it.mormao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class ManagedResourceTest {

	@Test
	void testManagingPasswords() throws Exception {
		final char[]
				  pwdChars = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'},
				  originalBackup = Arrays.copyOf(pwdChars, pwdChars.length);

		ManagedResource
				  .from(() -> ScopedString.from(pwdChars).build())
				  .use(s -> System.out.println(String.valueOf(s.getContent())));

		Assertions.assertFalse(Arrays.equals(pwdChars, originalBackup), "The password has not been obfuscated");
	}

}
