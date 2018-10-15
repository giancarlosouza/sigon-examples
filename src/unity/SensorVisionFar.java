package unity;

import br.ufsc.ine.agent.context.communication.Sensor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SensorVisionFar extends Sensor {

    private String sensorName = "VisionFar";
    private Socket socket;
    private int port = 2401;
    private float mass = 80.0f;
    private HashSet<Perception> perceptions = new HashSet<>();
    private HashSet<Perception> oldPerceptions = new HashSet<>();
    private HashMap<String, Perception> latestObjectPerception = new HashMap<>();
    private double timeToCheckPerceptions = 0;
    private int minPriority = 1;
    private int maxPriority = 6;

    @Override
    public void run() {
        /*double startTime = System.nanoTime() /1000000.0;
        double endTime = (System.nanoTime() /1000000.0) - startTime;
        ArrayList<Double> time = new ArrayList<Double>();
        while(endTime <= 30000){
            super.publisher.onNext("test.");
            endTime = (System.nanoTime() /1000000.0) - startTime;
            time.add(endTime);
            //System.out.println(endTime);
        }
        ArrayList<Double> time2 = new ArrayList<Double>();
        time2.add(time.get(0));
        for(int i = 1; i < time.size(); i++){
            double tempo = time.get(i) - time.get(i-1);
            time2.add(tempo);
        }
        double soma = 0;
        for(int i = 0; i < time2.size(); i++){
            System.out.print(time2.get(i) + ",");
            soma += time2.get(i);
        }
        System.out.println("Media: " + (soma / time2.size()));*/
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
                effectiveMassPriorityStrategy(msg);
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

    public void differentPerceptionPriorityStrategy(String msg){
        Perception p = new Perception(msg.substring(0, msg.lastIndexOf(",")));
        perceptions.remove(p);
        perceptions.add(p);

        HashSet<Perception> temp = new HashSet<>();

        perceptions.stream().forEach(perception -> {
            perception.setTimeCount(perception.getTimeCount() - 1);
            if (perception.getTimeCount() == 0) {
                temp.add(perception);
            }
        });

        perceptions.removeAll(temp);
        oldPerceptions.removeAll(temp);

        if (Double.valueOf(msg.substring(msg.lastIndexOf(",")+1)) - timeToCheckPerceptions > 2) {
            int oldPerceptionsSize = oldPerceptions.size();
            oldPerceptions.addAll(perceptions);

            if (oldPerceptions.size() > oldPerceptionsSize) {
                if (this.getPriority() < this.getMaxPriority()) {
                    this.setPriority(this.getPriority() + 1);
                }
            } else {
                if (this.getPriority() > this.getMinPriority()) {
                    this.setPriority(this.getPriority() - 1);
                }
            }
            System.out.println(this.sensorName + ", " + this.getPriority() + ", "
                    + msg.substring(msg.lastIndexOf(",")+1) + ", " + (oldPerceptions.size() - oldPerceptionsSize));
            timeToCheckPerceptions = Double.valueOf(msg.substring(msg.lastIndexOf(",")+1));
        }
        checkLatestPerceptionToPublish(p);
    }

    public void effectiveMassPriorityStrategy(String msg){
        Perception p = new Perception(msg.substring(0, msg.lastIndexOf(",")));
        perceptions.remove(p);
        perceptions.add(p);

        if (Double.valueOf(msg.substring(msg.lastIndexOf(",")+1)) - timeToCheckPerceptions > 2) {
            float priorityCalculation = 1 - (mass / perceptions.size());
            int newPriority = Math.round(priorityCalculation * 10);
            if(newPriority > this.getMaxPriority()){
                newPriority = this.getMaxPriority();
            } else if (newPriority < this.getMinPriority()){
                newPriority = this.getMinPriority();
            }
            this.setPriority(newPriority);
            System.out.println(this.sensorName + ", " + this.getPriority() + ", "
                    + msg.substring(msg.lastIndexOf(",")+1) + ", " + this.perceptions.size());
            timeToCheckPerceptions = Double.valueOf(msg.substring(msg.lastIndexOf(",")+1));
            perceptions.clear();
        }
        //checkLatestPerceptionToPublish(p);
    }

    public void checkLatestPerceptionToPublish(Perception p){
        String objectKey = p.getValue().substring(0, p.getValue().indexOf(","));
        if(latestObjectPerception.get(objectKey) != null){
            if(!latestObjectPerception.get(objectKey).getValue().equals(p.getValue())){
                //System.out.println("Atualizando e Publicando: " + p.getValue());
                latestObjectPerception.put(objectKey,p);
                super.publisher.onNext(p.getValue());
            }
        } else {
            //System.out.println("Colocando: " + p.getValue());
            latestObjectPerception.put(objectKey,p);
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