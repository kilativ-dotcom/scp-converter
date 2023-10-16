## scp-converter

This is a program that translates scp agents from old format into new format.

In order to convert agents you will need to build kb with old agents, run sc-server and then run this converter. It will send websocket requests to sc-server to get agents structures and then save new agents in output directory(use flag -o to override default directory)  

to run you can use script
```shell
./scripts/convert.sh
```

### **_If you want to run this program you will need Java8 installed_**

