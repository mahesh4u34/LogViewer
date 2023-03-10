package com.logviewer;

import com.logviewer.config.LogViewerServerConfig;
import com.logviewer.web.dto.LogList;
import com.logviewer.web.dto.RestRecord;
import com.logviewer.web.dto.RestStatus;
import com.logviewer.web.dto.events.EventScrollToEdgeResponse;
import com.logviewer.web.session.LogSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogSessionRemoteTest extends LogSessionTest {

    private static final String LOCALHOST_SUFFIX = "@localhost:" + TEST_SERVER_PORT;

    @Before
    public void startupServer() {
        createLogServiceWithContext(MultifileConfiguration.class, LogViewerServerConfig.class);
    }

    @Test
    public void connectionProblemInvalidPort() throws InterruptedException {
        connectionProblem("/tmp/l.log@localhost:" + (TEST_SERVER_PORT - 1));
    }

    @Test
    public void connectionProblemInvalidHost() throws InterruptedException {
        connectionProblem("/tmp/l.log@null012347dulrnk");
    }

    private void connectionProblem(String invalidUrl) throws InterruptedException {
        ConfigurableApplicationContext context = createContext(MultifileConfiguration.class);

        LogSession session = LogSession.fromContext(adapter,context);

        session.init(LogList.of(super.getTestLog("multilog/server-a.log"), invalidUrl));
        session.scrollToEdge(2, 2, null, false);

        EventScrollToEdgeResponse init = adapter.waitForType(EventScrollToEdgeResponse.class);
        Map<String, RestStatus> hashes = init.statuses;
        assert hashes.size() == 2;

        Map<String, RestStatus> t2 = new HashMap<>(hashes);
        RestStatus status = t2.remove("server-a.log");
        assert status.getHash() != null;

        RestStatus connectionProblemStatus = hashes.entrySet().stream().filter(e -> !e.getKey().equals("server-a.log")).findAny().get().getValue();
        assert connectionProblemStatus.getHash() == null;
        Assert.assertEquals("ConnectionProblem", connectionProblemStatus.getErrorType());

        List<RestRecord> list = init.data.records;
        Assert.assertEquals(Arrays.asList("150101 10:00:03 a 3", "150101 10:00:03 a 4"), list.stream().map(RestRecord::getText).collect(Collectors.toList()));
    }

    @Override
    protected String getTestLog(String relativePath) {
        return super.getTestLog(relativePath) + LOCALHOST_SUFFIX;
    }

    @Override
    public String[] createMultifileLog(String file) throws IOException {
        if (file.endsWith(LOCALHOST_SUFFIX))
            file = file.substring(0, file.length() - LOCALHOST_SUFFIX.length());

        String[] res = super.createMultifileLog(file).clone();
        for (int i = 0; i < res.length; i++) {
            res[i] = res[i] + LOCALHOST_SUFFIX;
        }

        return res;
    }
}