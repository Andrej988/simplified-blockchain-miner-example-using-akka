package blockchain.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import blockchain.model.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.*;

public class Manager extends AbstractBehavior<Manager.Command> {
    public interface Command extends Serializable {}

    @AllArgsConstructor
    @Getter
    public static class StartCommand implements Command {
        private BlockChainMiningWorkOrder workOrder;
    }

    private static class MineNextBlockCommand implements Command {}
    private static class AssignWorkloadCommand implements Command {}
    private static class MiningFinishedCommand implements Command {}

    @AllArgsConstructor
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class WorkerFinishedCommand implements Command {
        private ActorRef<Worker.Command> worker;
        private Block block;
        private Optional<HashResult> result;
    }

    private Manager(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(Manager::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return miningNotYetStartedMessageHandler();
    }

    private Receive<Command> miningNotYetStartedMessageHandler() {
        return newReceiveBuilder()
                .onMessage(StartCommand.class, message -> {
                    BlockChainMining mining = BlockChainMining.builder()
                            .startTime(System.currentTimeMillis())
                            .blockchain(new BlockChain())
                            .numberOfBlocksToMine(message.getWorkOrder().getNumberOfBlocksToMine())
                            .workloadPerWorker(message.getWorkOrder().getWorkloadPerWorker())
                            .difficulty(message.getWorkOrder().getDifficulty())
                            .assignNewBlock(true)
                            .nextStartNonce(0)
                            .build();

                    Set<ActorRef<Worker.Command>> workers = spinUpWorkers(message.workOrder.getNumberOfSimultaneousWorkers());
                    mining.initWorkers(workers);

                    getContext().getSelf().tell(new MineNextBlockCommand());
                    return miningRunningMessageHandler(mining);
                })
                .build();
    }

    private Receive<Command> miningRunningMessageHandler(BlockChainMining mining) {
        return newReceiveBuilder()
                .onMessage(MineNextBlockCommand.class, message -> {
                    if(mining.getBlockChainSize() == mining.getNumberOfBlocksToMine()) {
                        getContext().getSelf().tell(new MiningFinishedCommand());
                        return miningFinishedMessageHandler(mining);
                    }

                    mining.setCurrentBlock(Block.generateNewBlockWithRandomData(mining.getHashOfPreviousBlock()));
                    mining.setNextStartNonce(0);
                    mining.setAssignNewBlock(false);
                    getContext().getSelf().tell(new AssignWorkloadCommand());
                    return miningRunningMessageHandler(mining);
                })
                .onMessage(AssignWorkloadCommand.class, message -> {
                    for(ActorRef<Worker.Command> worker : mining.getIdleWorkers()) {
                        worker.tell(new Worker.StartMiningCommand(mining.getCurrentBlock(), mining.getNextStartNonce(), mining.calculateEndNonce(),
                                mining.getDifficulty(), getContext().getSelf()));
                        mining.increaseStartNonce();
                        mining.setWorkerStatus(worker, WorkerStatus.MINING);
                    }
                    return miningRunningMessageHandler(mining);
                })
                .onMessage(WorkerFinishedCommand.class, message -> {
                    if(mining.isActualBlock(message.getBlock()) && message.getResult().isPresent()) {
                        mining.addCurrentBlockToBlockChain(message.getResult().get());
                        mining.setAssignNewBlock(true);
                        mining.setWorkerStatus(message.getWorker(), WorkerStatus.IDLE);
                        getContext().getSelf().tell(new MineNextBlockCommand());

                    } else {
                        mining.setWorkerStatus(message.getWorker(), WorkerStatus.IDLE);
                        getContext().getSelf().tell(new AssignWorkloadCommand());
                    }
                    return miningRunningMessageHandler(mining);
                })
                .build();
    }

    public Receive<Command> miningFinishedMessageHandler(BlockChainMining mining) {
        return newReceiveBuilder()
                .onMessage(MiningFinishedCommand.class, message -> {
                    mining.getAllWorkers().forEach(x -> x.tell(new Worker.DecommissionWorkerCommand()));
                    mining.printAndValidateBlockChain();
                    long endTime = System.currentTimeMillis();
                    System.out.println("Elapsed time: " + (endTime - mining.getStartTime()) + " ms.");
                    return Behaviors.stopped();
                })
                .build();
    }

    private Set<ActorRef<Worker.Command>> spinUpWorkers(int numberOfSimultaneousWorkers) {
        Set<ActorRef<Worker.Command>> workers = new HashSet<>();
        for(int i=0; i<numberOfSimultaneousWorkers; i++) {
            //workers.add(getContext().spawn(Worker.create(), "worker_" + UUID.randomUUID()));
            workers.add(getContext().spawn(Worker.create(), "worker_" + i));
        }
        return workers;
    }
}
