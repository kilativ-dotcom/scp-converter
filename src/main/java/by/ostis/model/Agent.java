package by.ostis.model;

import java.util.*;
import java.util.stream.Collectors;

public class Agent {
    private final String addr;
    private final Map<String, Operand> operands;
    private final Map<String, Operator> operators;

    public Agent(String addr) {
        this(addr, new TreeMap<>(), new LinkedHashMap<>());
    }

    public Agent(String addr, Map<String, Operand> operands, Map<String, Operator> operators) {
        this.addr = addr;
        this.operands = operands;
        this.operators = operators;
    }

    public static String createNewId(String addr, String prefix, Map<String, String> sysIdToAddr, Map<String, String> addrToSysId) {
        int number = 0;
        while (sysIdToAddr.containsKey(prefix + number)) {
            number++;
        }
        String freeId = prefix + number;
        sysIdToAddr.put(freeId, addr);
        addrToSysId.put(addr, freeId);
        return freeId;
    }

    public void addOperand(String id, String role) {
        Operand operand = operands.get(id);
        if (operand != null) {
            operand.addRole(role);
        } else {
            operands.put(id, new Operand(id, new HashSet<>(Arrays.asList(role))));
        }
    }

    public void addOperator(String id) {
        if (!operators.containsKey(id)) {
            operators.put(id, new Operator(id));
        }
    }

    public Operator getOperator(String id) {
        if (!hasOperator(id)) {
            addOperator(id);
        }
        return operators.get(id);
    }

    public boolean hasOperator(String id) {
        return operators.containsKey(id);
    }

    @Override
    public String toString() {
        return "Agent{" +
                "addr='" + addr + '\'' +
                ", operands=" + operands +
                ", operators=" + operators +
                '}';
    }

    public String toNewFormat(Map<String, String> sysIdToAddr, Map<String, String> addrToSysId, Map<String, String> enIdtf, Map<String, String> ruIdtf) {
        StringBuilder builder = new StringBuilder();
        if (!(addrToSysId.containsKey(addr) || sysIdToAddr.containsKey(addr))) {
            throw new RuntimeException("cannot find sys id for " + addr);
        }
        String agentSysId = addrToSysId.getOrDefault(addr, addr);
        Set<Operand> rrelInOperands = operands.values().stream().map(operand -> operand.replaceAddrsWithId(agentSysId, sysIdToAddr, addrToSysId)).filter(operand -> operand.getRoles().contains("rrel_in")).collect(Collectors.toSet());
        builder.append(agentSysId).append("\n")
                .append("=> nrel_main_idtf:\n")
                .append("\t[").append(ruIdtf.getOrDefault(sysIdToAddr.get(agentSysId), "")).append("] (* <- lang_ru;; *);\n")
                .append("\t[").append(enIdtf.getOrDefault(sysIdToAddr.get(agentSysId), "")).append("] (* <- lang_en;; *);\n")
                .append("<- scp_program;\n")
                .append("<- agent_scp_program;\n")
                .append("-> rrel_key_sc_element: .._process1;;\n")
                .append("\n")
                .append(agentSysId).append(" = [*\n")
                .append(".._process1\n")
                .append("_<- scp_process;\n")
                .append("\n");

        Set<Operand> newOperands = operands.values().stream().map(operand -> operand.replaceAddrsWithId(agentSysId, sysIdToAddr, addrToSysId)).collect(Collectors.toCollection(TreeSet::new));
        for (Operand operand : newOperands) {
            builder.append(operand.toNewFormat(0, rrelInOperands)).append(";\n");
        }
        builder.append("_<= nrel_decomposition_of_action:: .._actions (*\n")
                .append("\n");
        boolean isFirst = true;
        for (String operatorAddr : operators.keySet()) {
            builder.append("\t_-> ");
            if (isFirst) {
                isFirst = false;
                builder.append("rrel_1:: ");
            }
            builder.append(operators.get(operatorAddr).toNewFormat(agentSysId, sysIdToAddr, addrToSysId, 2, rrelInOperands)).append("\n")
                    .append("\n");
        }
        builder.append("*);;\n");
        builder.append("\n");
        builder.append("*];;\n");
        return builder.toString();
    }

    private static final String newFileTemplate = "${agent_name}\n" +
                    "=> nrel_main_idtf:\n" +
                    "\t[Добавляет посещение ребра _edge и инцидентные ему вершины в маршрут _route] (* <- lang_ru;; *);\n" +
                    "\t[Add edge and vertex in path] (* <- lang_en;; *);\n" +
                    "<- scp_program;\n" +
                    "<- agent_scp_program;\n" +
                    "-> rrel_key_sc_element: .._process1;;\n" +
                    "\n" +
                    "${agent_name} = [*\n" +
                    ".._process1\n" +
                    "_<- scp_process;\n" +
                    "\n" +
                    "%s\n" +
                    "_-> rrel_2:: rrel_in:: .._edge;\n" +
                    "_-> rrel_3:: rrel_in:: .._from_vertex;\n" +
                    "_-> rrel_4:: rrel_in:: .._to_vertex;\n" +
                    "_-> rrel_5:: rrel_out:: .._edge_visit;\n" +
                    "\n" +
                    "_<= nrel_decomposition_of_action:: .._actions (*\n" +
                    "\n" +
                    "\t_-> rrel_1:: .._${agent_name}_operator1 (*\n" +
                    "\t\t_<- call;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_const:: proc_get_route_struct;;\n" +
                    "\t\t_-> rrel_2:: rrel_fixed:: rrel_scp_const:: .._params1 (*\n" +
                    "\t\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_const:: .._route;;\n" +
                    "\t\t\t_-> rrel_2:: rrel_assign:: rrel_scp_var:: .._route_struct;;\n" +
                    "\t\t*);;\n" +
                    "\t\t_-> rrel_3:: rrel_assign:: rrel_scp_var:: .._descr;;\n" +
                    "\n" +
                    "\t\t_=> nrel_goto:: .._${agent_name}_operator2;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t_-> .._${agent_name}_operator2 (*\n" +
                    "\t\t_<-waitReturn;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_var:: .._descr;;\n" +
                    "\n" +
                    "\t\t_=> nrel_goto:: .._${agent_name}_operator3;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t\t// Находим отношение соответствия маршрута\n" +
                    "\t\t//\n" +
                    "\t_-> .._${agent_name}_operator3 (*\n" +
                    "\t\t_<- call;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_const:: proc_get_route_visit;;\n" +
                    "\t\t_-> rrel_2:: rrel_fixed:: rrel_scp_const:: .._params2 (*\n" +
                    "\t\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_const:: .._route;;\n" +
                    "\t\t\t_-> rrel_2:: rrel_assign:: rrel_scp_var:: .._route_visit;;\n" +
                    "\t\t*);;\n" +
                    "\t\t_-> rrel_3:: rrel_assign:: rrel_scp_var:: .._descr;;\n" +
                    "\n" +
                    "\t\t_=> nrel_goto:: .._${agent_name}_operator4;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t_-> .._${agent_name}_operator4 (*\n" +
                    "\t\t_<-waitReturn;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_var:: .._descr;;\n" +
                    "\n" +
                    "\t  \t_=> nrel_goto:: .._${agent_name}_operator5;;\n" +
                    "\t*);;\n" +
                    "\t\t// Добавим посещение вершины _from_vertex в маршрут\n" +
                    "\t\t//\n" +
                    "\t_-> .._${agent_name}_operator5 (*\n" +
                    "\t\t_<- call;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_const:: proc_add_vertex_visit_to_route;;\n" +
                    "\t\t_-> rrel_2:: rrel_fixed:: rrel_scp_const:: .._params3 (*\n" +
                    "\t\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_const:: .._route;;\n" +
                    "\t\t\t_-> rrel_2:: rrel_fixed:: rrel_scp_const:: .._from_vertex;;\n" +
                    "\t\t\t_-> rrel_3:: rrel_assign:: rrel_scp_var:: .._from_vertex_visit;;\n" +
                    "\t\t*);;\n" +
                    "\t\t_-> rrel_3:: rrel_assign:: rrel_scp_var:: .._descr;;\n" +
                    "\n" +
                    "\t\t_=> nrel_goto:: .._${agent_name}_operator6;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t_-> .._${agent_name}_operator6 (*\n" +
                    "\t\t_<- waitReturn;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_var:: .._descr;;\n" +
                    "\n" +
                    "\t  \t_=> nrel_goto:: .._${agent_name}_operator7;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t\t//Получаем посещение вершины _to_vertex.\n" +
                    "\t\t//\n" +
                    "\t_-> .._${agent_name}_operator7 (*\n" +
                    "\t\t_<- searchElStr5;;\n" +
                    "\t\t_-> rrel_1:: rrel_assign:: rrel_scp_var:: .._to_vertex_visit;;\n" +
                    "\t\t_-> rrel_2:: rrel_assign:: rrel_common:: rrel_scp_var:: .._arc2;;\n" +
                    "\t\t_-> rrel_3:: rrel_fixed:: rrel_scp_const:: .._to_vertex;;\n" +
                    "\t\t_-> rrel_4:: rrel_assign:: rrel_pos_const_perm:: rrel_scp_var:: .._arc4;;\n" +
                    "\t\t_-> rrel_5:: rrel_fixed:: rrel_scp_var:: .._route_visit;;\n" +
                    "\n" +
                    "\t\t_=> nrel_goto:: .._${agent_name}_operator8;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t\t// Создаём посещение дуги...\n" +
                    "\t\t//\n" +
                    "\t_-> .._${agent_name}_operator8 (*\n" +
                    "\t\t_<- genElStr3;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_var:: .._from_vertex_visit;;\n" +
                    "\t\t_-> rrel_2:: rrel_assign:: rrel_common:: rrel_scp_var:: .._edge_visit;;\n" +
                    "\t\t_-> rrel_3:: rrel_fixed:: rrel_scp_var:: .._to_vertex_visit;;\n" +
                    "\n" +
                    "\t\t_=> nrel_goto:: .._${agent_name}_operator9;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t\t// Добавим её в структуру маршрута.\n" +
                    "\t\t//\n" +
                    "\t_-> .._${agent_name}_operator9 (*\n" +
                    "\t\t_<- genElStr5;;\n" +
                    "\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_var:: .._route_struct;;\n" +
                    "\t\t_-> rrel_2:: rrel_assign:: rrel_pos_const_perm:: rrel_scp_var:: .._arc2;;\n" +
                    "\t\t_-> rrel_3:: rrel_fixed:: rrel_scp_var:: .._edge_visit;;\n" +
                    "\t\t_-> rrel_4:: rrel_assign:: rrel_pos_const_perm:: rrel_scp_var:: .._arc4;;\n" +
                    "\t\t_-> rrel_5:: rrel_fixed:: rrel_scp_const:: rrel_arc;;\n" +
                    "\n" +
                    "\t\t_=> nrel_goto:: .._${agent_name}_operator10;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\n" +
                    "\t\t// Укажем, что _edge_visit является посещением edge\n" +
                    "\t\t//\n" +
                    "\t_-> .._${agent_name}_operator10 (*\n" +
                    "\t\t\t_<- genElStr5;;\n" +
                    "\t\t\t_-> rrel_1:: rrel_fixed:: rrel_scp_var:: .._edge_visit;;\n" +
                    "\t\t\t_-> rrel_2:: rrel_assign:: rrel_common:: rrel_scp_var:: .._arc2;;\n" +
                    "\t\t\t_-> rrel_3:: rrel_fixed:: rrel_scp_const:: .._edge;;\n" +
                    "\t\t\t_-> rrel_4:: rrel_assign:: rrel_pos_const_perm:: rrel_scp_var:: .._arc4;;\n" +
                    "\t\t\t_-> rrel_5:: rrel_fixed:: rrel_scp_var:: .._route_visit;;\n" +
                    "\n" +
                    "\t\t\t_=> nrel_goto:: .._${agent_name}_operator_return;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "\t_-> .._${agent_name}_operator_return (*\n" +
                    "\t\t_<- return;;\n" +
                    "\t*);;\n" +
                    "\n" +
                    "*);;\n" +
                    "\n" +
                    "*];;";
}
