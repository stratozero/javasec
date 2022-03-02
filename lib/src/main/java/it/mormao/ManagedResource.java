package it.mormao;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record ManagedResource<C extends AutoCloseable>(Supplier<C> resourceSupplier) {

	public static <C extends AutoCloseable> ManagedResource<C> fromResource(final C resource) {
		return new ManagedResource<>(() -> resource);
	}

	public static <C extends AutoCloseable> ManagedResource<C> from(final Supplier<C> resourceSupplier) {
		return new ManagedResource<>(
				  Objects.requireNonNull(resourceSupplier, "The resource can be null, the supplier should not"));
	}

	public void use(final Consumer<C> resourceConsumer) throws Exception {
		Objects.requireNonNull(resourceConsumer, "A valid consumer should be provided: a null one is not permitted");
		try (C resource = resourceSupplier.get()) {
			resourceConsumer.accept(resource);
		}
	}

	public <T> T useAndGet(final Function<C, T> func) throws Exception {
		Objects.requireNonNull(func);
		try (C resource = resourceSupplier.get()) {
			return func.apply(resource);
		}
	}

	/* TODO: implement filter and map functionalities
	   The main goal should be to emulate stream API and to interact with more readable objects, like:
	   ManagedResource.from(channel::read)
	      .filter(response::isAcceptable)
	      .map(response::toString)
	      .use(Library::doSomethingAwesomeWithResource)

	   The library should be lazy, in the sense that every operation should be performed only when needed,
	   and should have a nice way to interact with ScopedString(s), in such a way that it should be preferable
	   not to manage them directly.
	   To be evaluated > A ScopedString "could" probably be a ManagedResource<char[]>?
	   char[] password = "password".toCharArray();
	   ScopedString.from(password)
	         .use(pwd -> login(username, pwd))
	 */
}
