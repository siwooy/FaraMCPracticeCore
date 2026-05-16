package lol.siwoo.faramcpracticecore.train;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingSessionTest {

    @Mock
    private Player player;

    private UUID playerUuid;
    private TrainingMode mode;
    private TrainingSession session;

    @BeforeEach
    void setUp() {
        playerUuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerUuid);
        mode = TrainingMode.STRAFE;
        session = new TrainingSession(player, mode);
    }

    @Test
    void testConstructor() {
        assertEquals(playerUuid, session.getPlayerId());
        assertEquals(mode, session.getMode());
        assertTrue(session.isActive());
        assertEquals(0.0, session.getScore());
        assertEquals(0, session.getAttempts());
        assertTrue(session.getStartTime() <= System.currentTimeMillis());
    }

    @Test
    void testSetActive() {
        session.setActive(false);
        assertFalse(session.isActive());
        session.setActive(true);
        assertTrue(session.isActive());
    }

    @Test
    void testSetScore() {
        session.setScore(10.5);
        assertEquals(10.5, session.getScore());
        session.setScore(-5.0);
        assertEquals(-5.0, session.getScore());
    }

    @Test
    void testIncrementAttempts() {
        session.incrementAttempts();
        assertEquals(1, session.getAttempts());
        session.incrementAttempts();
        assertEquals(2, session.getAttempts());
    }

    @Test
    void testGetDuration() throws InterruptedException {
        long durationStart = session.getDuration();
        assertTrue(durationStart >= 0);

        Thread.sleep(10);
        long durationAfter = session.getDuration();
        assertTrue(durationAfter >= 10);
    }
}
