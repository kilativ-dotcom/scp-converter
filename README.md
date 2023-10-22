## scsi-removing

This is a program that translates inserts scs/scsi files into places where they are included and then all included files are being renamed('-inserted' is appended to the end of their extensions) in order to make sc-builder to skip them during kb build

You can pass multiple directories in which you want to remove scsi files. Note that files in passed directories are modified so maybe you want to copy kb into temporary directory before running this script

to run you can use script
```shell
./scripts/start.sh /absolute/path/to/directory/goes/here
```

### **_If you want to run this program you will need Java8 installed_**
