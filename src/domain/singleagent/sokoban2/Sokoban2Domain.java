package domain.singleagent.sokoban2;

import java.util.List;

import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.explorer.VisualExplorer;
import burlap.oomdp.visualizer.Visualizer;

public class Sokoban2Domain implements DomainGenerator {

	public static final String					ATTX = "x";
	public static final String					ATTY = "y";
	public static final String					ATTDIR = "direction"; //optionally added attribute to include the agent's direction
	public static final String					ATTTOP = "top";
	public static final String					ATTLEFT = "left";
	public static final String					ATTBOTTOM = "bottom";
	public static final String					ATTRIGHT = "right";
	public static final String					ATTCOLOR = "color";
	public static final String					ATTSHAPE = "shape";
	
	
	public static final String					CLASSAGENT = "agent";
	public static final String					CLASSBLOCK = "block";
	public static final String					CLASSROOM = "room";
	public static final String					CLASSDOOR = "door";
	
	
	public static final String					ACTIONNORTH = "north";
	public static final String					ACTIONSOUTH = "south";
	public static final String					ACTIONEAST = "east";
	public static final String					ACTIONWEST = "west";
	
	public static final String					PFAGENTINROOM = "agentInRoom";
	public static final String					PFBLOCKINROOM = "blockInRoom";
	public static final String					PFAGENTINDOOR = "agentInDoor";
	public static final String					PFBLOCKINDOOR = "blockInDoor";
	
	
	public static final String[] 				COLORS = new String[]{"blue",
														"green", "magenta", 
														"red", "yellow"};

	public static final String[]				SHAPES = new String[]{"chair", "bag",
														"backpack", "basket"};
	
	
	public static final String[]				DIRECTIONS = new String[]{"north", "south", "east", "west"};
	
	protected static final String				PFRCOLORBASE = "roomIs";
	protected static final String				PFBCOLORBASE = "blockIs";
	protected static final String				PFBSHAPEBASE = "shape";
	
	
	
	
	protected int								maxX = 24;
	protected int								maxY = 24;
	protected boolean							includeDirectionAttribute = false;;
	
	
	public void setMaxX(int maxX){
		this.maxX = maxX;
	}
	
	public void setMaxY(int maxY){
		this.maxY = maxY;
	}
	
	public void includeDirectionAttribute(boolean includeDirectionAttribute){
		this.includeDirectionAttribute = includeDirectionAttribute;
	}
	
	
	@Override
	public Domain generateDomain() {
		
		SADomain domain = new SADomain();
		
		Attribute xatt = new Attribute(domain, ATTX, Attribute.AttributeType.DISC);
		xatt.setDiscValuesForRange(0, maxX, 1);
		
		Attribute yatt = new Attribute(domain, ATTY, Attribute.AttributeType.DISC);
		yatt.setDiscValuesForRange(0, maxY, 1);
		
		Attribute topAtt = new Attribute(domain, ATTTOP, Attribute.AttributeType.DISC);
		topAtt.setDiscValuesForRange(0, maxY, 1);
		
		Attribute leftAtt = new Attribute(domain, ATTLEFT, Attribute.AttributeType.DISC);
		leftAtt.setDiscValuesForRange(0, maxX, 1);
		
		Attribute bottomAtt = new Attribute(domain, ATTBOTTOM, Attribute.AttributeType.DISC);
		bottomAtt.setDiscValuesForRange(0, maxY, 1);
		
		Attribute rightAtt = new Attribute(domain, ATTRIGHT, Attribute.AttributeType.DISC);
		rightAtt.setDiscValuesForRange(0, maxX, 1);
		
		Attribute colAtt = new Attribute(domain, ATTCOLOR, Attribute.AttributeType.DISC);
		colAtt.setDiscValues(COLORS);
		
		Attribute shapeAtt = new Attribute(domain, ATTSHAPE, Attribute.AttributeType.DISC);
		shapeAtt.setDiscValues(SHAPES);
		
		if(this.includeDirectionAttribute){
			Attribute dirAtt = new Attribute(domain, ATTDIR, Attribute.AttributeType.DISC);
			dirAtt.setDiscValues(DIRECTIONS);
		}
		
		
		ObjectClass agent = new ObjectClass(domain, CLASSAGENT);
		agent.addAttribute(xatt);
		agent.addAttribute(yatt);
		if(this.includeDirectionAttribute){
			agent.addAttribute(domain.getAttribute(ATTDIR));
		}
		
		ObjectClass block = new ObjectClass(domain, CLASSBLOCK);
		block.addAttribute(xatt);
		block.addAttribute(yatt);
		block.addAttribute(colAtt);
		block.addAttribute(shapeAtt);
		
		ObjectClass room = new ObjectClass(domain, CLASSROOM);
		this.addRectAtts(domain, room);
		room.addAttribute(colAtt);
		
		ObjectClass door = new ObjectClass(domain, CLASSDOOR);
		this.addRectAtts(domain, door);
		
		
		Action northAction = new MovementAction(ACTIONNORTH, domain, 0, 1);
		Action southAction = new MovementAction(ACTIONSOUTH, domain, 0, -1);
		Action eastAction = new MovementAction(ACTIONEAST, domain, 1, 0);
		Action westAction = new MovementAction(ACTIONWEST, domain, -1, 0);
		
		
		
		PropositionalFunction air = new PFInRegion(PFAGENTINROOM, domain, new String[]{CLASSAGENT, CLASSROOM}, false);
		PropositionalFunction bir = new PFInRegion(PFBLOCKINROOM, domain, new String[]{CLASSBLOCK, CLASSROOM}, false);
		
		PropositionalFunction aid = new PFInRegion(PFAGENTINDOOR, domain, new String[]{CLASSAGENT, CLASSDOOR}, true);
		PropositionalFunction bid = new PFInRegion(PFBLOCKINDOOR, domain, new String[]{CLASSBLOCK, CLASSDOOR}, true);
		
		for(String col : COLORS){
			PropositionalFunction pfr = new PFIsColor(PFRoomColorName(col), domain, new String[]{CLASSROOM}, col);
			PropositionalFunction pfb = new PFIsColor(PFBlockColorName(col), domain, new String[]{CLASSBLOCK}, col);
		}
		
		for(String shape : SHAPES){
			PropositionalFunction pf = new PFIsShape(PFBlockShapeName(shape), domain, new String[]{CLASSBLOCK}, shape);
		}
		
		
		return domain;
	}
	
	
	
	protected void addRectAtts(Domain domain, ObjectClass oc){
		oc.addAttribute(domain.getAttribute(ATTTOP));
		oc.addAttribute(domain.getAttribute(ATTLEFT));
		oc.addAttribute(domain.getAttribute(ATTBOTTOM));
		oc.addAttribute(domain.getAttribute(ATTRIGHT));
	}
	
	
	
	public static String PFRoomColorName(String color){
		String capped = firstLetterCapped(color);
		return PFRCOLORBASE + capped;
	}
	public static String PFBlockColorName(String color){
		String capped = firstLetterCapped(color);
		return PFBCOLORBASE + capped;
	}
	public static String PFBlockShapeName(String shape){
		String capped = firstLetterCapped(shape);
		return PFBSHAPEBASE + capped;
	}
	
	
	public static State getCleanState(Domain domain, int nRooms, int nDoors, int nBlocks){
		
		State s = new State();
		
		//create  rooms
		createNInstances(domain, s, CLASSROOM, nRooms);
		
		//now create doors
		createNInstances(domain, s, CLASSDOOR, nDoors);
		
		//now create blocks
		createNInstances(domain, s, CLASSBLOCK, nBlocks);
		
		//create agent
		ObjectInstance o = new ObjectInstance(domain.getObjectClass(CLASSAGENT), CLASSAGENT+0);
		s.addObject(o);
		
		Attribute dirAtt = o.getObjectClass().getAttribute(ATTDIR);
		if(dirAtt != null){
			o.setValue(ATTDIR, "south");
		}
		
		return s;
		
	}
	
	public static State getClassicState(Domain domain){
		
		State s = getCleanState(domain, 3, 2, 1);
		
		setRoom(s, 0, 4, 0, 0, 8, "red");
		setRoom(s, 1, 8, 0, 4, 4, "green");
		setRoom(s, 2, 8, 4, 4, 8, "blue");
		
		setDoor(s, 0, 4, 6, 4, 6);
		setDoor(s, 1, 4, 2, 4, 2);
		
		setAgent(s, 6, 6);
		setBlock(s, 0, 2, 2, "basket", "red");
		
		
		return s;
		
	}
	
	
	public static void setAgent(State s, int x, int y){
		ObjectInstance o = s.getFirstObjectOfClass(CLASSAGENT);
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
	}
	
	public static void setAgent(State s, int x, int y, int dir){
		ObjectInstance o = s.getFirstObjectOfClass(CLASSAGENT);
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTDIR, dir);
	}
	
	public static void setBlockPos(State s, int i, int x, int y){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSBLOCK).get(i);
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
	}
	
	public static void setBlock(State s, int i, int x, int y, String shape, String color){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSBLOCK).get(i);
		setBlock(o, x, y, shape, color);
	}
	
	public static void setBlock(ObjectInstance o, int x, int y, String shape, String color){
		o.setValue(ATTX, x);
		o.setValue(ATTY, y);
		o.setValue(ATTSHAPE, shape);
		o.setValue(ATTCOLOR, color);
	}
	
	public static void setRoom(State s, int i, int top, int left, int bottom, int right, String color){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSROOM).get(i);
		setRegion(o, top, left, bottom, right);
		o.setValue(ATTCOLOR, color);
	}
	
	public static void setDoor(State s, int i, int top, int left, int bottom, int right){
		ObjectInstance o = s.getObjectsOfTrueClass(CLASSDOOR).get(i);
		setRegion(o, top, left, bottom, right);
	}
	
	public static void setRoom(ObjectInstance o, int top, int left, int bottom, int right, String color){
		setRegion(o, top, left, bottom, right);
		o.setValue(ATTCOLOR, color);
	}
	
	public static void setRegion(ObjectInstance o, int top, int left, int bottom, int right){
		o.setValue(ATTTOP, top);
		o.setValue(ATTLEFT, left);
		o.setValue(ATTBOTTOM, bottom);
		o.setValue(ATTRIGHT, right);
	}
	
	protected static void createNInstances(Domain domain, State s, String className, int n){
		for(int i = 0; i < n; i++){
			ObjectInstance o = new ObjectInstance(domain.getObjectClass(className), className+i);
			s.addObject(o);
		}
	}
	
	
	public static int maxRoomXExtent(State s){
		
		int max = 0;
		List <ObjectInstance> rooms = s.getObjectsOfTrueClass(CLASSROOM);
		for(ObjectInstance r : rooms){
			int right = r.getDiscValForAttribute(ATTRIGHT);
			if(right > max){
				max = right;
			}
		}
		
		return max;
	}
	
	public static int maxRoomYExtent(State s){
		
		int max = 0;
		List <ObjectInstance> rooms = s.getObjectsOfTrueClass(CLASSROOM);
		for(ObjectInstance r : rooms){
			int top = r.getDiscValForAttribute(ATTTOP);
			if(top > max){
				max = top;
			}
		}
		
		return max;
	}
	
	
	protected static String firstLetterCapped(String s){
		String firstLetter = s.substring(0, 1);
		String remainder = s.substring(1);
		return firstLetter.toUpperCase() + remainder;
	}
	
	public static ObjectInstance roomContainingPoint(State s, int x, int y){
		List<ObjectInstance> rooms = s.getObjectsOfTrueClass(CLASSROOM);
		return regionContainingPoint(rooms, x, y, false);
	}
	
	public static ObjectInstance roomContainingPointIncludingBorder(State s, int x, int y){
		List<ObjectInstance> rooms = s.getObjectsOfTrueClass(CLASSROOM);
		return regionContainingPoint(rooms, x, y, true);
	}
	
	public static ObjectInstance doorContainingPoint(State s, int x, int y){
		List<ObjectInstance> doors = s.getObjectsOfTrueClass(CLASSDOOR);
		return regionContainingPoint(doors, x, y, true);
	}
	
	protected static ObjectInstance regionContainingPoint(List <ObjectInstance> objects, int x, int y, boolean countBoundary){
		for(ObjectInstance o : objects){
			if(regionContainsPoint(o, x, y, countBoundary)){
				return o;
			}
			
		}
		
		return null;
	}
	
	public static boolean regionContainsPoint(ObjectInstance o, int x, int y, boolean countBoundary){
		int top = o.getDiscValForAttribute(ATTTOP);
		int left = o.getDiscValForAttribute(ATTLEFT);
		int bottom = o.getDiscValForAttribute(ATTBOTTOM);
		int right = o.getDiscValForAttribute(ATTRIGHT);
		
		if(countBoundary){
			if(y >= bottom && y <= top && x >= left && x <= right){
				return true;
			}
		}
		else{
			if(y > bottom && y < top && x > left && x < right){
				return true;
			}
		}
		
		return false;
	}
	
	public static ObjectInstance blockAtPoint(State s, int x, int y){
		
		List<ObjectInstance> blocks = s.getObjectsOfTrueClass(CLASSBLOCK);
		for(ObjectInstance b : blocks){
			int bx = b.getDiscValForAttribute(ATTX);
			int by = b.getDiscValForAttribute(ATTY);
			
			if(bx == x && by == y){
				return b;
			}
		}
		
		return null;
		
	}
	
	
	public static boolean wallAt(State s, ObjectInstance r, int x, int y){
		
		int top = r.getDiscValForAttribute(ATTTOP);
		int left = r.getDiscValForAttribute(ATTLEFT);
		int bottom = r.getDiscValForAttribute(ATTBOTTOM);
		int right = r.getDiscValForAttribute(ATTRIGHT);
		
		//agent along wall of room check
		if(((x == left || x == right) && y >= bottom && y <= top) || ((y == bottom || y == top) && x >= left && x <= right)){
			
			//then only way for this to be a valid pos is if a door contains this point
			ObjectInstance door = doorContainingPoint(s, x, y);
			if(door == null){
				return true;
			}
			
		}
		
		return false;
	}
	
	public class MovementAction extends Action{

		protected int xdelta;
		protected int ydelta;
		
		public MovementAction(String name, Domain domain, int xdelta, int ydelta){
			super(name, domain, "");
			this.xdelta = xdelta;
			this.ydelta = ydelta;
		}
		
		@Override
		protected State performActionHelper(State s, String[] params) {
			
			ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
			int ax = agent.getDiscValForAttribute(ATTX);
			int ay = agent.getDiscValForAttribute(ATTY);
			
			int nx = ax+xdelta;
			int ny = ay+ydelta;
			
			//ObjectInstance roomContaining = roomContainingPoint(s, ax, ay);
			ObjectInstance roomContaining = regionContainingPoint(s.getObjectsOfTrueClass(CLASSROOM), ax, ay, true);
			
			
			boolean permissibleMove = false;
			ObjectInstance pushedBlock = blockAtPoint(s, nx, ny);
			if(pushedBlock != null){
				int bx = pushedBlock.getDiscValForAttribute(ATTX);
				int by = pushedBlock.getDiscValForAttribute(ATTY);
				
				int nbx = bx + xdelta;
				int nby = by + ydelta;
				
				if(!wallAt(s, roomContaining, nbx, nby) && blockAtPoint(s, nbx, nby) == null){
					permissibleMove = true;
					
					//move the block
					pushedBlock.setValue(ATTX, nbx);
					pushedBlock.setValue(ATTY, nby);
					
				}
				
			}
			else if(!wallAt(s, roomContaining, nx, ny)){
				permissibleMove = true;
			}
			
			if(permissibleMove){
				agent.setValue(ATTX, nx);
				agent.setValue(ATTY, ny);
			}
			
			
			if(Sokoban2Domain.this.includeDirectionAttribute){
				if(this.xdelta == 1){
					agent.setValue(ATTDIR, "east");
				}
				else if(this.xdelta == -1){
					agent.setValue(ATTDIR, "west");
				}
				else if(this.ydelta == 1){
					agent.setValue(ATTDIR, "north");
				}
				else if(this.ydelta == -1){
					agent.setValue(ATTDIR, "south");
				}
			}
			
			
			return s;
		}
		
		
	}
	
	
	
	public class PFInRegion extends PropositionalFunction{

		protected boolean countBoundary;
		
		public PFInRegion(String name, Domain domain, String [] params, boolean countBoundary){
			super(name, domain, params);
			this.countBoundary = countBoundary;
		}
		
		@Override
		public boolean isTrue(State s, String[] params) {
			
			ObjectInstance o = s.getObject(params[0]);
			int x = o.getDiscValForAttribute(ATTX);
			int y = o.getDiscValForAttribute(ATTY);
			
			
			ObjectInstance region = s.getObject(params[1]);
			return regionContainsPoint(region, x, y, countBoundary);
			
		}
		
	}
	
	
	public class PFIsColor extends PropositionalFunction{
		
		protected String colorName;
		
		public PFIsColor(String name, Domain domain, String [] params, String color){
			super(name, domain, params);
			this.colorName = color;
		}

		@Override
		public boolean isTrue(State s, String[] params) {
			
			ObjectInstance o = s.getObject(params[0]);
			String col = o.getStringValForAttribute(ATTCOLOR);
			
			return this.colorName.equals(col);
			
		}

	}
	
	
	public class PFIsShape extends PropositionalFunction{

		protected String shapeName;
		
		public PFIsShape(String name, Domain domain, String [] params, String shape){
			super(name, domain, params);
			this.shapeName = shape;
		}
		
		@Override
		public boolean isTrue(State s, String[] params) {
			ObjectInstance o = s.getObject(params[0]);
			String shape = o.getStringValForAttribute(ATTSHAPE);
			
			return this.shapeName.equals(shape);
		}
		
		
		
	}
	
	
	
	
	public static void main(String [] args){
		
		Sokoban2Domain dgen = new Sokoban2Domain();
		dgen.includeDirectionAttribute(true);
		Domain domain = dgen.generateDomain();
		
		State s = Sokoban2Domain.getClassicState(domain);
		
		/*ObjectInstance b2 = new ObjectInstance(domain.getObjectClass(CLASSBLOCK), CLASSBLOCK+1);
		s.addObject(b2);
		setBlock(s, 1, 3, 2, "moon", "red");*/
		
		Visualizer v = Sokoban2Visualizer.getVisualizer("robotImages");
		VisualExplorer exp = new VisualExplorer(domain, v, s);
		
		exp.addKeyAction("w", ACTIONNORTH);
		exp.addKeyAction("s", ACTIONSOUTH);
		exp.addKeyAction("d", ACTIONEAST);
		exp.addKeyAction("a", ACTIONWEST);
		
		exp.initGUI();
		
	}

}
