package simulation.jobs.handlers;

import desmoj.core.simulator.*;
import desmoj.core.dist.*;

import desmoj.core.statistic.Aggregate;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;
import simulation.jobs.Job;
import simulation.station.Station;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class JobShop extends Model {
    private ContDistExponential jobArrivalTime;

    public boolean isRoundRobin = false;

    public Count distanceCovered;
    public ArrayList<Tally> delayForAGVByJobType;
    public ArrayList<Tally> delayInQueueByJobType;

    public ArrayList<Aggregate[]> timeMachineSpentByState;

    public Aggregate agvMoving;

    public ArrayList<Count> numberOfJobsInQueueByStation;
    public ArrayList<Aggregate> timeAverageNumberOfJobsInQueueByStation;

    public Tally overallJobCycle;

    int machinesPerStation[] = {3, 3, 4, 4, 1};
    char nameStations[] = {'A', 'B', 'C', 'D', 'E'};

    public double totalSimTime = 2900.0 * 60.0;

    public int jobCounter = 0;

    private int jobServiceTimeMean[][] = {
            {36, 51, 30, -1, 30},
            {48, -1, 45, 66, -1},
            {42, 72, 60, 54, 15}
    };

    private ContDistErlang jobServiceTime[][];

    int distanceBetweenStations[][] = {
            {0, 45, 50, 90, 100, 135},
            {45, 0, 50, 100, 90, 135},
            {50, 50, 0, 50, 50, 90},
            {90, 100, 50, 0, 45, 50},
            {100, 90, 50, 45, 0, 50},
            {135, 135, 90, 50, 50, 0}
    };

    public int jobRouting[][] = {
            {2, 0, 1, 4},
            {3, 0, 2},
            {1, 4, 0, 3, 2}
    };

    public Station stations[];
    public Station ioStation;

    public AGV agv;

    public double getJobArrivalTime() {
        return jobArrivalTime.sample();
    }

    public double getJobServiceTime(int jobType, int station) {
        return jobServiceTime[jobType][station].sample();
    }

    /**
     * Calculates stations distances according to a table
     * @param station1 first station to be used
     * @param station2 second station to be used
     * @return distance distance between the 2 designated Stations
     */
    public int getDistanceBetweenStations(int station1, int station2) {
        return distanceBetweenStations[station1][station2];
    }

    public String description() {
        return "This is the job shop model";
    }

    public void doInitialSchedules() {
        stations = new Station[6];

        for (int i = 0; i < 5; i++) {
            stations[i] = new Station(this, "simulation.station.Station " + nameStations[i], i, true, machinesPerStation[i]);
            stations[i].activate(new TimeSpan(0));
        }

        ioStation = new Station(this, "IO simulation.station.Station", 5, true, 1);

        ioStation.activate(new TimeSpan(0));
        stations[5] = ioStation;

        agv = new AGV(this, "simulation.jobs.handlers.AGV", true);

        JobGenerator jobGenerator = new JobGenerator(this, "JobArrival", true);
        jobGenerator.activate(new TimeSpan(0));
    }

    public void init() {
        jobArrivalTime = new ContDistExponential(this, "JobTimeStream", 15, true, true);

        jobServiceTime = new ContDistErlang[5][5];

        for (int i = 0; i < 3; i++) {
            for (int u = 0; u < 5; u++) {
                if (jobServiceTimeMean[i][u] != -1) {
                    jobServiceTime[i][u] = new ContDistErlang(this, "JobServiceStream", 2, jobServiceTimeMean[i][u], true, true);
                }
            }
        }

        /* Statistics */
        distanceCovered = new Count(this, "Distance Covered", true, true);

        timeMachineSpentByState = new ArrayList<Aggregate[]>();
        for (int j = 0; j < 5; j++) {
            Aggregate[] timesByState = new Aggregate[3];
            String[] states = new String[]{"idle", "blocked", "working"};
            for (int v = 0; v < 3; v++) {
                timesByState[v] = new Aggregate(this, String.format("[Station %s] Time machines spent %s", nameStations[j], states[v]), true, true);
            }
            timeMachineSpentByState.add(timesByState);
        }

        delayForAGVByJobType = new ArrayList<Tally>();
        delayForAGVByJobType.add(new Tally(this, "Delay for AGV Job Type 1", true, true));
        delayForAGVByJobType.add(new Tally(this, "Delay for AGV Job Type 2", true, true));
        delayForAGVByJobType.add(new Tally(this, "Delay for AGV Job Type 3", true, true));

        delayInQueueByJobType = new ArrayList<Tally>();
        delayInQueueByJobType.add(new Tally(this, "Delay In Queue Job Type 1", true, true));
        delayInQueueByJobType.add(new Tally(this, "Delay In Queue Job Type 2", true, true));
        delayInQueueByJobType.add(new Tally(this, "Delay In Queue Job Type 3", true, true));

        numberOfJobsInQueueByStation = new ArrayList<Count>();
        numberOfJobsInQueueByStation.add(new Count(this, "Number of Jobs in Queue A", false, false));
        numberOfJobsInQueueByStation.add(new Count(this, "Number of Jobs in Queue B", false, false));
        numberOfJobsInQueueByStation.add(new Count(this, "Number of Jobs in Queue C", false, false));
        numberOfJobsInQueueByStation.add(new Count(this, "Number of Jobs in Queue D", false, false));
        numberOfJobsInQueueByStation.add(new Count(this, "Number of Jobs in Queue E", false, false));
        numberOfJobsInQueueByStation.add(new Count(this, "Number of Jobs in IO Queue", false, false));

        timeAverageNumberOfJobsInQueueByStation = new ArrayList<Aggregate>();
        timeAverageNumberOfJobsInQueueByStation.add(new Aggregate(this, "Mean Number of Jobs in Queue A", true, true));
        timeAverageNumberOfJobsInQueueByStation.add(new Aggregate(this, "Mean Number of Jobs in Queue B", true, true));
        timeAverageNumberOfJobsInQueueByStation.add(new Aggregate(this, "Mean Number of Jobs in Queue C", true, true));
        timeAverageNumberOfJobsInQueueByStation.add(new Aggregate(this, "Mean Number of Jobs in Queue D", true, true));
        timeAverageNumberOfJobsInQueueByStation.add(new Aggregate(this, "Mean Number of Jobs in Queue E", true, true));
        timeAverageNumberOfJobsInQueueByStation.add(new Aggregate(this, "Mean Number of Jobs in IO Queue", true, true));

        agvMoving = new Aggregate(this, "AGV Moving Time", false, false);

        overallJobCycle = new Tally(this, "Overall Job Cycle", true, true);

        setStatUnits();
    }

    private void setStatUnits() {
        distanceCovered.setUnit("Meters");
        for (Aggregate[] aggregateList : timeMachineSpentByState) {
            for (int i = 0; i < aggregateList.length; i++) {
                aggregateList[i].setUnit("Percentage");
            }
        }
        for (Tally delay : delayForAGVByJobType) {
            delay.setUnit("Minutes");
        }

        for (Tally delay : delayInQueueByJobType) {
            delay.setUnit("Minutes");
        }

        for (Aggregate aggregate : timeAverageNumberOfJobsInQueueByStation) {
            aggregate.setUnit("Jobs per Minute");
        }
        overallJobCycle.setUnit("Minutes");
    }

    public JobShop(Model owner, String modelName, boolean showInReport, boolean showInTrace) {
        super(owner, modelName, showInReport, showInTrace);

        Experiment exp = new Experiment("JobShopExperiment",
                TimeUnit.SECONDS, TimeUnit.MINUTES, null);

        this.connectToExperiment(exp);

        exp.setShowProgressBar(false);  // display a progress bar (or not)
        exp.stop(new TimeInstant(totalSimTime, TimeUnit.MINUTES));   // set end of simulation at 2900 hours
        exp.tracePeriod(new TimeInstant(0), new TimeInstant(totalSimTime, TimeUnit.MINUTES));
        // set the period of the trace
        exp.debugPeriod(new TimeInstant(0), new TimeInstant(totalSimTime, TimeUnit.MINUTES));   // and debug output

        exp.start();

        handleFinalStats();
        exp.report();
        exp.finish();
    }

    public static void main(String[] args) {
        JobShop model = new JobShop(null, "Trabalho 3 de SCC", true, true);
    }

    private void handleFinalStats() {
        for (int i = 0; i < 6; i++) {
            timeAverageNumberOfJobsInQueueByStation.get(i).update(numberOfJobsInQueueByStation.get(i).getValue() / presentTime().getTimeAsDouble());
        }

        for (int j = 0; j < 5; j++) {
            double timeSpentBlocked = timeMachineSpentByState.get(j)[1].getValue();
            double timeSpentWorking = timeMachineSpentByState.get(j)[2].getValue();
            timeMachineSpentByState.get(j)[0].update(100.0 - (timeSpentBlocked + timeSpentWorking));
        }

        System.out.println(String.format("AGV spent %f percentage of time moving.", agvMoving.getValue()));
    }

    /**
     * Method used by the AGV to get access to the next Job to move (from all the stations)
     * @return nextJob reference to the Job that will be available first
     */
    public Job getNextJob() {
        Job nextJob = null;
        for (int i = 0; i < stations.length; i++) {
            if (!stations[i].getJobQueue().isEmpty()) {
                sendTraceNote(String.format("Job was found in queue %d", i));
                if (nextJob == null) {
                    nextJob = stations[i].getNextJob();
                } else {
                    if (nextJob.getTimeSinceBeg() > stations[i].getNextJob().getTimeSinceBeg()) {
                        nextJob = stations[i].getNextJob();
                    }
                }
            }
        }
        return nextJob;
    }
}