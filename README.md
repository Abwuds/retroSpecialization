# RetroSpecialization : The specialization of generics on Java 7
## Context
### Description
The Generic Specialization is an advanced Java VM and Java Language feature incubated
by the project [Valhalla](http://openjdk.java.net/projects/valhalla/), planed for
a futur version of Java SE.
		
### Value
The Generic Specialization aims to allow the use of primitive type with generics
by specializing them at runtime. The two principal values are :

1. Suppression of the boxing 
2. Suppression of pseudo-specialized types such as `IntStream`, `ToIntFunction`


## Project
The goal of the retroSpecialization project is to back port the Generic Specialization
as described in the project [Valhalla](http://openjdk.java.net/projects/valhalla/) to 
Java 7 and 8 projects.
The project is more a rewriter than a compiler since Java files have to first be
compiled with the Valhalla's compiler and then rewritten by the retroSpecialization
executable to be executed under Java 7 and 8.

## Example
```
class Box<any T> {
    private final T t;

    public Box(T t) { this.t = t; }

    public T get() { return t; }
}

class Main {
	public static void main(String[] args) {
		Box<float> box = new Box<float>(3.4f);	
		System.out.println(box.get());
	}
}
```

## User Guide
The project RetroSpecialization can be run from the [distribution files](###Modify), or directly from
a modified code by using the `ant` [file](###Step-1-:-Valhalla-Compilation).
### Step 1 : Valhalla Compilation
In order to back port a java project containing generics instantiated with primitive 
types, the project has to be compiled by the 
[Valhalla project's JDK](https://wiki.openjdk.java.net/display/valhalla/Main) 
(steps are described in the section **Source Code and Building Valhalla**) 
and respect the [syntax imposed](http://cr.openjdk.java.net/~briangoetz/valhalla/specialization.html).

The output of the first step should be all the java files compiled into class files.

### Step 2 : Download the distribution
[Download](https://github.com/Abwuds/retroSpecialization/tree/master/Backport/build) the files present in the dist directory
which consist in the `Backport.jar` and the `rt` folder.

*The `rt` folder contains necessary files copied into the rewritten project in order
to perform the generics specialization at runtime.*

### Step 3 : Execute the *RetroSpecialization* rewriter
Run the Backport, using Java 7+ on the class files produced by the Valhalla JDK.
Run `java -jar Backport.jar -Dfolder=folder`.

The output of the rewriting will be localized inside a folder named `backport_result`.

## Modify the source code
An `ant` build is available to run the project from the source code.

### Run from ant
Run `ant -Dfolder={Folder to rewrite} run` to run the project.

### Usage
Run `ant help` to get the ant usage.

## Known Limitations
* Still in development
* Does not support the inheritance
	
## Version History
	
