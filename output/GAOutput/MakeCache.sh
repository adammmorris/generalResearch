#!/usr/local/bin/bash
#$ -cwd
#PBS -e /dev/null
#PBS -o /dev/null

# Set output file
output_file="/home/amm4/git/generalResearch/output/GAOutput/StealPunish/SymFM/Mem2/2b"

# Set run parameters
row=$SGE_TASK_ID
lr=.1
probBackTurned=0

# Set payoff matrix
rwd_init=1.0
rwd_initee=-1.0
rwd_respond=-.5
rwd_respondee=-2.5

# Set params
nParams=2
param1_min=0
param1_max=2
param1_step=2
param2_min=0
param2_max=1
param2_step=1
param3_min=0
param3_max=10
param3_step=10
param4_min=0
param4_max=10
param4_step=10

/gpfs/main/home/amm4/java/jdk1.8.0_05/bin/java -jar StealPunish/SymFM/Mem2/Game.jar $output_file $row $lr $probBackTurned $rwd_init $rwd_initee $rwd_respond $rwd_respondee $nParams $param1_min $param1_max $param1_step $param2_min $param2_max $param2_step $param3_min $param3_max $param3_step $param4_min $param4_max $param4_step
