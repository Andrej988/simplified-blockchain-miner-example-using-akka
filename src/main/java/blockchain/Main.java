package blockchain;

import akka.actor.typed.ActorSystem;
import blockchain.actors.Manager;
import blockchain.model.BlockChainMiningWorkOrder;

public class Main {
    public static void main(String[] args) {
        ActorSystem<Manager.Command> miningManager = ActorSystem.create(Manager.create(), "miningManager");
        BlockChainMiningWorkOrder workOrder = BlockChainMiningWorkOrder.builder()
                .numberOfSimultaneousWorkers(10) //How many threads should run in parallel
                .numberOfBlocksToMine(5)         //How many blocks should be mined in a blockchain
                .workloadPerWorker(5000)         //How many nonce should one worker calculate
                .difficulty(5)                   //Blockchain difficulty level (how many leading zeros should be in a hash)
                .build();
        miningManager.tell(new Manager.StartCommand(workOrder));
    }
}
