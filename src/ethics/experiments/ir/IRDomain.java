package ethics.experiments.ir;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SingleAction;


/**
 * State node identification:
 * 	0: theif chooses, state when has not been punished in the past
 * 	1: theif chooes, state when has been punished previously
 * 	2: punisher chooses
 * @author James MacGlashan
 *
 */
public class IRDomain implements DomainGenerator {

	public static final String				ATTPN = "playerNum";
	public static final String				ATTSTATENODE = "stateNode";
	//public static final String				ATTFA = "forageAlternative";
	//public static final String				ATTBACKTURNED = "backIsTurned";
	
	public static final String				CLASSPLAYER = "player";
	//public static final String				CLASSFALT = "forageAlternative";
	public static final String				CLASSSTATENODE = "stateNode";
	
	
	//public static final String				ACTIONFORAGEBASE = "forage";
	public static final String				ACTIONINITIATE = "initiate";
	public static final String				ACTIONRESPOND = "respond";
	public static final String				ACTIONDONOTHING = "nothing";
	
	
	protected int nfalts = 5;
	protected int maxStateNodeID = 2;
	
	
	public IRDomain(){
		
	}
	
	@Override
	public Domain generateDomain() {
		
		SGDomain domain = new SGDomain();
		
		Attribute pnAtt = new Attribute(domain, ATTPN, Attribute.AttributeType.DISC);
		pnAtt.setDiscValuesForRange(0, 1, 1);
		
		Attribute snAtt = new Attribute(domain, ATTSTATENODE, Attribute.AttributeType.DISC);
		snAtt.setDiscValuesForRange(0, this.maxStateNodeID, 1);
		
		/*Attribute faAtt = new Attribute(domain, ATTFA, Attribute.AttributeType.DISC);
		faAtt.setDiscValuesForRange(0, 4, 1);
		
		Attribute btAtt = new Attribute(domain, ATTBACKTURNED, AttributeType.BOOLEAN);*/
		
		ObjectClass playerClass = new ObjectClass(domain, CLASSPLAYER);
		playerClass.addAttribute(pnAtt);
		//playerClass.addAttribute(btAtt);
		
		ObjectClass stateNodeClass = new ObjectClass(domain, CLASSSTATENODE);
		stateNodeClass.addAttribute(snAtt);
		
		/*ObjectClass forageClass = new ObjectClass(domain, CLASSFALT);
		forageClass.addAttribute(faAtt);
		
		
		for(int i = 0; i < 5; i++){
			SingleAction faction = new ForageAction(domain, i);
		}*/
		
		new InitiateAction(domain);
		new RespondAction(domain);
		new DoNothingAction(domain);

		return domain;
	}
	
	
	public static State getInitialState(Domain domain, String player0Name, String player1Name, int backTurned, int...forageValues){
		
		State s = new State();
		
		ObjectInstance player0 = new ObjectInstance(domain.getObjectClass(CLASSPLAYER), player0Name);
		ObjectInstance player1 = new ObjectInstance(domain.getObjectClass(CLASSPLAYER), player1Name);
		
		player0.setValue(ATTPN, 0);
		//player0.setValue(ATTBACKTURNED, 0);
		player1.setValue(ATTPN, 1);
		//player1.setValue(ATTBACKTURNED, backTurned);
		
		s.addObject(player0);
		s.addObject(player1);
		
		/*for(int i = 0; i < forageValues.length; i++){
			ObjectInstance f = new ObjectInstance(domain.getObjectClass(CLASSFALT), CLASSFALT+i);
			f.setValue(ATTFA, forageValues[i]);
			s.addObject(f);
		}*/
		
		ObjectInstance snode = new ObjectInstance(domain.getObjectClass(CLASSSTATENODE), CLASSSTATENODE);
		snode.setValue(ATTSTATENODE, 0);
		
		s.addObject(snode);
		
		return s;
		
	}
	
	public static void setStateNode(State s, int sn){
		ObjectInstance sno = s.getFirstObjectOfClass(CLASSSTATENODE);
		sno.setValue(ATTSTATENODE, sn);
	}
	
	
	
	
	public static boolean isRootNode(State s){
		int n = stateNode(s);
		return n == 0 || n == 1 || n == 3;
	}
	
	
	public static int stateNode(State s){
		ObjectInstance sn = s.getFirstObjectOfClass(CLASSSTATENODE);
		int n = sn.getDiscValForAttribute(CLASSSTATENODE);
		return n;
	}
	
	/*public class ForageAction extends SingleAction{

		public int falt;
		
		public ForageAction(SGDomain domain, int f){
			super(domain, ACTIONFORAGEBASE+f);
			this.falt = f;
		}
		
		@Override
		public boolean isApplicableInState(State s, String actingAgent, String[] params) {
			
			if(!isRootNode(s)){
				return false;
			}
			
			ObjectInstance p = s.getObject(actingAgent);
			if(p.getDiscValForAttribute(ATTPN) != 0){
				return false;
			}
			
			List<ObjectInstance> forageAlts = s.getObjectsOfTrueClass(CLASSFALT);
			for(ObjectInstance f : forageAlts){
				if(f.getDiscValForAttribute(ATTFA) == this.falt){
					return true;
				}
			}
			
			return false;
		}
		
	}*/
	
	
	public class InitiateAction extends SingleAction{

		public InitiateAction(SGDomain d) {
			super(d, ACTIONINITIATE);
		}

		@Override
		public boolean isApplicableInState(State s, String actingAgent,
				String[] params) {
			
			if(!isRootNode(s)){
				return false;
			}
			
			ObjectInstance p = s.getObject(actingAgent);
			if(p.getDiscValForAttribute(ATTPN) != 0){
				return false;
			}
			
			return true;
		}
		
		
		
	}
	
	public class RespondAction extends SingleAction{

		public RespondAction(SGDomain d) {
			super(d, ACTIONRESPOND);
		}

		@Override
		public boolean isApplicableInState(State s, String actingAgent,
				String[] params) {
			
			if(isRootNode(s)){
				return false;
			}
			
			ObjectInstance p = s.getObject(actingAgent);
			if(p.getDiscValForAttribute(ATTPN) != 1){
				return false;
			}
			
			
			return true;
		}
		
		
		
	}
	
	public class DoNothingAction extends SingleAction{

		public DoNothingAction(SGDomain d) {
			super(d, ACTIONDONOTHING);
		}

		@Override
		public boolean isApplicableInState(State s, String actingAgent,
				String[] params) {
			
			/*if (!isRootNode(s)) return false;
			
			ObjectInstance p = s.getObject(actingAgent);
			if(p.getDiscValForAttribute(ATTPN) != 0){
				return false;
			}*/
			
			return true;
		}
		
		
	}
	
	
	public static void main(String [] args){
		
/*		FSSimple gen = new FSSimple(3);
		SGDomain domain = (SGDomain)gen.generateDomain();
		//JointActionModel jam = new FSSimpleJAM();
		//JointActionModel jam = new FSSimpleBTJAM(0.5);
		JointActionModel jam = new FSSimpleBTSJAM(0.5, 0.5);
		//JointReward r = new FSSimpleJR();
		JointReward r = new FSSubjectiveRF(new FSSimplePOJR(1., -0.5, -2.5, 0.));
		((FSSubjectiveRF)r).setParameters(new double[]{0.,0.});
		
		State s = FSSimple.getInitialState(domain, "player0", "player1", 0, 0);
		
		SGTerminalExplorer exp = new SGTerminalExplorer(domain, jam);
		exp.setTrackingRF(r);
		
		for(int i = 0; i < gen.nfalts; i++){
			exp.addActionShortHand("f"+i, ACTIONFORAGEBASE+i);
		}
		exp.addActionShortHand("s", ACTIONSTEAL);
		exp.addActionShortHand("p", ACTIONPUNISH);
		exp.addActionShortHand("n", ACTIONDONOTHING);
		
		exp.exploreFromState(s);*/
		
		
		
	}
	

}
