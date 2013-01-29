#! /usr/bin/env bash
cd `cd $(dirname $0) && pwd`

Usage() {
    echo $1
    cat <<HSTRING
process log file job_timestamp_num_XXXXX 

Usage: `basename $0` [options] 

options: 
    gettitle                            get summary title
    list <job-dir> [output.csv]         list all jobs summary
    detail <job file>                   get job detail info
HSTRING
    exit 1
}

extractSH=./fetchJobInfo.py  

case $1 in 
  gettitle) 
  $extractSH gettitle
  ;;
  list)
  jobDir=$2;  
  if [[ ! -d $jobDir ]]; then Usage "not exists dir: $jobDir"; fi
  if [[ "$3" == '' ]]; then outfile=`tty`; else outfile=$3; fi
  $extractSH gettitle | tr '\t' ',' > $outfile
  find $jobDir -name 'job_*' | grep -v '.*_conf.xml' | sort | xargs -I{} $extractSH {} | tr '\t' ',' >> $outfile
  ;;
  detail)
  jobfile=$2
  hadoop jar jobInfoLog-1.0.0.jar com.taomee.JobInfoLog $jobfile
  ;;
  *)
  Usage;
  ;;
esac
if [[ "$1" == "" ]]; then Usage; fi
