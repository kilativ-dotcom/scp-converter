## scp-converter

This is a program that translates scp agents from old format into new format.

In order to convert agents you will need to build kb with old agents, run sc-server and then run this converter. It will send websocket requests to sc-server to get agents structures and then save new agents in output directory(use flag -o to override default directory)  

If you want to preserve operators names you can replace `(?<beforePrefix>^|\s)(?<prefix>(nrel_goto\s*:)|(nrel_then\s*:)|(nrel_else\s*:)|(->)|(rrel_init\s*:))(?<spaces>\s*)(?<!\.)\.{1,2}(?<operator>\w+)` with `${beforePrefix}${prefix}${spaces}_${operator}` in ide before kb build so translator will be able to use operators name after parsing.
                                                         
## This operation will change operators names but it can also change other files so you better save old files somewhere if you want to restore them.
## Also if your agents use operators with similar names then something unpredicted will happen because after replacements those operators will have equal ScAddr

to run you can use script
```shell
./scripts/convert.sh
```

### **_If you want to run this program you will need Java8 installed_**

