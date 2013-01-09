#! /usr/bin/env bash
cd `cd $(dirname $0) && pwd`

Usage() {
    echo $1
    cat <<HSTRING
iterate to process all job_timestamp_num_XXXXX log file

Usage: `basename $0` job-dir output.csv

HSTRING
    exit 1
}

extractSH=./fetchJobInfo.py  
jobDir=$1
if [[ ! -d $jobDir ]]; then Usage "not exists dir: $jobDir"; fi
if [[ "$2" == '' ]]; then outfile=`tty`; else outfile=$2; fi

$extractSH gettitle | tr '\t' ',' > $outfile
find $jobDir -name 'job_*' | grep -v '.*_conf.xml' | sort | xargs -I{} $extractSH {} | tr '\t' ',' >> $outfile
