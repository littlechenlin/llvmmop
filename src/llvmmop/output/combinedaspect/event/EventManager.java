package llvmmop.output.combinedaspect.event;

import java.util.ArrayList;
import java.util.List;

import llvmmop.LLVMMOPMain;
import llvmmop.MOPException;
import llvmmop.output.MOPVariable;
import llvmmop.output.combinedaspect.CombinedAspect;
import llvmmop.output.combinedaspect.event.advice.AdviceAndPointCut;
import llvmmop.parser.ast.aspect.PointCut;
import llvmmop.parser.ast.mopspec.EventDefinition;
import llvmmop.parser.ast.mopspec.LLVMMOPSpec;
import llvmmop.parser.ast.visitor.ConvertPointcutToCNFVisitor;

public class EventManager {

	public ArrayList<AdviceAndPointCut> advices = new ArrayList<AdviceAndPointCut>();
	public ArrayList<EndObject> endObjectEvents = new ArrayList<EndObject>();
	public ArrayList<EndThread> endThreadEvents = new ArrayList<EndThread>();
	public ArrayList<StartThread> startThreadEvents = new ArrayList<StartThread>();
	public EndProgram endProgramEvent = null;

	
	MOPVariable commonPointcut = new MOPVariable("MOP_CommonPointCut");

	public EventManager(String name, List<LLVMMOPSpec> specs, CombinedAspect combinedAspect) throws MOPException {

		this.endProgramEvent = new EndProgram(name);

		for (LLVMMOPSpec spec : specs) {
			if (spec.isEnforce()) {
				endThreadEvents.add(new ThreadStatusMonitor(spec, combinedAspect));
			}
			for (EventDefinition event : spec.getEvents()) {
				// normal event
				if (!event.isEndObject() && !event.isEndProgram() && !event.isEndThread() && !event.isStartThread()) {
					boolean added = false;
					for (AdviceAndPointCut advice : advices) {
						if (advice.isAround != event.getPos().equals("around"))
							continue;
						if (advice.isAround) {
							if (!advice.retType.equals(event.getRetType().toString()))
								continue;
						}
						if (!advice.pos.equals(event.getPos()))
							continue;
						if (!advice.retVal.equals(event.getRetVal()))
							continue;
						if (!advice.throwVal.equals(event.getThrowVal()))
							continue;

						PointcutComparator comparator = new PointcutComparator();
						PointCut p1 = event.getPointCut().accept(new ConvertPointcutToCNFVisitor(), null);
						PointCut p2 = advice.getPointCut().accept(new ConvertPointcutToCNFVisitor(), null);
						
						if (comparator.compare(p1, p2)) {
							added = advice.addEvent(spec, event, combinedAspect);
							if(added)
								break;
						}
					}

					if (!added) {
						advices.add(new AdviceAndPointCut(spec, event, combinedAspect));
					}
				}

				// endObject
				if (event.isEndObject()) {
					endObjectEvents.add(new EndObject(spec, event, combinedAspect));
				}

				// endThread
				if (event.isEndThread()) {
					endThreadEvents.add(new EndThread(spec, event, combinedAspect));
				}

				// startThread
				if (event.isStartThread()) {
					startThreadEvents.add(new StartThread(spec, event, combinedAspect));
				}

				// endProgram
				if (event.isEndProgram()) {
					endProgramEvent.addEndProgramEvent(spec, event, combinedAspect);
				}

			} // end of for event

		} // end of for spec

		endProgramEvent.registerEndThreadEvents(endThreadEvents);

	}

	public String printConstructor() {
		String ret = "";

		if (endProgramEvent != null) {
			ret += endProgramEvent.printAddStatement();
		}

		return ret;
	}

	public String advices() {
		String ret = "";

		ret += "pointcut " + commonPointcut + "() : ";
		ret += "!within(com.runtimeverification.rvmonitor.java.rt.RVMObject+) && !adviceexecution();\n";

		int numAdvice = 1;
		advices = this.adjustAdviceOrder();
		for (AdviceAndPointCut advice : advices) {
			if(LLVMMOPMain.empty_advicebody){
				ret += "// " + numAdvice++ + "\n";
			}
			
			if (LLVMMOPMain.translate2RV) {
				ret += advice.toRVString();
			} else {
				ret += advice;
			}
			ret += "\n";
			if (advice.beCounted) {
				ret += "\n";
				ret += "// Declaration of the count variable for above pointcut\n";
				ret += "static int " + advice.getPointCutName() + "_count = 0;";
				ret += "\n\n\n";
			}
		}

		for (EndObject endObject : endObjectEvents) {
			ret += endObject.printDecl();
			ret += "\n";
		}

		for (EndThread endThread : endThreadEvents) {
			ret += endThread.printAdvices();
			ret += "\n";
		}

		for (StartThread startThread : startThreadEvents) {
			ret += startThread.printAdvices();
			ret += "\n";
		}

		ret += endProgramEvent.printHookThread();

		return ret;
	} 
	
	/* 
	 * 
	 * Adjust advice order in the aspect so the before advice 
	 * comes before the after advices
	 * 
	 * **/
	private ArrayList<AdviceAndPointCut> adjustAdviceOrder() {
		ArrayList<AdviceAndPointCut> result = new ArrayList<AdviceAndPointCut>();
		for (AdviceAndPointCut advice : this.advices) {
			if (advice.pos.equals("before")) {
				result.add(0, advice);
			} else {
				result.add(advice);
			}
		}
		return result;
	}

	public static class EventMethodHelper {
		public static String methodName(String enclosingspec, EventDefinition event, String aspectName) {
			boolean mangle = false;
			if (LLVMMOPMain.merge && LLVMMOPMain.aspectname != null && LLVMMOPMain.aspectname.length() > 0) {
				mangle = true;
			}
			
			StringBuilder s = new StringBuilder();
			if (mangle && LLVMMOPMain.specifiedAJName) {
				s.append(LLVMMOPMain.aspectname);
			} else {
				s.append(aspectName);
			}
			s.append("RuntimeMonitor");
			s.append('.');
			if (mangle) {
				s.append(enclosingspec);
				s.append('_');
			}
			s.append(event.getId());
			s.append("Event");
			return s.toString();
		}

		public static String methodName(LLVMMOPSpec enclosing, EventDefinition evt, String aspectName) {
			return methodName(enclosing.getName(), evt, aspectName);
		}
	}
}
