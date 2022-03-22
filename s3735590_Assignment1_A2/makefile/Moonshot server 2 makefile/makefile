#define a variable for compiler flags(JFLAGS)
#define a variable for the compiler (JC)

JFLAGS = -g
JC = javac

# Clear any default targets for building .class files from .java files; we
# # # will provide our own target entry to do this in this makefile.
# # # make has a set of default targets for different suffixes (like .c.o)
# # # Currently, clearing the default for .java.class is not necessary since
# # # make does not have a definition for this target, but later versions of
# # # make may, so it doesn't hurt to make sure that we clear any default
# # # definitions for these
#
.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

#CLASSES is consist of only client.java file

CLASSES = \
	Client.java 

#default makes the target entry, here it is the target classes

default: classes

#This target entry uses Suffix Replacement within a macro:
#($macroname:stringA=StringB)

classes:$(CLASSES:.java=.class)

# RM is a predefined macro in make (RM = rm -f)

clean:
	 $(RM) *.class
