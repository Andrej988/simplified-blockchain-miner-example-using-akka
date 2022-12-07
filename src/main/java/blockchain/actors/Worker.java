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

	public static class DecommissionWorkerCommand implements Command {}

	private Worker(ActorContext<Command> context) {
		super(context);
	}
	
	public static Behavior<Command> create() {
		return Behaviors.setup(Worker::new);
	}

	@Override
	public Receive<Command> createReceive() {
		return newReceiveBuilder()
				.onMessage(StartMiningCommand.class, message -> {
					System.out.println(getContext().getSelf().path() + " received start mining command for nonce from " + message.getStartNonce() + " to " + message.getEndNonce());
					Optional<HashResult> hashResult = BlockChainUtils.mineBlock(message.getBlock(), message.getDifficulty(), message.getStartNonce(), message.getEndNonce());

					if(hashResult.isPresent()) {
						getContext().getLog().debug(hashResult.get().getNonce() + " : " + hashResult.get().getHash());
					}
					else {
						getContext().getLog().debug("null");
					}
					message.getController().tell(new Manager.WorkerFinishedCommand(getContext().getSelf(), message.getBlock(), hashResult));
					return Behaviors.same();
				})
				.onMessage(DecommissionWorkerCommand.class, message -> waitingToStop())
				.build();
	}

	public Receive<Command> waitingToStop() {
		return newReceiveBuilder()
				.onAnyMessage(message -> Behaviors.same())
				.onSignal(PostStop.class, signal -> {
					getContext().getLog().info("I'm about to terminate");
					return Behaviors.same();
				})
				.build();
	}
}
