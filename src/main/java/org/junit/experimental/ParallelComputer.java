package org.junit.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.runner.Computer;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

public class ParallelComputer extends Computer {
	private final boolean fClasses;
	private final boolean fMethods;

	public ParallelComputer(boolean classes, boolean methods) {
		fClasses= classes;
		fMethods= methods;
	}

	public static Computer classes() {
		return new ParallelComputer(true, false);
	}
	
	// TODO(parallel) extract commonality from ParallelSuite and ParallelRunner
	public static class ParallelSuite extends Suite {
		public ParallelSuite(RunnerBuilder builder, Class<?>[] classes) throws InitializationError {
			super(builder, classes);
		}
		
		private final ParallelCollator fCollator = new ParallelCollator();
	
		@Override
		protected void runChild(final Runner runner, final RunNotifier notifier) {
			Callable<Object> callable= new Callable<Object>() {
				public Object call() throws Exception {
					superRunChild(runner, notifier);
					return null;
				}
			};
			fCollator.process(callable);
		}
		
		protected void superRunChild(Runner runner, RunNotifier notifier) {
			super.runChild(runner, notifier);
		}
		
		@Override
		public void run(RunNotifier notifier) {
			super.run(notifier);
			for (Future<Object> each : fCollator.results)
				try {
					each.get();
				} catch (Exception e) {
					e.printStackTrace();
				} 
		}
	}

	public static class ParallelRunner extends BlockJUnit4ClassRunner {
		public ParallelRunner(Class<?> klass) throws InitializationError {
			super(klass);
		}
		
		private final ParallelCollator fCollator = new ParallelCollator();
	
		@Override
		protected void runChild(final FrameworkMethod method, final RunNotifier notifier) {
			Callable<Object> callable= new Callable<Object>() {
				public Object call() throws Exception {
					superRunChild(method, notifier);
					return null;
				}
			};
			fCollator.process(callable);
		}
		
		protected void superRunChild(FrameworkMethod method, RunNotifier notifier) {
			super.runChild(method, notifier);
		}
		
		@Override
		public void run(RunNotifier notifier) {
			super.run(notifier);
			for (Future<Object> each : fCollator.results)
				try {
					each.get();
				} catch (Exception e) {
					e.printStackTrace();
				} 
		}
	}

	@Override
	public Suite getSuite(RunnerBuilder builder, java.lang.Class<?>[] classes) throws InitializationError {
		return fClasses
			? new ParallelSuite(builder, classes)
			: super.getSuite(builder, classes);
	}
	
	@Override
	protected Runner getRunner(RunnerBuilder builder, Class<?> testClass)
			throws Throwable {
		return fMethods
			? new ParallelRunner(testClass)
			: super.getRunner(builder, testClass);
	}

	public static Computer methods() {
		return new ParallelComputer(false, true);
	}
	
	private static class ParallelCollator {
		private final List<Future<Object>> results = new ArrayList<Future<Object>>();
		private final ExecutorService service = Executors.newCachedThreadPool();

		public void process(Callable<Object> callable) {
			this.results.add(service.submit(callable));
		}
	}
}