package InfrastructureManager.Rest;

import InfrastructureManager.Master;
import InfrastructureManager.MasterInput;
import spark.*;

public class RestInput implements MasterInput {
    private static String command = "";

    public static Route readParameterTest = (Request request, Response response) -> request.params(":input");

    /**
     * Execute route for POST request that only sends one way data to the Master
     * Returns true if the request is succesful with code 200
     */
    public static Route executeCommand = (Request request, Response response) -> {
        command = request.params(":command").replaceAll("\\s+","");
        return response.status();
    };

    @Override
    public String read() throws Exception {
        if(command.isEmpty()) {
           throw new Exception("No command exception");
        }
        String executed = command;
        command = "";
        return executed;
    }
}