#!/usr/local/bin/bash

/gpfs/main/sys/shared/psfu/local/projects/matlab/R2013b/bin/matlab -nodisplay -nosplash -nodesktop -r "addpath '/home/amm4/git/generalResearch/Behavioral/Scripts';AnalyzeData_SP('/home/amm4/git/generalResearch/Behavioral/Fiery Studies/Lit3/data_processed.mat','/home/amm4/git/generalResearch/Behavioral/Fiery Studies/Lit3/Params',$SGE_TASK_ID);exit;"
