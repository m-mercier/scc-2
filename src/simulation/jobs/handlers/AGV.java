package simulation.jobs.handlers;

import desmoj.core.simulator.*;
import desmoj.core.statistic.Accumulate;
import simulation.jobs.Job;
import simulation.station.Station;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class AGV extends SimProcess {
    private static final double SPEED = 2.5;
    public int currentStation;
    private double[] AGVDelays;

    public AGV(Model owner, String name, boolean showInTrace) {
        super(owner, name, showInTrace);
        currentStation = 5; // simulation.station 5 is IO simulation.station
        AGVDelays = new double[3];
    }

    public void lifeCycle() {
        JobShop jobShop = (JobShop) getModel();

        while(true) {
            Job jobToMove = jobShop.getNextJob();
            sendTraceNote(String.format("AGV found %s Job with route %s", jobToMove.toString(), jobToMove.route.toString()));

            if (jobToMove == null) {
                passivate();
            } else {
                int jobStation = jobToMove.route.current();
                int destStation = jobToMove.route.next();
                double timeToPickup = jobShop.getDistanceBetweenStations(currentStation, jobStation) / SPEED;
                double timeToDelivery = jobShop.getDistanceBetweenStations(jobStation, destStation) / SPEED;

                updateDelays(jobToMove.jobType, timeToPickup, timeToDelivery);

                /*handles Job pickup*/
                hold(new TimeSpan(timeToPickup, TimeUnit.SECONDS));                         //holds the time it takes for AGV to retrieve Job
                jobShop.stations[jobStation].getJobQueue().remove(jobToMove);               //removes Job from current queue
                jobShop.stations[jobStation].freeMachine();                                 //frees a machine from the station

                jobShop.delayForAGVByJobType.get(jobToMove.jobType).update(getAGVDelay(jobToMove.jobType));
                jobShop.delayInQueueByJobType.get(jobToMove.jobType).update(jobToMove.timeInQueue);

                /*handles Job delivery*/
                hold(new TimeSpan(timeToDelivery, TimeUnit.SECONDS));                       //holds the time it takes for AGV to deliver the Job
                jobShop.stations[destStation].getJobQueue().insert(jobToMove);              //adds Job to the next queue
                jobToMove.setTimeOfArrival(jobShop.presentTime().getTimeAsDouble());        //Sets the timer for Job arrival at Queue

                jobShop.agvMoving.update(100.0 * (timeToDelivery + timeToPickup) / (jobShop.totalSimTime*60.0));

                //AGV moves to detStation
                if (jobShop.isRoundRobin) {
                    double timeToIO = jobShop.getDistanceBetweenStations(destStation, 5) / SPEED;
                    jobShop.agvMoving.update(100.0 * (timeToIO)/(jobShop.totalSimTime*60.0));
                    hold(new TimeSpan(timeToIO, TimeUnit.SECONDS));
                    currentStation = 5;
                } else {
                    currentStation = destStation;
                }

                jobShop.stations[destStation].activateAfter(this);

                jobShop.distanceCovered.update(jobShop.getDistanceBetweenStations(jobStation, destStation));

                sendTraceNote(String.format("Taking Job from %d to %d", jobStation, destStation));
                passivate();
            }
        }
    }

    /**
     * Method used to calculate the AGV delays for each type of Job
     * @param jobType the type of Job that is being targeted/serviced by the AGV
     * @param timeToPickup time it takes for the AGV to get to the Job's Station
     * @param timeToDelivery time it takes for the AGV to deliver the Job to the next Station
     */
    private void updateDelays(int jobType, double timeToPickup, double timeToDelivery) {
        for (int i = 0; i < 3; i++) {
            AGVDelays[i] += timeToPickup;
            if (i != jobType) {
                AGVDelays[i] += timeToDelivery;
            }
        }
    }

    private double getAGVDelay(int jobType) {
        double delay = AGVDelays[jobType];
        AGVDelays[jobType] = 0.0;
        return delay;
    }
}
