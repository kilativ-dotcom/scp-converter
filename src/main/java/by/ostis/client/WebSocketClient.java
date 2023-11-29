package by.ostis.client;

import by.ostis.model.Agent;
import by.ostis.model.Pair;
import by.ostis.model.Operand;
import org.glassfish.tyrus.client.ClientManager;

import javax.json.*;
import javax.websocket.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ClientEndpoint
public class WebSocketClient {
    private final String outputDirectory;

    private final Map<String, String> linkAddrToContent = new HashMap<>();
    private final Map<String, String> systemIdtfToAddr = new HashMap<>();
    private final Map<String, String> addrToMainIdtfEn = new HashMap<>();
    private final Map<String, String> addrToMainIdtfRu = new HashMap<>();
    private WebSocketContainer client;
    private Session session;
    private final Map<String, Agent> agents = new HashMap<>();
    private static final List<String> errors = new LinkedList<>();

    public static final String TEST_AGENT = "agent_of_counting_variables_in_structure";

    private List<Pair<Consumer<String>, Supplier<String>>> stack = new LinkedList<>(Arrays.asList(
            keynodeResolver("agent_scp_program"),
            keynodeResolver("scp_program"),
            keynodeResolver("call"),
            keynodeResolver("nrel_main_idtf"),
            keynodeResolver("nrel_system_identifier"),
            keynodeResolver("lang_en"),
            keynodeResolver("lang_ru"),
            keynodeResolver("rrel_1"),
            keynodeResolver("rrel_2"),
            keynodeResolver("rrel_params"),
            keynodeResolver("rrel_init"),
            keynodeResolver("rrel_operators"),
            findAgents(),
            addAgentsVerification(),
            finish()
    ));

    private Pair<Consumer<String>, Supplier<String>> findAgents() {
        Consumer<String> localConsumer = response -> {
            System.out.println("findAgents::Consumer");
            String payload = getPayload(response);
            JsonReader jsonReader = Json.createReader(new StringReader(payload));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            JsonArray addrs = jsonObject.getJsonArray("addrs");
            for (JsonValue addr : addrs) {
                String agentAddr = Integer.toString(addr.asJsonArray().getInt(2));
                addAgentSearch(agentAddr);
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("findAgents::Supplier");
            int conceptAddr = Integer.parseInt(systemIdtfToAddr.get("scp_program"));
            return String.format(tripleTemplate, conceptAddr);
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> addAgentsVerification() {
        Consumer<String> localConsumer = response -> {
        };
        Supplier<String> localSupplier = () -> {
            markAgents();
            return null;
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private void addAgentSearch(String agentAddr) {
        stack.add(0, searchOperators(agentAddr));
        stack.add(0, searchParams(agentAddr));
        stack.add(0, searchMainIdtfRu(agentAddr));
        stack.add(0, searchMainIdtfEn(agentAddr));
        stack.add(0, putAgent(agentAddr));
        stack.add(0, searchSystemIdtf(agentAddr));
    }

    private Pair<Consumer<String>, Supplier<String>> putAgent(String agentAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("putAgent::Consumer"); };
        Supplier<String> localSupplier = () -> {
            System.out.println("putAgent::Supplier");
            String placeholder = "definitely no presento";
            String sysId = systemIdtfToAddr.entrySet().stream().filter(entry -> entry.getValue().equals(agentAddr)).findFirst().orElse(new AbstractMap.SimpleEntry<>(placeholder, placeholder)).getValue();
            if (sysId.equals(placeholder)) {
                errors.add("cannot found sysid for addr " + agentAddr);
                stack.remove(0);
                stack.remove(0);
                stack.remove(0);
                stack.remove(0);
                return null;
            }
            if (!agents.containsKey(sysId)) {
                agents.put(sysId, new Agent(sysId));
            }
            return null;
        };
        return new Pair<>(localConsumer, localSupplier);

    }

    private Pair<Consumer<String>, Supplier<String>> searchMainIdtfEn(String agentAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("searchMainIdtfEn::Consumer");
            String payload = getPayload(response);
            if (payload.length() > 2) {
                JsonReader jsonReader = Json.createReader(new StringReader(payload));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                JsonArray jsonArray = jsonObject.getJsonArray("addrs");
                if (!jsonArray.isEmpty()) {
                    int linkAddr = jsonArray.getJsonArray(0).getInt(2);
                    stack.add(0, getLinkContentEn(agentAddr, String.valueOf(linkAddr)));
                }
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("searchMainIdtfEn::Supplier");
            int nrelMainIdtf = Integer.parseInt(systemIdtfToAddr.get("nrel_main_idtf"));
            int langEn = Integer.parseInt(systemIdtfToAddr.get("lang_en"));
            if (addrToMainIdtfEn.containsKey(agentAddr)) {
                return null;
            }
            int nodeIntAddr = Integer.parseInt(agentAddr);
            return String.format(mainIdtfTemplate, nodeIntAddr, nrelMainIdtf, langEn);
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> searchMainIdtfRu(String agentAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("searchMainIdtfRu::Consumer");
            String payload = getPayload(response);
            if (payload.length() > 2) {
                JsonReader jsonReader = Json.createReader(new StringReader(payload));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                JsonArray jsonArray = jsonObject.getJsonArray("addrs");
                if (!jsonArray.isEmpty()) {
                    int linkAddr = jsonArray.getJsonArray(0).getInt(2);
                    stack.add(0, getLinkContentRu(agentAddr, String.valueOf(linkAddr)));
                }
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("searchMainIdtfRu::Supplier");
            int nrelMainIdtf = Integer.parseInt(systemIdtfToAddr.get("nrel_main_idtf"));
            int langRu = Integer.parseInt(systemIdtfToAddr.get("lang_ru"));
            if (addrToMainIdtfRu.containsKey(agentAddr)) {
                return null;
            }
            int nodeIntAddr = Integer.parseInt(agentAddr);
            return String.format(mainIdtfTemplate, nodeIntAddr, nrelMainIdtf, langRu);
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> searchOperators(String agentAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("searchOperators::Consumer");
            String agentIdtf = systemIdtfToAddr.entrySet().stream().filter(entry -> entry.getValue().equals(agentAddr)).findFirst().get().getValue();
            String payload = getPayload(response);
            JsonReader jsonReader = Json.createReader(new StringReader(payload));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            JsonArray addrs = jsonObject.getJsonArray("addrs");
            for (JsonValue addr : addrs) {
                String firstOperator = Integer.toString(addr.asJsonArray().getInt(8));
                addOperatorSearches(agentIdtf, firstOperator);
                stack.add(0, searchSystemIdtf(firstOperator));
                agents.get(agentIdtf).addOperator(firstOperator);
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("searchOperators::Supplier");
            int agentAddrInt = Integer.parseInt(agentAddr);
            int initAddr = Integer.parseInt(systemIdtfToAddr.get("rrel_init"));
            int operatorsAddr = Integer.parseInt(systemIdtfToAddr.get("rrel_operators"));
            return String.format(firstOperatorTemplate, agentAddrInt, operatorsAddr, initAddr);
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private void addOperatorSearches(String agentIdtf, String operatorAddr) {
        addOperatorClassesSearch(agentIdtf, operatorAddr);
        addOutgoingTransitionsSearch(agentIdtf, operatorAddr);
        addOperatorOperandSearch(agentIdtf, operatorAddr);
    }

    private void addOperatorOperandSearch(String agentIdtf, String operatorAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("addOperatorOperandSearch::Consumer");
            String payload = getPayload(response);
            JsonReader jsonReader = Json.createReader(new StringReader(payload));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            JsonArray addrs = jsonObject.getJsonArray("addrs");
            for (JsonValue addr : addrs) {
                String operand = Integer.toString(addr.asJsonArray().getInt(2));
                String role = Integer.toString(addr.asJsonArray().getInt(3));
                stack.add(0, searchSystemIdtf(role));
                stack.add(0, searchSystemIdtf(operand));
                stack.add(0, getLinkContent(operand, operand));
                agents.get(agentIdtf).getOperator(operatorAddr).addOperand(new Operand(operand, new TreeSet<>(Arrays.asList(role))));
            }

        };
        Supplier<String> localSupplier = () -> {
            System.out.println("addOperatorOperandSearch::Supplier");
            return String.format(outGoingRoleRelationTemplate, Integer.parseInt(operatorAddr));
        };
        stack.add(0, new Pair<>(localConsumer, localSupplier));
    }

    private void addOperatorClassesSearch(String agentIdtf, String operatorAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("addOperatorClassesSearch::Consumer");
            String payload = getPayload(response);
            JsonReader jsonReader = Json.createReader(new StringReader(payload));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            JsonArray addrs = jsonObject.getJsonArray("addrs");
            for (JsonValue addr : addrs) {
                String classAddr = Integer.toString(addr.asJsonArray().getInt(0));
                stack.add(0, searchSystemIdtf(classAddr));
                agents.get(agentIdtf).getOperator(operatorAddr).addClass(classAddr);
                if (classAddr.equals(systemIdtfToAddr.get("call"))) {
                    Collection<Operand> operands = agents.get(agentIdtf).getOperator(operatorAddr).getOperands().values();
                    for (Operand operand : operands) {
                        if (operand.getRoles().contains(systemIdtfToAddr.get("rrel_2"))) {
                            addOperandSuboperandsSearch(agentIdtf, operatorAddr, operand.getAddr());
                        }
                    }
                }
            }

        };
        Supplier<String> localSupplier = () -> {
            System.out.println("addOperatorClassesSearch::Supplier");
            return String.format(operatorClassesTemplate, Integer.parseInt(operatorAddr));
        };
        stack.add(0, new Pair<>(localConsumer, localSupplier));
    }

    private void addOperandSuboperandsSearch(String agentIdtf, String operatorAddr, String operandAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("addOperandSuboperandsSearch::Consumer");
            String payload = getPayload(response);
            JsonReader jsonReader = Json.createReader(new StringReader(payload));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            JsonArray addrs = jsonObject.getJsonArray("addrs");
            for (JsonValue addr : addrs) {
                String operand = Integer.toString(addr.asJsonArray().getInt(2));
                String role = Integer.toString(addr.asJsonArray().getInt(3));
                stack.add(0, searchSystemIdtf(role));
                stack.add(0, searchSystemIdtf(operand));
                stack.add(0, getLinkContent(operand, operand));
                agents.get(agentIdtf).getOperator(operatorAddr).getOperands().get(operandAddr).addOperand(new Operand(operand, new TreeSet<>(Arrays.asList(role))));
            }

        };
        Supplier<String> localSupplier = () -> {
            System.out.println("addOperandSuboperandsSearch::Supplier");
            return String.format(outGoingRoleRelationTemplate, Integer.parseInt(operandAddr));
        };
        stack.add(0, new Pair<>(localConsumer, localSupplier));
    }

    private void addOutgoingTransitionsSearch(String agentIdtf, String operatorAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("addOutgoingTransitionsSearch::Consumer");
            String payload = getPayload(response);
            JsonReader jsonReader = Json.createReader(new StringReader(payload));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            JsonArray addrs = jsonObject.getJsonArray("addrs");
            for (JsonValue addr : addrs) {
                String operator = Integer.toString(addr.asJsonArray().getInt(2));
                String role = Integer.toString(addr.asJsonArray().getInt(3));
                stack.add(0, searchSystemIdtf(role));
                stack.add(0, searchSystemIdtf(operator));
                if (!agents.get(agentIdtf).hasOperator(operator)) {
                    addOperatorSearches(agentIdtf, operator);
                }
                agents.get(agentIdtf).getOperator(operatorAddr).addTransition(role, agents.get(agentIdtf).getOperator(operator));
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("addOutgoingTransitionsSearch::Supplier");
            return String.format(outGoingNoroleRelation, Integer.parseInt(operatorAddr));
        };
        stack.add(0, new Pair<>(localConsumer, localSupplier));
    }

    private Pair<Consumer<String>, Supplier<String>> searchParams(String agentAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("searchParams::Consumer");
            String agentIdtf = systemIdtfToAddr.entrySet().stream().filter(entry -> entry.getValue().equals(agentAddr)).findFirst().get().getValue();
            String payload = getPayload(response);
            if (payload.length() > 2) {
                JsonReader jsonReader = Json.createReader(new StringReader(payload));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                JsonArray addrs = jsonObject.getJsonArray("addrs");
                for (JsonValue addr : addrs) {
                    String operand = Integer.toString(addr.asJsonArray().getInt(8));
                    String role = Integer.toString(addr.asJsonArray().getInt(9));
                    stack.add(0, searchSystemIdtf(role));
                    stack.add(0, searchSystemIdtf(operand));
                    stack.add(0, getLinkContent(operand, operand));
                    agents.get(agentIdtf).addOperand(operand, role);
                }
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("searchParams::Supplier");
            int paramsIntAddr = Integer.parseInt(systemIdtfToAddr.get("rrel_params"));
            int agentIntAddr = Integer.parseInt(agentAddr);
            return String.format(paramTemplate, agentIntAddr, paramsIntAddr);
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> searchSystemIdtf(String nodeAddr) {
        Consumer<String> localConsumer = response -> {
            System.out.println("searchSystemIdtf::Consumer");
            String payload = getPayload(response);
            if (payload.length() > 2) {
                JsonReader jsonReader = Json.createReader(new StringReader(payload));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                JsonArray jsonArray = jsonObject.getJsonArray("addrs");
                if (!jsonArray.isEmpty()) {
                    int linkAddr = jsonArray.getJsonArray(0).getInt(2);
                    stack.add(0, getLinkContent(nodeAddr, String.valueOf(linkAddr)));
                }
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("searchSystemIdtf::Supplier");
            if (systemIdtfToAddr.containsValue(nodeAddr)) {
                return null;
            }
            int nrelSystemIdtf = Integer.parseInt(systemIdtfToAddr.get("nrel_system_identifier"));
            int nodeIntAddr = Integer.parseInt(nodeAddr);
            return String.format(outGoingLinkTemplate, nodeIntAddr, nrelSystemIdtf);
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> getLinkContent(String source, String link) {
        Consumer<String> localConsumer = response -> {
            System.out.println("getLinkContent::Consumer");
            String payload = getPayload(response);
            if (payload.length() > 2) {
                JsonReader jsonReader = Json.createReader(new StringReader(response));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                if (jsonObject.get("payload").getValueType().equals(JsonValue.ValueType.ARRAY)) {
                    String sysId = jsonObject.getJsonArray("payload").getJsonObject(0).getString("value");
                    if (!sysId.isEmpty()) {
                        linkAddrToContent.put(link, "[" + sysId + "]");
                        if (source.equals(link)) {
                            systemIdtfToAddr.put("[" + sysId + "]", source);
                        } else {
                            systemIdtfToAddr.put(sysId, source);
                        }
                    }
                }
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("getLinkContent::Supplier");
            if (systemIdtfToAddr.containsValue(source)) {
                return null;
            } else {
                int linkAddr = Integer.parseInt(link);
                return String.format(linkContentTemplate, linkAddr);
            }
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> getLinkContentEn(String source, String link) {
        Consumer<String> localConsumer = response -> {
            System.out.println("getLinkContentEn::Consumer");
            String payload = getPayload(response);
            if (payload.length() > 2) {
                JsonReader jsonReader = Json.createReader(new StringReader(response));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                String mainId = jsonObject.getJsonArray("payload").getJsonObject(0).getString("value");
                if (!mainId.isEmpty()) {
                    addrToMainIdtfEn.put(source, mainId);
                }
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("getLinkContentEn::Supplier");
            if (addrToMainIdtfEn.get(source) != null) {
                return null;
            } else {
                int linkAddr = Integer.parseInt(link);
                return String.format(linkContentTemplate, linkAddr);
            }
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> getLinkContentRu(String source, String link) {
        Consumer<String> localConsumer = response -> {
            System.out.println("getLinkContentRu::Consumer");
            String payload = getPayload(response);
            if (payload.length() > 2) {
                JsonReader jsonReader = Json.createReader(new StringReader(response));
                JsonObject jsonObject = jsonReader.readObject();
                jsonReader.close();
                String mainId = jsonObject.getJsonArray("payload").getJsonObject(0).getString("value");
                if (!mainId.isEmpty()) {
                    addrToMainIdtfRu.put(source, mainId);
                }
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("getLinkContentRu::Supplier");
            if (addrToMainIdtfRu.get(source) != null) {
                return null;
            } else {
                int linkAddr = Integer.parseInt(link);
                return String.format(linkContentTemplate, linkAddr);
            }
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private Pair<Consumer<String>, Supplier<String>> keynodeResolver(String keynode) {
        Consumer<String> localConsumer = response -> {
            System.out.println("keynodeResolver::Consumer");
            systemIdtfToAddr.put(keynode, getAddr(response));
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("keynodeResolver::Supplier");
            return applyKeynodeTemplate(keynode);
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private void markAgents() {
        int agentProgramAddrInt = Integer.parseInt(systemIdtfToAddr.get("agent_scp_program"));
        for (String agentAddr : agents.keySet()) {
            Consumer<String> localConsumer = response -> {
                System.out.println("markAgents::Consumer");
                String payload = getPayload(response);
                if (payload.length() > 2) {
                    JsonReader jsonReader = Json.createReader(new StringReader(payload));
                    JsonObject jsonObject = jsonReader.readObject();
                    jsonReader.close();
                    JsonArray addrs = jsonObject.getJsonArray("addrs");
                    for (JsonValue addr : addrs) {
                        agents.get(agentAddr).setIsAgent();
                        break;
                    }
                }
            };
            Supplier<String> localSupplier = () -> {
                System.out.println("markAgents::Supplier");
                int agentAddrInt = Integer.parseInt(agentAddr);
                return String.format(fafTripleTemplate, agentProgramAddrInt, agentAddrInt);
            };
            stack.add(1, new Pair<>(localConsumer, localSupplier));
        }
    }

    private Pair<Consumer<String>, Supplier<String>> finish() {
        Consumer<String> localConsumer = response -> {
            System.out.println("finish::Consumer");
            if (!errors.isEmpty()) {
                System.out.println("errors:");
                for (String error : errors) {
                    System.out.println(error);
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        };
        Supplier<String> localSupplier = () -> {
            System.out.println("finish::Supplier");
            Map<String, String> addrToSystemIdtf = new HashMap<>();
            for (String sysId : systemIdtfToAddr.keySet()) {
                String addr = systemIdtfToAddr.get(sysId);
                if (addrToSystemIdtf.containsKey(addr)) {
                    throw new RuntimeException("duplicated sysid for " + addr + ": " + sysId + " and " + addrToSystemIdtf.get(addr));
                }
                addrToSystemIdtf.put(addr, sysId);
            }
            for (String agentAddr : agents.keySet()) {
                String agentName = systemIdtfToAddr.entrySet().stream().filter(entry -> entry.getValue().equals(agentAddr)).findFirst().orElse(new AbstractMap.SimpleEntry<>(null, null)).getKey();
                if (agentName == null) {
                    errors.add("addr " + agentAddr + " does not have sys id");
                    continue;
                }
                File output = new File(outputDirectory + File.separator + agentName + ".scs");
                output.getParentFile().mkdirs();
                try (FileWriter writer = new FileWriter(output)) {
                    String agentCode = agents.get(agentAddr).toNewFormat(systemIdtfToAddr, addrToSystemIdtf, addrToMainIdtfEn, addrToMainIdtfRu, linkAddrToContent);
                    writer.write(agentCode);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            localConsumer.accept("");
            throw new RuntimeException("End of calls");
        };
        return new Pair<>(localConsumer, localSupplier);
    }

    private String applyKeynodeTemplate(String keynode) {
        return String.format(keynodeTemplate, keynode);
    }

    private String getAddr(String message) {
        String payload = getPayload(message).substring(1);
        return payload.substring(0, payload.length() - 1);
    }

    private String getPayload(String message) {
        JsonReader jsonReader = Json.createReader(new StringReader(message));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();
        return jsonObject.get("payload").toString();
    }

    public WebSocketClient(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void start() {
        client = ClientManager.createClient();
        try {
            URI uri = new URI("ws://127.0.0.1:8090/");
            session = client.connectToServer(this, uri);
            sendRequest(stack.get(0).getValue().get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendRequest(String request) {
        System.out.println("sending: " + request);
        session.getAsyncRemote().sendText(request);
    }

    public void close() {
        try {
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("received: " + message);
        if (!stack.isEmpty()) {
            stack.remove(0).getKey().accept(message);
        }
        while (!stack.isEmpty()) {
            String request = stack.get(0).getValue().get();
            if (request != null) {
                sendRequest(request);
                return;
            } else {
                stack.remove(0);
            }
        }
    }


    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println(throwable.getMessage());
        finish().getKey().accept(throwable.getMessage());
    }

    private final static String keynodeTemplate = "{\"id\":1,\"type\":\"keynodes\",\"payload\":[{\"command\":\"find\",\"idtf\":\"%s\"}]}";
    private final static String paramTemplate = "{\"id\": 92, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_agent_to_params\"}, {\"type\": \"type\", \"value\": 65, \"alias\": \"_params\"}], [{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_params_to_edge\"}, {\"type\": \"alias\", \"value\": \"_agent_to_params\"}], [{\"type\": \"alias\", \"value\": \"_params\"}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_params_to_param\"}, {\"type\": \"type\", \"value\": 1, \"alias\": \"_param\"}], [{\"type\": \"type\", \"value\": 577, \"alias\": \"_param_role\"}, {\"type\": \"type\", \"value\": 2256}, {\"type\": \"alias\", \"value\": \"_params_to_param\"}]], \"params\": {}}}";
    private final static String outGoingLinkTemplate = "{\"id\": 97, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 72, \"alias\": \"_node_to_link\"}, {\"type\": \"type\", \"value\": 66, \"alias\": \"_link\"}], [{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_rel_to_edge\"}, {\"type\": \"alias\", \"value\": \"_node_to_link\"}]], \"params\": {}}}";
    private final static String linkContentTemplate = "{\"id\": 103, \"type\": \"content\", \"payload\": [{\"command\": \"get\", \"addr\": %d}]}";
    public static final String firstOperatorTemplate = "{\"id\": 109, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_agent_to_operators\"}, {\"type\": \"type\", \"value\": 65, \"alias\": \"_operators\"}], [{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_operators_to_edge\"}, {\"type\": \"alias\", \"value\": \"_agent_to_operators\"}], [{\"type\": \"alias\", \"value\": \"_operators\"}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_operators_to_operator\"}, {\"type\": \"type\", \"value\": 1, \"alias\": \"_operator\"}], [{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256}, {\"type\": \"alias\", \"value\": \"_operators_to_operator\"}]], \"params\": {}}}";
    public static final String outGoingRoleRelationTemplate = "{\"id\": 114, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_operator_to_operand\"}, {\"type\": \"type\", \"value\": 0, \"alias\": \"_operand\"}], [{\"type\": \"type\", \"value\": 577, \"alias\": \"_role\"}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_role_to_edge\"}, {\"type\": \"alias\", \"value\": \"_operator_to_operand\"}]], \"params\": {}}}";
    public static final String operatorClassesTemplate = "{\"id\": 122, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"type\", \"value\": 2113, \"alias\": \"_class\"}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_class_to_operand\"}, {\"type\": \"addr\", \"value\": %d}]], \"params\": {}}}";
    public static final String outGoingNoroleRelation = "{\"id\": 125, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 72, \"alias\": \"_operator_to_next\"}, {\"type\": \"type\", \"value\": 1, \"alias\": \"_next_operand\"}], [{\"type\": \"type\", \"value\": 1089, \"alias\": \"_norole\"}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_role_to_edge\"}, {\"type\": \"alias\", \"value\": \"_operator_to_next\"}]], \"params\": {}}}";
    public static final String mainIdtfTemplate = "{\"id\": 129, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 72, \"alias\": \"_node_to_link\"}, {\"type\": \"type\", \"value\": 66, \"alias\": \"_link\"}], [{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_rel_to_edge\"}, {\"type\": \"alias\", \"value\": \"_node_to_link\"}], [{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_lang_to_link\"}, {\"type\": \"alias\", \"value\": \"_link\"}]], \"params\": {}}}";
    public static final String tripleTemplate = "{\"id\": 135, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_edge\"}, {\"type\": \"type\", \"value\": 65, \"alias\": \"_agent\"}]], \"params\": {}}}";
    public static final String fafTripleTemplate = "{\"id\": 139, \"type\": \"search_template\", \"payload\": {\"templ\": [[{\"type\": \"addr\", \"value\": %d}, {\"type\": \"type\", \"value\": 2256, \"alias\": \"_edge\"}, {\"type\": \"addr\", \"value\": %d, \"alias\": \"_agent\"}]], \"params\": {}}}";
}
