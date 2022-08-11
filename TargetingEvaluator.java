package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;
    private ExecutorService service;

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */
    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
        this.service = Executors.newCachedThreadPool();
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */

    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {
        return targetingGroup.getTargetingPredicates().stream()
                .map(tp -> {
                    Runnable task = () -> tp.evaluate(requestContext);
                    return service.submit(task);
                })
                .map(future -> {
                    try {
                        return (TargetingPredicateResult) future.get();
                    } catch (ExecutionException | InterruptedException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(TargetingPredicateResult::isTrue)
                .count() == targetingGroup.getTargetingPredicates().size() ? TargetingPredicateResult.TRUE :
                TargetingPredicateResult.FALSE;
//        return targetingGroup.getTargetingPredicates().stream()
//                .map(tp -> {
//                    try {
//                        return getResult(tp);
//                    } catch (ExecutionException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    return null;
//                }).filter(Objects::nonNull)
//                .filter(TargetingPredicateResult::isTrue)
//                .count() == targetingGroup.getTargetingPredicates().size() ? TargetingPredicateResult.TRUE :
//                                                                            TargetingPredicateResult.FALSE;
    }

    public TargetingPredicateResult getResult(TargetingPredicate predicate) throws ExecutionException, InterruptedException {
        Runnable task = () -> predicate.evaluate(requestContext);
        return (TargetingPredicateResult) service.submit(task).get();
    }
}
