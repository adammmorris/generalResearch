%% Analyze Data
% This script gets called by the grid, starts the whole modeling process.

%% Params
% datapath: should have id, subjMarkers, roles, opponents, myActions, rewards1, rewards2
% savepath: path to save output to
% tasknum: start #

function AnalyzeData_SP_v2(datapath,savepath,tasknum)

% load shit
load(datapath);

numStarts = 100;
curMemory = 2;
numSubjects = length(subjMarkers);
%numSubjects = 5;

% tasknum is from 1 to numStarts
if (tasknum < 1 || tasknum > numStarts)
    error('tasknum must be between 1 and numStarts');
end

% Do params
% [lr,temp,gamma,steal_biases,punish_biases]
bias_lo = 0;
bias_hi = 10;
bounds_lo = [0 0 0];
bounds_hi = [1 3 1];

for i=1:(numSubjects*2)
    bounds_lo(end+1) = bias_lo;
    bounds_hi(end+1) = bias_hi;
end

bounds = [bounds_lo; bounds_hi];
numParams = size(bounds,2);

% Calculate starts
starts = zeros(numStarts,numParams);
for i=1:numParams
    starts(:,i) = linspace(bounds(1,i),bounds(2,i),numStarts);
end

model = @(x,roles,opponents,myActions,rewards1,rewards2,memory) getIndivLike_SP_v2(subjMarkers(1:numSubjects),x,roles,opponents,myActions,rewards1,rewards2,curMemory);

if numSubjects < length(subjMarkers)
    index = subjMarkers(1):(subjMarkers(numSubjects + 1) - 1);
else
    index = subjMarkers(1):length(roles);
end
[max_params,lik] = getIndivParams_SP_v2(model,roles(index),opponents(index),myActions(index),rewards1(index),rewards2(index),curMemory,[],[],bounds,starts(tasknum,:));

name = [savepath '/Params_Start' num2str(tasknum) '.txt'];
csvwrite(name,[lik max_params]);
end