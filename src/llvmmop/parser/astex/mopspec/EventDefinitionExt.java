package llvmmop.parser.astex.mopspec;

import java.io.ByteArrayInputStream;
import java.util.List;

import llvmmop.parser.aspect_parser.AspectParser;
import llvmmop.parser.ast.aspect.PointCut;
import llvmmop.parser.ast.aspect.TypePattern;
import llvmmop.parser.ast.mopspec.MOPParameter;
import llvmmop.parser.ast.mopspec.MOPParameters;
import llvmmop.parser.ast.stmt.BlockStmt;
import llvmmop.parser.ast.type.Type;
import llvmmop.parser.astex.ExtNode;
import llvmmop.parser.astex.visitor.GenericVisitor;
import llvmmop.parser.astex.visitor.VoidVisitor;

public class EventDefinitionExt extends ExtNode {

	String id;
	Type retType;
	String pos;

	boolean abstractEvent = false;
	String pointCutStr;
	String purePointCutStr;
	PointCut pointCut;

	MOPParameters parameters;
	boolean hasReturning;
	MOPParameters retVal;
	boolean hasThrowing;
	MOPParameters throwVal;

	MOPParameters mopParameters;

	BlockStmt block;

	// will be modified by LLVMMOPSpec when creation events are not specified
	boolean startEvent = false;
	
	// similar to startEvent, but won't be modified
	boolean creationEvent = false;
	
	boolean blockingEvent = false;

	String condition;
	String threadVar;
	TypePattern endObjectType;
	String endObjectId;
	boolean endProgram = false;
	boolean endThread = false;
	boolean startThread = false;
	boolean endObject = false;

	// things that should be defined afterward
	int idnum; // will be defined in LLVMMOPSpec
	boolean duplicated = false; // will be defined in LLVMMOPSpec
	String uniqueId = null; // will be defined in LLVMMOPSpec
	MOPParameters mopParametersOnSpec; // will be defined in LLVMMOPSpec

	MOPParameters parametersWithoutThreadVar = null;
	private Boolean cachedHas__SKIP = null;
	private Boolean cachedHas__LOC = null;

	public EventDefinitionExt(int line, int column, String id, Type retType, String pos, List<MOPParameter> parameters, String pointCutStr, BlockStmt block, boolean hasReturning,
			List<MOPParameter> retVal, boolean hasThrowing, List<MOPParameter> throwVal, boolean startEvent, boolean abstractEvent,
			boolean isBlockingEvent)
			throws llvmmop.parser.main_parser.ParseException {
		super(line, column);
		this.id = id;
		this.retType = retType;
		this.pos = pos;
		this.parameters = new MOPParameters(parameters);
		this.pointCutStr = pointCutStr;
		this.block = block;
		this.hasReturning = hasReturning;
		this.retVal = new MOPParameters(retVal);
		this.hasThrowing = hasThrowing;
		this.throwVal = new MOPParameters(throwVal);
		
		if (pointCutStr != null)
			this.pointCut = parsePointCutAsRaw(pointCutStr);
		
		this.startEvent = startEvent;
		this.creationEvent = startEvent;
		this.mopParameters = new MOPParameters();
		this.mopParameters.addAll(this.parameters);
		this.mopParameters.addAll(this.retVal);
		this.mopParameters.addAll(this.throwVal);
		this.abstractEvent = abstractEvent;
		this.blockingEvent = isBlockingEvent;
	}

	public EventDefinitionExt(int line, int column, EventDefinitionExt e) {
		super(line, column);
		this.id = e.getId();
		this.retType = e.getRetType();
		this.pos = e.getPos();
		this.parameters = e.getParameters();
		this.pointCutStr = e.getPointCutString();
		this.block = e.getBlock();
		this.hasReturning = e.getHasRetruning();
		this.retVal = e.getRetVal();
		this.hasThrowing = e.getHasThrowing();
		this.throwVal = e.getThrowVal();
		this.pointCut = e.getPointCut();
		this.startEvent = e.isStartEvent();
		this.creationEvent = e.isCreationEvent();
		this.blockingEvent = e.isBlockingEvent();
		this.mopParameters = e.getMOPParameters();
		this.condition = e.getCondition();
		this.threadVar = e.getThreadVar();
		this.endObjectType = e.getEndObjectType();
		this.endObjectId = e.getEndObjectId();

		this.endProgram = e.isEndProgram();
		this.endThread = e.isEndThread();
		this.endObject = e.isEndObject();

		this.idnum = e.getIdNum(); // will be defined in LLVMMOPSpec
		this.duplicated = e.isDuplicated(); // will be defined in LLVMMOPSpec
		this.uniqueId = e.getUniqueId(); // will be defined in LLVMMOPSpec
		this.mopParametersOnSpec = e.getMOPParametersOnSpec(); // will be
																// defined in
																// LLVMMOPSpec

		this.parametersWithoutThreadVar = e.getParametersWithoutThreadVar();
		this.cachedHas__SKIP = e.isCashedHas__SKIP();
		// this.cachedHas__LOC = e.isCachedHas__LOC();

	}

	private PointCut parsePointCutAsRaw(String input) throws llvmmop.parser.main_parser.ParseException {
		// create a token for exceptions
		llvmmop.parser.main_parser.Token t = new llvmmop.parser.main_parser.Token();
		t.beginLine = super.getBeginLine();
		t.beginColumn = super.getBeginColumn();

		PointCut originalPointCut;
		PointCut resultPointCut;
		purePointCutStr = "";
		threadVar = "";
		condition = "";

		try {
			originalPointCut = AspectParser.parse(new ByteArrayInputStream(input.getBytes()));
		} catch (llvmmop.parser.aspect_parser.ParseException e) {
			throw new llvmmop.parser.main_parser.ParseException("The following error encountered when parsing the pointcut in the event definition: " + e.getMessage());
		}

		resultPointCut = originalPointCut;

		if (resultPointCut == null)
			throw new llvmmop.parser.main_parser.ParseException("endObject() pointcut should appear at the root level in a conjuction form");

		purePointCutStr = resultPointCut.toString();

		return resultPointCut;
	}

	public String getId() {
		return id;
	}

	public int getIdNum() {
		return idnum;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public Type getRetType() {
		return retType;
	}

	public String getPos() {
		return pos;
	}

	public MOPParameters getParameters() {
		return parameters;
	}

	public MOPParameters getParametersWithoutThreadVar() {
		if (parametersWithoutThreadVar != null)
			return parametersWithoutThreadVar;

		parametersWithoutThreadVar = new MOPParameters();
		for (MOPParameter param : parameters) {
			if (getThreadVar() != null && getThreadVar().length() != 0 && param.getName().equals(getThreadVar()))
				continue;
			parametersWithoutThreadVar.add(param);
		}

		return parametersWithoutThreadVar;
	}

	public MOPParameters getMOPParameters() {
		return mopParameters;
	}

	MOPParameters mopParametersWithoutThreadVar = null;

	public MOPParameters getMOPParametersWithoutThreadVar() {
		if (mopParametersWithoutThreadVar != null)
			return mopParametersWithoutThreadVar;

		mopParametersWithoutThreadVar = new MOPParameters();
		for (MOPParameter param : mopParameters) {
			if (getThreadVar() != null && getThreadVar().length() != 0 && param.getName().equals(getThreadVar()))
				continue;
			mopParametersWithoutThreadVar.add(param);
		}
		return mopParametersWithoutThreadVar;
	}

	public MOPParameters getMOPParametersOnSpec() {
		return mopParametersOnSpec;
	}

	public String getPointCutString() {
		return pointCutStr;
	}

	public BlockStmt getAction() {
		return block;
	}

	public boolean hasReturning() {
		return hasReturning;
	}

	public MOPParameters getRetVal() {
		return retVal;
	}

	public boolean hasThrowing() {
		return hasThrowing;
	}

	public MOPParameters getThrowVal() {
		return throwVal;
	}

	public String getThreadVar() {
		return threadVar;
	}

	public String getCondition() {
		return condition;
	}

	public boolean isAbstract() {
		return this.abstractEvent;
	}

	public String getPurePointCutString() {
		return purePointCutStr;
	}

	public String getEndObjectVar() {
		if (this.endObject)
			return endObjectId;
		else
			return null;
	}

	public TypePattern getEndObjectType() {
		if (this.endObject)
			return endObjectType;
		else
			return null;
	}

	public boolean isStartEvent() {
		return this.startEvent;
	}
	
	public boolean isBlockingEvent() {
		return this.blockingEvent;
	}
	
	public boolean isCreationEvent() {
		return this.creationEvent;
	}

	public boolean isEndProgram() {
		return this.endProgram;
	}

	public boolean isEndThread() {
		return this.endThread;
	}

	public boolean isEndObject() {
		return this.endObject;
	}

	public boolean has__SKIP() {
		if (cachedHas__SKIP != null)
			return cachedHas__SKIP.booleanValue();

		if (this.getAction() != null) {
			String eventAction = this.getAction().toString();
			if (eventAction.indexOf("__SKIP") != -1) {
				cachedHas__SKIP = new Boolean(true);
				return true;
			}
		}
		cachedHas__SKIP = new Boolean(false);
		return false;
	}

	public boolean has__LOC() {
		if (cachedHas__LOC != null)
			return cachedHas__LOC.booleanValue();

		if (this.getAction() != null) {
			String eventAction = this.getAction().toString();
			if (eventAction.indexOf("__LOC") != -1
          || 
          eventAction.indexOf("__DEFAULT_MESSAGE") != -1) {
				cachedHas__LOC = new Boolean(true);
				return true;
			}
		}
		cachedHas__LOC = new Boolean(false);
		return false;
	}

	@Override
	public <A> void accept(VoidVisitor<A> v, A arg) {
		v.visit(this, arg);
	}

	@Override
	public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
		return v.visit(this, arg);
	}

	public BlockStmt getBlock() {
		return this.block;
	}

	public boolean getHasRetruning() {
		return this.hasReturning;
	}

	public boolean getHasThrowing() {
		return this.hasThrowing;
	}

	public PointCut getPointCut() {
		return this.pointCut;
	}

	public String getEndObjectId() {
		return this.endObjectId;
	}

	public boolean isDuplicated() {
		return this.duplicated;
	}

	public Boolean isCashedHas__SKIP() {

		return this.cachedHas__SKIP;
	}

	public Boolean isCachedHas__LOC() {
		return this.isCachedHas__LOC();
	}
	
	public boolean isImplementing(EventDefinitionExt absEvent){
		if (this.isAbstract())
			return false;
		if (!this.getId().equals(absEvent.getId()))
			return false;
		if (this.getParameters().matchTypes(absEvent.getParameters()))
			return false;
		if (this.getRetVal().matchTypes(absEvent.getRetVal()))
			return false;
		if (this.getThrowVal().matchTypes(absEvent.getThrowVal()))
			return false;
		if (!this.getPos().equals(absEvent.getPos()))
			return false;

		return true; 
	}
}
