package by.ostis.model;

import java.util.*;
import java.util.stream.Collectors;

public class Operand implements Comparable<Operand> {
    private final String addr;
    private final Set<String> roles;
    private final Map<String, Operand> subOperands = new TreeMap<>();

    public Operand(String addr) {
        this(addr, new TreeSet<>());
    }

    public Operand(String addr, Set<String> roles) {
        this.roles = roles;
        this.addr = addr;
    }

    public String getAddr() {
        return addr;
    }

    public void addRole(String role) {
        roles.add(role);
    }

    public List<String> getRoles() {
        return new ArrayList<>(roles);
    }

    @Override
    public int compareTo(Operand o) {
        return roles.toString().compareTo(o.roles.toString());
    }

    public void addOperand(Operand operand) {
        Operand existingOperand = subOperands.get(operand.getAddr());
        if (existingOperand != null) {
            for (String role : operand.getRoles()) {
                existingOperand.addRole(role);
            }
        } else {
            subOperands.put(operand.getAddr(), operand);
        }
    }

    @Override
    public String toString() {
        return "Operand{" +
                "addr='" + addr + '\'' +
                ", roles=" + roles +
                '}';
    }

    public String toNewFormat(String agentName, Map<String, String> sysIdToAddr, Map<String, String> addrToSysId) {
        String operandSysId;
        if (!(addrToSysId.containsKey(addr) || sysIdToAddr.containsKey(addr))) {
            operandSysId = Agent.createNewId(addr, ".._" + agentName + "_param", sysIdToAddr, addrToSysId);
        } else {
            operandSysId = addrToSysId.getOrDefault(addr, addr);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("_-> ");
        for (String role : roles) {
            if (!(addrToSysId.containsKey(role) || sysIdToAddr.containsKey(role))) {
                throw new RuntimeException("cannot find sys id for " + role);
            }
            String roleSysId = addrToSysId.getOrDefault(role, role);
            builder.append(roleSysId).append(":: ");
        }
        builder.append(operandSysId);
        return builder.toString();
    }

    public Operand replaceAddrsWithId(String agentName, Map<String, String> sysIdToAddr, Map<String, String> addrToSysId) {
        String operandSysId;
        if (!(addrToSysId.containsKey(addr) || sysIdToAddr.containsKey(addr))) {
            operandSysId = Agent.createNewId(addr, ".._" + agentName + "_param", sysIdToAddr, addrToSysId);
        } else {
            operandSysId = addrToSysId.getOrDefault(addr, addr);
        }
        Operand newOperand = new Operand(operandSysId);
        for (String role : roles) {
            if (!(addrToSysId.containsKey(role) || sysIdToAddr.containsKey(role))) {
                throw new RuntimeException("cannot find sys id for " + role);
            }
            String roleSysId = addrToSysId.getOrDefault(role, role);
            newOperand.addRole(roleSysId);
        }
        for (Operand subOperand : subOperands.values()) {
            Operand newSuboperand = subOperand.replaceAddrsWithId(agentName, sysIdToAddr, addrToSysId);
            newOperand.addOperand(newSuboperand);
        }
        return newOperand;
    }

    public String toNewFormat(int numberOfTabs, Set<Operand> rrelInOperands) {
        String tabs = new String(new char[numberOfTabs]).replace('\0', '\t');
        StringBuilder builder = new StringBuilder();
        builder.append(tabs).append("_-> ");
        for (String role : roles) {
            builder.append((rrelInOperands.stream().map(Operand::getAddr).collect(Collectors.toSet()).contains(addr) && role.equals("rrel_scp_var")) ? "rrel_scp_const" : role).append(":: ");
        }
        builder.append(addr);
        if (!subOperands.isEmpty()) {
            builder.append(" (*\n");
            for (Operand subOperand : new TreeSet<>(subOperands.values())) {
                builder.append(subOperand.toNewFormat(numberOfTabs + 1, rrelInOperands)).append(";;\n");
            }
            builder.append(tabs).append("*)");
        }
        return builder.toString();
    }
}
