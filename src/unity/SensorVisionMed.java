package unity;

import br.ufsc.ine.agent.context.communication.Sensor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

public class SensorVisionMed extends Sensor {

    private String sensorName = "VisionMed";
    private Socket socket;
    private int port = 2402;
    private HashSet<Perception> perceptions = new HashSet<>();
    private HashSet<Perception> oldPerceptions = new HashSet<>();
    private int timeToCheckPerceptions = 1000;
    private int minPriority = 3;
    private int maxPriority = 8;

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server Started and listening to the port " + port);
            socket = serverSocket.accept();

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while (true) {
                String msg = br.readLine();
                if (msg == null) {
                    break;
                }
                //System.out.println("Message received from client is " + msg);
                Perception p = new Perception(msg);
                perceptions.remove(p);
                perceptions.add(p);

                HashSet<Perception> temp = new HashSet<>();

                perceptions.stream().forEach(perception -> {
                    perception.setTimeCount(perception.getTimeCount() - 1);
                    if (perception.getTimeCount() == 0) {
                        //System.out.println("Removing " + perception.getValue());
                        temp.add(perception);
                    }
                });

                perceptions.removeAll(temp);
                oldPerceptions.removeAll(temp);

                //System.out.println(perceptions.size());
                //System.out.println();

                if (timeToCheckPerceptions == 0) {
                    int oldPerceptionsSize = oldPerceptions.size();
                    oldPerceptions.addAll(perceptions);

                    if (oldPerceptions.size() > oldPerceptionsSize) {
                        if (this.getPriority() < this.getMaxPriority()) {
                            this.setPriority(this.getPriority() + 1);
                            System.out.println(this.sensorName + " Priority " + this.getPriority());
                        }
                    } else {
                        if (this.getPriority() > this.getMinPriority()) {
                            this.setPriority(this.getPriority() - 1);
                        }
                        System.out.println(this.sensorName + " Priority " + this.getPriority());
                    }

                    timeToCheckPerceptions = 1000;
                }

                timeToCheckPerceptions--;

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getMinPriority() {
        return minPriority;
    }

    public void setMinPriority(int minPriority) {
        this.minPriority = minPriority;
    }

    public int getMaxPriority() {
        return maxPriority;
    }

    public void setMaxPriority(int maxPriority) {
        this.maxPriority = maxPriority;
    }
}