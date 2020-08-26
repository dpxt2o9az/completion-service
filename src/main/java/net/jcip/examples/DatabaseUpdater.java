package net.jcip.examples;

import java.util.*;
import java.util.concurrent.*;
import static net.jcip.examples.LaunderThrowable.launderThrowable;

/**
 * Renderer
 * <p/>
 * Using CompletionService to render page elements as they become available
 *
 * @author Brian Goetz and Tim Peierls
 */
public abstract class DatabaseUpdater {

    private final ExecutorService executor;

    DatabaseUpdater(ExecutorService executor) {
        this.executor = executor;
    }

    void associate(InterceptAssociator associator) {

        final List<Long> interceptIds = fetchInterceptIds();
        final CompletionService<DatabaseUpdate> completionService = new ExecutorCompletionService<>(executor);

        for (final Long interceptId : interceptIds) {
            completionService.submit(new Callable<>() {
                @Override
                public DatabaseUpdate call() {
                    return associator.associateIntercept(interceptId);
                }
            });
        }

        try {
            for (int t = 0, n = interceptIds.size(); t < n; t++) {
                Future<DatabaseUpdate> f = completionService.take();
                DatabaseUpdate dbUpdate = f.get();
                doUpdate(dbUpdate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw launderThrowable(e.getCause());
        }
    }

    interface DatabaseUpdate {

        long getInterceptId();

        Integer getModeId();

        Integer getSiteId();
    }

    interface InterceptAssociator {

        DatabaseUpdate associateIntercept(long interceptId);
    }

    abstract List<Long> fetchInterceptIds();

    abstract void doUpdate(DatabaseUpdate i);

}
