package blockchain;

import akka.actor.typed.ActorSystem;
import blockchain.actors.Manager;
import blockchain.model.BlockChainMiningWorkOrder;

public class Main {
    public static void main(String[] args) {
        ActorSystem<Manager.Command> miningManager = ActorSystem.create(Manager.create(), "miningManager");
        BlockChainMiningWorkOrder workOrder = BlockChainMiningWorkOrder.builder()
                .numberOfSimultaneousWorkers(7) //How many threads should run in parallel
                .numberOfBlocksToMine(10)         //How many blocks should be mined in a blockchain
                .workloadPerWorker(50000000)         //How many nonce should one worker calculate
                .difficulty(6)                   //Blockchain difficulty level (how many leading zeros should be in a hash)
                .progressReportFrequency(1)      //Progress report frequency in seconds
                .build();
        miningManager.tell(new Manager.StartCommand(workOrder));
    }
}
