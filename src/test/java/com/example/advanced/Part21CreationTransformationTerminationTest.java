package com.example.advanced;

import com.example.advanced.Source;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.PublisherProbe;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Arrays;

import static com.example.advanced.Part21CreationTransformationTermination.*;

public class Part21CreationTransformationTerminationTest {

    @Test
    public void mergeSeveralSourcesTest() {

        StepVerifier
                .withVirtualTime(() -> mergeSeveralSources(
                        Flux.just("A").delaySubscription(Duration.ofSeconds(1)),
                        Flux.just("B")
                ))
                .expectSubscription()
                .expectNext("B")
                .expectNoEvent(Duration.ofSeconds(1))
                .expectNext("A")
                .verifyComplete();
    }


    @Test
    public void mergeSeveralSourcesOrderedTest() {
        PublisherProbe[] probes = new PublisherProbe[2];

        StepVerifier
                .withVirtualTime(() -> {
                    PublisherProbe<String> probeA = PublisherProbe.of(Mono.fromCallable(() -> "A").delaySubscription(Duration.ofSeconds(1)));
                    PublisherProbe<String> probeB = PublisherProbe.of(Mono.fromCallable(() -> "B"));

                    probes[0] = probeA;
                    probes[1] = probeB;

                    return mergeSeveralSourcesOrdered(
                            probeA.mono(),
                            probeB.mono()
                    );
                }, 0)
                .expectSubscription()
                .then(() -> probes[0].assertWasSubscribed())
                .then(() -> probes[1].assertWasSubscribed())
                .thenRequest(2)
                .expectNoEvent(Duration.ofSeconds(1))
                .expectNext("A")
                .expectNext("B")
                .verifyComplete();
    }

    @Test
    public void concatSeveralSourcesOrderedTest() {
        PublisherProbe[] probes = new PublisherProbe[2];

        StepVerifier
                .withVirtualTime(() -> {
                    PublisherProbe<String> probeA = PublisherProbe.of(Mono.fromCallable(() -> "A").delaySubscription(Duration.ofSeconds(1)));
                    PublisherProbe<String> probeB = PublisherProbe.of(Mono.fromCallable(() -> "B"));

                    probes[0] = probeA;
                    probes[1] = probeB;

                    return concatSeveralSourcesOrdered(
                            probeA.mono(),
                            probeB.mono()
                    );
                }, 0)
                .expectSubscription()
                .then(() -> probes[0].assertWasSubscribed())
                .then(() -> probes[1].assertWasNotSubscribed())
                .thenRequest(1)
                .then(() -> probes[0].assertWasRequested())
                .then(() -> probes[1].assertWasNotSubscribed())
                .expectNoEvent(Duration.ofSeconds(1))
                .expectNext("A")
                .thenRequest(1)
                .then(() -> probes[1].assertWasSubscribed())
                .then(() -> probes[1].assertWasRequested())
                .expectNext("B")
                .verifyComplete();
    }

    @Test
    public void zipSourcesTest() {
        StepVerifier
                .withVirtualTime(() -> Flux.from(zipSeveralSources(
                        Flux.interval(Duration.ofMillis(10)).map(Object::toString),
                        Flux.interval(Duration.ofMillis(5)).map(Object::toString),
                        Flux.interval(Duration.ofMillis(1)).map(Object::toString)
                )).take(3))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(10))
                .expectNext("000")
                .expectNoEvent(Duration.ofMillis(10))
                .expectNext("111")
                .expectNoEvent(Duration.ofMillis(10))
                .expectNext("222")
                .verifyComplete();
    }

    @Test
    public void combineLatestSourcesTest() {
        StepVerifier
                .withVirtualTime(() -> Flux.from(combineSeveralSources(
                        Flux.interval(Duration.ofMillis(10)).map(Object::toString),
                        Flux.interval(Duration.ofMillis(5)).map(Object::toString),
                        Flux.interval(Duration.ofMillis(1)).map(Object::toString)
                )).take(16))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(10))
                .expectNext("008", "018", "019")
                .expectNoEvent(Duration.ofMillis(1))
                .expectNext("0110")
                .thenAwait(Duration.ofMillis(4))
                .expectNext("0111", "0112", "0113", "0213", "0214")
                .thenAwait(Duration.ofMillis(5))
                .expectNext("0215", "0216", "0217", "0218", "1218", "1318", "1319")
                .verifyComplete();
    }

    @Test
    public void firstEmittedRaceTest() {
        StepVerifier
                .withVirtualTime(() -> fromFirstEmitted(
                        Flux.just("a").delaySubscription(Duration.ofSeconds(1)),
                        Flux.just("b", "c").delaySubscription(Duration.ofMillis(100)),
                        Flux.just("D", "Z").delaySubscription(Duration.ofMillis(10))
                ))
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(10))
                .expectNext("D", "Z")
                .verifyComplete();
    }

    @Test
    public void groupByWordsByFirstLatterTest() {
        StepVerifier
                .create(
                        Flux
                                .from(groupWordsByFirstLatter(Flux.just("ABC", "BCD", "CDE", "BEF", "ADE", "CFG")))
                                .flatMap(gf -> gf.collectList().map(l -> Tuples.of(gf.key(), l)))
                )
                .expectSubscription()
                .expectNext(Tuples.of('A', Arrays.asList("ABC", "ADE")))
                .expectNext(Tuples.of('B', Arrays.asList("BCD", "BEF")))
                .expectNext(Tuples.of('C', Arrays.asList("CDE", "CFG")))
                .verifyComplete();
    }

    @Test
    public void switchBetweenSourcesTest() {
        StepVerifier
                .withVirtualTime(() -> switchBetweenSources(
                        Flux.just(Source.A, Source.A, Source.B, Source.A, Source.B, Source.B).delayElements(Duration.ofSeconds(1)),
                        Flux.interval(Duration.ofMillis(500)).map(i -> "A" + i).onBackpressureDrop().publish(1).autoConnect(0),
                        Flux.interval(Duration.ofMillis(200)).map(i -> "B" + i).onBackpressureDrop().publish(1).autoConnect(0)
                ))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(1))
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("A1", "A2")
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("A3", "A4")
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("B14", "B15", "B16", "B17", "B18")
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("A7", "A8")
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("B24", "B25", "B26", "B27", "B28")
                .thenAwait(Duration.ofSeconds(1))
                .expectNext("B29", "B30", "B31", "B32", "B33")
                // Actually the last source is infinitive stream
                // and because of switchMap nature we required to cancel the upstream
                // instead of merely awaiting the completion signal from upstream
                .thenCancel()
                .verify();
    }

    @Test
    @Ignore
    public void collectToListTest() {
        StepVerifier
                .create(collectAllItemsToList(Flux.just("A", "B", "C")))
                .expectSubscription()
                .expectNext(Arrays.asList("A", "B", "C"))
                .verifyComplete();
    }

    @Test
    public void executeLazyTerminationOperationAndSendHelloTest() {
        StepVerifier
                .withVirtualTime(() -> executeLazyTerminationOperationAndSendHello(
                        Flux.just("A").delaySubscription(Duration.ofSeconds(1))
                ))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(1))
                .expectNext("Hello")
                .verifyComplete();
    }

    @Test
    public void firstElementFromSourceTest() {
        String element = firstElementFromSource(Flux.just("Hello", "World"));
        Assert.assertEquals("Expected 'Hello' but was [" + element + "]", "Hello", element);
    }

    @Test
    public void lastElementFromSourceTest() {
        String element = lastElementFromSource(Flux.just("Hello", "World"));
        Assert.assertEquals("Expected 'World' but was [" + element + "]", "World", element);
    }
}
