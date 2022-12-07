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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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

    @AllArgsConstructor
    @Getter
    public static class WorkerProgressCommand implements Command {
        private ActorRef<Worker.Command> worker;
        private BigDecimal progressPercentage;
    }

    private static class GetProgressReportCommand implements Command {}

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

    private Object TIMER_KEY;

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

                    Map<ActorRef<Worker.Command>, WorkerStatus> workers = spinUpWorkers(message.workOrder.getNumberOfSimultaneousWorkers());

                    getContext().getSelf().tell(new MineNextBlockCommand());
                    return Behaviors.withTimers(timer -> {
                        timer.startTimerAtFixedRate(TIMER_KEY, new GetProgressReportCommand(), Duration.ofSeconds(message.getWorkOrder().getProgressReportFrequency()));
                        return miningRunningMessageHandler(mining, workers);
                    });
                })
                .build();
    }

    private Receive<Command> miningRunningMessageHandler(BlockChainMining mining, Map<ActorRef<Worker.Command>, WorkerStatus> workers) {
        return newReceiveBuilder()
                .onMessage(MineNextBlockCommand.class, message -> {
                    if(mining.getBlockChainSize() == mining.getNumberOfBlocksToMine()) {
                        getContext().getSelf().tell(new MiningFinishedCommand());
                        return miningFinishedMessageHandler(mining, workers);
                    }

                    mining.setCurrentBlock(Block.generateNewBlockWithRandomData(mining.getHashOfPreviousBlock()));
                    mining.setNextStartNonce(0);
                    mining.setAssignNewBlock(false);
                    getContext().getSelf().tell(new AssignWorkloadCommand());
                    return miningRunningMessageHandler(mining, workers);
                })
                .onMessage(AssignWorkloadCommand.class, message -> {
                    workers.entrySet()
                            .stream()
                            .filter(x -> x.getValue() == WorkerStatus.IDLE)
                            .map(Map.Entry::getKey)
                            .forEach(worker -> {
                                worker.tell(buildWorkerStartMiningCommand(mining));
                                mining.increaseStartNonce();
                                workers.put(worker, WorkerStatus.MINING);
                            });
                    return miningRunningMessageHandler(mining, workers);
                })
                .onMessage(GetProgressReportCommand.class, message -> {
                    workers.keySet().forEach(worker -> worker.tell(new Worker.ProgressReportCommand(getContext().getSelf())));
                    return Behaviors.same();
                })
                .onMessage(WorkerProgressCommand.class, message -> {
                    System.out.println(message.getWorker().path()+ " progress is " + message.getProgressPercentage().setScale(2, RoundingMode.HALF_UP) + "%");
                    return Behaviors.same();
                })
                .onMessage(WorkerFinishedCommand.class, message -> {
                    workers.put(message.getWorker(), WorkerStatus.IDLE);

                    if(mining.isActualBlock(message.getBlock()) && message.getResult().isPresent()) {
                        mining.addCurrentBlockToBlockChain(message.getResult().get());
                        mining.setAssignNewBlock(true);
                        getContext().getSelf().tell(new MineNextBlockCommand());

                    } else {
                        getContext().getSelf().tell(new AssignWorkloadCommand());
                    }
                    return miningRunningMessageHandler(mining, workers);
                })
                .build();
    }

    public Receive<Command> miningFinishedMessageHandler(BlockChainMining mining, Map<ActorRef<Worker.Command>, WorkerStatus> workers) {
        return newReceiveBuilder()
                .onMessage(MiningFinishedCommand.class, message -> {
                    workers.keySet().forEach(worker -> worker.tell(new Worker.DecommissionWorkerCommand()));
                    mining.printAndValidateBlockChain();
                    long endTime = System.currentTimeMillis();
                    System.out.println("Elapsed time: " + (endTime - mining.getStartTime()) + " ms.");
                    return Behaviors.withTimers(timers -> {
                        timers.cancelAll();
                        return Behaviors.stopped();
                    });
                })
                .build();
    }

    private Map<ActorRef<Worker.Command>, WorkerStatus> spinUpWorkers(int numberOfSimultaneousWorkers) {
        Map<ActorRef<Worker.Command>, WorkerStatus> workers = new HashMap<>();
        for(int i=0; i<numberOfSimultaneousWorkers; i++) {
            workers.put(getContext().spawn(Worker.create(), "worker_" + i), WorkerStatus.IDLE);
        }
        return workers;
    }

    private Worker.Command buildWorkerStartMiningCommand(BlockChainMining mining) {
        return new Worker.StartMiningCommand(mining.getCurrentBlock(), mining.getNextStartNonce(),
                mining.calculateEndNonce(), mining.getDifficulty(), getContext().getSelf());
    }

}
