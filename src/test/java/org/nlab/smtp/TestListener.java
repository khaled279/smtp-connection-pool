package org.nlab.smtp;

import org.junit.Assert;
import org.junit.Test;
import org.nlab.smtp.transport.connection.ClosableSmtpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.event.TransportAdapter;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.MimeMessage;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nlabrot on 29/04/15.
 */
public class TestListener extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(TestListener.class);

    @Override
    public int getMaxTotalConnection() {
        return 1;
    }


    @Test
    public void testListenerPassivation() throws Exception {

        final Phaser phaser = new Phaser(2);

        final AtomicInteger countDelivered = new AtomicInteger();

        TransportListener listener = new TransportAdapter() {
            @Override
            public void messageDelivered(TransportEvent e) {
                countDelivered.incrementAndGet();
                phaser.arrive();
            }
        };

        try (ClosableSmtpConnection transport = smtpConnectionPool.borrowObject()) {
            transport.addTransportListener(listener);
            MimeMessage mimeMessage = createMessage(transport.getSession(), "nithril@example.com", "nithril@example.com", "foo", "example");
            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
        }

        phaser.arriveAndAwaitAdvance();

        Assert.assertEquals(1, countDelivered.get());

        try (ClosableSmtpConnection transport = smtpConnectionPool.borrowObject()) {
            transport.addTransportListener(listener);
            MimeMessage mimeMessage = createMessage(transport.getSession(), "nithril@example.com", "nithril@example.com", "foo", "example");
            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
        }

        phaser.arriveAndAwaitAdvance();

        Assert.assertEquals(2, countDelivered.get());
    }
}
