package by.ostis.model;

import java.util.*;
import java.util.stream.Collectors;

public class Operator {
    private final String addr;
    private final Map<String, Operand> operands;
    private final Map<String, Operator> transitions;
    private final Set<String> classes;

    public Operator(String addr) {
        this(addr, new TreeMap<>(), new HashMap<>(), new HashSet<>());
    }

    public Operator(String addr, Map<String, Operand> operands, Map<String, Operator> transitions, Set<String> classes) {
        this.addr = addr;
        this.operands = operands;
        this.transitions = transitions;
        this.classes = classes;
    }

    public void addOperand(Operand operand) {
        Operand existingOperand = operands.get(operand.getAddr());
        if (existingOperand != null) {
            for (String role : operand.getRoles()) {
                existingOperand.addRole(role);
            }
        } else {
            operands.put(operand.getAddr(), operand);
        }
    }

    public Map<String, Operand> getOperands() {
        return new HashMap<>(operands);
    }

    public void addClass(String label) {
        classes.add(label);
    }

    public void addTransition(String relation, Operator operator) {
        transitions.put(relation, operator);
    }

    @Override
    public String toString() {
        return "Operator{" +
                "addr='" + addr + '\'' +
                ", operands=" + operands +
                ", transitions=" + transitions +
                ", classes=" + classes +
                '}';
    }

    public String toNewFormat(String agentName, Map<String, String> sysIdToAddr, Map<String, String> addrToSysId, int numberOfTabs, Set<Operand> rrelInOperands) {
        String operatorSysId;
        if (!(addrToSysId.containsKey(addr) || sysIdToAddr.containsKey(addr))) {
            operatorSysId = Agent.createNewId(addr, ".._" + agentName + "_operator", sysIdToAddr, addrToSysId);
        } else {
            operatorSysId = addrToSysId.getOrDefault(addr, addr);
        }
        StringBuilder builder = new StringBuilder();
        String tabs = new String(new char[numberOfTabs]).replace('\0', '\t');
        builder.append(operatorSysId).append(" (*\n");
        for (String concept : classes) {
            builder.append(tabs).append("_<- ").append(addrToSysId.get(concept)).append(";;\n");
        }
        Set<Operand> newOperands = operands.values().stream().map(operand -> operand.replaceAddrsWithId(agentName, sysIdToAddr, addrToSysId)).collect(Collectors.toCollection(TreeSet::new));
        for (Operand operand : newOperands) {
            builder.append(operand.toNewFormat(numberOfTabs, rrelInOperands)).append(";;\n");
        }
        builder.append("\n");
        for (String transitionAddr : transitions.keySet()) {
            String transitionId = addrToSysId.get(transitionAddr);
            if (transitionId == null) {
                throw new RuntimeException("transition with addr " + transitionAddr + " does not have system identifier");
            }
            String nextAddr = transitions.get(transitionAddr).addr;
            String nextId;
            if (!(addrToSysId.containsKey(nextAddr) || sysIdToAddr.containsKey(nextAddr))) {
                nextId = Agent.createNewId(nextAddr, ".._" + agentName + "_operator", sysIdToAddr, addrToSysId);
            } else {
                nextId = addrToSysId.getOrDefault(nextAddr, nextAddr);
            }
            builder.append(tabs).append("_=> ").append(transitionId).append(":: ").append(nextId).append(";;\n");
        }
        builder.append(new String(new char[numberOfTabs - 1]).replace('\0', '\t')).append("*);;");
        return builder.toString();
    }
}
