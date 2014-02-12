package llvmmop.output.combinedaspect.event;

import llvmmop.MOPException;
import llvmmop.output.MOPVariable;
import llvmmop.output.combinedaspect.CombinedAspect;
import llvmmop.output.combinedaspect.GlobalLock;
import llvmmop.output.combinedaspect.event.advice.AdviceBody;
import llvmmop.parser.ast.mopspec.EventDefinition;
import llvmmop.parser.ast.mopspec.LLVMMOPSpec;

public class EndThread {
	LLVMMOPSpec mopSpec;
	EventDefinition event;
	GlobalLock globalLock;

	AdviceBody eventBody;

	MOPVariable runnableMap;
	MOPVariable mainThread;
	MOPVariable mainCounter;
	MOPVariable threadSet;

	MOPVariable commonPointcut = new MOPVariable("MOP_CommonPointCut");

	public EndThread(LLVMMOPSpec mopSpec, EventDefinition event, CombinedAspect combinedAspect) throws MOPException {
		if (!event.isEndThread())
			throw new MOPException("EndThread should be defined only for an endThread pointcut.");

		this.mopSpec = mopSpec;
		this.event = event;
		this.globalLock = combinedAspect.lockManager.getLock();
		
		this.runnableMap = new MOPVariable(mopSpec.getName() + "_" + event.getId() + "_ThreadToRunnable");
		this.mainThread = new MOPVariable(mopSpec.getName() + "_" + event.getId() + "_MainThread");
		this.mainCounter = new MOPVariable(mopSpec.getName() + "_" + event.getId() + "_MainCounter");
		this.threadSet = new MOPVariable(mopSpec.getName() + "_" + event.getId() + "_ThreadSet");

		this.eventBody = new AdviceBody(mopSpec, event, combinedAspect);
	}

	public EndThread() {
		
	}
	
	public String printDataStructures() {
		String ret = "";

		ret += "static HashMap<Thread, Runnable> " + runnableMap + " = new HashMap<Thread, Runnable>();\n";
		ret += "static Thread " + mainThread + " = null;\n";
		ret += "static long " + mainCounter + " = 0;\n";
		ret += "static HashSet<Thread> " + threadSet + " = new HashSet<Thread>();\n";

		return ret;
	}

	public String printAdviceForThreadWithRunnable() {
		String ret = "";

		ret += "after (Runnable r) returning (Thread t): ";
		ret += "(";
		ret += "(call(Thread+.new(Runnable+,..)) && args(r,..))";
		ret += "|| (initialization(Thread+.new(ThreadGroup+, Runnable+,..)) && args(ThreadGroup, r,..)))";
		ret += " && " + commonPointcut + "() {\n";
		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		ret += runnableMap + ".put(t, r);\n";
		ret += globalLock.getName() + ".unlock();\n";
		ret += "}\n";

		return ret;
	}

	public String printAdviceForEndThread() {
		String ret = "";
		MOPVariable threadVar = new MOPVariable("t");

		ret += "after (Thread " + threadVar + "): ( execution(void Thread+.run()) && target(" + threadVar + ") )";
		ret += " && " + commonPointcut + "() {\n";

		ret += "if(Thread.currentThread() == " + threadVar + ") {\n";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += "Thread " + event.getThreadVar() + " = Thread.currentThread();\n";
		}

		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		
		ret += threadSet + ".remove(Thread.currentThread());\n";

		ret += globalLock.getName() + ".unlock();\n";
		
		ret += EventManager.EventMethodHelper.methodName(eventBody.specName, event, eventBody.fileName);
		ret += "(";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += event.getThreadVar();
		}
		ret += ");\n";

		ret += "}\n";

		ret += "}\n";

		return ret;
	}

	public String printAdviceForEndRunnable() {
		String ret = "";
		MOPVariable runnableVar = new MOPVariable("r");

		ret += "after (Runnable " + runnableVar + "): ( execution(void Runnable+.run()) && !execution(void Thread+.run()) && target(" + runnableVar + ") )";
		ret += " && " + commonPointcut + "() {\n";

		ret += "if(" + runnableMap + ".get(Thread.currentThread()) == " + runnableVar + ") {\n";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += "Thread " + event.getThreadVar() + " = Thread.currentThread();\n";
		}
		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		ret += threadSet + ".remove(Thread.currentThread());\n";
		ret += globalLock.getName() + ".unlock();\n";

		ret += EventManager.EventMethodHelper.methodName(eventBody.specName, event, eventBody.fileName);
		ret += "(";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += event.getThreadVar();
		}
		ret += ");\n";

		ret += "}\n";

		ret += "}\n";

		return ret;
	}

	public String printAdviceForMainEnd() {
		String ret = "";

		ret += "before (): " + "(execution(void *.main(..)) )";
		ret += " && " + commonPointcut + "() {\n";
		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		ret += "if(" + mainThread + " == null){\n";
		ret += mainThread + " = Thread.currentThread();\n";
		ret += threadSet + ".add(Thread.currentThread());\n";
		ret += "}\n";
		ret += "if(" + mainThread + " == Thread.currentThread()){\n";
		ret += mainCounter + "++;\n";
		ret += "}\n";
		ret += globalLock.getName() + ".unlock();\n";
		ret += "}\n";
		ret += "\n";

		ret += "after (): " + "(execution(void *.main(..)) )";
		ret += " && " + commonPointcut + "() {\n";
		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		ret += "if(" + mainThread + " == Thread.currentThread()){\n";
		ret += mainCounter + "--;\n";
		ret += "if(" + mainCounter + " <= 0){\n";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += "Thread " + event.getThreadVar() + " = Thread.currentThread();\n";
		}
		ret += threadSet + ".remove(Thread.currentThread());\n";
		
		ret += EventManager.EventMethodHelper.methodName(eventBody.specName, event, eventBody.fileName);
		ret += "(";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += event.getThreadVar();
		}
		ret += ");\n";

		ret += "}\n";
		ret += "}\n";
		ret += globalLock.getName() + ".unlock();\n";
		ret += "}\n";
		ret += "\n";

		return ret;
	}

	public String printAdviceForNewThread() {
		String ret = "";
		
		ret += "after (Thread t): ";
		ret += "(";
		ret += "call(void Thread+.start()) && target(t))";
		ret += " && " + commonPointcut + "() {\n";
		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		ret += threadSet + ".add(t);\n";
		ret += globalLock.getName() + ".unlock();\n";
		ret += "}\n";
		
		return ret;
	}

	public String printAdviceBodyAtEndProgram(){
		String ret = "";
		MOPVariable t = new MOPVariable("t");
		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0){
			ret += "for(Thread " + event.getThreadVar() + " : " + threadSet + ") {\n";
			ret += threadSet + ".remove(" + event.getThreadVar() + ");\n";
		} else {
			ret += "for(Thread " + t + " : " + threadSet + ") {\n";
			ret += threadSet + ".remove(" + t + ");\n";
		}
		
		ret += EventManager.EventMethodHelper.methodName(eventBody.specName, event, eventBody.fileName);
		ret += "(";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += event.getThreadVar();
		}
		ret += ");\n";

		ret += "}\n";
		ret += globalLock.getName() + ".unlock();\n";
		
		return ret;
	}
	
	public String printAdvices() {
		String ret = "";

		ret += printDataStructures();
		ret += "\n";
		ret += printAdviceForThreadWithRunnable();
		ret += "\n";
		ret += printAdviceForEndThread();
		ret += "\n";
		ret += printAdviceForEndRunnable();
		ret += "\n";
		ret += printAdviceForMainEnd();
		ret += "\n";
		ret += printAdviceForNewThread();

		return ret;
	}

}
