package simulation.jobs.handlers;

import desmoj.core.simulator.*;
import simulation.jobs.Job;

import java.util.concurrent.TimeUnit;

import java.util.Random;

public class JobGenerator extends SimProcess {
    Random randomGenerator;

    public JobGenerator(Model owner, String name, boolean showInTrace) {
        super(owner, name, showInTrace);
        randomGenerator = new Random();
    }

    public void lifeCycle() {
        JobShop jobShop = (JobShop) getModel();
        double jobArrivalTime = 0.0;

        while (true) {
            double prob = randomGenerator.nextDouble();
            int type = 2;

            if (prob <= 0.3) {
                type = 0;
            } else if (prob <= 0.8) {
                type = 1;
            }

            jobShop.jobCounter++;
            Job job = new Job(jobShop, "simulation.jobs.Job", true, type);
            job.setGenTime(jobArrivalTime);
            job.setRoute(jobShop.jobRouting[type]);
            jobShop.ioStation.getJobQueue().insert(job);
            //jobShop.ioStation.activateAfter(this);
            jobShop.agv.activateAfter(this);
            //job.activateAfter(this);

            jobArrivalTime = jobShop.getJobArrivalTime();
            hold(new TimeSpan(jobArrivalTime, TimeUnit.MINUTES));
        }
    }
}
