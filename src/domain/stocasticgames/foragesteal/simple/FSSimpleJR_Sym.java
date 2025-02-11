package domain.stocasticgames.foragesteal.simple;

import java.util.HashMap;
import java.util.Map;

import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.GroundedSingleAction;
import burlap.oomdp.stochasticgames.JointAction;
import burlap.oomdp.stochasticgames.JointReward;
import domain.stocasticgames.foragesteal.simple.FSSimple_Sym.ForageAction;

public class FSSimpleJR_Sym implements JointReward {

	double stealerReward = 1.;
	double stealeeReward = -1.;
	double puncherReward = -2;
	double puncheeReward = -2;
	double [] forageRewards = new double[]{0,1,2,-1,-2};
	
	public FSSimpleJR_Sym(double stealValue, double puncherReward, double puncheeReward, double...forageRewards){
		this.stealerReward = stealValue;
		this.stealeeReward = -stealValue;
		this.puncherReward = puncherReward;
		this.puncheeReward = puncheeReward;
		this.forageRewards = forageRewards;
	}
	
	/*public FSSimpleJR(double stealerValue, double stealeeValue, double puncherReward, double puncheeReward, double...forageRewards){
		this.stealerReward = stealerValue;
		this.stealeeReward = stealeeValue;
		this.puncherReward = puncherReward;
		this.puncheeReward = puncheeReward;
		this.forageRewards = forageRewards;
	}*/
	
	@Override
	public Map<String, Double> reward(State s, JointAction ja, State sp) {
		
		Map<String, Double> r = new HashMap<String, Double>();
		
		GroundedSingleAction player0Action = null;
		GroundedSingleAction player1Action = null;
		
		for(GroundedSingleAction gsa : ja){
			ObjectInstance player = s.getObject(gsa.actingAgent);
			if(player.getDiscValForAttribute(FSSimple_Sym.ATTPN) == 0){
				player0Action = gsa;
			}
			else{
				player1Action = gsa;
			}
		}
		
		if(FSSimple_Sym.isRootNode(s)){
			
			if(player0Action.action.actionName.startsWith(FSSimple_Sym.ACTIONFORAGEBASE)){
				int fa = this.forageAltForAction(player0Action);
				r.put(player0Action.actingAgent, this.forageRewards[fa]);
				if(player1Action != null){
					r.put(player1Action.actingAgent, 0.);
				}
			}
			else{
				//must have stolen
				r.put(player0Action.actingAgent, this.stealerReward);
				if(player1Action != null){
					r.put(player1Action.actingAgent, this.stealeeReward);
				}
			}
			
		}
		else{
			
			if(player1Action.action.actionName.equals(FSSimple_Sym.ACTIONPUNISH)){
				
				if(player0Action != null){
					r.put(player0Action.actingAgent, this.puncheeReward);
				}
				r.put(player1Action.actingAgent, this.puncherReward);
			}
			else{
				//must have done nothing
				if(player0Action != null){
					r.put(player0Action.actingAgent, 0.);
				}
				r.put(player1Action.actingAgent, 0.);
			}
			
		}
		
		for(Double d : r.values()){
			if(Double.isNaN(d)){
				throw new RuntimeException("NaN reward");
			}
		}
		
		
		return r;
	}

	
	public double getStealerReward() {
		return stealerReward;
	}

	public double getStealeeReward() {
		return stealeeReward;
	}

	public double getPuncherReward() {
		return puncherReward;
	}

	public double getPuncheeReward() {
		return puncheeReward;
	}

	public double[] getForageRewards() {
		return forageRewards;
	}

	protected int forageAltForAction(GroundedSingleAction gsa){
		ForageAction sa = (ForageAction)gsa.action;
		return sa.falt;
	}
	
}