# javasec
Utilities to easily implement security

I really love the expressiveness of Java, so, inspired by the Stream framework, came my thought: why don't we use this expressiveness
to create more robust resource handling?

For the sake of reading the rest of this document: I intended passwords as resources themselves, as they need to be 
1. read / accessed only when needed,
2. used for the smallest time interval as possible
3. then cleaned, not relying on garbage collector, 'cause it can run in an arbitraty remote moment or can also be disabled (see epsilon-gc),
and you'll keep your precious imformation in clear text in memory (accessible with basic attacks or inadvertents memory dumps).

_Sorry for my bad english and such a small and hand-crafted libray, I hope it can be usefull to someone else other than me._

# Examples
Don't hate me, I love to start with examples...

## ScopedStrings
ScopedStrings are just a wrapper around a char array (no copy involved), they directly deal with the `char[]` you provide.
Once the `close()` method is invoked, the wrapped char array is wiped out (by default filled with `*` characters), but you can replace this behaviour as you like
using a custom `CharWiper` (see test cases for more detalied examples, or the next paragraph for some basic explanation)

```java
final char[] myReallyGoodPassword = getMyReallyGoodPassword();
try(ScopedString scopedPassword = ScopedString.from(myPassword).build()){
  System.out.println(scopedPassword.toString()); // Inadvertent `toString` invokations prints the obfuscated version, e.g.: "*****"
  login(getUserName(), scopedPassword.getContent()); // use the char array itself
}
// you don't risk to fall in temptation of resusing the wiped password...
```

If you (bad guy... GOF is watching you 💂‍♂️) decide anyway to use a syntax wich lets you use a scoped string out of its scope 
(after closign it) you'll get an `IllegalStateException`:

```java
final char[] myReallyGoodPassword = getMyReallyGoodPassword();
final ScopedString scopedPassword = ScopedString.from(myPassword).build();
login(getUserName(), scopedPassword.getContent());
scopedPassword.close();

// ops, I have to login again...
login(getUserName(), scopedPassword.getContent());

// throw an IllegalStateExcepion
```

## CharWipers
If you simply want your passwords to be wiped with a different character (e.g.: the obfuscated version can sometime be logged in a system where the `*`
character can be dangerous, or can be leveraged to do some injection), you can simply do:

```java
final char[] myReallyGoodPassword = getMyReallyGoodPassword();
try(ScopedString scopedPassword = ScopedString.from(myPassword)
                                              .withWiper('-') // this will set the wiper character to '-'
                                              .build())
{
  login(getUserName(), scopedPassword.getContent());
}
```

Or you can do some complex task to obfuscate passwords writing your own `CharWiper` (more than one write on each cell, random characters, ...)
like this:
```java
final CharWiper customWiper = chars -> {
  final Random rnd = new Random();
  for(int i = 0; i < chars.length; i++)
    chars[i] = (char) rnd.nextInt();
};
final char[] myReallyGoodPassword = getMyReallyGoodPassword();
try(ScopedString scopedPassword = ScopedString.from(myPassword)
                                              .withWiper(customWiper) // this will set the wiper to your own one
                                              .build())
{
  login(getUserName(), scopedPassword.getContent());
}
```

## ManagedResources
The purpose of this class is to provide it a `Supplier` of `AutoCloseable`, configure it, and then invoke some method that "consume" or "transform" the resource 
(like "forEach" and "map" do for Streams). After the usage, the resource is automatically closed.

What I imagined when it first come in mind to me was something like this:
```java
ManagedResource.from(channel::read)
  .filter(data::isValid)
  .map(Library::toCustomObject)
  .use(customObject -> consume(customObject));

// then make ScopedString extend this behaviour to something like
LoginResult loginResult = ScopedString.from("myGreatLongPassword".toCharArray())
  .useAndGet(pwd -> login(username, pwd)); // behave something like "map" but closing the resource
```

... or maybe ...

```java
LoginResult loginResult = ManagedResource.from(getPasswordAsScopedString())
  .filter(pwd -> pwd != null && !pwd.isEmpty())
  .useAndGet(pwd -> login(username, pwd));
```

# Reasons
In java there's plenty of utilities to manage security (most of the time "_automagically_"), but the vast majority of them comes with their whole framework (e.g.: Spring Security). 
Or it's simply too hard to modify an existing project to include some great but complex instrument into its lifecycle (owasp, for example).

> Most of the time you want a banana but you'll have to get the entire jungle and a monkey, that comes and brings you the banana.

_I thik this is a citation but I can't remember the author, nor where I read this phrase... sorry_

With all that said there is also a plethora of papers, books, blogs, post, (... whatever ...) telling you what good habits are to be taken if you want to
reduce the attack surface of your software.

The problem I want to address with this repo is to create a (really small) codebase to manage resources with an expressive toolset, 
that lets you commit your usual errors, but only intentionally, and that facilitate the correct usage of the resources you need.

# Next to come
I think that there are far too many things to be done for a library to remain small, but some of the most useful are those:
1. Secure Strings (to fill the gap with .NET), storing an encrypted version of a string and decrypting it only when necessary
2. a job tokener: see below

## Job tokener
Things tends to became really complicated when you have to deal with slow resources: some guy will repeatedly hit the "_parallelize_" button, until
the whole datacenter freeze while parallelizing millions of small tasks, recreating structures, objects, fetching resources, with an enormous waste
of memory and CPU.
Some other will pretend to have the definitive solution that (every time...) will need the complete rewriting of an entire module, or worse, the entire
service architecture.

The truth everytime stands in the middle: every time you face with those problems:
1. Do some initialization suff (open connections, preallocate memory, etc...)
2. collect similar tasks and group them together in some sort of "buffers" of similar tasks
3. once you met a break condition, stop filling the buffer and submit those tasks to the slower resource, all together, like a bulk-operation to a database (**maybe** in parallel)
4. merge the responses back to the original requests
5. do some final operation, like resource finalization, closing connections, cleaning memory, etc...

All those operations should be done in a meaningful way, so we probably neeed to define some common term before starting.

I imagine that it could look something like:

```java
try(JobTokener jobs = JobTokeners.build(Resources::init)
  .breakIf((newEntry, globalEntrySet) -> trueIfINeedToBreak(newEntry, globalEntrySet))
  .parallelIf(globalEntrySet -> entrySet.size() > 5) // the 
  .forEachBuffer(entrySet -> bulkOperation(entrySet))) // the entrySet here is a subset of the whole
{
  // the decision wheter or not submission can happen in parallel is yet to come... any contribute will be appreciated
  list.forEach(el -> jobs.submit(el));
  // the tasks are executed only if the break condition is met (eventually in parallel)
} // finalizations should run here
```

