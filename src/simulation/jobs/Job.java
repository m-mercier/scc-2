package simulation.jobs;

import desmoj.core.simulator.*;
import desmoj.core.statistic.Aggregate;
import desmoj.core.statistic.Tally;
import simulation.jobs.handlers.JobShop;
import simulation.jobs.navigation.Route;

import java.util.concurrent.TimeUnit;

public class Job extends SimProcess {
    public int jobType;
    public Route route;
    private boolean hasLeftIO;
    public double timeWhenGenerated;

    public double timeOfArrival;
    public double timeInQueue;

    public Job(Model owner, String name, boolean showInTrace, int jobType) {
        super(owner, name, showInTrace);
        this.jobType = jobType;
        this.hasLeftIO = false;
    }

    public void lifeCycle() {
        JobShop jobShop = (JobShop) getModel();

        while (true) {
            if (route.current() != 5) {
                double serviceTime = jobShop.getJobServiceTime(jobType, route.current());
                hold(new TimeSpan(serviceTime, TimeUnit.MINUTES));
            } else {
                if (hasLeftIO) {
                    sendTraceNote("Job has finished");
                    jobShop.jobCounter--;                                           //decrements number of running and waiting Jobs
                    jobShop.stations[route.current()].getJobQueue().remove(this);   //removes this Job from IO queue
                    jobShop.overallJobCycle.update(jobShop.presentTime().getTimeAsDouble() - this.timeWhenGenerated);
                    return;
                }
            }

            hasLeftIO = true;
            passivate();
        }
    }

    /**
     * Method used to set the time at which the Job was created
     * @param timeWhenGenerated double value that represents the time in seconds
     */
    public void setGenTime(double timeWhenGenerated) {
        this.timeWhenGenerated = timeWhenGenerated;
        this.timeOfArrival = timeWhenGenerated;
    }

    /**
     * Method used to set the route for this Job
     */
    public void setRoute(int[] sequence) {
        this.route = new Route(sequence);
    }

    /**
     * Method used to return the time since the beginning of the simulation;
     * i.e. the sum of: [the time at which the job was generated], [delays of the job] and [any other time the job will take to do something]
     * @return time value in seconds the job took, so far
     */
    public double getTimeSinceBeg() {
        return ((JobShop) getModel()).presentTime().getTimeAsDouble() - timeWhenGenerated;
    }

    public void setTimeOfArrival(double time) {
        this.timeOfArrival = time;
    }

    public String toString() {
        return String.format("[type %d]", jobType);
    }
}
