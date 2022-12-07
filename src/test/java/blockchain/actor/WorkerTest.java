package blockchain.actor;

import akka.actor.testkit.typed.CapturedLogEvent;
import akka.actor.testkit.typed.javadsl.BehaviorTestKit;
import akka.actor.testkit.typed.javadsl.TestInbox;
import blockchain.actors.Manager;
import blockchain.actors.Worker;
import blockchain.model.Block;
import blockchain.model.HashResult;
import blockchain.model.Transaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WorkerTest {
    private static Block testBlock;

    @BeforeAll
    static void setUp() {
        Transaction tx = new Transaction(UUID.fromString("33cdefb0-6f73-408b-8a5d-4e72ad13ffdc"), 1588883122, 35252435, 125.25);
        testBlock = new Block(tx, "0000sfdg24z6t32zdfb<dxbf");
    }

    //TODO Fix unit tests later on
    @Disabled
    @Test
    @DisplayName("Mining fails if nonce is not in range")
    void testMiningFailsIfNonceNotInRange() {
        BehaviorTestKit<Worker.Command> testActor = BehaviorTestKit.create(Worker.create());

        TestInbox<Manager.Command> textInbox = TestInbox.create();
        Worker.Command message = new Worker.StartMiningCommand(testBlock, 0, 1000, 5, textInbox.getRef());
        testActor.run(message);
        List<CapturedLogEvent> logMessages = testActor.getAllLogEntries();

        assertAll(
                () -> assertEquals(1, logMessages.size()),
                () -> assertEquals("null", logMessages.get(0).message()),
                () -> assertEquals(Level.DEBUG, logMessages.get(0).level())
        );
    }

    //TODO Fix unit tests later on
    @Disabled
    @Test
    @DisplayName("Mining passes if nonce is in range")
    void testMiningPassesIfNonceIsInRange() {
        BehaviorTestKit<Worker.Command> testActor = BehaviorTestKit.create(Worker.create());
        TestInbox<Manager.Command> testInbox = TestInbox.create();

        long startNonce = 1277000;
        Worker.Command message = new Worker.StartMiningCommand(testBlock, startNonce, startNonce + 1000, 5, testInbox.getRef());
        testActor.run(message);

        List<CapturedLogEvent> logMessages = testActor.getAllLogEntries();
        String expectedResult = "1277424 : 000000eb16b355b8324cea2f03ee03a242ba20393fec0358cf24248d3513e9b5";

        assertAll(
                () -> assertEquals(1, logMessages.size()),
                () -> assertEquals(expectedResult, logMessages.get(0).message()),
                () -> assertEquals(Level.DEBUG, logMessages.get(0).level())
        );
    }

    //TODO Fix unit tests later on
    @Disabled
    @Test
    @DisplayName("Message received if nonce is in range")
    void testMessageReceivedIfNonceInRange() {
        BehaviorTestKit<Worker.Command> testActor = BehaviorTestKit.create(Worker.create());

        TestInbox<Manager.Command> testInbox = TestInbox.create();
        long startNonce = 1277000;
        Worker.Command message = new Worker.StartMiningCommand(testBlock, startNonce, startNonce + 1000, 5, testInbox.getRef());
        testActor.run(message);

        HashResult expectedHashResult = new HashResult();
        expectedHashResult.foundAHash("000000eb16b355b8324cea2f03ee03a242ba20393fec0358cf24248d3513e9b5", 1277424);
        testInbox.expectMessage(new Manager.WorkerFinishedCommand(testActor.getRef(), testBlock, Optional.of(expectedHashResult)));
    }

    //TODO Fix unit tests later on
    @Disabled
    @Test
    @DisplayName("Message received if nonce not in range")
    void testNoMessageReceivedIfNonceNotInRange() {
        BehaviorTestKit<Worker.Command> testActor = BehaviorTestKit.create(Worker.create());

        TestInbox<Manager.Command> testInbox = TestInbox.create();
        Worker.Command message = new Worker.StartMiningCommand(testBlock, 0, 1000L, 5, testInbox.getRef());
        testActor.run(message);

        //HashResult expectedHashResult = new HashResult();
        //expectedHashResult.foundAHash("000000eb16b355b8324cea2f03ee03a242ba20393fec0358cf24248d3513e9b5", 1277424);
        //assertFalse(testInbox.hasMessages());
        testInbox.expectMessage(new Manager.WorkerFinishedCommand(testActor.getRef(), testBlock, Optional.ofNullable(null)));
    }
}
