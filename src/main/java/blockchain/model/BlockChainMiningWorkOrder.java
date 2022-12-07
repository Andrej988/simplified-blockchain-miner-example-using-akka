package blockchain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class BlockChainMiningWorkOrder {
    private int numberOfBlocksToMine;
    private int numberOfSimultaneousWorkers;
    private int workloadPerWorker;
    private int difficulty;
}
