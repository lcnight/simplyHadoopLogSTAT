package com.taomee;

import java.util.Map;
import java.net.URI;
import org.apache.hadoop.fs.*;
//import org.apache.hadoop.mapred.JobHistory.Keys;
//import org.apache.hadoop.mapred.JobHistory.Values;
import org.apache.hadoop.mapred.JobHistory.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.conf.*;

/**
 * Hello world!
 *
 */
public class JobInfoLog
{
    public static void main( String[] args ) throws Exception
    {
        // pathlogname
        Path logfilepath = new Path(args[0]);
        String logFileName = logfilepath.getName();
        String[] jobDetails = logFileName.split("_");
        String jobid = jobDetails[0] + "_" + jobDetails[1] + "_" + jobDetails[2];
        System.out.println("fetch Job ID: " + jobid);

        JobHistory.JobInfo jobInfo = new JobHistory.JobInfo(jobid);

        //FileSystem fs = new LocalFileSystem();
        FileSystem fs = new RawLocalFileSystem();
        fs.initialize(new URI("file:///"), new Configuration());

        DefaultJobHistoryParser.parseJobTasks(logfilepath.toUri().getPath(),
                jobInfo, fs);

        // map shuffle reduce
        int[] totalTime  = {0, 0, 0};
        int[] totalTasks = {0, 0, 0};
        totalTasks[0] = Integer.parseInt(jobInfo.get(Keys.TOTAL_MAPS));
        totalTasks[1] = Integer.parseInt(jobInfo.get(Keys.TOTAL_REDUCES));
        totalTasks[2] = totalTasks[1];
        //for (JobHistroy.Task task : jobInfo.getAllTasks().values()) {
        for (org.apache.hadoop.mapred.JobHistory.Task task : jobInfo.getAllTasks().values()) {
            String tasktype = task.get(Keys.TASK_TYPE);
            // diff according task type
            boolean isMapTask = Values.MAP.name().equals(tasktype);
            if(isMapTask) {
                printKeyValue(task, Keys.TASKID, Keys.START_TIME, Keys.FINISH_TIME, Keys.SPLITS);
            } else {
                printKeyValue(task, Keys.TASKID, Keys.START_TIME, Keys.FINISH_TIME);
            }
            Map<String, TaskAttempt> attempts = task.getTaskAttempts();
            for (JobHistory.TaskAttempt attempt : attempts.values()) {
                String attName = attempt.get(Keys.TASK_ATTEMPT_ID);
                String attStatus = attempt.get(Keys.TASK_STATUS);
                if (attStatus.equals(Values.SUCCESS.name())) {
                    long processTime = (attempt.getLong(Keys.FINISH_TIME) - 
                            attempt.getLong(Keys.START_TIME)) / 1000;
                    if (Values.MAP.name().equals(tasktype)) {
                        //mapTasks[mapIndex++] = attempt;
                        //avgMapTime += avgFinishTime;
                        println("map taskAttempt: " + attName);
                        println(String.format("map processing time: %d sec", processTime));
                        // map metrics
                        totalTime[0] += processTime;
                        //totalTasks[0] += 1;
                    } else if (Values.REDUCE.name().equals(tasktype)) {
                        //reduceTasks[reduceIndex++] = attempt;
                        long shuffleTime = (attempt.getLong(Keys.SHUFFLE_FINISHED) -
                                attempt.getLong(Keys.START_TIME)) / 1000;
                        long sortTime = (attempt.getLong(Keys.SORT_FINISHED) - 
                                attempt.getLong(Keys.SHUFFLE_FINISHED)) / 1000;
                        long reduceTime = (attempt.getLong(Keys.FINISH_TIME) -
                                attempt.getLong(Keys.SHUFFLE_FINISHED)) / 1000;
                        println("reduce taskAttempt: " + attName);
                        println(String.format("shuffle : %d sec", shuffleTime),
                                String.format("sort : %d sec", sortTime),
                                String.format("reduce : %d sec", reduceTime));
                        // shuffle metrics
                        totalTime[1] += shuffleTime;
                        //totalTasks[1] += 1;
                        // reduce metrics
                        totalTime[2] += reduceTime;
                        //totalTasks[2] += 1;
                    } else {
                        println(String.format("<< %s >> taskAttempt: %s ", tasktype, attName));
                    }
                    printKeyValue(attempt, Keys.TASK_STATUS, Keys.START_TIME, Keys.FINISH_TIME);
                } else {
                    System.out.printf("***%s*** task ID: %s attempt ID: %s\n", 
                            attStatus, attempt.get(Keys.TASKID), attName);
                }
                println();
            }
        } // end of for

        println("[SUMMARY]\n");
        printKeyValue(jobInfo, Keys.JOBID, Keys.JOBNAME, Keys.USER, 
                Keys.SUBMIT_TIME, Keys.LAUNCH_TIME, Keys.FINISH_TIME,
                Keys.JOB_STATUS, Keys.TOTAL_MAPS, Keys.TOTAL_REDUCES, 
                Keys.JOB_QUEUE, Keys.FINISHED_MAPS, Keys.FINISHED_REDUCES,
                Keys.FAILED_MAPS, Keys.FAILED_REDUCES, Keys.FAIL_REASON);
        long job_submit = jobInfo.getLong(Keys.SUBMIT_TIME);
        long job_lunch = jobInfo.getLong(Keys.LAUNCH_TIME);
        long job_finish = jobInfo.getLong(Keys.FINISH_TIME);
        println(String.format("pending time: %f sec",  (job_lunch - job_submit) / 1000f), 
                String.format("processing time: %f second\n",  (job_finish - job_lunch) / 1000f));
        println(String.format("\tMAP tasks: %d, avg map time %d sec", totalTasks[0], totalTime[0]/totalTasks[0]));
        println(String.format("\tREDUCE avg SHUFFLE time %d sec", totalTime[1]/totalTasks[1]));
        println(String.format("\tREDUCE tasks: %d, avg reduce time %d sec", totalTasks[2], totalTime[2]/totalTasks[2]));
    }

    public static void printKeyValue(JobInfo job, Keys... ks) {
        int pCnt = 0;
        for (Keys k : ks) {
            String kstr = k.toString();
            System.out.printf("%s: %s\t", kstr, job.get(k));

            if ((++pCnt) % 3 == 0) System.out.println();
        }
        System.out.println();
    }
    public static void printKeyValue(org.apache.hadoop.mapred.JobHistory.Task job, Keys... ks) {
        int pCnt = 0;
        for (Keys k : ks) {
            String kstr = k.toString();
            System.out.printf("%s: %s\t", kstr, job.get(k));

            if ((++pCnt) % 3 == 0) System.out.println();
        }
        if (pCnt % 3 > 0) { System.out.println(); }
    }

    public static void println(String... abc) {
        for(String s: abc) {
            System.out.printf("%s\t", s);
        }
        System.out.println();
    }
    public static void printKV(String k, String v) {
        System.out.printf("%s = %s\n", k, v);
    }
}
