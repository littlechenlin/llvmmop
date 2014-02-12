package llvmmop.output.combinedaspect.event;

import llvmmop.MOPException;
import llvmmop.output.MOPVariable;
import llvmmop.output.combinedaspect.CombinedAspect;
import llvmmop.output.combinedaspect.GlobalLock;
import llvmmop.output.combinedaspect.event.advice.AdviceBody;
import llvmmop.parser.ast.mopspec.EventDefinition;
import llvmmop.parser.ast.mopspec.LLVMMOPSpec;

public class StartThread {
	LLVMMOPSpec mopSpec;
	EventDefinition event;
	GlobalLock globalLock;

	AdviceBody eventBody;

	MOPVariable runnableMap;
	MOPVariable mainThread;

	MOPVariable commonPointcut = new MOPVariable("MOP_CommonPointCut");

	public StartThread(LLVMMOPSpec mopSpec, EventDefinition event, CombinedAspect combinedAspect) throws MOPException {
		if (!event.isStartThread())
			throw new MOPException("StartThread should be defined only for an startThread pointcut.");

		this.mopSpec = mopSpec;
		this.event = event;
		this.globalLock = combinedAspect.lockManager.getLock();
		this.runnableMap = new MOPVariable(mopSpec.getName() + "_" + event.getId() + "_ThreadToRunnable");
		this.mainThread = new MOPVariable(mopSpec.getName() + "_" + event.getId() + "_MainThread");

		this.eventBody = new AdviceBody(mopSpec, event, combinedAspect);
	}

	public String printDataStructures() {
		String ret = "";

		ret += "static HashMap<Thread, Runnable> " + runnableMap + " = new HashMap<Thread, Runnable>();\n";
		ret += "static Thread " + mainThread + " = null;\n";
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

	public String printAdviceForStartThread() {
		String ret = "";
		MOPVariable threadVar = new MOPVariable("t");

		ret += "before (Thread " + threadVar + "): ( execution(void Thread+.run()) && target(" + threadVar + ") )";
		ret += " && " + commonPointcut + "() {\n";
		
		ret += "if(Thread.currentThread() == " + threadVar + ") {\n";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += "Thread " + event.getThreadVar() + " = Thread.currentThread();\n";
		}

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

	public String printAdviceForStartRunnable() {
		String ret = "";
		MOPVariable runnableVar = new MOPVariable("r");

		ret += "before (Runnable " + runnableVar + "): ( execution(void Runnable+.run()) && !execution(void Thread+.run()) && target(" + runnableVar + ") )";
		ret += " && " + commonPointcut + "() {\n";
		ret += "while (!" + globalLock.getName() + ".tryLock()) {\n";
		ret += "Thread.yield();\n";
		ret += "}\n";
		ret += "if(" + runnableMap + ".get(Thread.currentThread()) == " + runnableVar + ") {\n";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += "Thread " + event.getThreadVar() + " = Thread.currentThread();\n";
		}
		
		ret += EventManager.EventMethodHelper.methodName(eventBody.specName, event, eventBody.fileName);
		ret += "(";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += event.getThreadVar();
		}
		ret += ");\n";

		ret += "}\n";
		ret += globalLock.getName() + ".unlock();\n";

		ret += "}\n";
		return ret;
	}

	public String printAdviceForMainStart() {
		String ret = "";

		ret += "before (): " + "(execution(void *.main(..)) )";
		ret += " && " + commonPointcut + "() {\n";
		ret += "if(" + mainThread + " == null){\n";
		ret += mainThread + " = Thread.currentThread();\n";
		
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += "Thread " + event.getThreadVar() + " = Thread.currentThread();\n";
		}

		ret += EventManager.EventMethodHelper.methodName(eventBody.specName, event, eventBody.fileName);
		ret += "(";
		if (event.getThreadVar() != null && event.getThreadVar().length() != 0) {
			ret += event.getThreadVar();
		}
		ret += ");\n";

		ret += "}\n";
		ret += "}\n";
		ret += "\n";

		return ret;
	}

	public String printAdvices() {
		String ret = "";

		ret += printDataStructures();
		ret += "\n";
		ret += printAdviceForThreadWithRunnable();
		ret += "\n";
		ret += printAdviceForStartThread();
		ret += "\n";
		ret += printAdviceForStartRunnable();
		ret += "\n";
		ret += printAdviceForMainStart();

		return ret;
	}

}
