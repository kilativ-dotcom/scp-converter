# gwf-open-save

This is a script that walks through all gwf files in passed directories. To use this script you have to make a little preparation.

1. open kbe
2. open any gwf file
3. double click somewhere to create a node and then delete this node

Then you can run script and immediately switch to kbe tab

Script will open every gwf file in all directories you pass. Processing speed is about a file per second.

## warning

It's better not to interrupt script during his work.
Script will generate inputs from your keyboard and I don't know about consequences of not letting keyboard events to finish on their own.

## run

example of a command to run script(you need java 8 to run successfully):
```bash
./start.sh /home/helicopter/Downloads/ostis-web-platform/sc-machine 
```


