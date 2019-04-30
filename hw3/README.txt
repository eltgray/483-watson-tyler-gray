README:

THIS PROGRAM WILL NOT RUN WITHOUT THE INDEX.
downlaad the index here: https://drive.google.com/open?id=1QYi1G0_OxAFALFwYJQtvGQy-erFIuY_s

The program is coded such that it will read from the folder "watsonIndex" at the top of the program directory,
so it must be copied to that location (the same directory level as the pom.xml file)

Once the watsonIndex is added to the directory, you can compile and run.
While in the directory with the pom.xml, compile and download dependencies with:
mvn clean install

then, run the program with the following command:
mvn exec:java -Pprofile1

this will run the program
it will not recreate the index from scratch as this is commented out,
to do so you will need to also import the wikipedia collection.

You will be prompted to enter a query into stdin,
this will query the wikipedia index and return the
answer to you. 

The program will need to be ran once for each query in this mode.

If you want to run all 100 queries and analyze the results, 
there is a segment at the bottom of the main method that is 
commented out with a comment indicating its purpose,
uncomment this to run all 100 given queries found in questions.txt..