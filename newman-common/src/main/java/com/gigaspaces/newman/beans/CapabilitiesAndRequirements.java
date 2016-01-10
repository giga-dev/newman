package com.gigaspaces.newman.beans;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tamirs
 * 9/24/15.
 */
public class CapabilitiesAndRequirements {

    public static List<Job> filterByCapabilities(List<Job> jobs, Set<String> capabilities){
        return jobs.stream().filter(job -> capabilities.containsAll(job.getSuite().getRequirements())).collect(Collectors.toList());
    }

    public static Comparator<Job> requirementsSort = (job_1, job_2) -> {
        int comp = job_2.getSuite().getRequirements().size() - job_1.getSuite().getRequirements().size();
        if(comp == 0){
            comp =  job_2.getId().compareTo(job_1.getId());
        }
        return comp;
    };

    public static Set<String> parse(String input){
        Set<String> output =  new TreeSet<>();
        if(input  != null) {
            StringTokenizer st = new StringTokenizer(input, ",");
            while (st.hasMoreTokens())
                output.add(st.nextToken());
        }
        return output;
    }
}
