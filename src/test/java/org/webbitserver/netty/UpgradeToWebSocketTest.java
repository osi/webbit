package org.webbitserver.netty;

import org.junit.Assert;
import org.junit.Test;

public class UpgradeToWebSocketTest {
    @Test
    public void shouldUpgradeToWebSocketOnSameIoThread() throws Exception {
        Assert.fail("ensure that upgrading keeps the same handler thread");
    }
}
