package blockchain;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import blockchain.actors.Manager;
import blockchain.model.BlockChain;
import blockchain.model.BlockChainMiningWorkOrder;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        ActorSystem<Manager.Command> miningManager = ActorSystem.create(Manager.create(), "miningManager");
        BlockChainMiningWorkOrder workOrder = BlockChainMiningWorkOrder.builder()
                .numberOfSimultaneousWorkers(7) //How many threads should run in parallel
                .numberOfBlocksToMine(10)         //How many blocks should be mined in a blockchain
                .workloadPerWorker(5000000)         //How many nonce should one worker calculate
                .difficulty(6)                   //Blockchain difficulty level (how many leading zeros should be in a hash)
                .progressReportFrequency(1)      //Progress report frequency in seconds
                .build();
        //miningManager.tell(new Manager.StartCommand(workOrder));

        CompletionStage<BlockChain> result = AskPattern.ask(miningManager, (me) -> new Manager.StartCommand(workOrder, me), Duration.ofHours(1), miningManager.scheduler());
        result.whenComplete((blockChain, failure) -> {
            if(blockChain != null) {
                blockChain.printAndValidate();
                long endTime = System.currentTimeMillis();
                System.out.println("Elapsed time: " + (endTime - startTime) + " ms.");
            } else {
                System.out.println("The system did not respond in time!!");
            }
            miningManager.terminate();
        });
    }
}
