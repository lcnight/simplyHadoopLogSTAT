#! /usr/bin/env python
import os
import sys

def Usage():
    print """
recv one job log file, then extract job info as following

Usage: exe getTitle|job-log-file

info collumns:
    jobId jobName submitter queue priority submit-time launch-time maps reduces
    fin-time fin-maps fin-reudces fail-maps fail-reduces"""
    sys.exit(1)
metaChkInfo="Meta VERSION=\"1\""
JOB_START='Job JOBID'
TASK_START='Task TASKID'
STR_SEP='"'

KJID="JOBID=";  KJNAME="JOBNAME=";  KUSER='USER=';  KSUBTIME='SUBMIT_TIME='; KJQ='JOB_QUEUE=';
KJP='JOB_PRIORITY=';   KLANTIME='LAUNCH_TIME=';  KTMAPS='TOTAL_MAPS=';    KTREDUCES='TOTAL_REDUCES=';
KJSTAT='JOB_STATUS='; KFINTIME='FINISH_TIME='; KFINMAPS='FINISHED_MAPS='; KFINREDUCES='FINISHED_REDUCES=';
KFAILMAPS='FAILED_MAPS=';   KFAILREDUCES='FAILED_REDUCES=';
# compute collumns
COM_PENDING='pending'; COM_PROCESSING='processing';

MetaInfo = {KJID:None, KJNAME:None, KUSER:None, KSUBTIME:None, KJQ:None,
        KJP:None, KLANTIME:None, KTMAPS:None, KTREDUCES:None, KJSTAT:None,
        KFINTIME:None, KFINMAPS:None, KFINREDUCES:None, KFAILMAPS:None, KFAILREDUCES:None}
KeysArray=[KJID, KJNAME, KUSER, KJQ, KJP, KJSTAT, KSUBTIME, KLANTIME, KFINTIME, KTMAPS, KTREDUCES,
        KFINMAPS, KFINREDUCES, KFAILMAPS, KFAILREDUCES, COM_PENDING, COM_PROCESSING]
########################### main process ###########################
if len(sys.argv) < 2: Usage()
# build info title
if sys.argv[1].lower() == 'gettitle':
    for i in KeysArray:
        print i[0:-1] + '\t',
    print
    sys.exit(0)
# extract info collumns
jlPath=sys.argv[1]
if not os.path.exists(jlPath) or not os.path.isfile(jlPath):
    print "not pass sys check: " + jlPath
    Usage();

fd = open(jlPath, 'r');
firstLine = fd.readline();
if not firstLine.startswith(metaChkInfo):
    print "not pass meta check, should be '%s', current: '%s'" %(metaChkInfo, firstLine)
    Usage();

def getMetaKeyValue(key, line):
    idx = line.find(key);
    if idx == -1: return None
    idx = idx + len(key)
    if line[idx] != STR_SEP:
        sys.stderr.write("not in string format: idx[%d], line: %s" %(idx, line));
        return None

    ## get value for specified key, de-escape string '\'
    subvalue = ''
    while True:
        idx += 1
        c = line[idx];
        if c == STR_SEP or c == '\n': break;
        if c == '\\':
            idx += 1
            subvalue += line[idx]
        else:
            subvalue += line[idx]

    #print "get " + key + subvalue

    return subvalue



while True:
    line = fd.readline();
    if not line: break;

    if line.startswith(JOB_START): # process JOB INFO
        for k in MetaInfo:
            sss = getMetaKeyValue(k, line);
            if sss == None: continue
            else: MetaInfo[k] = sss

#for k, v in MetaInfo.items():
    #print k + str(v)

## process compute collumns
job_subTime = long(MetaInfo[KSUBTIME]);
job_lanchTime = long(MetaInfo[KLANTIME]);
job_finTime = long(MetaInfo[KFINTIME]);

MetaInfo[COM_PENDING] = (job_lanchTime - job_subTime) / 1000.0;
MetaInfo[COM_PROCESSING] = (job_finTime - job_lanchTime) / 1000.0;

for i in KeysArray:
    print str(MetaInfo[i]) + '\t',
print
