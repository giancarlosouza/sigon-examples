package sensor;

import br.ufsc.ine.agent.context.communication.Sensor;
import rx.subjects.PublishSubject;

public class PositionSensor extends Sensor {

    public static final PublishSubject<String> positionObservable = PublishSubject.create();

    @Override
    public void run() {
        positionObservable.subscribe(super.getPublisher());
    }
}
