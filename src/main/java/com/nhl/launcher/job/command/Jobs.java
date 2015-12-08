package com.nhl.launcher.job.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.nhl.launcher.command.Command;
import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.locking.LocalSerialJobRunner;
import com.nhl.launcher.job.locking.LockType;
import com.nhl.launcher.job.locking.SerialJobRunner;
import com.nhl.launcher.job.locking.ZkClusterSerialJobRunner;
import com.nhl.launcher.job.scheduler.DefaultSchedulerProvider;
import com.nhl.launcher.job.scheduler.Scheduler;

public class Jobs {

	private Collection<Class<? extends Job>> jobTypes;
	private boolean enableZookeeperLocks;

	public static Jobs jobs() {
		return new Jobs();
	}

	private Jobs() {
		this.jobTypes = new HashSet<>();
	}

	@SafeVarargs
	public final Jobs addJobs(Class<? extends Job>... jobTypes) {
		Arrays.asList(jobTypes).forEach(jt -> this.jobTypes.add(jt));
		return this;
	}

	public Jobs enableZookeeperLocks() {
		this.enableZookeeperLocks = true;
		return this;
	}

	public Module module() {
		return binder -> {

			Multibinder.newSetBinder(binder, Command.class).addBinding().to(ExecCommand.class);
			Multibinder.newSetBinder(binder, Command.class).addBinding().to(ListCommand.class);

			jobTypes.forEach(jt -> Multibinder.newSetBinder(binder, Job.class).addBinding().to(jt).in(Singleton.class));

			MapBinder<LockType, SerialJobRunner> serialJobRunners = MapBinder.newMapBinder(binder, LockType.class,
					SerialJobRunner.class);
			serialJobRunners.addBinding(LockType.local).to(LocalSerialJobRunner.class);
			
			if(enableZookeeperLocks) {
				serialJobRunners.addBinding(LockType.clustered).to(ZkClusterSerialJobRunner.class);
			}

			binder.bind(Scheduler.class).toProvider(DefaultSchedulerProvider.class).in(Singleton.class);
		};
	}

}
