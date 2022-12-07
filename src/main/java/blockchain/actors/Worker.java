package blockchain.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Behaviors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import blockchain.model.Block;
import blockchain.model.HashResult;
import blockchain.utils.BlockChainUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class Worker extends AbstractBehavior<Worker.Command> {
	public interface Command extends Serializable {}

	@Getter
	@AllArgsConstructor
	public static class StartMiningCommand implements Command {
		private Block block;
		private long startNonce;
		private long endNonce;
		private int difficulty;
		private ActorRef<Manager.Command> controller;
	}

	@AllArgsConstructor
	@Getter
	public static class MineCommand implements Command {
		private long iteration;
		private long startNonce;
		private long endNonce;
		private ActorRef<Manager.Command> controller;
	}

	@AllArgsConstructor
	@Getter
	public static class ProgressReportCommand implements Command {
		private ActorRef<Manager.Command> controller;
	}

	public static class BlockAlreadyMinedAbortCurrentWorkCommand implements Command {}

	public static class DecommissionWorkerCommand implements Command {}

	private Worker(ActorContext<Command> context) {
		super(context);
	}
	
	public static Behavior<Command> create() {
		return Behaviors.setup(Worker::new);
	}

	private static final long ITERATION_SIZE = 10;
	private boolean abortCommandReceived = false;

	@Override
	public Receive<Command> createReceive() {
		return workerIdleMessageHandler();
	}

	private Receive<Command> workerIdleMessageHandler() {
		return newReceiveBuilder()
				.onMessage(StartMiningCommand.class, message -> {
					System.out.println(getContext().getSelf().path() + " received start command for nonce from " + message.getStartNonce() + " to " + message.getEndNonce() + ".");
					abortCommandReceived = false;
					long endNonce = Math.min((message.getStartNonce() + ITERATION_SIZE -1), message.getEndNonce());
					getContext().getSelf().tell(new MineCommand(0, message.getStartNonce(), endNonce, message.getController()));
					return workerMiningMessageHandler(message.getBlock(), message.getDifficulty(), message.getStartNonce(), message.getEndNonce(), 0);
				})
				.onMessage(DecommissionWorkerCommand.class, message -> waitingToStop())
				.build();
	}

	private Receive<Command> workerMiningMessageHandler(Block block, int difficulty, long startNonce, long endNonce, long processed) {
		return newReceiveBuilder()
				.onMessage(MineCommand.class, message -> {
					if(abortCommandReceived) {
						message.getController().tell(new Manager.WorkerFinishedCommand(getContext().getSelf(), block, Optional.empty()));
						return workerIdleMessageHandler();
					}

					Optional<HashResult> hashResult = BlockChainUtils.mineBlock(block, difficulty, message.getStartNonce(), message.getEndNonce());

					if(hashResult.isPresent()) {
						getContext().getLog().debug(hashResult.get().getNonce() + " : " + hashResult.get().getHash());
						message.getController().tell(new Manager.WorkerFinishedCommand(getContext().getSelf(), block, hashResult));
						return workerIdleMessageHandler();
					}
					else if(message.getEndNonce() == endNonce){
						getContext().getLog().debug("null");
						message.getController().tell(new Manager.WorkerFinishedCommand(getContext().getSelf(), block, hashResult));
						return workerIdleMessageHandler();
					} else {
						long newStartNonce = message.getEndNonce() + 1;
						long newEndNonce = Math.min(newStartNonce + ITERATION_SIZE - 1, endNonce);
						getContext().getSelf().tell(new MineCommand(message.getIteration() + 1, newStartNonce, newEndNonce,  message.getController()));
						long currentlyProcessed = processed + (message.getEndNonce() - message.getStartNonce() + 1);
						return workerMiningMessageHandler(block, difficulty, startNonce, endNonce, currentlyProcessed);
					}
				})
				.onMessage(BlockAlreadyMinedAbortCurrentWorkCommand.class, message -> {
					this.abortCommandReceived = true;
					return Behaviors.same();
				})
				.onMessage(ProgressReportCommand.class, message -> {
					long totalWorkload = (endNonce - startNonce) +1;
					BigDecimal percentage = BigDecimal.valueOf(processed).divide(BigDecimal.valueOf(totalWorkload), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
					message.getController().tell(new Manager.WorkerProgressCommand(getContext().getSelf(), percentage));
					return Behaviors.same();
				})
				.onMessage(DecommissionWorkerCommand.class, message -> waitingToStop())
				.build();
	}

	private Receive<Command> waitingToStop() {
		return newReceiveBuilder()
				.onAnyMessage(message -> Behaviors.same())
				.onSignal(PostStop.class, signal -> {
					getContext().getLog().info("I'm about to terminate");
					return Behaviors.same();
				})
				.build();
	}
}
