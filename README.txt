# Jack Compiler

This program will take a .jack input file or directory with .jack files and output compiled .vm files.

## How to Run the Program

Java 8 needs to be installed on whichever device runs this program. After installation please follow these directions for compiling and running.

Another requirement for running this program is that it be kept in the same folder structure and that commands are run from the project root directory (e.g., MeyerMichaelProject<#>).

1. Unzip the contents of the .zip file
2. From the root directory, enter `javac -d bin src/main/java/com/meyermt/jack/*.java` to compile the project
3. Then run `java -cp bin com.meyermt.jack.JackCompiler <path>/<filename.jack>` if it is one file or `java -cp bin com.meyermt.jack.JackCompiler <path>` if it is a directory of more than one file.
4. The program produces the vm files in the same directory, so please note, as it will overwrite other vm files if they exist.
5. Verify file output by running in your favorite VMEmulator.

