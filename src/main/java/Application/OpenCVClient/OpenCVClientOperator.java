package Application.OpenCVClient;

import Application.Utilities.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class OpenCVClientOperator {
    private Socket clientSocket;
    private BufferedReader in;
    private DataOutputStream out;
    private DetectMarker detector;
    private ObjectMapper mapper;

    private String fileName = "singlemarkerssource.png";
    private String resultFileName = "detected.png";
    private String fileExtension = "png";
    private Mat subject;
    private BufferedImage currentImage;

    public Semaphore ipLock;
    public Semaphore serverLock;

    //Server Communication Properties
    private boolean utilizeServer;
    private String ip;
    private int port;

    public HeartBeatRunner beatRunner;
    public MasterCommunicationRunner masterCommunicationRunner;
    public ProcessingRunner processingRunner;

    private static OpenCVClientOperator instance = null;

    private OpenCVClientOperator() {
        mapper = new ObjectMapper();
        ipLock = new Semaphore(1);
        serverLock = new Semaphore(1);
        utilizeServer = false;
        detector = new DetectMarker();
    }

    public static OpenCVClientOperator getInstance() {
        if (instance == null) {
            instance = new OpenCVClientOperator();
        }
        return instance;
    }

    public void startConnection() throws IOException, InterruptedException {
        try {
            ipLock.acquire();
            clientSocket = new Socket(ip, port);
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            //TODO set connected to true
        } finally {
            ipLock.release();
        }
    }

    public String sendImage() throws IOException {
        String resp = "";

        currentImage = ImageIO.read(new File(fileName));
        ImageIO.write(currentImage, fileExtension, clientSocket.getOutputStream());
        resp = in.readLine();

        return resp;
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        //TODO set connceted to false
    }

    public void setServerUtilization(boolean input) {
        try {
            serverLock.acquire();
            utilizeServer = input;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            serverLock.release();
        }

    }

    public void setup(String[] args) throws IllegalArgumentException, InterruptedException {
        String[] argument;
        // make a rest request to the master to ask for server and port for server
        // do locally if no response
        try {
            ipLock.acquire();
            for(int i=0; i<args.length; i++) {
                argument = args[i].split("=");
                switch(argument[0]) {
                    case "PORT":
                        port = Integer.parseInt(argument[1]);
                        System.out.println("port :" +port);
                        break;
                    case "IP":
                        ip = argument[1];
                        System.out.println("ip :" +ip);
                        break;
                    default :
                        throw new IllegalArgumentException("Invalid argument");
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid argument format");
        } finally {
            ipLock.release();
        }
    }

    public void setupClientRunners(String[] args) throws IllegalArgumentException {
        String[] argument;
        String clientId = ""; // for test: client13
        String masterUrl = ""; // for test: http://host.docker.internal:4567/
        String beatCommand = ""; // for test: client/register
        String getServerCommand = ""; // for test: client/get_node/
        processingRunner = new ProcessingRunner();

        List<String> missingParameterList = new ArrayList<>(List.of("CLIENT_ID", "MASTER_URL", "BEAT_COMMAND", "GET_SERVER_COMMAND"));

        for(int i=0; i<args.length; i++) {
            System.out.println(args[i]);
            argument = args[i].split("=");
            if(argument.length == 2) {
                switch (argument[0]) {
                    case "CLIENT_ID":
                        clientId = argument[1];
                        missingParameterList.remove("CLIENT_ID");
                        break;
                    case "MASTER_URL":
                        masterUrl = argument[1];
                        missingParameterList.remove("MASTER_URL");
                        break;
                    case "BEAT_COMMAND":
                        beatCommand = argument[1];
                        missingParameterList.remove("BEAT_COMMAND");
                        break;
                    case "GET_SERVER_COMMAND":
                        getServerCommand = argument[1];
                        missingParameterList.remove("GET_SERVER_COMMAND");
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid argument");
                }
            }
        }

        if(missingParameterList.size() > 0) {
            throw new IllegalArgumentException("Missing Parameter: " + String.join(",", missingParameterList));
        }
        String beatUrl = masterUrl + beatCommand;
        String beatBody =  "{" +
                "\"id\" : \"" + clientId + "\"" +
                "}";
        beatRunner = new HeartBeatRunner(beatUrl, beatBody);
        String masterCommunicationUrl = masterUrl + getServerCommand + clientId;
        masterCommunicationRunner = new MasterCommunicationRunner(masterCommunicationUrl);
    }

    public void detectMarkerInServer() throws RemoteExecutionException, IOException {
        System.out.println("detectMarkerInServer");
        // connect to server and detect marker
        String response;
        JsonNode node;
        try {
            //TODO if not !connected start connection
            startConnection();
            response = sendImage().replace("\\\\", "");

            node = mapper.readTree(response);
            Mat ids = OpenCVUtil.deserializeMat(node.get("ids").asText());
            List<Mat> corners = OpenCVUtil.deserializeMatList(node.get("corners").asText());
            System.out.println("Marker Drawn with remote informations");

            detector.setIds(ids);
            detector.setCorners(corners);
        } catch (IOException | InterruptedException e) {
            stopConnection();
            throw new RemoteExecutionException(e);
        }
    }

    public void detectMarkerInClient() {
        detector.detect(subject);
        System.out.println("Marker Drawn locally");
    }

    public void markerDetection() {
        Mat result;
        subject = ImageProcessor.getImageMat(fileName);
        try {
            serverLock.acquire();
            if(utilizeServer) {
                detectMarkerInServer();
            } else {
                detectMarkerInClient();
            }
        } catch (RemoteExecutionException e) {
            detectMarkerInClient();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            serverLock.release();
        }

        result = detector.drawDetectedMarkers(subject);
        ImageProcessor.writeImage(resultFileName, result);
    }
}
