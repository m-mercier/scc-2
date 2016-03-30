package simulation.station;

import desmoj.core.simulator.*;
import desmoj.core.statistic.Accumulate;
import simulation.jobs.Job;
import simulation.jobs.handlers.JobShop;

import java.util.ArrayList;

public class Station extends SimProcess {
    private int stationIndex;
    protected int maxMachines; // maximum number of machines
    protected int currentMachines; // currently available machines
    private ProcessQueue<Job> jobQueue;
    private int index;

    private boolean isPassivated;
    private double timeOfPassivation;
    private double lastTimeMeasure;

    public Station(Model owner, String name, int index, boolean showInTrace, int maxMachines) {
        super(owner, name, showInTrace);
        this.maxMachines = maxMachines;
        this.currentMachines = maxMachines;
        this.index = index;

        jobQueue = new ProcessQueue<Job>(owner, name + " Queue", true, true);

        this.stationIndex = index;
        timeOfPassivation = 0.0;
        lastTimeMeasure = 0.0;
        isPassivated = false;
    }

    public void lifeCycle() {
        JobShop jobShop = (JobShop) getModel();

        while (true) {
            jobShop.numberOfJobsInQueueByStation.get(index).update(jobQueue.size());

            if (jobQueue.isEmpty()) {
                sendTraceNote("no stuff in station queue");
                timeOfPassivation = jobShop.presentTime().getTimeAsDouble();        //Update time of passivation
                isPassivated = true;
                passivate();
            } else {
                sendTraceNote("station has stuff");
                double currentTime = jobShop.presentTime().getTimeAsDouble();

                if (stationIndex != 5) {
                    if (isPassivated == true) {                                                                             //station was just re-activated
                        isPassivated = false;
                        lastTimeMeasure = currentTime;
                    } else if (currentMachines == 0) {                                                                      //all machines are blocked
                        jobShop.timeMachineSpentByState.get(stationIndex)[1].update(100.0*(currentTime - lastTimeMeasure)/jobShop.totalSimTime);
                    } else {                                                                                                //some machines are working
                        jobShop.timeMachineSpentByState.get(stationIndex)[2].update(100.0*(currentTime - lastTimeMeasure)/jobShop.totalSimTime);
                    }
                }
                lastTimeMeasure = currentTime;

                //for each machine available, activate a new Job
                for (int i = (maxMachines-currentMachines); i < maxMachines && i < jobQueue.size(); i++) {
                    Job job = jobQueue.get(i);

                    if (job != null) {
                        job.activateAfter(this);
                        currentMachines--;
                        job.timeInQueue = jobShop.presentTime().getTimeAsDouble() - job.timeOfArrival;
                    }
                }

                passivate();
            }
        }
    }

    public void freeMachine() {
        this.currentMachines++;
    }

    public ProcessQueue<Job> getJobQueue() {
        return this.jobQueue;
    }

    /**
     * Method used to return the total time elapsed by the next Job to be completed (from the ones being attended by the machines)
     * @return time time elapsed from the beginning of the simulation to a process completion
     */
    public Job getNextJob() {
        Job nextJob = null;
        for (int i = 0; i < jobQueue.size() && i < maxMachines; i++) {
            if (nextJob == null) {
                nextJob = jobQueue.get(i);
            } else {
                if (nextJob.getTimeSinceBeg() > jobQueue.get(i).getTimeSinceBeg()) {
                    nextJob = jobQueue.get(i);
                }
            }
        }
        return nextJob;
    }
}
